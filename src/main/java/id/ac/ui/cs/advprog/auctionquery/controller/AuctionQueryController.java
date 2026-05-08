package id.ac.ui.cs.advprog.auctionquery.controller;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.AuctionSummaryResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.service.AuctionQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions")
public class AuctionQueryController {

    private final AuctionQueryService auctionQueryService;

    public AuctionQueryController(AuctionQueryService auctionQueryService) {
        this.auctionQueryService = auctionQueryService;
    }

    @GetMapping
    public List<AuctionSummaryResponse> listAuctions() {
        return auctionQueryService.listAuctions();
    }

    @GetMapping("/{auctionId}")
    public AuctionDetailResponse getAuctionDetail(@PathVariable UUID auctionId) {
        return auctionQueryService.getAuctionDetail(auctionId);
    }

    @GetMapping("/{auctionId}/bids")
    public List<BidResponse> getBidHistory(@PathVariable UUID auctionId) {
        return auctionQueryService.getBidHistory(auctionId);
    }
}
