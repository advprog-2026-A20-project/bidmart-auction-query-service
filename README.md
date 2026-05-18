# Bidmart Auction Query Service

Read-only service untuk auction query di Bidmart.

## Responsibilities

- Read auction list
- Read auction detail
- Read bid history
- Expose health check

## Endpoints

```txt
GET /api/auctions
GET /api/auctions/{auctionId}
GET /api/auctions/{auctionId}/bids
GET /actuator/health
```

## Local development

Run test:

```bash
./gradlew test
```

Run locally:

```bash
./gradlew bootRun
```

Build jar:

```bash
./gradlew bootJar
```

Build Docker image:

```bash
docker build -t bidmart-auction-query-service .
```

Run Docker container:

```bash
docker run --env-file .env -p 8083:8083 bidmart-auction-query-service
```

## Environment variables

```txt
PORT
SPRING_PROFILES_ACTIVE
AUCTION_QUERY_DB_URL
AUCTION_QUERY_DB_USERNAME
AUCTION_QUERY_DB_PASSWORD
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

## Migration status

This service is extracted from the legacy Bidmart gateway/monolith as the auction read-side microservice.

Current phase:

```txt
gateway + auction-query-service + bidding-command-service + wallet-service + auth-service
```

Still pending or outside this service:

```txt
notification-service
```

Compatibility note: auction-query-service is intentionally read-only. It reads the auction, bid, listing, and app_user tables projected by command-side services; larger projection or event-handling work should be handled separately if those schemas diverge.
