package id.ac.ui.cs.advprog.auctionquery.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.AuctionSummaryResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import id.ac.ui.cs.advprog.auctionquery.model.Listing;
import id.ac.ui.cs.advprog.auctionquery.model.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuctionResponseMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-22T08:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-05-22T09:00:00Z");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-05-22T08:30:00Z");

    private AuctionResponseMapper auctionResponseMapper;

    @BeforeEach
    void setUp() {
        auctionResponseMapper = new AuctionResponseMapper();
    }

    @Test
    void toSummaryResponseShouldMapAuctionSummaryFields() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Bid leadingBid = createBid(auction, "120.00", 1L, "bidder@example.com");

        AuctionSummaryResponse response = auctionResponseMapper.toSummaryResponse(
            auction,
            AuctionStatus.ACTIVE,
            leadingBid,
            2L,
            new BigDecimal("130.00")
        );

        assertEquals(auction.getId(), response.id());
        assertEquals(auction.getListing().getSeller().getEmail(), response.sellerEmail());
        assertEquals(new BigDecimal("120.00"), response.currentPrice());
        assertEquals(new BigDecimal("130.00"), response.nextMinimumBid());
    }

    @Test
    void toDetailResponseShouldMapLeadingBidAndMaskBidderEmail() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Bid leadingBid = createBid(auction, "120.00", 1L, "bidder@example.com");

        AuctionDetailResponse response = auctionResponseMapper.toDetailResponse(
            auction,
            AuctionStatus.ACTIVE,
            null,
            List.of(leadingBid),
            leadingBid,
            new BigDecimal("130.00"),
            true,
            true
        );

        assertNotNull(response.leadingBid());
        assertEquals("b****r@example.com", response.leadingBid().bidderEmail());
        assertNull(response.winningBid());
        assertTrue(response.reserveMet());
        assertTrue(response.biddable());
    }

    @Test
    void toDetailResponseShouldSetWinningBidWhenAuctionIsWon() {
        Auction auction = createAuction(AuctionStatus.WON);
        Bid winningBid = createBid(auction, "140.00", 1L, "winner@example.com");

        AuctionDetailResponse response = auctionResponseMapper.toDetailResponse(
            auction,
            AuctionStatus.WON,
            Instant.parse("2026-05-22T09:05:00Z"),
            List.of(winningBid),
            winningBid,
            new BigDecimal("150.00"),
            true,
            false
        );

        assertNotNull(response.winningBid());
        assertEquals(winningBid.getId(), response.winningBid().id());
        assertTrue(response.winningBid().winning());
        assertFalse(response.biddable());
    }

    @Test
    void toBidResponseShouldMarkOnlyLeadingBidAsWinningForWonAuctions() {
        Auction auction = createAuction(AuctionStatus.WON);
        Bid leadingBid = createBid(auction, "150.00", 1L, "winner@example.com");
        BidResponse response = auctionResponseMapper.toBidResponse(AuctionStatus.WON, leadingBid, leadingBid);

        assertTrue(response.winning());
        assertEquals("w****r@example.com", response.bidderEmail());
    }

    private Auction createAuction(AuctionStatus status) {
        User seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setEmail("seller@example.com");

        Listing listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setTitle("Test Listing");
        listing.setDescription("Test Description");
        listing.setPrice(new BigDecimal("50.00"));
        listing.setSeller(seller);

        Auction auction = new Auction();
        auction.setId(UUID.randomUUID());
        auction.setListing(listing);
        auction.setStatus(status);
        auction.setStartingPrice(new BigDecimal("50.00"));
        auction.setReservePrice(new BigDecimal("100.00"));
        auction.setMinimumBidIncrement(new BigDecimal("10.00"));
        auction.setDurationMinutes(60L);
        auction.setNextBidSequence(1L);
        auction.setExtensionCount(0);
        auction.setCreatedAt(CREATED_AT);
        auction.setStartsAt(CREATED_AT);
        auction.setEndsAt(ENDS_AT);
        return auction;
    }

    private Bid createBid(Auction auction, String amount, long sequenceNumber, String email) {
        User bidder = new User();
        bidder.setId(UUID.randomUUID());
        bidder.setEmail(email);

        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(new BigDecimal(amount));
        bid.setSequenceNumber(sequenceNumber);
        bid.setSubmittedAt(SUBMITTED_AT);
        return bid;
    }
}
