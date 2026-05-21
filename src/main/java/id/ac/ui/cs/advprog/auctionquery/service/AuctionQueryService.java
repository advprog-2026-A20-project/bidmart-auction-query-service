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

    private static final Comparator<Bid> LEADING_BID_COMPARATOR = Comparator
        .comparing(Bid::getAmount)
        .thenComparing(Bid::getSequenceNumber, Comparator.reverseOrder());

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AuctionQueryService(
        AuctionRepository auctionRepository,
        BidRepository bidRepository
    ) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    @Transactional(readOnly = true)
    public List<AuctionSummaryResponse> listAuctions(AuctionStatus status) {
        return auctionRepository.findAllWithListingAndSellerOrderByCreatedAtDesc().stream()
            .filter(auction -> matchesStatusFilter(auction, status))
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
        List<Bid> bids = bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId());
        Bid leadingBid = selectLeadingBid(bids);
        return bids.stream()
            .map(bid -> toBidResponse(auction.getStatus(), bid, leadingBid))
            .toList();
    }

    private AuctionSummaryResponse toSummaryResponse(Auction auction) {
        Bid leadingBid = bidRepository.findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(auction.getId())
            .orElse(null);
        long totalBids = bidRepository.countByAuctionId(auction.getId());
        BigDecimal currentPrice = leadingBid == null ? auction.getListing().getPrice() : leadingBid.getAmount();
        return new AuctionSummaryResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getListing().getSeller().getId(),
            auction.getListing().getSeller().getEmail(),
            currentPrice,
            auction.getStartingPrice(),
            auction.getMinimumBidIncrement(),
            auction.getStatus(),
            auction.getCreatedAt(),
            auction.getStartsAt(),
            auction.getEndsAt(),
            auction.getExtensionCount(),
            totalBids,
            calculateNextMinimumBid(auction, leadingBid)
        );
    }

    private AuctionDetailResponse toDetailResponse(Auction auction) {
        List<Bid> bids = bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId());
        Bid leadingBid = selectLeadingBid(bids);
        boolean reserveMet = leadingBid != null && leadingBid.getAmount().compareTo(auction.getReservePrice()) >= 0;
        Bid winningBid = auction.getStatus() == AuctionStatus.WON ? leadingBid : null;
        BigDecimal currentPrice = leadingBid == null ? auction.getListing().getPrice() : leadingBid.getAmount();
        return new AuctionDetailResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getListing().getSeller().getId(),
            auction.getListing().getSeller().getEmail(),
            currentPrice,
            auction.getStartingPrice(),
            auction.getReservePrice(),
            auction.getMinimumBidIncrement(),
            auction.getStatus(),
            auction.getCreatedAt(),
            auction.getStartsAt(),
            auction.getEndsAt(),
            auction.getClosedAt(),
            auction.getDurationMinutes(),
            auction.getExtensionCount(),
            bids.size(),
            calculateNextMinimumBid(auction, leadingBid),
            reserveMet,
            isBiddableStatus(auction.getStatus()),
            leadingBid == null ? null : toBidResponse(auction.getStatus(), leadingBid, leadingBid),
            winningBid == null ? null : toBidResponse(auction.getStatus(), winningBid, leadingBid),
            bids.stream()
                .map(bid -> toBidResponse(auction.getStatus(), bid, leadingBid))
                .toList()
        );
    }

    private boolean matchesStatusFilter(Auction auction, AuctionStatus requestedStatus) {
        if (requestedStatus == null) {
            return true;
        }
        return auction.getStatus() == requestedStatus;
    }

    private boolean isBiddableStatus(AuctionStatus status) {
        return status == AuctionStatus.ACTIVE || status == AuctionStatus.EXTENDED;
    }

    private BigDecimal calculateNextMinimumBid(Auction auction, Bid leadingBid) {
        if (leadingBid == null) {
            return auction.getStartingPrice();
        }
        return leadingBid.getAmount().add(auction.getMinimumBidIncrement());
    }

    private BidResponse toBidResponse(AuctionStatus effectiveStatus, Bid bid, Bid leadingBid) {
        boolean isWinningBid = leadingBid != null
            && Objects.equals(leadingBid.getId(), bid.getId())
            && effectiveStatus == AuctionStatus.WON;
        return new BidResponse(
            bid.getId(),
            bid.getBidder().getId(),
            bid.getBidder().getEmail(),
            bid.getAmount(),
            bid.getSequenceNumber(),
            bid.getSubmittedAt(),
            isWinningBid
        );
    }

    private Bid selectLeadingBid(List<Bid> bids) {
        return bids.stream().max(LEADING_BID_COMPARATOR).orElse(null);
    }
}
