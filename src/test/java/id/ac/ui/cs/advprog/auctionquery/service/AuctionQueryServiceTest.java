package id.ac.ui.cs.advprog.auctionquery.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import id.ac.ui.cs.advprog.auctionquery.model.Listing;
import id.ac.ui.cs.advprog.auctionquery.model.User;
import id.ac.ui.cs.advprog.auctionquery.repository.AuctionRepository;
import id.ac.ui.cs.advprog.auctionquery.repository.BidRepository;

class AuctionQueryServiceTest {

    private AuctionRepository auctionRepository;
    private BidRepository bidRepository;
    private AuctionQueryService service;

    @BeforeEach
    void setUp() {
        auctionRepository = Mockito.mock(AuctionRepository.class);
        bidRepository = Mockito.mock(BidRepository.class);
        service = new AuctionQueryService(auctionRepository, bidRepository);
    }

    @Test
void getBidHistoryShouldNotMarkLeadingBidAsWinningWhenAuctionIsStillActive() {
    Auction auction = createAuction(
        AuctionStatus.ACTIVE,
        Instant.now().minusSeconds(3600),
        new BigDecimal("100.00")
    );

    Bid lowerBid = createBid(auction, new BigDecimal("90.00"), 1L);
    Bid leadingBid = createBid(auction, new BigDecimal("120.00"), 2L);

    when(auctionRepository.findByIdWithListingAndSeller(auction.getId()))
        .thenReturn(Optional.of(auction));
    when(bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId()))
        .thenReturn(List.of(lowerBid, leadingBid));

    List<BidResponse> responses = service.getBidHistory(auction.getId());

    assertEquals(2, responses.size());
    assertFalse(responses.get(0).winning());
    assertFalse(responses.get(1).winning());
}

    @Test
void getAuctionDetailShouldKeepActiveStatusWhenAuctionExpiredAndReserveNotMet() {
    Auction auction = createAuction(
        AuctionStatus.ACTIVE,
        Instant.now().minusSeconds(3600),
        new BigDecimal("100.00")
    );

    Bid bid = createBid(auction, new BigDecimal("80.00"), 1L);

    when(auctionRepository.findByIdWithListingAndSeller(auction.getId()))
        .thenReturn(Optional.of(auction));
    when(bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId()))
        .thenReturn(List.of(bid));

    AuctionDetailResponse response = service.getAuctionDetail(auction.getId());

    assertEquals(AuctionStatus.ACTIVE, response.status());
    assertFalse(response.reserveMet());
    assertTrue(response.biddable());
    assertEquals(1, response.totalBids());
    assertEquals(new BigDecimal("90.00"), response.nextMinimumBid());
}

@Test
void getAuctionDetailShouldKeepActiveStatusWhenAuctionExpiredAndReserveMet() {
    Auction auction = createAuction(
        AuctionStatus.ACTIVE,
        Instant.now().minusSeconds(3600),
        new BigDecimal("100.00")
    );

    Bid bid = createBid(auction, new BigDecimal("120.00"), 1L);

    when(auctionRepository.findByIdWithListingAndSeller(auction.getId()))
        .thenReturn(Optional.of(auction));
    when(bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId()))
        .thenReturn(List.of(bid));

    AuctionDetailResponse response = service.getAuctionDetail(auction.getId());

    assertEquals(AuctionStatus.ACTIVE, response.status());
    assertTrue(response.reserveMet());
    assertTrue(response.biddable());
    assertEquals(bid.getId(), response.leadingBid().id());

    // Saat status masih ACTIVE, kemungkinan winningBid belum diset.
    assertNull(response.winningBid());
}

    @Test
    void getAuctionDetailShouldThrowNotFoundWhenAuctionDoesNotExist() {
        UUID auctionId = UUID.randomUUID();

        when(auctionRepository.findByIdWithListingAndSeller(auctionId))
            .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.getAuctionDetail(auctionId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private Auction createAuction(
        AuctionStatus status,
        Instant endsAt,
        BigDecimal reservePrice
    ) {
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
        auction.setReservePrice(reservePrice);
        auction.setMinimumBidIncrement(new BigDecimal("10.00"));
        auction.setDurationMinutes(60L);
        auction.setNextBidSequence(1L);
        auction.setExtensionCount(0);
        auction.setCreatedAt(Instant.now().minusSeconds(7200));
        auction.setStartsAt(Instant.now().minusSeconds(7200));
        auction.setEndsAt(endsAt);

        return auction;
    }

    private Bid createBid(Auction auction, BigDecimal amount, long sequenceNumber) {
        User bidder = new User();
        bidder.setId(UUID.randomUUID());
        bidder.setEmail("bidder" + sequenceNumber + "@example.com");

        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setSequenceNumber(sequenceNumber);
        bid.setSubmittedAt(Instant.now().minusSeconds(1800));

        return bid;
    }
}