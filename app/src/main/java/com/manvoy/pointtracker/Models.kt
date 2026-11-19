package com.manvoy.pointtracker

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

enum class TransactionType(val label: String, val pointsPer20k: Int) {
    REGULER("Reguler", 3),
    AIRFARE("Airfare", 5),
    VALAS("Valas", 5),
    MARRIOTT("Marriott", 5);

    companion object {
        fun fromName(name: String): TransactionType {
            return entries.firstOrNull { it.name == name } ?: REGULER
        }
    }
}

data class TransactionItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateMillis: Long,
    val amount: Long,
    val type: TransactionType
) {
    val points: Long
        get() = calculateManvoyPoints(amount, type)
}

fun calculateManvoyPoints(amount: Long, type: TransactionType): Long {
    if (amount < 20_000L) return 0L
    val multiplesOf20k = amount / 20_000L
    return multiplesOf20k * type.pointsPer20k.toLong()
}

fun formatRupiah(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp ${formatter.format(amount)}"
}

fun formatNumber(value: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return formatter.format(value)
}

fun formatDateUtc(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
}

fun todayUtcMillis(): Long {
    val now = System.currentTimeMillis()
    val tzOffset = TimeZone.getDefault().getOffset(now)
    val localDays = (now + tzOffset) / 86_400_000L
    return localDays * 86_400_000L
}
