package id.ac.ui.cs.advprog.auctionquery.service;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.AuctionSummaryResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import id.ac.ui.cs.advprog.auctionquery.repository.AuctionRepository;
import id.ac.ui.cs.advprog.auctionquery.repository.BidRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuctionQueryService {

    private static final BigDecimal COMMAND_SIDE_MINIMUM_BID_STEP = new BigDecimal("0.01");

    private static final Comparator<Bid> LEADING_BID_COMPARATOR = Comparator
        .comparing(Bid::getAmount)
        .thenComparing(Bid::getCreatedAt, Comparator.reverseOrder());

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final Clock clock;

    public AuctionQueryService(
        AuctionRepository auctionRepository,
        BidRepository bidRepository,
        Clock clock
    ) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AuctionSummaryResponse> listAuctions() {
        return auctionRepository.findAllWithListingAndSellerOrderByCreatedAtDesc().stream()
            .map(this::toSummaryResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public AuctionDetailResponse getAuctionDetail(UUID auctionId) {
        Auction auction = auctionRepository.findByIdWithListingAndSeller(auctionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        return toDetailResponse(auction);
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getBidHistory(UUID auctionId) {
        Auction auction = auctionRepository.findByIdWithListingAndSeller(auctionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        List<Bid> bids = bidRepository.findByAuctionIdOrderByCreatedAtAsc(auction.getId());
        Bid leadingBid = selectLeadingBid(bids);
        AuctionStatus effectiveStatus = resolveEffectiveStatus(auction, leadingBid);
        return bids.stream()
            .map(bid -> toBidResponse(effectiveStatus, bid, leadingBid, calculateBidSequence(bids, bid)))
            .toList();
    }

    private AuctionSummaryResponse toSummaryResponse(Auction auction) {
        Bid leadingBid = bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAsc(auction.getId())
            .orElse(null);
        long totalBids = bidRepository.countByAuctionId(auction.getId());
        AuctionStatus effectiveStatus = resolveEffectiveStatus(auction, leadingBid);
        BigDecimal currentPrice = resolveCurrentPrice(auction, leadingBid);
        return new AuctionSummaryResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getSellerId(),
            auction.getListing().getSeller().getEmail(),
            currentPrice,
            auction.getStartingPrice(),
            COMMAND_SIDE_MINIMUM_BID_STEP,
            effectiveStatus,
            auction.getCreatedAt(),
            auction.getCreatedAt(),
            auction.getEndsAt(),
            0,
            totalBids,
            calculateNextMinimumBid(auction, leadingBid)
        );
    }

    private AuctionDetailResponse toDetailResponse(Auction auction) {
        List<Bid> bids = bidRepository.findByAuctionIdOrderByCreatedAtAsc(auction.getId());
        Bid leadingBid = selectLeadingBid(bids);
        AuctionStatus effectiveStatus = resolveEffectiveStatus(auction, leadingBid);
        BigDecimal reservePrice = auction.getStartingPrice();
        boolean reserveMet = leadingBid != null && leadingBid.getAmount().compareTo(reservePrice) >= 0;
        Bid winningBid = effectiveStatus == AuctionStatus.WON ? leadingBid : null;
        BigDecimal currentPrice = resolveCurrentPrice(auction, leadingBid);
        return new AuctionDetailResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getSellerId(),
            auction.getListing().getSeller().getEmail(),
            currentPrice,
            auction.getStartingPrice(),
            reservePrice,
            COMMAND_SIDE_MINIMUM_BID_STEP,
            effectiveStatus,
            auction.getCreatedAt(),
            auction.getCreatedAt(),
            auction.getEndsAt(),
            resolveEffectiveClosedAt(auction, effectiveStatus),
            null,
            0,
            bids.size(),
            calculateNextMinimumBid(auction, leadingBid),
            reserveMet,
            isBiddableStatus(effectiveStatus),
            leadingBid == null ? null : toBidResponse(
                effectiveStatus,
                leadingBid,
                leadingBid,
                calculateBidSequence(bids, leadingBid)
            ),
            winningBid == null ? null : toBidResponse(
                effectiveStatus,
                winningBid,
                leadingBid,
                calculateBidSequence(bids, winningBid)
            ),
            bids.stream()
                .map(bid -> toBidResponse(effectiveStatus, bid, leadingBid, calculateBidSequence(bids, bid)))
                .toList()
        );
    }

    private AuctionStatus resolveEffectiveStatus(Auction auction, Bid leadingBid) {
        if (!shouldDeriveResolvedStatus(auction)) {
            return auction.getStatus();
        }
        return isReserveMet(auction, leadingBid) ? AuctionStatus.WON : AuctionStatus.UNSOLD;
    }

    private Instant resolveEffectiveClosedAt(Auction auction, AuctionStatus effectiveStatus) {
        if (effectiveStatus == auction.getStatus()) {
            return isResolvedStatus(auction.getStatus()) ? auction.getUpdatedAt() : null;
        }
        return auction.getEndsAt();
    }

    private boolean shouldDeriveResolvedStatus(Auction auction) {
        return isBiddableStatus(auction.getStatus())
            && auction.getEndsAt() != null
            && !auction.getEndsAt().isAfter(Instant.now(clock));
    }

    private boolean isReserveMet(Auction auction, Bid leadingBid) {
        return leadingBid != null && leadingBid.getAmount().compareTo(auction.getStartingPrice()) >= 0;
    }

    private boolean isBiddableStatus(AuctionStatus status) {
        return status == AuctionStatus.ACTIVE || status == AuctionStatus.EXTENDED;
    }

    private boolean isResolvedStatus(AuctionStatus status) {
        return status == AuctionStatus.CLOSED || status == AuctionStatus.WON || status == AuctionStatus.UNSOLD;
    }

    private BigDecimal calculateNextMinimumBid(Auction auction, Bid leadingBid) {
        if (leadingBid == null) {
            return auction.getStartingPrice();
        }
        return leadingBid.getAmount().add(COMMAND_SIDE_MINIMUM_BID_STEP);
    }

    private BigDecimal resolveCurrentPrice(Auction auction, Bid leadingBid) {
        if (auction.getCurrentHighestBid() != null) {
            return auction.getCurrentHighestBid();
        }
        return leadingBid == null ? auction.getStartingPrice() : leadingBid.getAmount();
    }

    private Long calculateBidSequence(List<Bid> bids, Bid bid) {
        int index = bids.indexOf(bid);
        return index < 0 ? null : index + 1L;
    }

    private BidResponse toBidResponse(AuctionStatus effectiveStatus, Bid bid, Bid leadingBid, Long sequenceNumber) {
        boolean isWinningBid = leadingBid != null
            && Objects.equals(leadingBid.getId(), bid.getId())
            && effectiveStatus != AuctionStatus.UNSOLD;
        return new BidResponse(
            bid.getId(),
            bid.getBidder().getId(),
            bid.getBidder().getEmail(),
            bid.getAmount(),
            sequenceNumber,
            bid.getCreatedAt(),
            isWinningBid
        );
    }

    private Bid selectLeadingBid(List<Bid> bids) {
        return bids.stream().max(LEADING_BID_COMPARATOR).orElse(null);
    }
}
