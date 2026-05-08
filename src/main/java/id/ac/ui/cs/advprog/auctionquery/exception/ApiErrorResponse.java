package id.ac.ui.cs.advprog.auctionquery.exception;

import java.time.Instant;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path
) {
}
