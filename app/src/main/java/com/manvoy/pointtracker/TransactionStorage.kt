package com.manvoy.pointtracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object TransactionStorage {
    private const val PREF_NAME = "manvoy_point_tracker_prefs"
    private const val KEY_TRANSACTIONS = "transactions_json"
    private const val KEY_DELETED_IDS = "deleted_transactions_json"
    private const val KEY_BILLING_CONFIG = "billing_config_json"
    private const val KEY_SYNC_CONFIG = "cloud_sync_config_json"
    private const val KEY_DEVICE_ID = "sync_device_id"

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "android-${UUID.randomUUID().toString().take(8)}"
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    fun loadTransactions(context: Context): List<TransactionItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        return try {
            jsonArrayToTransactions(JSONArray(rawJson))
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveTransactions(context: Context, transactions: List<TransactionItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TRANSACTIONS, transactionsToJsonArray(transactions).toString()).apply()
    }

    fun loadDeletedTransactions(context: Context): Map<String, Long> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_DELETED_IDS, null) ?: return emptyMap()
        return try {
            jsonObjectToDeletedMap(JSONObject(rawJson))
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveDeletedTransactions(context: Context, deletedMap: Map<String, Long>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DELETED_IDS, deletedMapToJsonObject(deletedMap).toString()).apply()
    }

    fun loadBillingConfig(context: Context): BillingConfig {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_BILLING_CONFIG, null) ?: return BillingConfig()
        return try {
            jsonObjectToBillingConfig(JSONObject(rawJson))
        } catch (_: Exception) {
            BillingConfig()
        }
    }

    fun saveBillingConfig(context: Context, config: BillingConfig) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BILLING_CONFIG, billingConfigToJsonObject(config).toString()).apply()
    }

    fun loadSyncConfig(context: Context): CloudSyncConfig {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_SYNC_CONFIG, null) ?: return CloudSyncConfig()
        return try {
            val obj = JSONObject(rawJson)
            CloudSyncConfig(
                workerUrl = obj.optString("workerUrl", ""),
                apiToken = obj.optString("apiToken", ""),
                syncKey = obj.optString("syncKey", "pribadi").ifBlank { "pribadi" },
                autoSyncEnabled = obj.optBoolean("autoSyncEnabled", false),
                autoSyncIntervalMinutes = obj.optInt("autoSyncIntervalMinutes", 15).coerceIn(0, 1440),
                lastSyncAt = obj.optLong("lastSyncAt", 0L),
                lastSyncMessage = obj.optString("lastSyncMessage", "")
            )
        } catch (_: Exception) {
            CloudSyncConfig()
        }
    }

    fun saveSyncConfig(context: Context, config: CloudSyncConfig) {
        val obj = JSONObject().apply {
            put("workerUrl", config.workerUrl.trim())
            put("apiToken", config.apiToken.trim())
            put("syncKey", config.syncKey.trim().ifBlank { "pribadi" })
            put("autoSyncEnabled", config.autoSyncEnabled)
            put("autoSyncIntervalMinutes", config.autoSyncIntervalMinutes)
            put("lastSyncAt", config.lastSyncAt)
            put("lastSyncMessage", config.lastSyncMessage)
        }
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SYNC_CONFIG, obj.toString()).apply()
    }

    fun transactionsToJsonArray(transactions: List<TransactionItem>): JSONArray {
        val jsonArray = JSONArray()
        transactions.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("dateMillis", item.dateMillis)
                put("amount", item.amount)
                put("type", item.type.name)
                put("updatedAt", item.updatedAt)
            }
            jsonArray.put(obj)
        }
        return jsonArray
    }

    fun jsonArrayToTransactions(jsonArray: JSONArray): List<TransactionItem> {
        val result = mutableListOf<TransactionItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val id = obj.optString("id", "")
            val name = obj.optString("name", "")
            if (id.isBlank() || name.isBlank()) continue
            val dateMillis = obj.optLong("dateMillis", todayUtcMillis())
            val amount = obj.optLong("amount", 0L)
            val type = TransactionType.fromName(obj.optString("type", TransactionType.REGULER.name))
            val updatedAt = obj.optLong("updatedAt", dateMillis)
            result.add(
                TransactionItem(
                    id = id,
                    name = name,
                    dateMillis = dateMillis,
                    amount = amount,
                    type = type,
                    updatedAt = updatedAt
                )
            )
        }
        return result.sortedWith(
            compareByDescending<TransactionItem> { it.dateMillis }
                .thenByDescending { it.updatedAt }
        )
    }

    fun deletedMapToJsonObject(deletedMap: Map<String, Long>): JSONObject {
        val obj = JSONObject()
        deletedMap.forEach { (id, ts) ->
            if (id.isNotBlank()) obj.put(id, ts)
        }
        return obj
    }

    fun jsonObjectToDeletedMap(obj: JSONObject?): Map<String, Long> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, Long>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = obj.optLong(key, 0L)
        }
        return result
    }

    fun billingConfigToJsonObject(config: BillingConfig): JSONObject {
        val customArr = JSONArray()
        config.customPeriods.forEach { cp ->
            customArr.put(
                JSONObject().apply {
                    put("id", cp.id)
                    put("label", cp.label)
                    put("startMillis", cp.startMillis)
                    put("endMillis", cp.endMillis)
                }
            )
        }
        return JSONObject().apply {
            put("startDay", config.startDay.coerceIn(1, 31))
            put("pastPeriodsCount", config.pastPeriodsCount.coerceIn(1, 24))
            put("showAllTab", config.showAllTab)
            put("customPeriods", customArr)
            put("updatedAt", config.updatedAt)
        }
    }

    fun jsonObjectToBillingConfig(obj: JSONObject?): BillingConfig {
        if (obj == null) return BillingConfig()
        val customList = mutableListOf<CustomBillingPeriod>()
        val customArr = obj.optJSONArray("customPeriods")
        if (customArr != null) {
            for (i in 0 until customArr.length()) {
                val cp = customArr.optJSONObject(i) ?: continue
                val id = cp.optString("id", UUID.randomUUID().toString())
                val label = cp.optString("label", "")
                val startMillis = cp.optLong("startMillis", todayUtcMillis())
                val endMillis = cp.optLong("endMillis", startMillis)
                customList.add(
                    CustomBillingPeriod(
                        id = id,
                        label = label,
                        startMillis = startMillis,
                        endMillis = endMillis
                    )
                )
            }
        }
        return BillingConfig(
            startDay = obj.optInt("startDay", 16).coerceIn(1, 31),
            pastPeriodsCount = obj.optInt("pastPeriodsCount", 6).coerceIn(1, 24),
            showAllTab = obj.optBoolean("showAllTab", true),
            customPeriods = customList,
            updatedAt = obj.optLong("updatedAt", 0L)
        )
    }
}
