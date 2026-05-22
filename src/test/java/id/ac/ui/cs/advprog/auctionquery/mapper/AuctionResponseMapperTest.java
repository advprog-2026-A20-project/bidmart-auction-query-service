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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AuctionResponseMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-22T08:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-05-22T09:00:00Z");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-05-22T08:30:00Z");
    private static final BigDecimal LISTING_PRICE = new BigDecimal("50.00");
    private static final BigDecimal RESERVE_PRICE = new BigDecimal("100.00");
    private static final BigDecimal MINIMUM_INCREMENT = new BigDecimal("10.00");
    private static final BigDecimal NEXT_MINIMUM_BID = new BigDecimal("130.00");
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final String LONG_EMAIL = "bidder@example.com";

    private AuctionResponseMapper auctionResponseMapper;

    @BeforeEach
    void setUp() {
        auctionResponseMapper = new AuctionResponseMapper();
    }

    @Test
    void toSummaryResponseShouldMapAuctionSummaryFields() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Bid leadingBid = createBid(auction, "120.00", 1L, LONG_EMAIL);

        AuctionSummaryResponse response = auctionResponseMapper.toSummaryResponse(
            auction,
            AuctionStatus.ACTIVE,
            leadingBid,
            2L,
            NEXT_MINIMUM_BID
        );

        assertEquals(auction.getId(), response.id());
        assertEquals(auction.getListing().getSeller().getEmail(), response.sellerEmail());
        assertEquals(new BigDecimal("120.00"), response.currentPrice());
        assertEquals(NEXT_MINIMUM_BID, response.nextMinimumBid());
    }

    @Test
    void toSummaryResponseShouldUseListingPriceWhenLeadingBidDoesNotExist() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);

        AuctionSummaryResponse response = auctionResponseMapper.toSummaryResponse(
            auction,
            AuctionStatus.ACTIVE,
            null,
            3L,
            NEXT_MINIMUM_BID
        );

        assertEquals(LISTING_PRICE, response.currentPrice());
        assertEquals(NEXT_MINIMUM_BID, response.nextMinimumBid());
        assertEquals(3L, response.totalBids());
    }

    @Test
    void toDetailResponseShouldMapLeadingBidAndMaskBidderEmail() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Bid leadingBid = createBid(auction, "120.00", 1L, LONG_EMAIL);

        AuctionDetailResponse response = auctionResponseMapper.toDetailResponse(
            auction,
            AuctionStatus.ACTIVE,
            null,
            List.of(leadingBid),
            leadingBid,
            NEXT_MINIMUM_BID,
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
    void toDetailResponseShouldUseListingPriceAndNullLeadingBidWhenNoBidsExist() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);

        AuctionDetailResponse response = auctionResponseMapper.toDetailResponse(
            auction,
            AuctionStatus.ACTIVE,
            null,
            List.of(),
            null,
            NEXT_MINIMUM_BID,
            false,
            true
        );

        assertEquals(LISTING_PRICE, response.currentPrice());
        assertNull(response.leadingBid());
        assertNull(response.winningBid());
        assertTrue(response.bidHistory().isEmpty());
        assertEquals(0, response.totalBids());
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

    @Test
    void toBidResponseShouldReturnFalseWhenAuctionStatusIsNotWon() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Bid leadingBid = createBid(auction, "150.00", 1L, "winner@example.com");

        BidResponse response = auctionResponseMapper.toBidResponse(AuctionStatus.ACTIVE, leadingBid, leadingBid);

        assertFalse(response.winning());
    }

    @Test
    void toBidResponseShouldReturnFalseWhenLeadingBidIsNull() {
        Auction auction = createAuction(AuctionStatus.WON);
        Bid bid = createBid(auction, "150.00", 1L, "winner@example.com");

        BidResponse response = auctionResponseMapper.toBidResponse(AuctionStatus.WON, bid, null);

        assertFalse(response.winning());
    }

    @Test
    void toBidResponseShouldReturnFalseWhenBidIsNotLeadingBid() {
        Auction auction = createAuction(AuctionStatus.WON);
        Bid leadingBid = createBid(auction, "160.00", 1L, "winner@example.com");
        Bid otherBid = createBid(auction, "150.00", 2L, "other@example.com");

        BidResponse response = auctionResponseMapper.toBidResponse(AuctionStatus.WON, otherBid, leadingBid);

        assertFalse(response.winning());
    }

    @ParameterizedTest(name = "email \"{0}\" should mask to \"{1}\"")
    @MethodSource("emailMaskingCases")
    void toBidResponseShouldMaskBidderEmail(String email, String expectedMaskedEmail) {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Bid bid = createBid(auction, "120.00", 1L, email);

        BidResponse response = auctionResponseMapper.toBidResponse(AuctionStatus.ACTIVE, bid, null);

        assertEquals(expectedMaskedEmail, response.bidderEmail());
    }

    private static Stream<Arguments> emailMaskingCases() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of("   ", "   "),
            Arguments.of("invalid-email", "***"),
            Arguments.of("invalid@", "***"),
            Arguments.of("a@example.com", "*@example.com"),
            Arguments.of("ab@example.com", "a*@example.com"),
            Arguments.of(LONG_EMAIL, "b****r@example.com")
        );
    }

    private Auction createAuction(AuctionStatus status) {
        User seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setEmail(SELLER_EMAIL);

        Listing listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setTitle("Test Listing");
        listing.setDescription("Test Description");
        listing.setPrice(LISTING_PRICE);
        listing.setSeller(seller);

        Auction auction = new Auction();
        auction.setId(UUID.randomUUID());
        auction.setListing(listing);
        auction.setStatus(status);
        auction.setStartingPrice(LISTING_PRICE);
        auction.setReservePrice(RESERVE_PRICE);
        auction.setMinimumBidIncrement(MINIMUM_INCREMENT);
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
