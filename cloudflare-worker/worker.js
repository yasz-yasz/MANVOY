/**
 * Tracker Poin Manvoy - Private Cloudflare Worker Sync API
 *
 * Bindings yang dibutuhkan di Cloudflare Worker:
 * 1. KV Namespace binding: MANVOY_KV
 * 2. Environment Secret / Variable: SYNC_SECRET (token rahasia pribadimu)
 */

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, PUT, POST, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Sync-Token, X-Sync-Key",
  "Access-Control-Max-Age": "86400",
};

function jsonResponse(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      ...CORS_HEADERS,
    },
  });
}

function constantTimeEquals(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  const encoder = new TextEncoder();
  const bufA = encoder.encode(a);
  const bufB = encoder.encode(b);
  if (bufA.length !== bufB.length) return false;
  let diff = 0;
  for (let i = 0; i < bufA.length; i++) {
    diff |= bufA[i] ^ bufB[i];
  }
  return diff === 0;
}

function extractToken(request) {
  const authHeader = request.headers.get("Authorization") || "";
  if (authHeader.toLowerCase().startsWith("bearer ")) {
    return authHeader.slice(7).trim();
  }
  return (request.headers.get("X-Sync-Token") || "").trim();
}

function sanitizeSyncKey(rawKey) {
  const cleaned = (rawKey || "pribadi")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]/g, "_")
    .slice(0, 48);
  return cleaned || "pribadi";
}

function storageKeyFor(syncKey) {
  return `manvoy:v1:${syncKey}`;
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";

    if (path === "/" || path === "/health") {
      return jsonResponse({
        ok: true,
        service: "tracker-poin-manvoy-sync",
        kvBound: Boolean(env.MANVOY_KV),
        secretConfigured: Boolean(env.SYNC_SECRET),
      });
    }

    if (!env.SYNC_SECRET) {
      return jsonResponse(
        { ok: false, error: "SYNC_SECRET belum diatur di Cloudflare Worker." },
        500
      );
    }

    if (!env.MANVOY_KV) {
      return jsonResponse(
        { ok: false, error: "KV Namespace binding MANVOY_KV belum terpasang." },
        500
      );
    }

    const providedToken = extractToken(request);
    if (!providedToken || !constantTimeEquals(providedToken, String(env.SYNC_SECRET).trim())) {
      return jsonResponse(
        { ok: false, error: "Unauthorized: Secret token tidak cocok." },
        401
      );
    }

    const rawKey = url.searchParams.get("key") || request.headers.get("X-Sync-Key") || "pribadi";
    const syncKey = sanitizeSyncKey(rawKey);
    const kvKey = storageKeyFor(syncKey);

    if (path === "/sync/ping" && request.method === "GET") {
      return jsonResponse({
        ok: true,
        message: "Authenticated",
        storage: "Cloudflare KV",
        key: syncKey,
        serverTime: Date.now(),
      });
    }

    if (path === "/sync") {
      if (request.method === "GET") {
        const raw = await env.MANVOY_KV.get(kvKey);
        if (!raw) {
          return jsonResponse({
            ok: true,
            exists: false,
            key: syncKey,
            data: null,
          });
        }
        try {
          const parsed = JSON.parse(raw);
          return jsonResponse({
            ok: true,
            exists: true,
            key: syncKey,
            data: parsed,
          });
        } catch {
          return jsonResponse(
            { ok: false, error: "Data tersimpan di KV rusak / bukan JSON valid." },
            500
          );
        }
      }

      if (request.method === "PUT" || request.method === "POST") {
        let body;
        try {
          body = await request.json();
        } catch {
          return jsonResponse({ ok: false, error: "Body request harus berupa JSON valid." }, 400);
        }

        if (!body || !Array.isArray(body.transactions)) {
          return jsonResponse(
            { ok: false, error: "Payload tidak valid: field 'transactions' wajib berupa array." },
            400
          );
        }

        const record = {
          schemaVersion: Number(body.schemaVersion) || 1,
          syncKey,
          deviceId: typeof body.deviceId === "string" ? body.deviceId : "unknown",
          updatedAt: Number(body.updatedAt) || Date.now(),
          serverSavedAt: Date.now(),
          transactions: body.transactions,
          deletedIds: body.deletedIds && typeof body.deletedIds === "object" ? body.deletedIds : {},
          billingConfig:
            body.billingConfig && typeof body.billingConfig === "object"
              ? body.billingConfig
              : null,
        };

        await env.MANVOY_KV.put(kvKey, JSON.stringify(record));

        return jsonResponse({
          ok: true,
          key: syncKey,
          updatedAt: record.updatedAt,
          transactionCount: record.transactions.length,
        });
      }

      if (request.method === "DELETE") {
        await env.MANVOY_KV.delete(kvKey);
        return jsonResponse({ ok: true, deleted: true, key: syncKey });
      }
    }

    return jsonResponse({ ok: false, error: "Endpoint tidak ditemukan." }, 404);
  },
};
