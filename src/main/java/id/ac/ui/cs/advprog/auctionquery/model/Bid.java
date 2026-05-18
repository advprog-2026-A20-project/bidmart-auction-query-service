package id.ac.ui.cs.advprog.auctionquery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "bid")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "auction_id", nullable = false)
    private UUID auctionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "auction_id", nullable = false, insertable = false, updatable = false)
    private Auction auction;

    @Column(name = "bidder_id", nullable = false)
    private UUID bidderId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bidder_id", nullable = false, insertable = false, updatable = false)
    private User bidder;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    @Column(nullable = false)
    private Instant createdAt;
}
