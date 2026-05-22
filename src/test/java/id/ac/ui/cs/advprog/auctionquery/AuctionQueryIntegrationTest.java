package id.ac.ui.cs.advprog.auctionquery;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void readEndpointsShouldReturnAuctionSummariesDetailsAndMaskedBidHistory() throws Exception {
        User seller = persistUser("seller@example.com");
        User buyer = persistUser("buyer@example.com");
        Listing listing = persistListing(seller, "Gaming Phone", "Competitive smartphone", "1200.00");
        Auction auction = persistAuction(listing, AuctionStatus.ACTIVE, baseInstant().plus(2, ChronoUnit.HOURS), baseInstant());
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
            .andExpect(jsonPath("$.leadingBid.bidderEmail").value("b***r@example.com"))
            .andExpect(jsonPath("$.bidHistory.length()").value(1));

        mockMvc.perform(get("/api/auctions/{auctionId}/bids", auction.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(bid.getId().toString()))
            .andExpect(jsonPath("$[0].bidderEmail").value("b***r@example.com"))
            .andExpect(jsonPath("$[0].winning").value(false));
    }

    @Test
    void listEndpointShouldApplyPaginationAndStatusFilter() throws Exception {
        User seller = persistUser("seller-closed@example.com");
        Listing newestListing = persistListing(seller, "Newest Auction", "Newest", "1500.00");
        Listing olderListing = persistListing(seller, "Older Auction", "Older", "1200.00");
        Auction closedAuction = persistAuction(
            newestListing,
            AuctionStatus.CLOSED,
            baseInstant().minus(1, ChronoUnit.MINUTES),
            baseInstant().plus(5, ChronoUnit.MINUTES)
        );
        closedAuction.setClosedAt(baseInstant().plus(6, ChronoUnit.MINUTES));
        persistAuction(
            olderListing,
            AuctionStatus.ACTIVE,
            baseInstant().plus(2, ChronoUnit.HOURS),
            baseInstant()
        );
        entityManager.flush();

        mockMvc.perform(get("/api/auctions")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(closedAuction.getId().toString()));

        mockMvc.perform(get("/api/auctions")
                .param("status", "CLOSED")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(closedAuction.getId().toString()))
            .andExpect(jsonPath("$[0].status").value("CLOSED"));
    }

    @Test
    void listEndpointShouldRejectPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/auctions").param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("must be less than or equal to 100"));
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

    private Auction persistAuction(Listing listing, AuctionStatus status, Instant endsAt, Instant createdAt) {
        Auction auction = new Auction();
        auction.setListing(listing);
        auction.setStatus(status);
        auction.setStartingPrice(listing.getPrice());
        auction.setReservePrice(listing.getPrice());
        auction.setMinimumBidIncrement(new BigDecimal("10.00"));
        auction.setDurationMinutes(60L);
        auction.setNextBidSequence(1L);
        auction.setExtensionCount(0);
        auction.setCreatedAt(createdAt.truncatedTo(ChronoUnit.SECONDS));
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
        bid.setSubmittedAt(baseInstant().plus(30, ChronoUnit.MINUTES));
        entityManager.persist(bid);
        entityManager.flush();
        return bid;
    }

    private Instant baseInstant() {
        return Instant.parse("2026-05-22T08:00:00Z");
    }
}
