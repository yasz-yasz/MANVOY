# Tracker Poin Manvoy

Aplikasi Android pencatat transaksi kartu kredit untuk menghitung perolehan poin **Mandiri × Marriott Bonvoy** secara otomatis, memantau siklus tagihan lewat **Tab Bar Periode Billing yang bisa dicustom**, **sinkronisasi pribadi ke Cloudflare Worker**, dan mengekspor laporan ke **PDF**.

## Fitur Utama
- **Tab Bar Periode Billing (Custom):** Navigasi horizontal siklus tagihan (`16 Sep - 15 Okt`, `16 Okt - 15 Nov`, `Belum Tertagih`, serta `Semua` opsional), custom tanggal mulai siklus (1–31), jumlah riwayat periode, dan periode custom manual.
- **Sinkronisasi Pribadi ke Cloudflare Worker:** Sinkronisasi dua arah (*two-way merge* dengan *tombstone* hapus) dilindungi `SYNC_SECRET`, tes koneksi, auto-sync, upload paksa, dan ambil cloud. Lihat [`cloudflare-worker/README.md`](cloudflare-worker/README.md).
- **Perhitungan Poin Otomatis:** Kelipatan penuh Rp20.000 (`Reguler`: 3 poin; `Airfare`, `Valas`, `Marriott`: 5 poin) + bonus **+2.500 poin** di akumulasi **Rp30.000.000** + rata-rata *earning rate* + ekspor PDF per periode.
