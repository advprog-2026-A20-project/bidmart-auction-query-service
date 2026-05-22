package id.ac.ui.cs.advprog.auctionquery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AuctionStatusResolverTest {

    private AuctionStatusResolver auctionStatusResolver;

    @BeforeEach
    void setUp() {
        auctionStatusResolver = new AuctionStatusResolver();
    }

    @Test
    void resolveEffectiveStatusShouldPreserveActiveStatus() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T09:00:00Z"));

        AuctionStatus result = auctionStatusResolver.resolveEffectiveStatus(auction);

        assertEquals(AuctionStatus.ACTIVE, result);
    }

    @ParameterizedTest
    @EnumSource(AuctionStatus.class)
    void resolveEffectiveStatusShouldPreserveEveryAuctionStatus(AuctionStatus status) {
        Auction auction = createAuction(status, Instant.parse("2026-05-22T09:00:00Z"));

        AuctionStatus result = auctionStatusResolver.resolveEffectiveStatus(auction);

        assertEquals(status, result);
    }

    @Test
    void resolveEffectiveClosedAtShouldReturnStoredClosedTime() {
        Auction auction = createAuction(AuctionStatus.CLOSED, null);
        auction.setClosedAt(Instant.parse("2026-05-22T09:05:00Z"));

        Instant result = auctionStatusResolver.resolveEffectiveClosedAt(auction, AuctionStatus.CLOSED);

        assertEquals(auction.getClosedAt(), result);
    }

    @Test
    void resolveEffectiveClosedAtShouldAllowNullEndTimeWithoutCrashing() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, null);

        Instant result = auctionStatusResolver.resolveEffectiveClosedAt(auction, AuctionStatus.ACTIVE);

        assertNull(result);
    }

    @Test
    void resolveEffectiveClosedAtShouldReturnNullWhenClosedAtNull() {
        Auction auction = createAuction(AuctionStatus.CLOSED, Instant.parse("2026-05-22T09:00:00Z"));
        auction.setClosedAt(null);

        Instant result = auctionStatusResolver.resolveEffectiveClosedAt(auction, AuctionStatus.CLOSED);

        assertNull(result);
    }

    @Test
    void isReserveMetShouldReturnFalseWhenNoLeadingBidExists() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T09:00:00Z"));

        assertFalse(auctionStatusResolver.isReserveMet(auction, null));
    }

    @Test
    void isReserveMetShouldReturnFalseWhenBidIsBelowReserve() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T09:00:00Z"));
        Bid leadingBid = new Bid();
        leadingBid.setAmount(new BigDecimal("99.99"));

        assertFalse(auctionStatusResolver.isReserveMet(auction, leadingBid));
    }

    @Test
    void isReserveMetShouldReturnTrueWhenBidEqualsReserve() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T09:00:00Z"));
        Bid leadingBid = new Bid();
        leadingBid.setAmount(new BigDecimal("100.00"));

        assertTrue(auctionStatusResolver.isReserveMet(auction, leadingBid));
    }

    @Test
    void isReserveMetShouldReturnTrueWhenLeadingBidExceedsReservePrice() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T09:00:00Z"));
        Bid leadingBid = new Bid();
        leadingBid.setAmount(new BigDecimal("120.00"));

        assertTrue(auctionStatusResolver.isReserveMet(auction, leadingBid));
    }

    @Test
    void isBiddableStatusShouldOnlyAllowActiveAndExtendedStatuses() {
        assertTrue(auctionStatusResolver.isBiddableStatus(AuctionStatus.ACTIVE));
        assertTrue(auctionStatusResolver.isBiddableStatus(AuctionStatus.EXTENDED));
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"DRAFT", "CLOSED", "WON", "UNSOLD", "CANCELLED"})
    void isBiddableStatusShouldReturnFalseForNonBiddableStatuses(AuctionStatus status) {
        assertFalse(auctionStatusResolver.isBiddableStatus(status));
    }

    private Auction createAuction(AuctionStatus status, Instant endsAt) {
        Auction auction = new Auction();
        auction.setStatus(status);
        auction.setEndsAt(endsAt);
        auction.setReservePrice(new BigDecimal("100.00"));
        return auction;
    }
}
