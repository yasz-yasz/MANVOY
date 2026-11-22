# Tracker Poin Manvoy

Aplikasi Android pencatat transaksi kartu kredit untuk menghitung perolehan poin **Mandiri × Marriott Bonvoy** secara otomatis, memantau siklus tagihan lewat **Tab Bar Periode Billing yang bisa dicustom**, **sinkronisasi pribadi ke Cloudflare Worker**, dan mengekspor laporan ke **PDF**.

## Fitur Utama
- **Tab Bar Periode Billing (Custom):**
  - Navigasi horizontal bergaya aplikasi perbankan (`16 Sep - 15 Okt`, `16 Okt - 15 Nov`, `Belum Tertagih`, serta tab `Semua` opsional).
  - Bisa dicustom **tanggal mulai siklus (1–31)**, **jumlah riwayat periode**, dan mendukung **Periode Custom Manual** dengan rentang tanggal bebas.
  - Ringkasan poin, progress target bonus Rp30.000.000, earning rate, filter kategori, dan ekspor PDF otomatis mengikuti periode billing yang sedang dipilih.
- **Sinkronisasi Pribadi ke Cloudflare Worker:**
  - Sinkronisasi dua arah (two-way merge dengan timestamp & tombstone penghapusan) untuk transaksi maupun pengaturan periode billing ke Worker pribadimu.
  - Dilindungi **Secret Token pribadi (`SYNC_SECRET`)** + dukungan multi-profil (`syncKey`).
  - Mendukung **Tes Koneksi**, **Auto-Sync Otomatis** (saat buka aplikasi, tiap perubahan data, serta interval berkala), **Upload Paksa**, dan **Ambil Cloud**.
  - Script backend siap pakai tersedia di folder `cloudflare-worker/` (`worker.js`, `wrangler.toml`, `README.md`).
- **Pencatatan & Edit Transaksi Lengkap:** Nama Transaksi, Tanggal Transaksi (Date Picker), Nominal Transaksi, dan Jenis Transaksi (Reguler, Airfare, Valas, Marriott). Ketuk kartu transaksi untuk mengedit.
- **Perhitungan Poin Otomatis** per kelipatan Rp20.000 (pembulatan ke bawah):
  - **Reguler**: 3 poin / Rp20.000
  - **Airfare, Valas, Marriott**: 5 poin / Rp20.000
  - Nominal di bawah Rp20.000 memperoleh 0 poin.
- **Bonus Target Rp30.000.000**: Otomatis menambahkan **+2.500 poin bonus** ketika akumulasi transaksi pada periode mencapai Rp30.000.000, lengkap dengan progress bar target.
- **Rata-rata Earning Rate**: Menghitung rata-rata perolehan poin dalam format **Rp / poin** dan **poin / Rp20.000**.
- **Ekspor PDF per Periode**: Mencetak laporan lengkap dengan keterangan periode billing aktif.

## Setup Sinkronisasi Cloudflare Worker
Lihat panduan lengkap di [`cloudflare-worker/README.md`](cloudflare-worker/README.md) untuk deploy `worker.js` dengan binding KV `MANVOY_KV` dan secret `SYNC_SECRET`.

## Build APK Otomatis (GitHub Actions)
1. Upload seluruh isi repository ini ke GitHub.
2. Buka tab **Actions** pada repository GitHub Anda.
3. Pilih workflow **Build APK Tracker Poin Manvoy** (otomatis jalan saat push ke branch `main`/`master` atau klik **Run workflow**).
4. Setelah selesai, unduh APK melalui bagian **Artifacts** (`Tracker-Poin-Manvoy-APK`).
