package id.ac.ui.cs.advprog.auctionquery.controller;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.AuctionSummaryResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.service.AuctionQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/auctions")
public class AuctionQueryController {

    private final AuctionQueryService auctionQueryService;

    public AuctionQueryController(AuctionQueryService auctionQueryService) {
        this.auctionQueryService = auctionQueryService;
    }

    @GetMapping
    public List<AuctionSummaryResponse> listAuctions(
        @RequestParam(required = false) AuctionStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return auctionQueryService.listAuctions(status, PageRequest.of(page, size));
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
