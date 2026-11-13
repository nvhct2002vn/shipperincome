package com.example.shipperincome.ui.viewmodel

import com.example.shipperincome.data.model.Shift
import com.example.shipperincome.utils.DateFull
import com.example.shipperincome.utils.DateShort
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class StatsPeriod(val label: String) {
    CURRENT_WEEK("Tuần này"),
    PREVIOUS_WEEK("Tuần trước"),
    CURRENT_MONTH("Tháng này"),
    PREVIOUS_MONTH("Tháng trước"),
    CURRENT_YEAR("Năm nay"),
    ALL_TIME("Tất cả"),
    SPECIFIC_DAY("Chọn ngày"),
    DATE_RANGE("Khoảng ngày")
}

data class DailySummary(
    val date: LocalDate,
    val shiftCount: Int,
    val totalHours: Double,
    val totalPlusGrossIncome: Double,
    val totalNormalGrossIncome: Double,
    val totalGrossIncome: Double,
    val totalPlusNetIncome: Double,
    val totalNormalNetIncome: Double,
    val totalNetIncome: Double,
    val averageHourlyRate: Double,
    val totalDeliveries: Int = 0
)

data class StatsSummary(
    val periodLabel: String,
    val totalDays: Long,
    val totalShifts: Int,
    val totalHours: Double,
    val totalPlusGrossIncome: Double,
    val totalNormalGrossIncome: Double,
    val totalGrossIncome: Double,
    val totalPlusNetIncome: Double,
    val totalNormalNetIncome: Double,
    val totalNetIncome: Double,
    val averageHourlyRate: Double,
    val totalDeliveries: Int = 0
)

/** Mot cot tren bieu do: theo ngay, hoac theo thang khi giai doan qua dai. */
data class ChartPoint(val label: String, val netIncome: Double)

/** Hieu qua theo thu trong tuan: chi tinh nhung ngay co di lam. */
data class WeekdayStat(
    val day: DayOfWeek,
    val workedDays: Int,
    val totalHours: Double,
    val totalNetIncome: Double
) {
    val avgNetPerDay: Double get() = if (workedDays > 0) totalNetIncome / workedDays else 0.0
    val hourlyRate: Double get() = if (totalHours > 0) totalNetIncome / totalHours else 0.0
}

data class StatsData(
    val start: LocalDate,
    val end: LocalDate,
    val summary: StatsSummary?,
    val days: List<DailySummary>,
    val chart: List<ChartPoint>,
    val chartByMonth: Boolean,
    /** Thu Hai -> Chu nhat; rong khi giai doan khong co ca nao. */
    val weekdays: List<WeekdayStat> = emptyList()
)

object StatsCalculations {
    // Han Quoc tinh tuan tu Chu nhat.
    private val weekFields: WeekFields = WeekFields.of(Locale.KOREA)
    private val firstDayOfWeek: DayOfWeek = weekFields.firstDayOfWeek
    private const val MAX_DAILY_CHART_POINTS = 62

    fun firstDayOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    fun lastDayOfWeek(date: LocalDate): LocalDate = firstDayOfWeek(date).plusDays(6)

    /**
     * Khoang ngay cua mot giai doan. ALL_TIME duoc tinh tu chinh danh sach ca nen
     * ca moi them ngoai khoang cu van duoc tinh vao.
     */
    fun periodRange(
        period: StatsPeriod,
        today: LocalDate,
        shifts: List<Shift>,
        customStart: LocalDate?,
        customEnd: LocalDate?
    ): Pair<LocalDate, LocalDate> {
        val (start, end) = when (period) {
            StatsPeriod.SPECIFIC_DAY -> (customStart ?: today).let { it to it }
            StatsPeriod.DATE_RANGE -> (customStart ?: today.minusDays(6)) to (customEnd ?: today)
            StatsPeriod.CURRENT_WEEK -> firstDayOfWeek(today) to lastDayOfWeek(today)
            StatsPeriod.PREVIOUS_WEEK -> {
                val prevWeekStart = firstDayOfWeek(today).minusWeeks(1)
                prevWeekStart to lastDayOfWeek(prevWeekStart)
            }
            StatsPeriod.CURRENT_MONTH ->
                today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
            StatsPeriod.PREVIOUS_MONTH -> {
                val prevMonth = today.minusMonths(1)
                prevMonth.withDayOfMonth(1) to prevMonth.with(TemporalAdjusters.lastDayOfMonth())
            }
            StatsPeriod.CURRENT_YEAR ->
                today.withDayOfYear(1) to today.with(TemporalAdjusters.lastDayOfYear())
            StatsPeriod.ALL_TIME -> {
                val valid = shifts.map { it.localDate }.filter { it != LocalDate.MIN }
                (valid.minOrNull() ?: today) to (valid.maxOrNull() ?: today)
            }
        }
        return if (end.isBefore(start)) start to start else start to end
    }

    fun summarizeDay(date: LocalDate, shiftsInDay: List<Shift>): DailySummary {
        val totalHours = shiftsInDay.sumOf { it.calculateDurationHours() }
        val totalPlusNet = shiftsInDay.sumOf { it.calculatePlusNetIncome() }
        val totalNormalNet = shiftsInDay.sumOf { it.calculateNormalNetIncome() }
        val totalNet = totalPlusNet + totalNormalNet
        return DailySummary(
            date = date,
            shiftCount = shiftsInDay.size,
            totalHours = totalHours,
            totalPlusGrossIncome = shiftsInDay.sumOf { it.calculatePlusGrossIncome() },
            totalNormalGrossIncome = shiftsInDay.sumOf { it.calculateNormalGrossIncome() },
            totalGrossIncome = shiftsInDay.sumOf { it.calculateGrossIncome() },
            totalPlusNetIncome = totalPlusNet,
            totalNormalNetIncome = totalNormalNet,
            totalNetIncome = totalNet,
            averageHourlyRate = if (totalHours > 0) totalNet / totalHours else 0.0,
            totalDeliveries = shiftsInDay.sumOf { it.deliveryCount }
        )
    }

    fun compute(
        period: StatsPeriod,
        today: LocalDate,
        shifts: List<Shift>,
        customStart: LocalDate?,
        customEnd: LocalDate?
    ): StatsData {
        val (start, end) = periodRange(period, today, shifts, customStart, customEnd)
        val filtered = shifts.filter { !it.localDate.isBefore(start) && !it.localDate.isAfter(end) }
        val days = filtered
            .groupBy { it.localDate }
            .map { (date, inDay) -> summarizeDay(date, inDay) }
            .sortedByDescending { it.date }

        val summary = if (filtered.isEmpty()) null else {
            val totalHours = days.sumOf { it.totalHours }
            val totalNet = days.sumOf { it.totalNetIncome }
            StatsSummary(
                periodLabel = periodLabel(period, start, end),
                totalDays = days.size.toLong(),
                totalShifts = filtered.size,
                totalHours = totalHours,
                totalPlusGrossIncome = days.sumOf { it.totalPlusGrossIncome },
                totalNormalGrossIncome = days.sumOf { it.totalNormalGrossIncome },
                totalGrossIncome = days.sumOf { it.totalGrossIncome },
                totalPlusNetIncome = days.sumOf { it.totalPlusNetIncome },
                totalNormalNetIncome = days.sumOf { it.totalNormalNetIncome },
                totalNetIncome = totalNet,
                averageHourlyRate = if (totalHours > 0) totalNet / totalHours else 0.0,
                totalDeliveries = days.sumOf { it.totalDeliveries }
            )
        }

        return StatsData(
            start, end, summary, days, chartPoints(start, end, days), isMonthlyChart(start, end),
            weekdays = if (filtered.isEmpty()) emptyList() else weekdayStats(days)
        )
    }

    fun weekdayStats(days: List<DailySummary>): List<WeekdayStat> {
        val byWeekday = days.groupBy { it.date.dayOfWeek }
        return DayOfWeek.entries.map { dow ->
            val inDay = byWeekday[dow].orEmpty()
            WeekdayStat(dow, inDay.size, inDay.sumOf { it.totalHours }, inDay.sumOf { it.totalNetIncome })
        }
    }

    fun isMonthlyChart(start: LocalDate, end: LocalDate): Boolean =
        java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAILY_CHART_POINTS

    /** Ngay trong khoang ngan duoc ve tung ngay (ke ca ngay nghi = 0); khoang dai gop theo thang. */
    fun chartPoints(start: LocalDate, end: LocalDate, days: List<DailySummary>): List<ChartPoint> {
        if (days.isEmpty()) return emptyList()
        val byDate = days.associateBy { it.date }
        if (!isMonthlyChart(start, end)) {
            val dayCount = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1
            return (0 until dayCount).map { offset ->
                val date = start.plusDays(offset)
                ChartPoint(date.format(DateShort), byDate[date]?.totalNetIncome ?: 0.0)
            }
        }
        val monthFormatter = DateTimeFormatter.ofPattern("MM/yy")
        val first = days.minOf { it.date }.withDayOfMonth(1)
        val last = days.maxOf { it.date }.withDayOfMonth(1)
        val byMonth = days.groupBy { it.date.withDayOfMonth(1) }
        return generateSequence(first) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(last) }
            .map { month ->
                ChartPoint(month.format(monthFormatter), byMonth[month]?.sumOf { it.totalNetIncome } ?: 0.0)
            }
            .toList()
    }

    fun periodLabel(period: StatsPeriod, start: LocalDate, end: LocalDate): String {
        val monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy")
        return when (period) {
            StatsPeriod.SPECIFIC_DAY -> "Ngày ${start.format(DateFull)}"
            StatsPeriod.DATE_RANGE -> "${start.format(DateShort)} – ${end.format(DateShort)}/${end.year}"
            StatsPeriod.CURRENT_WEEK, StatsPeriod.PREVIOUS_WEEK -> {
                val weekNum = start.get(weekFields.weekOfWeekBasedYear())
                "Tuần $weekNum (${start.format(DateShort)} – ${end.format(DateShort)})"
            }
            StatsPeriod.CURRENT_MONTH, StatsPeriod.PREVIOUS_MONTH -> "Tháng ${start.format(monthFormatter)}"
            StatsPeriod.CURRENT_YEAR -> "Năm ${start.year}"
            StatsPeriod.ALL_TIME -> "Toàn bộ (${start.format(DateFull)} – ${end.format(DateFull)})"
        }
    }
}
