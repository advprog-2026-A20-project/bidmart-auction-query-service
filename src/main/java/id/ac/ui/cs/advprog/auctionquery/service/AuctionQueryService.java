package id.ac.ui.cs.advprog.auctionquery.service;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.AuctionSummaryResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.mapper.AuctionResponseMapper;
import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import id.ac.ui.cs.advprog.auctionquery.repository.AuctionRepository;
import id.ac.ui.cs.advprog.auctionquery.repository.BidRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AuctionQueryService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final BidCalculator bidCalculator;
    private final AuctionStatusResolver auctionStatusResolver;
    private final AuctionResponseMapper auctionResponseMapper;

    public AuctionQueryService(
        AuctionRepository auctionRepository,
        BidRepository bidRepository,
        BidCalculator bidCalculator,
        AuctionStatusResolver auctionStatusResolver,
        AuctionResponseMapper auctionResponseMapper
    ) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.bidCalculator = bidCalculator;
        this.auctionStatusResolver = auctionStatusResolver;
        this.auctionResponseMapper = auctionResponseMapper;
    }

    public List<AuctionSummaryResponse> listAuctions(AuctionStatus status, Pageable pageable) {
        return loadAuctions(status, pageable).stream()
            .map(this::buildSummaryResponse)
            .toList();
    }

    public AuctionDetailResponse getAuctionDetail(UUID auctionId) {
        AuctionBidContext context = loadAuctionBidContext(auctionId);

        return auctionResponseMapper.toDetailResponse(
            context.auction(),
            context.effectiveStatus(),
            context.effectiveClosedAt(),
            context.bids(),
            context.leadingBid(),
            bidCalculator.calculateNextMinimumBid(context.auction(), context.leadingBid()),
            auctionStatusResolver.isReserveMet(context.auction(), context.leadingBid()),
            auctionStatusResolver.isBiddableStatus(context.effectiveStatus())
        );
    }

    public List<BidResponse> getBidHistory(UUID auctionId) {
        AuctionBidContext context = loadAuctionBidContext(auctionId);
        return auctionResponseMapper.toBidResponses(
            context.effectiveStatus(),
            context.bids(),
            context.leadingBid()
        );
    }

    private List<Auction> loadAuctions(AuctionStatus status, Pageable pageable) {
        if (status == null) {
            return auctionRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
        }
        return auctionRepository.findByStatusOrderByCreatedAtDesc(status, pageable).getContent();
    }

    private AuctionSummaryResponse buildSummaryResponse(Auction auction) {
        Bid leadingBid = bidRepository.findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(auction.getId())
            .orElse(null);
        long totalBids = bidRepository.countByAuctionId(auction.getId());
        AuctionStatus effectiveStatus = auctionStatusResolver.resolveEffectiveStatus(auction);

        return auctionResponseMapper.toSummaryResponse(
            auction,
            effectiveStatus,
            leadingBid,
            totalBids,
            bidCalculator.calculateNextMinimumBid(auction, leadingBid)
        );
    }

    private AuctionBidContext loadAuctionBidContext(UUID auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        List<Bid> bids = bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId());
        Bid leadingBid = bidCalculator.selectLeadingBid(bids);
        AuctionStatus effectiveStatus = auctionStatusResolver.resolveEffectiveStatus(auction);
        Instant effectiveClosedAt = auctionStatusResolver.resolveEffectiveClosedAt(auction, effectiveStatus);

        return new AuctionBidContext(auction, bids, leadingBid, effectiveStatus, effectiveClosedAt);
    }

    private Auction findAuctionOrThrow(UUID auctionId) {
        return auctionRepository.findByIdWithListingAndSeller(auctionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
    }

    private record AuctionBidContext(
        Auction auction,
        List<Bid> bids,
        Bid leadingBid,
        AuctionStatus effectiveStatus,
        Instant effectiveClosedAt
    ) {
    }
}
