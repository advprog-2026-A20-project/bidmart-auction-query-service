package id.ac.ui.cs.advprog.auctionquery.service;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BidCalculator {

    private static final Comparator<Bid> LEADING_BID_COMPARATOR = Comparator
        .comparing(Bid::getAmount)
        .thenComparing(Bid::getSequenceNumber, Comparator.reverseOrder());

    public Bid selectLeadingBid(List<Bid> bids) {
        return bids.stream().max(LEADING_BID_COMPARATOR).orElse(null);
    }

    public BigDecimal calculateNextMinimumBid(Auction auction, Bid leadingBid) {
        if (leadingBid == null) {
            return auction.getStartingPrice();
        }
        return leadingBid.getAmount().add(auction.getMinimumBidIncrement());
    }
}
