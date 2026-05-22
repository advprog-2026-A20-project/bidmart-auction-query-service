package id.ac.ui.cs.advprog.auctionquery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.AuctionSummaryResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.service.AuctionQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AuctionQueryController.class)
class AuctionQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuctionQueryService auctionQueryService;

    @Test
    void listAuctionsShouldReturnAuctionSummaries() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        AuctionSummaryResponse response = new AuctionSummaryResponse(
            auctionId,
            listingId,
            "Test Listing",
            "Test Description",
            sellerId,
            "seller@example.com",
            new BigDecimal("50.00"),
            new BigDecimal("50.00"),
            new BigDecimal("10.00"),
            AuctionStatus.ACTIVE,
            Instant.parse("2026-05-22T08:00:00Z"),
            Instant.parse("2026-05-22T08:00:00Z"),
            Instant.parse("2026-05-22T09:00:00Z"),
            0,
            0,
            new BigDecimal("50.00")
        );

        when(auctionQueryService.listAuctions(eq(null), any(Pageable.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(auctionId.toString()))
            .andExpect(jsonPath("$[0].listingId").value(listingId.toString()))
            .andExpect(jsonPath("$[0].title").value("Test Listing"))
            .andExpect(jsonPath("$[0].sellerEmail").value("seller@example.com"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void listAuctionsShouldAcceptStatusAndPaginationParameters() throws Exception {
        when(auctionQueryService.listAuctions(eq(AuctionStatus.CLOSED), any(Pageable.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/auctions")
                .param("status", "CLOSED")
                .param("page", "1")
                .param("size", "5"))
            .andExpect(status().isOk());
    }

    @Test
    void listAuctionsShouldRejectNegativePage() throws Exception {
        mockMvc.perform(get("/api/auctions").param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("must be greater than or equal to 0"));
    }

    @Test
    void listAuctionsShouldRejectPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/auctions").param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("must be less than or equal to 100"));
    }

    @Test
    void getAuctionDetailShouldReturnAuctionDetail() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        AuctionDetailResponse response = new AuctionDetailResponse(
            auctionId,
            listingId,
            "Test Listing",
            "Test Description",
            sellerId,
            "seller@example.com",
            new BigDecimal("50.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            new BigDecimal("10.00"),
            AuctionStatus.ACTIVE,
            Instant.parse("2026-05-22T08:00:00Z"),
            Instant.parse("2026-05-22T08:00:00Z"),
            Instant.parse("2026-05-22T09:00:00Z"),
            null,
            60L,
            0,
            0,
            new BigDecimal("50.00"),
            false,
            true,
            null,
            null,
            List.of()
        );

        when(auctionQueryService.getAuctionDetail(auctionId)).thenReturn(response);

        mockMvc.perform(get("/api/auctions/{auctionId}", auctionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(auctionId.toString()))
            .andExpect(jsonPath("$.listingId").value(listingId.toString()))
            .andExpect(jsonPath("$.title").value("Test Listing"))
            .andExpect(jsonPath("$.biddable").value(true));
    }

    @Test
    void getBidHistoryShouldReturnMaskedBidResponses() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        BidResponse response = new BidResponse(
            bidId,
            bidderId,
            "b****r@example.com",
            new BigDecimal("120.00"),
            1L,
            Instant.parse("2026-05-22T08:30:00Z"),
            false
        );

        when(auctionQueryService.getBidHistory(auctionId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions/{auctionId}/bids", auctionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(bidId.toString()))
            .andExpect(jsonPath("$[0].bidderId").value(bidderId.toString()))
            .andExpect(jsonPath("$[0].bidderEmail").value("b****r@example.com"))
            .andExpect(jsonPath("$[0].amount").value(120.00))
            .andExpect(jsonPath("$[0].winning").value(false));
    }

    @Test
    void getAuctionDetailShouldReturnNotFoundWhenServiceThrowsNotFound() throws Exception {
        UUID auctionId = UUID.randomUUID();

        when(auctionQueryService.getAuctionDetail(auctionId))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));

        mockMvc.perform(get("/api/auctions/{auctionId}", auctionId))
            .andExpect(status().isNotFound());
    }
}
