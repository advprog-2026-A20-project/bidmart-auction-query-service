package id.ac.ui.cs.advprog.auctionquery.service;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AuctionStatusResolver {

    public AuctionStatus resolveEffectiveStatus(Auction auction) {
        return auction.getStatus();
    }

    public Instant resolveEffectiveClosedAt(Auction auction, AuctionStatus effectiveStatus) {
        return auction.getClosedAt();
    }

    public boolean isReserveMet(Auction auction, Bid leadingBid) {
        return leadingBid != null && leadingBid.getAmount().compareTo(auction.getReservePrice()) >= 0;
    }

    public boolean isBiddableStatus(AuctionStatus status) {
        return status == AuctionStatus.ACTIVE || status == AuctionStatus.EXTENDED;
    }
}
