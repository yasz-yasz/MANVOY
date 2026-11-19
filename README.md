# Tracker Poin Manvoy

Aplikasi Android pencatat transaksi kartu kredit untuk menghitung perolehan poin **Mandiri × Marriott Bonvoy** secara otomatis dan mengekspor laporan ke **PDF**.

## Fitur Utama
- Catat transaksi lengkap: Nama Transaksi, Tanggal Transaksi (Date Picker), Nominal Transaksi, dan Jenis Transaksi (Reguler, Airfare, Valas, Marriott).
- Perhitungan poin otomatis per kelipatan Rp20.000 (pembulatan ke bawah):
  - **Reguler**: 3 poin / Rp20.000
  - **Airfare, Valas, Marriott**: 5 poin / Rp20.000
  - Nominal di bawah Rp20.000 memperoleh 0 poin.
- Ringkasan total transaksi, total nominal, dan total poin keseluruhan.
- Filter riwayat transaksi per kategori.
- Ekspor laporan transaksi dan ringkasan poin ke file **PDF**.

## Build APK Otomatis (GitHub Actions)
1. Upload seluruh isi repository ini ke GitHub.
2. Buka tab **Actions** pada repository GitHub Anda.
3. Pilih workflow **Build APK Tracker Poin Manvoy** (otomatis jalan saat push ke branch `main`/`master` atau klik **Run workflow**).
4. Setelah selesai, unduh APK melalui bagian **Artifacts** (`Tracker-Poin-Manvoy-APK`).
