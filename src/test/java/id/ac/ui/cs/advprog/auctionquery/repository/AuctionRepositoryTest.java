package id.ac.ui.cs.advprog.auctionquery.repository;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Listing;
import id.ac.ui.cs.advprog.auctionquery.model.User;

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
        User seller = new User();
        seller.setEmail("seller@example.com");
        entityManager.persist(seller);

        Listing listing = new Listing();
        listing.setTitle("Test Listing");
        listing.setDescription("Test Description");
        listing.setPrice(new BigDecimal("50.00"));
        listing.setSeller(seller);
        entityManager.persist(listing);

        Auction auction = new Auction();
        auction.setListing(listing);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setStartingPrice(new BigDecimal("50.00"));
        auction.setReservePrice(new BigDecimal("100.00"));
        auction.setMinimumBidIncrement(new BigDecimal("10.00"));
        auction.setDurationMinutes(60L);
        auction.setNextBidSequence(1L);
        auction.setExtensionCount(0);
        auction.setCreatedAt(Instant.now());
        entityManager.persist(auction);

        entityManager.flush();
        entityManager.clear();

        var result = auctionRepository.findByIdWithListingAndSeller(auction.getId());

        assertTrue(result.isPresent());
        assertEquals("Test Listing", result.get().getListing().getTitle());
        assertEquals("seller@example.com", result.get().getListing().getSeller().getEmail());
    }
}