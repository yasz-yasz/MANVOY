package com.manvoy.pointtracker

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.roundToLong

const val POINT_MULTIPLE_AMOUNT = 20_000L
const val BONUS_THRESHOLD_AMOUNT = 30_000_000L
const val BONUS_POINTS_AMOUNT = 2_500L

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

data class PointsSummary(
    val transactionCount: Int,
    val totalAmount: Long,
    val basePoints: Long,
    val bonusPoints: Long,
    val totalPoints: Long,
    val remainingForBonus: Long,
    val bonusProgress: Float,
    val isBonusUnlocked: Boolean,
    val rupiahPerPoint: Double?,
    val pointsPer20k: Double?
)

fun calculateManvoyPoints(amount: Long, type: TransactionType): Long {
    if (amount < POINT_MULTIPLE_AMOUNT) return 0L
    val multiplesOf20k = amount / POINT_MULTIPLE_AMOUNT
    return multiplesOf20k * type.pointsPer20k.toLong()
}

fun calculatePointsSummary(transactions: List<TransactionItem>): PointsSummary {
    val totalAmount = transactions.sumOf { it.amount }
    val basePoints = transactions.sumOf { it.points }
    val isBonusUnlocked = totalAmount >= BONUS_THRESHOLD_AMOUNT
    val bonusPoints = if (isBonusUnlocked) BONUS_POINTS_AMOUNT else 0L
    val totalPoints = basePoints + bonusPoints
    val remainingForBonus = (BONUS_THRESHOLD_AMOUNT - totalAmount).coerceAtLeast(0L)
    val bonusProgress = (totalAmount.toFloat() / BONUS_THRESHOLD_AMOUNT.toFloat()).coerceIn(0f, 1f)

    val rupiahPerPoint = if (totalPoints > 0L) {
        totalAmount.toDouble() / totalPoints.toDouble()
    } else {
        null
    }

    val pointsPer20k = if (totalAmount > 0L) {
        totalPoints.toDouble() / (totalAmount.toDouble() / POINT_MULTIPLE_AMOUNT.toDouble())
    } else {
        null
    }

    return PointsSummary(
        transactionCount = transactions.size,
        totalAmount = totalAmount,
        basePoints = basePoints,
        bonusPoints = bonusPoints,
        totalPoints = totalPoints,
        remainingForBonus = remainingForBonus,
        bonusProgress = bonusProgress,
        isBonusUnlocked = isBonusUnlocked,
        rupiahPerPoint = rupiahPerPoint,
        pointsPer20k = pointsPer20k
    )
}

fun formatRupiah(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp ${formatter.format(amount)}"
}

fun formatNumber(value: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return formatter.format(value)
}

fun formatRupiahPerPoint(value: Double?): String {
    if (value == null) return "-"
    val rounded = value.roundToLong()
    return "${formatRupiah(rounded)} / poin"
}

fun formatPointsPer20k(value: Double?): String {
    if (value == null) return "-"
    return String.format(Locale("id", "ID"), "%.2f poin / Rp20rb", value)
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
