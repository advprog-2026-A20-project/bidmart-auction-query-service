package id.ac.ui.cs.advprog.auctionquery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.auctionquery.dto.AuctionDetailResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.AuctionSummaryResponse;
import id.ac.ui.cs.advprog.auctionquery.dto.BidResponse;
import id.ac.ui.cs.advprog.auctionquery.mapper.AuctionResponseMapper;
import id.ac.ui.cs.advprog.auctionquery.model.Auction;
import id.ac.ui.cs.advprog.auctionquery.model.AuctionStatus;
import id.ac.ui.cs.advprog.auctionquery.model.Bid;
import id.ac.ui.cs.advprog.auctionquery.model.Listing;
import id.ac.ui.cs.advprog.auctionquery.model.User;
import id.ac.ui.cs.advprog.auctionquery.repository.AuctionRepository;
import id.ac.ui.cs.advprog.auctionquery.repository.BidRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AuctionQueryServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-22T08:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-05-22T09:00:00Z");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-05-22T08:30:00Z");

    private AuctionRepository auctionRepository;
    private BidRepository bidRepository;
    private BidCalculator bidCalculator;
    private AuctionStatusResolver auctionStatusResolver;
    private AuctionResponseMapper auctionResponseMapper;
    private AuctionQueryService service;

    @BeforeEach
    void setUp() {
        auctionRepository = Mockito.mock(AuctionRepository.class);
        bidRepository = Mockito.mock(BidRepository.class);
        bidCalculator = Mockito.mock(BidCalculator.class);
        auctionStatusResolver = Mockito.mock(AuctionStatusResolver.class);
        auctionResponseMapper = Mockito.mock(AuctionResponseMapper.class);
        service = new AuctionQueryService(
            auctionRepository,
            bidRepository,
            bidCalculator,
            auctionStatusResolver,
            auctionResponseMapper
        );
    }

    @Test
    void listAuctionsShouldLoadRequestedPageAndMapSummaryResponses() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Bid leadingBid = createBid(auction, "120.00", 1L, "buyer@example.com");
        AuctionSummaryResponse response = new AuctionSummaryResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getListing().getSeller().getId(),
            auction.getListing().getSeller().getEmail(),
            new BigDecimal("120.00"),
            auction.getStartingPrice(),
            auction.getMinimumBidIncrement(),
            AuctionStatus.ACTIVE,
            CREATED_AT,
            CREATED_AT,
            ENDS_AT,
            0,
            3,
            new BigDecimal("130.00")
        );
        Pageable pageable = PageRequest.of(0, 20);

        when(auctionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.ACTIVE, pageable))
            .thenReturn(new PageImpl<>(List.of(auction)));
        when(bidRepository.findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(auction.getId()))
            .thenReturn(Optional.of(leadingBid));
        when(bidRepository.countByAuctionId(auction.getId())).thenReturn(3L);
        when(auctionStatusResolver.resolveEffectiveStatus(auction)).thenReturn(AuctionStatus.ACTIVE);
        when(bidCalculator.calculateNextMinimumBid(auction, leadingBid)).thenReturn(new BigDecimal("130.00"));
        when(auctionResponseMapper.toSummaryResponse(
            auction,
            AuctionStatus.ACTIVE,
            leadingBid,
            3L,
            new BigDecimal("130.00")
        )).thenReturn(response);

        List<AuctionSummaryResponse> results = service.listAuctions(AuctionStatus.ACTIVE, pageable);

        assertEquals(List.of(response), results);
        verify(auctionRepository).findByStatusOrderByCreatedAtDesc(AuctionStatus.ACTIVE, pageable);
        verify(auctionRepository, never()).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void listAuctionsShouldLoadAllAuctionsWhenStatusIsNull() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Pageable pageable = PageRequest.of(0, 10);
        AuctionSummaryResponse response = new AuctionSummaryResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getListing().getSeller().getId(),
            auction.getListing().getSeller().getEmail(),
            new BigDecimal("50.00"),
            auction.getStartingPrice(),
            auction.getMinimumBidIncrement(),
            AuctionStatus.ACTIVE,
            CREATED_AT,
            CREATED_AT,
            ENDS_AT,
            0,
            0,
            new BigDecimal("60.00")
        );

        when(auctionRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(new PageImpl<>(List.of(auction)));
        when(bidRepository.findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(auction.getId())).thenReturn(Optional.empty());
        when(bidRepository.countByAuctionId(auction.getId())).thenReturn(0L);
        when(auctionStatusResolver.resolveEffectiveStatus(auction)).thenReturn(AuctionStatus.ACTIVE);
        when(bidCalculator.calculateNextMinimumBid(auction, null)).thenReturn(new BigDecimal("60.00"));
        when(auctionResponseMapper.toSummaryResponse(auction, AuctionStatus.ACTIVE, null, 0L, new BigDecimal("60.00")))
            .thenReturn(response);

        List<AuctionSummaryResponse> results = service.listAuctions(null, pageable);

        assertEquals(List.of(response), results);
        verify(auctionRepository).findAllByOrderByCreatedAtDesc(pageable);
        verify(auctionRepository, never()).findByStatusOrderByCreatedAtDesc(any(), eq(pageable));
    }

    @Test
    void listAuctionsShouldReturnEmptyListWhenRepositoryPageIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);

        when(auctionRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(new PageImpl<>(List.of()));

        List<AuctionSummaryResponse> results = service.listAuctions(null, pageable);

        assertEquals(List.of(), results);
        verify(auctionRepository).findAllByOrderByCreatedAtDesc(pageable);
        verify(bidRepository, never()).findTopByAuctionIdOrderByAmountDescSequenceNumberAsc(any());
        verify(auctionResponseMapper, never()).toSummaryResponse(any(), any(), any(), any(Long.class), any());
    }

    @Test
    void getAuctionDetailShouldAssembleMappedDetailResponse() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        Bid leadingBid = createBid(auction, "80.00", 1L, "buyer@example.com");
        List<Bid> bids = List.of(leadingBid);
        AuctionDetailResponse response = new AuctionDetailResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getListing().getSeller().getId(),
            auction.getListing().getSeller().getEmail(),
            new BigDecimal("80.00"),
            auction.getStartingPrice(),
            auction.getReservePrice(),
            auction.getMinimumBidIncrement(),
            AuctionStatus.ACTIVE,
            CREATED_AT,
            CREATED_AT,
            ENDS_AT,
            null,
            auction.getDurationMinutes(),
            auction.getExtensionCount(),
            1,
            new BigDecimal("90.00"),
            false,
            true,
            null,
            null,
            List.of()
        );

        when(auctionRepository.findByIdWithListingAndSeller(auction.getId())).thenReturn(Optional.of(auction));
        when(bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId())).thenReturn(bids);
        when(bidCalculator.selectLeadingBid(bids)).thenReturn(leadingBid);
        when(auctionStatusResolver.resolveEffectiveStatus(auction)).thenReturn(AuctionStatus.ACTIVE);
        when(auctionStatusResolver.resolveEffectiveClosedAt(auction, AuctionStatus.ACTIVE)).thenReturn(null);
        when(bidCalculator.calculateNextMinimumBid(auction, leadingBid)).thenReturn(new BigDecimal("90.00"));
        when(auctionStatusResolver.isReserveMet(auction, leadingBid)).thenReturn(false);
        when(auctionStatusResolver.isBiddableStatus(AuctionStatus.ACTIVE)).thenReturn(true);
        when(auctionResponseMapper.toDetailResponse(
            auction,
            AuctionStatus.ACTIVE,
            null,
            bids,
            leadingBid,
            new BigDecimal("90.00"),
            false,
            true
        )).thenReturn(response);

        AuctionDetailResponse result = service.getAuctionDetail(auction.getId());

        assertSame(response, result);
        verify(auctionRepository).findByIdWithListingAndSeller(auction.getId());
        verify(bidRepository).findByAuctionIdOrderBySequenceNumberAsc(auction.getId());
        verify(auctionResponseMapper).toDetailResponse(
            auction,
            AuctionStatus.ACTIVE,
            null,
            bids,
            leadingBid,
            new BigDecimal("90.00"),
            false,
            true
        );
    }

    @Test
    void getAuctionDetailShouldHandleAuctionWithoutBids() {
        Auction auction = createAuction(AuctionStatus.ACTIVE);
        List<Bid> bids = List.of();
        AuctionDetailResponse response = new AuctionDetailResponse(
            auction.getId(),
            auction.getListing().getId(),
            auction.getListing().getTitle(),
            auction.getListing().getDescription(),
            auction.getListing().getSeller().getId(),
            auction.getListing().getSeller().getEmail(),
            auction.getListing().getPrice(),
            auction.getStartingPrice(),
            auction.getReservePrice(),
            auction.getMinimumBidIncrement(),
            AuctionStatus.ACTIVE,
            CREATED_AT,
            CREATED_AT,
            ENDS_AT,
            null,
            auction.getDurationMinutes(),
            auction.getExtensionCount(),
            0,
            new BigDecimal("50.00"),
            false,
            true,
            null,
            null,
            List.of()
        );

        when(auctionRepository.findByIdWithListingAndSeller(auction.getId())).thenReturn(Optional.of(auction));
        when(bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId())).thenReturn(bids);
        when(bidCalculator.selectLeadingBid(bids)).thenReturn(null);
        when(auctionStatusResolver.resolveEffectiveStatus(auction)).thenReturn(AuctionStatus.ACTIVE);
        when(auctionStatusResolver.resolveEffectiveClosedAt(auction, AuctionStatus.ACTIVE)).thenReturn(null);
        when(bidCalculator.calculateNextMinimumBid(auction, null)).thenReturn(new BigDecimal("50.00"));
        when(auctionStatusResolver.isReserveMet(auction, null)).thenReturn(false);
        when(auctionStatusResolver.isBiddableStatus(AuctionStatus.ACTIVE)).thenReturn(true);
        when(auctionResponseMapper.toDetailResponse(
            auction,
            AuctionStatus.ACTIVE,
            null,
            bids,
            null,
            new BigDecimal("50.00"),
            false,
            true
        )).thenReturn(response);

        AuctionDetailResponse result = service.getAuctionDetail(auction.getId());

        assertSame(response, result);
        verify(bidCalculator).selectLeadingBid(bids);
        verify(auctionResponseMapper).toDetailResponse(
            auction,
            AuctionStatus.ACTIVE,
            null,
            bids,
            null,
            new BigDecimal("50.00"),
            false,
            true
        );
    }

    @Test
    void getBidHistoryShouldDelegateBidResponseMapping() {
        Auction auction = createAuction(AuctionStatus.WON);
        Bid winningBid = createBid(auction, "120.00", 1L, "winner@example.com");
        List<Bid> bids = List.of(winningBid);
        List<BidResponse> responses = List.of(new BidResponse(
            winningBid.getId(),
            winningBid.getBidder().getId(),
            "w****r@example.com",
            winningBid.getAmount(),
            winningBid.getSequenceNumber(),
            winningBid.getSubmittedAt(),
            true
        ));

        when(auctionRepository.findByIdWithListingAndSeller(auction.getId())).thenReturn(Optional.of(auction));
        when(bidRepository.findByAuctionIdOrderBySequenceNumberAsc(auction.getId())).thenReturn(bids);
        when(bidCalculator.selectLeadingBid(bids)).thenReturn(winningBid);
        when(auctionStatusResolver.resolveEffectiveStatus(auction)).thenReturn(AuctionStatus.WON);
        when(auctionStatusResolver.resolveEffectiveClosedAt(auction, AuctionStatus.WON))
            .thenReturn(Instant.parse("2026-05-22T09:05:00Z"));
        when(auctionResponseMapper.toBidResponses(AuctionStatus.WON, bids, winningBid)).thenReturn(responses);

        List<BidResponse> result = service.getBidHistory(auction.getId());

        assertSame(responses, result);
        verify(auctionResponseMapper).toBidResponses(AuctionStatus.WON, bids, winningBid);
    }

    @Test
    void getAuctionDetailShouldThrowNotFoundWhenAuctionDoesNotExist() {
        UUID auctionId = UUID.randomUUID();

        when(auctionRepository.findByIdWithListingAndSeller(auctionId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.getAuctionDetail(auctionId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Auction not found", exception.getReason());
    }

    @Test
    void getBidHistoryShouldThrowNotFoundWhenAuctionDoesNotExist() {
        UUID auctionId = UUID.randomUUID();

        when(auctionRepository.findByIdWithListingAndSeller(auctionId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.getBidHistory(auctionId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Auction not found", exception.getReason());
    }

    private Auction createAuction(AuctionStatus status) {
        User seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setEmail("seller@example.com");

        Listing listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setTitle("Test Listing");
        listing.setDescription("Test Description");
        listing.setPrice(new BigDecimal("50.00"));
        listing.setSeller(seller);

        Auction auction = new Auction();
        auction.setId(UUID.randomUUID());
        auction.setListing(listing);
        auction.setStatus(status);
        auction.setStartingPrice(new BigDecimal("50.00"));
        auction.setReservePrice(new BigDecimal("100.00"));
        auction.setMinimumBidIncrement(new BigDecimal("10.00"));
        auction.setDurationMinutes(60L);
        auction.setNextBidSequence(1L);
        auction.setExtensionCount(0);
        auction.setCreatedAt(CREATED_AT);
        auction.setStartsAt(CREATED_AT);
        auction.setEndsAt(ENDS_AT);
        return auction;
    }

    private Bid createBid(Auction auction, String amount, long sequenceNumber, String bidderEmail) {
        User bidder = new User();
        bidder.setId(UUID.randomUUID());
        bidder.setEmail(bidderEmail);

        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(new BigDecimal(amount));
        bid.setSequenceNumber(sequenceNumber);
        bid.setSubmittedAt(SUBMITTED_AT);
        return bid;
    }
}
