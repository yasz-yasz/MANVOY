package com.manvoy.pointtracker

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.roundToLong

const val POINT_MULTIPLE_AMOUNT = 20_000L
const val BONUS_THRESHOLD_AMOUNT = 30_000_000L
const val BONUS_POINTS_AMOUNT = 2_500L

const val TAB_ID_ALL = "all"
const val TAB_ID_UNBILLED = "unbilled"

private val SHORT_MONTHS_ID = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agt", "Sep", "Okt", "Nov", "Des"
)

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
    val type: TransactionType,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val points: Long
        get() = calculateManvoyPoints(amount, type)
}

data class CustomBillingPeriod(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val startMillis: Long,
    val endMillis: Long
)

data class BillingConfig(
    val startDay: Int = 16,
    val pastPeriodsCount: Int = 6,
    val showAllTab: Boolean = true,
    val customPeriods: List<CustomBillingPeriod> = emptyList(),
    val updatedAt: Long = 0L
)

data class BillingPeriodTab(
    val id: String,
    val label: String,
    val rangeLabel: String,
    val startMillis: Long?,
    val endMillis: Long?,
    val isUnbilled: Boolean = false,
    val isCustom: Boolean = false,
    val isAll: Boolean = false
)

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

fun utcMillisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}

fun localDateToUtcMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun todayLocalDate(): LocalDate {
    return LocalDate.now(ZoneId.systemDefault())
}

fun todayUtcMillis(): Long {
    return localDateToUtcMillis(todayLocalDate())
}

fun formatShortDayMonth(date: LocalDate, includeYear: Boolean = false): String {
    val monthStr = SHORT_MONTHS_ID[date.monthValue - 1]
    return if (includeYear) {
        "${date.dayOfMonth} $monthStr ${date.year % 100}"
    } else {
        "${date.dayOfMonth} $monthStr"
    }
}

fun formatFullDayMonthYear(date: LocalDate): String {
    val monthStr = SHORT_MONTHS_ID[date.monthValue - 1]
    return "${date.dayOfMonth} $monthStr ${date.year}"
}

fun formatPeriodTabLabel(start: LocalDate, end: LocalDate, referenceYear: Int): String {
    val showYear = start.year != referenceYear || end.year != referenceYear
    return if (showYear && start.year != end.year) {
        "${formatShortDayMonth(start, true)} - ${formatShortDayMonth(end, true)}"
    } else if (showYear) {
        "${formatShortDayMonth(start, false)} - ${formatShortDayMonth(end, true)}"
    } else {
        "${formatShortDayMonth(start, false)} - ${formatShortDayMonth(end, false)}"
    }
}

fun formatPeriodRangeLabel(start: LocalDate, end: LocalDate): String {
    return if (start.year == end.year) {
        "${formatShortDayMonth(start, false)} - ${formatFullDayMonthYear(end)}"
    } else {
        "${formatFullDayMonthYear(start)} - ${formatFullDayMonthYear(end)}"
    }
}

fun getBillingCycleBounds(startYearMonth: YearMonth, startDay: Int): Pair<LocalDate, LocalDate> {
    val clampedDay = startDay.coerceIn(1, 31)
    val startDate = startYearMonth.atDay(minOf(clampedDay, startYearMonth.lengthOfMonth()))
    val nextYm = startYearMonth.plusMonths(1)
    val nextStartDate = nextYm.atDay(minOf(clampedDay, nextYm.lengthOfMonth()))
    val endDate = nextStartDate.minusDays(1)
    return startDate to endDate
}

fun findCycleStartYearMonth(date: LocalDate, startDay: Int): YearMonth {
    val clampedDay = startDay.coerceIn(1, 31)
    val ym = YearMonth.from(date)
    val cycleStartInYm = ym.atDay(minOf(clampedDay, ym.lengthOfMonth()))
    return if (!date.isBefore(cycleStartInYm)) ym else ym.minusMonths(1)
}

fun buildBillingPeriodTabs(
    config: BillingConfig,
    transactions: List<TransactionItem>,
    today: LocalDate = todayLocalDate()
): List<BillingPeriodTab> {
    val tabs = mutableListOf<BillingPeriodTab>()
    val startDay = config.startDay.coerceIn(1, 31)
    val pastCount = config.pastPeriodsCount.coerceIn(1, 24)

    if (config.showAllTab) {
        tabs.add(
            BillingPeriodTab(
                id = TAB_ID_ALL,
                label = "Semua",
                rangeLabel = "Semua Periode",
                startMillis = null,
                endMillis = null,
                isAll = true
            )
        )
    }

    val currentCycleYm = findCycleStartYearMonth(today, startDay)
    val defaultOldestYm = currentCycleYm.minusMonths(pastCount.toLong())
    val minAllowedYm = currentCycleYm.minusMonths(36L)

    val oldestTxYm = transactions
        .minOfOrNull { it.dateMillis }
        ?.let { findCycleStartYearMonth(utcMillisToLocalDate(it), startDay) }
        ?.let { if (it.isBefore(minAllowedYm)) minAllowedYm else it }

    val startYm = if (oldestTxYm != null && oldestTxYm.isBefore(defaultOldestYm)) {
        oldestTxYm
    } else {
        defaultOldestYm
    }

    var cursorYm = startYm
    while (cursorYm.isBefore(currentCycleYm)) {
        val (startDate, endDate) = getBillingCycleBounds(cursorYm, startDay)
        tabs.add(
            BillingPeriodTab(
                id = "cycle_${cursorYm.year}_${cursorYm.monthValue}",
                label = formatPeriodTabLabel(startDate, endDate, today.year),
                rangeLabel = formatPeriodRangeLabel(startDate, endDate),
                startMillis = localDateToUtcMillis(startDate),
                endMillis = localDateToUtcMillis(endDate)
            )
        )
        cursorYm = cursorYm.plusMonths(1)
    }

    val (currentStart, currentEnd) = getBillingCycleBounds(currentCycleYm, startDay)
    tabs.add(
        BillingPeriodTab(
            id = TAB_ID_UNBILLED,
            label = "Belum Tertagih",
            rangeLabel = formatPeriodRangeLabel(currentStart, currentEnd),
            startMillis = localDateToUtcMillis(currentStart),
            endMillis = localDateToUtcMillis(currentEnd),
            isUnbilled = true
        )
    )

    config.customPeriods.forEach { custom ->
        val startDate = utcMillisToLocalDate(minOf(custom.startMillis, custom.endMillis))
        val endDate = utcMillisToLocalDate(maxOf(custom.startMillis, custom.endMillis))
        val cleanLabel = custom.label.ifBlank {
            formatPeriodTabLabel(startDate, endDate, today.year)
        }
        tabs.add(
            BillingPeriodTab(
                id = "custom_${custom.id}",
                label = cleanLabel,
                rangeLabel = formatPeriodRangeLabel(startDate, endDate),
                startMillis = localDateToUtcMillis(startDate),
                endMillis = localDateToUtcMillis(endDate),
                isCustom = true
            )
        )
    }

    return tabs
}

fun filterTransactionsByPeriod(
    transactions: List<TransactionItem>,
    tab: BillingPeriodTab
): List<TransactionItem> {
    if (tab.isAll) return transactions
    val start = tab.startMillis ?: Long.MIN_VALUE
    val end = tab.endMillis ?: Long.MAX_VALUE
    return if (tab.isUnbilled) {
        transactions.filter { it.dateMillis >= start }
    } else {
        transactions.filter { it.dateMillis in start..end }
    }
}

fun defaultDateMillisForTab(tab: BillingPeriodTab): Long {
    val today = todayUtcMillis()
    if (tab.isAll || tab.isUnbilled) return today
    val start = tab.startMillis ?: return today
    val end = tab.endMillis ?: return today
    return if (today in start..end) today else end
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

fun formatDateTimeLocal(millis: Long): String {
    if (millis <= 0L) return "Belum pernah"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(millis))
}
