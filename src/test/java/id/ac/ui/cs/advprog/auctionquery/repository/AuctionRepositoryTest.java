package id.ac.ui.cs.advprog.auctionquery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Listing;
import id.ac.ui.cs.advprog.auctionquery.model.User;
import java.math.BigDecimal;
import java.time.Instant;
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
    void findByStatusOrderByCreatedAtDescShouldFilterAuctionsByStatus() {
        persistAuction("seller-1@example.com", "Closed", AuctionStatus.CLOSED, CREATED_AT);
        persistAuction("seller-2@example.com", "Active", AuctionStatus.ACTIVE, CREATED_AT.minusSeconds(60));

        entityManager.flush();
        entityManager.clear();

        var result = auctionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.CLOSED, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(AuctionStatus.CLOSED, result.getContent().getFirst().getStatus());
    }

    private Auction persistAuction(String sellerEmail, String title, AuctionStatus status, Instant createdAt) {
        User seller = new User();
        seller.setEmail(sellerEmail);
        entityManager.persist(seller);

        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription("Test Description");
        listing.setPrice(new BigDecimal("50.00"));
        listing.setSeller(seller);
        entityManager.persist(listing);

        Auction auction = new Auction();
        auction.setListing(listing);
        auction.setStatus(status);
        auction.setStartingPrice(new BigDecimal("50.00"));
        auction.setReservePrice(new BigDecimal("100.00"));
        auction.setMinimumBidIncrement(new BigDecimal("10.00"));
        auction.setDurationMinutes(60L);
        auction.setNextBidSequence(1L);
        auction.setExtensionCount(0);
        auction.setCreatedAt(createdAt);
        auction.setStartsAt(createdAt);
        auction.setEndsAt(createdAt.plusSeconds(3600));
        entityManager.persist(auction);
        return auction;
    }

    private static final Instant CREATED_AT = Instant.parse("2026-05-22T08:00:00Z");
}
