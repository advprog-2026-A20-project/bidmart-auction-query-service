# BidMart Auction Query Service

Service read-only untuk query auction di BidMart. Service ini diekstrak dari gateway/monolith lama sebagai read-side pertama dalam migrasi strangler.

## Tanggung Jawab

- Membaca daftar auction.
- Membaca detail auction.
- Membaca bid history.
- Mengekspos health check.

Service ini bersifat read-only. Auction lifecycle, place bid, wallet settlement, dan event command-side tetap menjadi tanggung jawab bidding command service atau gateway legacy selama fase strangler.

## Endpoint

```txt
GET /api/auctions
GET /api/auctions/{auctionId}
GET /api/auctions/{auctionId}/bids
GET /actuator/health
```

## Run Lokal

Jalankan test:

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

Run container:

```bash
docker run --env-file .env -p 8081:8081 bidmart-auction-query-service
```

## Environment Variable

```txt
PORT
SPRING_PROFILES_ACTIVE
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

## Service Boundary

Dokumentasi boundary ada di `docs/service-boundary.md`.

## Status Migrasi

Fase saat ini:

```txt
gateway + auction-query-service
```

Belum termasuk:

```txt
bidding-command-service
wallet-service
auth-service
notification-service
```
