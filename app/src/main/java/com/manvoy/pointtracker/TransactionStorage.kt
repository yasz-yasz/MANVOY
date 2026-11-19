package com.manvoy.pointtracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TransactionStorage {
    private const val PREF_NAME = "manvoy_point_tracker_prefs"
    private const val KEY_TRANSACTIONS = "transactions_json"

    fun loadTransactions(context: Context): List<TransactionItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        val result = mutableListOf<TransactionItem>()

        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    TransactionItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        dateMillis = obj.getLong("dateMillis"),
                        amount = obj.getLong("amount"),
                        type = TransactionType.fromName(obj.getString("type"))
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }

        return result.sortedByDescending { it.dateMillis }
    }

    fun saveTransactions(context: Context, transactions: List<TransactionItem>) {
        val jsonArray = JSONArray()
        transactions.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("dateMillis", item.dateMillis)
                put("amount", item.amount)
                put("type", item.type.name)
            }
            jsonArray.put(obj)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TRANSACTIONS, jsonArray.toString()).apply()
    }
}
