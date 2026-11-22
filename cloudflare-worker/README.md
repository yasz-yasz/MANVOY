# Cloudflare Worker Sync Pribadi (Tracker Poin Manvoy)

Folder ini berisi backend serverless **Cloudflare Worker** + **KV Storage** agar data transaksi dan pengaturan periode billing di aplikasi Android **Tracker Poin Manvoy** dapat disinkronkan secara pribadi dan aman (dilindungi `SYNC_SECRET`).

## Cara Deploy Cepat (Lewat Cloudflare Dashboard — Tanpa CLI)

1. Buka [Cloudflare Dashboard](https://dash.cloudflare.com/) -> menu **Storage & Databases** -> **KV**.
2. Klik **Create a namespace**, beri nama misalnya `manvoy-sync-kv`.
3. Buka menu **Workers & Pages** -> **Create** -> **Create Worker**, beri nama misalnya `manvoy-sync`, lalu klik **Deploy**.
4. Klik **Edit code**, hapus isi default, lalu salin-tempel seluruh isi file `worker.js` dari folder ini, lalu klik **Deploy**.
5. Buka tab **Settings** pada Worker tersebut:
   - **Bindings** -> **Add** -> **KV Namespace**:
     - Variable name: `MANVOY_KV`
     - KV namespace: pilih `manvoy-sync-kv`
   - **Variables and Secrets** -> **Add**:
     - Type: **Secret**
     - Variable name: `SYNC_SECRET`
     - Value: buat password/token rahasia pribadimu (misal `manvoy-rahasia-2026-xyz`)
6. Klik **Deploy / Save**. Catat URL Worker kamu (contoh: `https://manvoy-sync.<subdomain>.workers.dev`).

## Cara Deploy via Wrangler CLI (Opsional)

```bash
cd cloudflare-worker
npx wrangler kv namespace create MANVOY_KV
# Masukkan ID namespace ke wrangler.toml
npx wrangler secret put SYNC_SECRET
npx wrangler deploy
```

## Konfigurasi di Aplikasi Android

1. Buka aplikasi **Tracker Poin Manvoy**.
2. Ketuk ikon **Cloud** di kanan atas (`TopAppBar`).
3. Isi:
   - **URL Cloudflare Worker**: `https://manvoy-sync.<subdomain>.workers.dev`
   - **Secret Token Pribadi**: token `SYNC_SECRET` yang kamu atur di Cloudflare
   - **ID Profil / Ruang Data**: `pribadi` (atau nama profil lain jika ingin memisahkan data)
4. Ketuk **Tes Koneksi** untuk memastikan Worker & Token aktif.
5. Ketuk **Sync Sekarang** atau aktifkan **Auto-Sync Otomatis**.

## Endpoint API

| Method | Endpoint | Keterangan |
| :--- | :--- | :--- |
| `GET` | `/health` | Cek status Worker, binding KV, dan secret |
| `GET` | `/sync/ping` | Tes autentikasi token (header `Authorization: Bearer <token>`) |
| `GET` | `/sync?key=pribadi` | Mengambil data transaksi, tombstone hapus, & konfigurasi billing |
| `PUT` / `POST` | `/sync?key=pribadi` | Menyimpan snapshot data hasil merge ke Cloudflare KV |
| `DELETE` | `/sync?key=pribadi` | Menghapus data profil tertentu dari Cloudflare KV |
