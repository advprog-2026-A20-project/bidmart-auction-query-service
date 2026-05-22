package id.ac.ui.cs.advprog.auctionquery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import id.ac.ui.cs.advprog.auctionquery.model.Listing;
import id.ac.ui.cs.advprog.auctionquery.model.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:bidrepositorytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
class BidRepositoryTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-22T08:00:00Z");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-05-22T08:30:00Z");
    private static final BigDecimal PRICE = new BigDecimal("50.00");
    private static final BigDecimal RESERVE_PRICE = new BigDecimal("100.00");
    private static final BigDecimal MINIMUM_INCREMENT = new BigDecimal("10.00");

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void countByAuctionIdShouldReturnOnlyBidsForThatAuction() {
        User seller = persistUser("seller@example.com");
        User firstBidder = persistUser("first@example.com");
        User secondBidder = persistUser("second@example.com");
        Listing firstListing = persistListing(seller, "First");
        Listing secondListing = persistListing(seller, "Second");
        Auction firstAuction = persistAuction(firstListing, AuctionStatus.ACTIVE, CREATED_AT);
        Auction secondAuction = persistAuction(secondListing, AuctionStatus.ACTIVE, CREATED_AT.plusSeconds(60));

        persistBid(firstAuction, firstBidder, new BigDecimal("60.00"), 1L, SUBMITTED_AT);
        persistBid(firstAuction, secondBidder, new BigDecimal("70.00"), 2L, SUBMITTED_AT.plusSeconds(60));
        persistBid(secondAuction, firstBidder, new BigDecimal("80.00"), 1L, SUBMITTED_AT.plusSeconds(120));

        flushAndClear();

        long count = bidRepository.countByAuctionId(firstAuction.getId());

        assertEquals(2L, count);
    }

    @Test
    void findByAuctionIdOrderBySequenceNumberAscShouldReturnBidsInSequenceOrder() {
        User seller = persistUser("seller@example.com");
        User bidder = persistUser("bidder@example.com");
        Listing listing = persistListing(seller, "Ordered Auction");
        Auction auction = persistAuction(listing, AuctionStatus.ACTIVE, CREATED_AT);
        Bid thirdBid = persistBid(auction, bidder, new BigDecimal("90.00"), 3L, SUBMITTED_AT.plusSeconds(120));
        Bid firstBid = persistBid(auction, bidder, new BigDecimal("70.00"), 1L, SUBMITTED_AT);
        Bid secondBid = persistBid(auction, bidder, new BigDecimal("80.00"), 2L, SUBMITTED_AT.plusSeconds(60));

        flushAndClear();

        List<Bid> result = bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId());

        assertEquals(List.of(firstBid.getId(), secondBid.getId(), thirdBid.getId()), result.stream().map(Bid::getId).toList());
    }

    @Test
    void findTopByAuctionIdOrderByAmountDescSequenceNumberAscShouldReturnHighestAmount() {
        User seller = persistUser("seller@example.com");
        User bidder = persistUser("bidder@example.com");
        Listing listing = persistListing(seller, "Highest Bid Auction");
        Auction auction = persistAuction(listing, AuctionStatus.ACTIVE, CREATED_AT);
        persistBid(auction, bidder, new BigDecimal("80.00"), 1L, SUBMITTED_AT);
        Bid highestBid = persistBid(auction, bidder, new BigDecimal("120.00"), 2L, SUBMITTED_AT.plusSeconds(60));

        flushAndClear();

        var result = bidRepository.findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(auction.getId());

        assertTrue(result.isPresent());
        assertEquals(highestBid.getId(), result.get().getId());
    }

    @Test
    void findTopByAuctionIdOrderByAmountDescSequenceNumberAscShouldUseLowestSequenceWhenAmountsTie() {
        User seller = persistUser("seller@example.com");
        User bidder = persistUser("bidder@example.com");
        Listing listing = persistListing(seller, "Tie Auction");
        Auction auction = persistAuction(listing, AuctionStatus.ACTIVE, CREATED_AT);
        Bid firstHighestBid = persistBid(auction, bidder, new BigDecimal("120.00"), 1L, SUBMITTED_AT);
        persistBid(auction, bidder, new BigDecimal("120.00"), 2L, SUBMITTED_AT.plusSeconds(60));

        flushAndClear();

        var result = bidRepository.findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(auction.getId());

        assertTrue(result.isPresent());
        assertEquals(firstHighestBid.getId(), result.get().getId());
    }

    @Test
    void findTopByAuctionIdOrderByAmountDescSequenceNumberAscShouldReturnEmptyWhenAuctionHasNoBids() {
        User seller = persistUser("seller@example.com");
        Listing listing = persistListing(seller, "Empty Auction");
        Auction auction = persistAuction(listing, AuctionStatus.ACTIVE, CREATED_AT);

        flushAndClear();

        var result = bidRepository.findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(auction.getId());

        assertFalse(result.isPresent());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        entityManager.persist(user);
        return user;
    }

    private Listing persistListing(User seller, String title) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription("Test Description");
        listing.setPrice(PRICE);
        listing.setSeller(seller);
        entityManager.persist(listing);
        return listing;
    }

    private Auction persistAuction(Listing listing, AuctionStatus status, Instant createdAt) {
        Auction auction = new Auction();
        auction.setListing(listing);
        auction.setStatus(status);
        auction.setStartingPrice(PRICE);
        auction.setReservePrice(RESERVE_PRICE);
        auction.setMinimumBidIncrement(MINIMUM_INCREMENT);
        auction.setDurationMinutes(60L);
        auction.setNextBidSequence(1L);
        auction.setExtensionCount(0);
        auction.setCreatedAt(createdAt);
        auction.setStartsAt(createdAt);
        auction.setEndsAt(createdAt.plusSeconds(3600));
        entityManager.persist(auction);
        return auction;
    }

    private Bid persistBid(Auction auction, User bidder, BigDecimal amount, long sequenceNumber, Instant submittedAt) {
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setSequenceNumber(sequenceNumber);
        bid.setSubmittedAt(submittedAt);
        entityManager.persist(bid);
        return bid;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
