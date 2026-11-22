package com.manvoy.pointtracker

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CloudSyncConfig(
    val workerUrl: String = "",
    val apiToken: String = "",
    val syncKey: String = "pribadi",
    val autoSyncEnabled: Boolean = false,
    val autoSyncIntervalMinutes: Int = 15,
    val lastSyncAt: Long = 0L,
    val lastSyncMessage: String = ""
) {
    val isConfigured: Boolean
        get() = workerUrl.trim().isNotEmpty() && apiToken.trim().isNotEmpty()
}

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class SyncResult(
    val success: Boolean,
    val message: String,
    val transactions: List<TransactionItem>? = null,
    val deletedIds: Map<String, Long>? = null,
    val billingConfig: BillingConfig? = null,
    val syncedAt: Long = System.currentTimeMillis()
)

private data class HttpResponseData(
    val code: Int,
    val body: String
)

private data class RemotePayload(
    val exists: Boolean,
    val transactions: List<TransactionItem>,
    val deletedIds: Map<String, Long>,
    val billingConfig: BillingConfig?
)

object CloudSyncManager {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000
    private const val TOMBSTONE_RETENTION_MS = 90L * 24L * 60L * 60L * 1000L

    fun normalizeBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun buildEndpoint(baseUrl: String, path: String, syncKey: String? = null): String {
        val cleanBase = normalizeBaseUrl(baseUrl)
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        if (syncKey == null) return "$cleanBase$cleanPath"
        val encodedKey = URLEncoder.encode(syncKey.trim().ifBlank { "pribadi" }, "UTF-8")
        return "$cleanBase$cleanPath?key=$encodedKey"
    }

    private fun executeRequest(
        endpoint: String,
        method: String,
        apiToken: String,
        jsonBody: String? = null
    ): HttpResponseData {
        val url = URL(endpoint)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer ${apiToken.trim()}")
            setRequestProperty("X-Sync-Token", apiToken.trim())
            setRequestProperty("Accept", "application/json")
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (jsonBody != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }
            return HttpResponseData(code, body)
        } finally {
            connection.disconnect()
        }
    }

    private fun describeHttpError(code: Int, body: String): String {
        val serverMsg = try {
            JSONObject(body).optString("error", "").ifBlank {
                JSONObject(body).optString("message", "")
            }
        } catch (_: Exception) {
            ""
        }

        return when (code) {
            401, 403 -> "Token tidak valid (HTTP $code). Periksa Secret Token."
            404 -> "Endpoint tidak ditemukan (HTTP 404). Periksa URL Cloudflare Worker."
            413 -> "Ukuran data terlalu besar (HTTP 413)."
            in 500..599 -> {
                if (serverMsg.isNotBlank()) "Error Worker (HTTP $code): $serverMsg"
                else "Error server Cloudflare Worker (HTTP $code). Cek binding KV & secret."
            }
            else -> {
                if (serverMsg.isNotBlank()) "Gagal (HTTP $code): $serverMsg"
                else "Permintaan gagal (HTTP $code)."
            }
        }
    }

    suspend fun testConnection(config: CloudSyncConfig): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (config.workerUrl.isBlank()) {
                return@withContext false to "URL Cloudflare Worker belum diisi."
            }
            if (config.apiToken.isBlank()) {
                return@withContext false to "Secret Token pribadi belum diisi."
            }

            try {
                val endpoint = buildEndpoint(config.workerUrl, "/sync/ping", config.syncKey)
                val response = executeRequest(endpoint, "GET", config.apiToken)
                if (response.code in 200..299) {
                    val storageType = try {
                        JSONObject(response.body).optString("storage", "KV")
                    } catch (_: Exception) {
                        "KV"
                    }
                    true to "Koneksi berhasil! Worker aktif ($storageType) dan token valid."
                } else {
                    false to describeHttpError(response.code, response.body)
                }
            } catch (e: Exception) {
                false to "Gagal terhubung: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }

    private fun parseRemotePayload(response: HttpResponseData): RemotePayload {
        if (response.code == 404) {
            return RemotePayload(
                exists = false,
                transactions = emptyList(),
                deletedIds = emptyMap(),
                billingConfig = null
            )
        }
        val root = JSONObject(response.body)
        val exists = root.optBoolean("exists", true)
        val dataObj = root.optJSONObject("data") ?: root
        if (!exists || (dataObj.length() == 0)) {
            return RemotePayload(
                exists = false,
                transactions = emptyList(),
                deletedIds = emptyMap(),
                billingConfig = null
            )
        }

        val txArray = dataObj.optJSONArray("transactions") ?: JSONArray()
        val transactions = TransactionStorage.jsonArrayToTransactions(txArray)
        val deletedIds = TransactionStorage.jsonObjectToDeletedMap(dataObj.optJSONObject("deletedIds"))
        val billingObj = dataObj.optJSONObject("billingConfig")
        val billingConfig = if (billingObj != null) {
            TransactionStorage.jsonObjectToBillingConfig(billingObj)
        } else {
            null
        }

        return RemotePayload(
            exists = true,
            transactions = transactions,
            deletedIds = deletedIds,
            billingConfig = billingConfig
        )
    }

    private fun buildUploadBody(
        deviceId: String,
        syncKey: String,
        transactions: List<TransactionItem>,
        deletedIds: Map<String, Long>,
        billingConfig: BillingConfig,
        nowMillis: Long
    ): String {
        val payload = JSONObject().apply {
            put("schemaVersion", 1)
            put("syncKey", syncKey.trim().ifBlank { "pribadi" })
            put("deviceId", deviceId)
            put("updatedAt", nowMillis)
            put("transactions", TransactionStorage.transactionsToJsonArray(transactions))
            put("deletedIds", TransactionStorage.deletedMapToJsonObject(deletedIds))
            put("billingConfig", TransactionStorage.billingConfigToJsonObject(billingConfig))
        }
        return payload.toString()
    }

    private fun pruneOldTombstones(deletedIds: Map<String, Long>, nowMillis: Long): Map<String, Long> {
        val cutoff = nowMillis - TOMBSTONE_RETENTION_MS
        return deletedIds.filterValues { it >= cutoff }
    }

    private fun mergeData(
        localTransactions: List<TransactionItem>,
        localDeleted: Map<String, Long>,
        localBillingConfig: BillingConfig,
        remote: RemotePayload,
        nowMillis: Long
    ): Triple<List<TransactionItem>, Map<String, Long>, BillingConfig> {
        val mergedDeleted = mutableMapOf<String, Long>()
        localDeleted.forEach { (id, ts) ->
            mergedDeleted[id] = maxOf(mergedDeleted[id] ?: 0L, ts)
        }
        remote.deletedIds.forEach { (id, ts) ->
            mergedDeleted[id] = maxOf(mergedDeleted[id] ?: 0L, ts)
        }

        val mergedTxMap = mutableMapOf<String, TransactionItem>()
        localTransactions.forEach { item ->
            mergedTxMap[item.id] = item
        }
        remote.transactions.forEach { remoteItem ->
            val existing = mergedTxMap[remoteItem.id]
            if (existing == null || remoteItem.updatedAt > existing.updatedAt) {
                mergedTxMap[remoteItem.id] = remoteItem
            }
        }

        val finalTransactions = mergedTxMap.values
            .filter { item ->
                val deletedAt = mergedDeleted[item.id]
                deletedAt == null || item.updatedAt > deletedAt
            }
            .sortedWith(
                compareByDescending<TransactionItem> { it.dateMillis }
                    .thenByDescending { it.updatedAt }
            )

        val remoteBilling = remote.billingConfig
        val finalBilling = when {
            remoteBilling == null -> localBillingConfig
            remoteBilling.updatedAt > localBillingConfig.updatedAt -> remoteBilling
            else -> localBillingConfig
        }

        val cleanedDeleted = pruneOldTombstones(mergedDeleted, nowMillis)
        return Triple(finalTransactions, cleanedDeleted, finalBilling)
    }

    suspend fun syncTwoWay(
        context: Context,
        config: CloudSyncConfig,
        localTransactions: List<TransactionItem>,
        localDeleted: Map<String, Long>,
        localBillingConfig: BillingConfig
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!config.isConfigured) {
            return@withContext SyncResult(
                success = false,
                message = "Lengkapi URL Cloudflare Worker dan Secret Token terlebih dahulu."
            )
        }

        try {
            val nowMillis = System.currentTimeMillis()
            val syncKey = config.syncKey.trim().ifBlank { "pribadi" }
            val endpoint = buildEndpoint(config.workerUrl, "/sync", syncKey)

            val getResp = executeRequest(endpoint, "GET", config.apiToken)
            if (getResp.code !in 200..299 && getResp.code != 404) {
                return@withContext SyncResult(
                    success = false,
                    message = describeHttpError(getResp.code, getResp.body)
                )
            }

            val remote = parseRemotePayload(getResp)

            val (mergedTransactions, mergedDeleted, mergedBilling) = mergeData(
                localTransactions = localTransactions,
                localDeleted = localDeleted,
                localBillingConfig = localBillingConfig,
                remote = remote,
                nowMillis = nowMillis
            )

            val deviceId = TransactionStorage.getOrCreateDeviceId(context)
            val bodyJson = buildUploadBody(
                deviceId = deviceId,
                syncKey = syncKey,
                transactions = mergedTransactions,
                deletedIds = mergedDeleted,
                billingConfig = mergedBilling,
                nowMillis = nowMillis
            )

            val putResp = executeRequest(endpoint, "PUT", config.apiToken, bodyJson)
            if (putResp.code !in 200..299) {
                return@withContext SyncResult(
                    success = false,
                    message = describeHttpError(putResp.code, putResp.body)
                )
            }

            TransactionStorage.saveTransactions(context, mergedTransactions)
            TransactionStorage.saveDeletedTransactions(context, mergedDeleted)
            TransactionStorage.saveBillingConfig(context, mergedBilling)

            SyncResult(
                success = true,
                message = "Sinkronisasi berhasil (${mergedTransactions.size} transaksi).",
                transactions = mergedTransactions,
                deletedIds = mergedDeleted,
                billingConfig = mergedBilling,
                syncedAt = nowMillis
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Sync gagal: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    suspend fun pushOverwrite(
        context: Context,
        config: CloudSyncConfig,
        localTransactions: List<TransactionItem>,
        localDeleted: Map<String, Long>,
        localBillingConfig: BillingConfig
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!config.isConfigured) {
            return@withContext SyncResult(
                success = false,
                message = "Lengkapi URL Cloudflare Worker dan Secret Token terlebih dahulu."
            )
        }

        try {
            val nowMillis = System.currentTimeMillis()
            val syncKey = config.syncKey.trim().ifBlank { "pribadi" }
            val endpoint = buildEndpoint(config.workerUrl, "/sync", syncKey)
            val deviceId = TransactionStorage.getOrCreateDeviceId(context)
            val cleanedDeleted = pruneOldTombstones(localDeleted, nowMillis)
            val stampedBilling = localBillingConfig.copy(
                updatedAt = maxOf(localBillingConfig.updatedAt, nowMillis)
            )

            val bodyJson = buildUploadBody(
                deviceId = deviceId,
                syncKey = syncKey,
                transactions = localTransactions,
                deletedIds = cleanedDeleted,
                billingConfig = stampedBilling,
                nowMillis = nowMillis
            )

            val putResp = executeRequest(endpoint, "PUT", config.apiToken, bodyJson)
            if (putResp.code !in 200..299) {
                return@withContext SyncResult(
                    success = false,
                    message = describeHttpError(putResp.code, putResp.body)
                )
            }

            TransactionStorage.saveBillingConfig(context, stampedBilling)
            TransactionStorage.saveDeletedTransactions(context, cleanedDeleted)

            SyncResult(
                success = true,
                message = "Upload selesai! Cloud ditimpa dengan ${localTransactions.size} transaksi lokal.",
                transactions = localTransactions,
                deletedIds = cleanedDeleted,
                billingConfig = stampedBilling,
                syncedAt = nowMillis
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Upload gagal: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    suspend fun pullOverwrite(
        context: Context,
        config: CloudSyncConfig
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!config.isConfigured) {
            return@withContext SyncResult(
                success = false,
                message = "Lengkapi URL Cloudflare Worker dan Secret Token terlebih dahulu."
            )
        }

        try {
            val nowMillis = System.currentTimeMillis()
            val syncKey = config.syncKey.trim().ifBlank { "pribadi" }
            val endpoint = buildEndpoint(config.workerUrl, "/sync", syncKey)
            val getResp = executeRequest(endpoint, "GET", config.apiToken)
            if (getResp.code == 404) {
                return@withContext SyncResult(
                    success = false,
                    message = "Belum ada data tersimpan di cloud untuk key '$syncKey'."
                )
            }
            if (getResp.code !in 200..299) {
                return@withContext SyncResult(
                    success = false,
                    message = describeHttpError(getResp.code, getResp.body)
                )
            }

            val remote = parseRemotePayload(getResp)
            if (!remote.exists) {
                return@withContext SyncResult(
                    success = false,
                    message = "Belum ada data tersimpan di cloud untuk key '$syncKey'."
                )
            }

            val billingToSave = remote.billingConfig ?: TransactionStorage.loadBillingConfig(context)
            TransactionStorage.saveTransactions(context, remote.transactions)
            TransactionStorage.saveDeletedTransactions(context, remote.deletedIds)
            TransactionStorage.saveBillingConfig(context, billingToSave)

            SyncResult(
                success = true,
                message = "Download selesai! Memuat ${remote.transactions.size} transaksi dari cloud.",
                transactions = remote.transactions,
                deletedIds = remote.deletedIds,
                billingConfig = billingToSave,
                syncedAt = nowMillis
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Download gagal: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }
}
