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

class AuctionStatusResolverTest {

    private AuctionStatusResolver auctionStatusResolver;

    @BeforeEach
    void setUp() {
        auctionStatusResolver = new AuctionStatusResolver();
    }

    @Test
    void resolveEffectiveStatusShouldKeepActiveAuctionActiveBeforeEndTime() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T09:00:00Z"));

        AuctionStatus result = auctionStatusResolver.resolveEffectiveStatus(auction);

        assertEquals(AuctionStatus.ACTIVE, result);
    }

    @Test
    void resolveEffectiveStatusShouldPreserveStoredStatusForExpiredActiveAuctions() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T07:00:00Z"));

        AuctionStatus result = auctionStatusResolver.resolveEffectiveStatus(auction);

        assertEquals(AuctionStatus.ACTIVE, result);
    }

    @Test
    void resolveEffectiveStatusShouldNotOverwriteFinalStatuses() {
        Auction auction = createAuction(AuctionStatus.WON, Instant.parse("2026-05-22T07:00:00Z"));

        AuctionStatus result = auctionStatusResolver.resolveEffectiveStatus(auction);

        assertEquals(AuctionStatus.WON, result);
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
    void isReserveMetShouldReturnFalseWhenNoLeadingBidExists() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T09:00:00Z"));

        assertFalse(auctionStatusResolver.isReserveMet(auction, null));
    }

    @Test
    void isReserveMetShouldReturnTrueWhenLeadingBidMeetsReservePrice() {
        Auction auction = createAuction(AuctionStatus.ACTIVE, Instant.parse("2026-05-22T09:00:00Z"));
        Bid leadingBid = new Bid();
        leadingBid.setAmount(new BigDecimal("120.00"));

        assertTrue(auctionStatusResolver.isReserveMet(auction, leadingBid));
    }

    @Test
    void isBiddableStatusShouldOnlyAllowActiveAndExtendedStatuses() {
        assertTrue(auctionStatusResolver.isBiddableStatus(AuctionStatus.ACTIVE));
        assertTrue(auctionStatusResolver.isBiddableStatus(AuctionStatus.EXTENDED));
        assertFalse(auctionStatusResolver.isBiddableStatus(AuctionStatus.WON));
    }

    private Auction createAuction(AuctionStatus status, Instant endsAt) {
        Auction auction = new Auction();
        auction.setStatus(status);
        auction.setEndsAt(endsAt);
        auction.setReservePrice(new BigDecimal("100.00"));
        return auction;
    }
}
