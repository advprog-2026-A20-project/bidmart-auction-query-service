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
docker run --env-file .env -p 8081:8081 bidmart-auction-query-service
```

## Environment variables

```txt
PORT
SPRING_PROFILES_ACTIVE
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

## Migration status

This service is extracted from the legacy Bidmart gateway/monolith as the first read-side microservice.

Current phase:

```txt
gateway + auction-query-service
```

Not included yet:

```txt
auction-command-service
wallet-service
auth-service
bid-service
notification-service
```
