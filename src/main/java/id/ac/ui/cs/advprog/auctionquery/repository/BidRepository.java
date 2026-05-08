package id.ac.ui.cs.advprog.auctionquery.repository;

import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, UUID> {

    long countByAuctionId(UUID auctionId);

    List<Bid> findByAuctionIdOrderBySequenceNumberAsc(UUID auctionId);

    Optional<Bid> findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(UUID auctionId);
}
