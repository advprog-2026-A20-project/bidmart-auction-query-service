package id.ac.ui.cs.advprog.auctionquery.repository;

import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    @Query("""
        select a
        from Auction a
        join fetch a.listing l
        join fetch l.seller
        order by a.createdAt desc
        """)
    List<Auction> findAllWithListingAndSellerOrderByCreatedAtDesc();

    @Query("""
        select a
        from Auction a
        join fetch a.listing l
        join fetch l.seller
        where a.id = :auctionId
        """)
    Optional<Auction> findByIdWithListingAndSeller(@Param("auctionId") UUID auctionId);
}
