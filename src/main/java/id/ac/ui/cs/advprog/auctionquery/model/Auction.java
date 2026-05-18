package id.ac.ui.cs.advprog.auctionquery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Getter
@Setter
@Immutable
@Entity
@Table(name = "auction")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "listing_id", nullable = false, unique = true)
    private UUID listingId;

    @OneToOne(optional = false)
    @JoinColumn(name = "listing_id", nullable = false, unique = true, insertable = false, updatable = false)
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "current_highest_bid", precision = 19, scale = 2)
    private BigDecimal currentHighestBid;

    @Column(name = "current_highest_bidder_id")
    private UUID currentHighestBidderId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant endsAt;
}
