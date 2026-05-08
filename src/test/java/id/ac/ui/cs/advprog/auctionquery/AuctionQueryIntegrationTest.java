package id.ac.ui.cs.advprog.auctionquery;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import id.ac.ui.cs.advprog.auctionquery.model.Listing;
import id.ac.ui.cs.advprog.auctionquery.model.User;
import id.ac.ui.cs.advprog.auctionquery.repository.AuctionRepository;
import id.ac.ui.cs.advprog.auctionquery.repository.BidRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auctionquerytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuctionQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAll();
        auctionRepository.deleteAll();
        entityManager.createNativeQuery("delete from listing").executeUpdate();
        entityManager.createNativeQuery("delete from app_user").executeUpdate();
    }

    @Test
    void healthcheckShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readEndpointsShouldReturnAuctionSummariesAndDetails() throws Exception {
        User seller = persistUser("seller@example.com");
        User buyer = persistUser("buyer@example.com");
        Listing listing = persistListing(seller, "Gaming Phone", "Competitive smartphone", "1200.00");
        Auction auction = persistAuction(listing, AuctionStatus.ACTIVE, Instant.now().plus(2, ChronoUnit.HOURS));
        Bid bid = persistBid(auction, buyer, "1250.00", 1L);

        mockMvc.perform(get("/api/auctions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(auction.getId().toString()))
            .andExpect(jsonPath("$[0].sellerEmail").value("seller@example.com"));

        mockMvc.perform(get("/api/auctions/{auctionId}", auction.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(auction.getId().toString()))
            .andExpect(jsonPath("$.leadingBid.id").value(bid.getId().toString()))
            .andExpect(jsonPath("$.bidHistory.length()").value(1));

        mockMvc.perform(get("/api/auctions/{auctionId}/bids", auction.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(bid.getId().toString()))
            .andExpect(jsonPath("$[0].winning").value(true));
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        entityManager.persist(user);
        return user;
    }

    private Listing persistListing(User seller, String title, String description, String price) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setPrice(new BigDecimal(price));
        listing.setSeller(seller);
        entityManager.persist(listing);
        return listing;
    }

    private Auction persistAuction(Listing listing, AuctionStatus status, Instant endsAt) {
        Auction auction = new Auction();
        auction.setListing(listing);
        auction.setStatus(status);
        auction.setStartingPrice(listing.getPrice());
        auction.setReservePrice(listing.getPrice());
        auction.setMinimumBidIncrement(new BigDecimal("10.00"));
        auction.setDurationMinutes(60L);
        auction.setNextBidSequence(1L);
        auction.setExtensionCount(0);
        auction.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        auction.setStartsAt(auction.getCreatedAt());
        auction.setEndsAt(endsAt);
        entityManager.persist(auction);
        return auction;
    }

    private Bid persistBid(Auction auction, User bidder, String amount, long sequenceNumber) {
        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(new BigDecimal(amount));
        bid.setSequenceNumber(sequenceNumber);
        bid.setSubmittedAt(Instant.now());
        entityManager.persist(bid);
        entityManager.flush();
        return bid;
    }
}
