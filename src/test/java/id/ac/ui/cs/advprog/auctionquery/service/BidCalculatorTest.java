package id.ac.ui.cs.advprog.auctionquery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import id.ac.ui.cs.advprog.auctionquery.model.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BidCalculatorTest {

    private BidCalculator bidCalculator;

    @BeforeEach
    void setUp() {
        bidCalculator = new BidCalculator();
    }

    @Test
    void selectLeadingBidShouldReturnNullWhenNoBidsExist() {
        assertNull(bidCalculator.selectLeadingBid(List.of()));
    }

    @Test
    void selectLeadingBidShouldReturnHighestAmount() {
        Bid lowerBid = createBid("80.00", 1L);
        Bid higherBid = createBid("120.00", 2L);

        Bid result = bidCalculator.selectLeadingBid(List.of(lowerBid, higherBid));

        assertEquals(higherBid.getId(), result.getId());
    }

    @Test
    void selectLeadingBidShouldPreferLowerSequenceNumberWhenAmountsTie() {
        Bid earlierBid = createBid("120.00", 1L);
        Bid laterBid = createBid("120.00", 2L);

        Bid result = bidCalculator.selectLeadingBid(List.of(earlierBid, laterBid));

        assertEquals(earlierBid.getId(), result.getId());
    }

    @Test
    void selectLeadingBidShouldHandleSingleBid() {
        Bid bid = createBid("90.00", 1L);

        Bid result = bidCalculator.selectLeadingBid(List.of(bid));

        assertEquals(bid.getId(), result.getId());
    }

    @Test
    void selectLeadingBidShouldPreferHighestAmountEvenIfSequenceNumberIsHigher() {
        Bid earlierLowerBid = createBid("100.00", 1L);
        Bid laterHigherBid = createBid("110.00", 2L);

        Bid result = bidCalculator.selectLeadingBid(List.of(earlierLowerBid, laterHigherBid));

        assertEquals(laterHigherBid.getId(), result.getId());
    }

    @Test
    void calculateNextMinimumBidShouldUseStartingPriceWhenNoLeadingBidExists() {
        Auction auction = createAuction();

        BigDecimal result = bidCalculator.calculateNextMinimumBid(auction, null);

        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    void calculateNextMinimumBidShouldUseLeadingAmountPlusMinimumIncrement() {
        Auction auction = createAuction();
        Bid leadingBid = createBid("120.00", 1L);

        BigDecimal result = bidCalculator.calculateNextMinimumBid(auction, leadingBid);

        assertEquals(new BigDecimal("130.00"), result);
    }

    @Test
    void calculateNextMinimumBidShouldPreserveBigDecimalScaleIfApplicable() {
        Auction auction = new Auction();
        auction.setStartingPrice(new BigDecimal("50.00"));
        auction.setMinimumBidIncrement(new BigDecimal("0.05"));
        Bid leadingBid = createBid("100.10", 1L);

        BigDecimal result = bidCalculator.calculateNextMinimumBid(auction, leadingBid);

        assertEquals(new BigDecimal("100.15"), result);
    }

    private Auction createAuction() {
        Auction auction = new Auction();
        auction.setStartingPrice(new BigDecimal("50.00"));
        auction.setMinimumBidIncrement(new BigDecimal("10.00"));
        return auction;
    }

    private Bid createBid(String amount, long sequenceNumber) {
        User bidder = new User();
        bidder.setId(UUID.randomUUID());
        bidder.setEmail("buyer@example.com");

        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setBidder(bidder);
        bid.setAmount(new BigDecimal(amount));
        bid.setSequenceNumber(sequenceNumber);
        return bid;
    }
}
