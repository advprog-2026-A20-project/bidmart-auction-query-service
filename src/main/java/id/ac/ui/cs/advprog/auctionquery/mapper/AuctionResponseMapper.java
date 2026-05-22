package id.ac.ui.cs.advprog.auctionquery.mapper;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.AuctionSummaryResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AuctionResponseMapper {

    public AuctionSummaryResponse toSummaryResponse(
        Auction auction,
        AuctionStatus effectiveStatus,
        Bid leadingBid,
        long totalBids,
        BigDecimal nextMinimumBid
    ) {
        return new AuctionSummaryResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getListing().getSeller().getId(),
            auction.getListing().getSeller().getEmail(),
            resolveCurrentPrice(auction, leadingBid),
            auction.getStartingPrice(),
            auction.getMinimumBidIncrement(),
            effectiveStatus,
            auction.getCreatedAt(),
            auction.getStartsAt(),
            auction.getEndsAt(),
            auction.getExtensionCount(),
            totalBids,
            nextMinimumBid
        );
    }

    public AuctionDetailResponse toDetailResponse(
        Auction auction,
        AuctionStatus effectiveStatus,
        Instant effectiveClosedAt,
        List<Bid> bids,
        Bid leadingBid,
        BigDecimal nextMinimumBid,
        boolean reserveMet,
        boolean biddable
    ) {
        List<BidResponse> bidHistory = toBidResponses(effectiveStatus, bids, leadingBid);
        BidResponse leadingBidResponse = leadingBid == null ? null : toBidResponse(effectiveStatus, leadingBid, leadingBid);
        BidResponse winningBid = effectiveStatus == AuctionStatus.WON ? leadingBidResponse : null;

        return new AuctionDetailResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getListing().getSeller().getId(),
            auction.getListing().getSeller().getEmail(),
            resolveCurrentPrice(auction, leadingBid),
            auction.getStartingPrice(),
            auction.getReservePrice(),
            auction.getMinimumBidIncrement(),
            effectiveStatus,
            auction.getCreatedAt(),
            auction.getStartsAt(),
            auction.getEndsAt(),
            effectiveClosedAt,
            auction.getDurationMinutes(),
            auction.getExtensionCount(),
            bids.size(),
            nextMinimumBid,
            reserveMet,
            biddable,
            leadingBidResponse,
            winningBid,
            bidHistory
        );
    }

    public List<BidResponse> toBidResponses(AuctionStatus effectiveStatus, List<Bid> bids, Bid leadingBid) {
        return bids.stream()
            .map(bid -> toBidResponse(effectiveStatus, bid, leadingBid))
            .toList();
    }

    public BidResponse toBidResponse(AuctionStatus effectiveStatus, Bid bid, Bid leadingBid) {
        boolean winningBid = leadingBid != null
            && Objects.equals(leadingBid.getId(), bid.getId())
            && effectiveStatus == AuctionStatus.WON;

        return new BidResponse(
            bid.getId(),
            bid.getBidder().getId(),
            maskEmail(bid.getBidder().getEmail()),
            bid.getAmount(),
            bid.getSequenceNumber(),
            bid.getSubmittedAt(),
            winningBid
        );
    }

    private BigDecimal resolveCurrentPrice(Auction auction, Bid leadingBid) {
        if (leadingBid == null) {
            return auction.getListing().getPrice();
        }
        return leadingBid.getAmount();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        int separatorIndex = email.indexOf('@');
        if (separatorIndex <= 0 || separatorIndex == email.length() - 1) {
            return "***";
        }

        String localPart = email.substring(0, separatorIndex);
        String domain = email.substring(separatorIndex);

        if (localPart.length() == 1) {
            return "*" + domain;
        }

        if (localPart.length() == 2) {
            return localPart.charAt(0) + "*" + domain;
        }

        return localPart.charAt(0)
            + "*".repeat(localPart.length() - 2)
            + localPart.charAt(localPart.length() - 1)
            + domain;
    }
}
