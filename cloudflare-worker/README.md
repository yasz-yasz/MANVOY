# Cloudflare Worker Sync Pribadi (Tracker Poin Manvoy)

Backend serverless **Cloudflare Worker** + **KV Storage** untuk sinkronisasi pribadi transaksi dan konfigurasi periode billing aplikasi Android **Tracker Poin Manvoy** (dilindungi `SYNC_SECRET`).

## Deploy Cepat via Cloudflare Dashboard
1. Buka Cloudflare Dashboard -> **Storage & Databases** -> **KV** -> buat namespace (mis. `manvoy-sync-kv`).
2. Buka **Workers & Pages** -> **Create** -> **Create Worker** -> **Edit code** -> salin isi `worker.js` -> **Deploy**.
3. Di tab **Settings** Worker:
   - **Bindings** -> **Add** -> **KV Namespace**: `MANVOY_KV` -> pilih namespace tadi.
   - **Variables and Secrets** -> **Add** -> **Secret**: `SYNC_SECRET` -> isi token rahasia pribadimu.
4. Simpan/deploy ulang lalu masukkan URL Worker & `SYNC_SECRET` ke menu **Cloud** di aplikasi Android.
