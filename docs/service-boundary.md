# Auction Query Service Boundary

## Tanggung Jawab

- Membaca daftar auction.
- Membaca detail auction.
- Membaca bid history.
- Menghitung field read model seperti `currentPrice`, `totalBids`, `nextMinimumBid`, `leadingBid`, dan `winningBid`.

## Tidak Ditangani

- `POST /api/auctions`.
- `POST /api/auctions/{auctionId}/activate`.
- `POST /api/auctions/{auctionId}/bids`.
- `POST /api/auctions/{auctionId}/close`.
- Wallet hold/release/capture.
- Event command-side.

## Catatan Risiko

Query service boleh menurunkan effective status untuk tampilan, tetapi command-side tetap harus menutup auction secara resmi agar wallet settlement dan event publish terjadi.

## Dependency

Fase awal masih membaca database yang sama secara read-only. Target berikutnya adalah projection/read model yang diisi dari event bidding command.
