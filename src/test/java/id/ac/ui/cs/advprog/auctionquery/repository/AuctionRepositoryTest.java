package id.ac.ui.cs.advprog.auctionquery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Listing;
import id.ac.ui.cs.advprog.auctionquery.model.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:auctionquerytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
class AuctionRepositoryTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-22T08:00:00Z");
    private static final BigDecimal PRICE = new BigDecimal("50.00");
    private static final BigDecimal RESERVE_PRICE = new BigDecimal("100.00");
    private static final BigDecimal MINIMUM_INCREMENT = new BigDecimal("10.00");

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByIdWithListingAndSellerShouldReturnAuction() {
        Auction auction = persistAuction("seller@example.com", "Test Listing", AuctionStatus.ACTIVE, CREATED_AT);

        entityManager.flush();
        entityManager.clear();

        var result = auctionRepository.findByIdWithListingAndSeller(auction.getId());

        assertTrue(result.isPresent());
        assertEquals("Test Listing", result.get().getListing().getTitle());
        assertEquals("seller@example.com", result.get().getListing().getSeller().getEmail());
    }

    @Test
    void findByIdWithListingAndSellerShouldReturnEmptyWhenAuctionDoesNotExist() {
        var result = auctionRepository.findByIdWithListingAndSeller(UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    @Test
    void findAllByOrderByCreatedAtDescShouldReturnRequestedPageInDescendingOrder() {
        Auction olderAuction = persistAuction("seller-1@example.com", "Older", AuctionStatus.ACTIVE, CREATED_AT.minusSeconds(60));
        Auction newerAuction = persistAuction("seller-2@example.com", "Newer", AuctionStatus.ACTIVE, CREATED_AT);

        entityManager.flush();
        entityManager.clear();

        var result = auctionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1));

        assertEquals(1, result.getContent().size());
        assertEquals(newerAuction.getId(), result.getContent().getFirst().getId());
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().getFirst().getCreatedAt().isAfter(olderAuction.getCreatedAt()));
    }

    @Test
    void findAllByOrderByCreatedAtDescShouldReturnSecondPage() {
        Auction oldestAuction = persistAuction("seller-1@example.com", "Oldest", AuctionStatus.ACTIVE, CREATED_AT.minusSeconds(120));
        Auction middleAuction = persistAuction("seller-2@example.com", "Middle", AuctionStatus.ACTIVE, CREATED_AT.minusSeconds(60));
        persistAuction("seller-3@example.com", "Newest", AuctionStatus.ACTIVE, CREATED_AT);

        entityManager.flush();
        entityManager.clear();

        var result = auctionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(1, 1));

        assertEquals(1, result.getContent().size());
        assertEquals(middleAuction.getId(), result.getContent().getFirst().getId());
        assertTrue(result.getContent().getFirst().getCreatedAt().isAfter(oldestAuction.getCreatedAt()));
    }

    @Test
    void findByStatusOrderByCreatedAtDescShouldFilterAuctionsByStatus() {
        persistAuction("seller-1@example.com", "Closed", AuctionStatus.CLOSED, CREATED_AT);
        persistAuction("seller-2@example.com", "Active", AuctionStatus.ACTIVE, CREATED_AT.minusSeconds(60));

        entityManager.flush();
        entityManager.clear();

        var result = auctionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.CLOSED, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(AuctionStatus.CLOSED, result.getContent().getFirst().getStatus());
    }

    @Test
    void findByStatusOrderByCreatedAtDescShouldReturnEmptyWhenNoAuctionMatchesStatus() {
        persistAuction("seller-1@example.com", "Active", AuctionStatus.ACTIVE, CREATED_AT);

        entityManager.flush();
        entityManager.clear();

        var result = auctionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.CLOSED, PageRequest.of(0, 10));

        assertTrue(result.getContent().isEmpty());
    }

    private Auction persistAuction(String sellerEmail, String title, AuctionStatus status, Instant createdAt) {
        User seller = persistUser(sellerEmail);
        Listing listing = persistListing(seller, title);

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
}
