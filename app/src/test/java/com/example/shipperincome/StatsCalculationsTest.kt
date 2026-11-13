package com.example.shipperincome

import com.example.shipperincome.data.model.Shift
import com.example.shipperincome.ui.viewmodel.StatsCalculations
import com.example.shipperincome.ui.viewmodel.StatsPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatsCalculationsTest {

    private fun shift(date: String, net: Double = 10_000.0) = Shift(
        date = date,
        startTime = "10:00",
        endTime = "12:00",
        normalGrossIncome = net,
        normalTaxRateSnapshot = 0.0
    )

    @Test
    fun weekStartsOnSunday() {
        val wednesday = LocalDate.of(2026, 10, 28)
        val (start, end) = StatsCalculations.periodRange(StatsPeriod.CURRENT_WEEK, wednesday, emptyList(), null, null)
        assertEquals(DayOfWeek.SUNDAY, start.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 25), start)
        assertEquals(LocalDate.of(2026, 10, 31), end)
    }

    @Test
    fun allTimeFollowsTheShiftList() {
        val today = LocalDate.of(2026, 10, 28)
        val shifts = listOf(shift("2026-01-03"), shift("2026-09-10"))
        val (start, end) = StatsCalculations.periodRange(StatsPeriod.ALL_TIME, today, shifts, null, null)
        assertEquals(LocalDate.of(2026, 1, 3), start)
        assertEquals(LocalDate.of(2026, 9, 10), end)

        // Ca moi them ngoai khoang cu van duoc tinh (truoc day khoang bi "dong bang" luc chon).
        val later = shifts + shift("2026-10-20")
        val data = StatsCalculations.compute(StatsPeriod.ALL_TIME, today, later, null, null)
        assertEquals(3, data.summary!!.totalShifts)
    }

    @Test
    fun invertedRangeIsClamped() {
        val (start, end) = StatsCalculations.periodRange(
            StatsPeriod.DATE_RANGE, LocalDate.now(), emptyList(),
            LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1)
        )
        assertEquals(start, end)
    }

    @Test
    fun computeSumsOnlyShiftsInRange() {
        val today = LocalDate.of(2026, 10, 28)
        val shifts = listOf(
            shift("2026-10-26", 10_000.0),
            shift("2026-10-26", 5_000.0),
            shift("2026-10-27", 20_000.0),
            shift("2026-10-20", 99_000.0) // tuan truoc
        )
        val data = StatsCalculations.compute(StatsPeriod.CURRENT_WEEK, today, shifts, null, null)
        val summary = data.summary!!
        assertEquals(35_000.0, summary.totalNetIncome, 0.001)
        assertEquals(2L, summary.totalDays)
        assertEquals(3, summary.totalShifts)
        assertEquals(LocalDate.of(2026, 10, 27), data.days.first().date)
    }

    @Test
    fun emptyPeriodHasNoSummaryOrChart() {
        val data = StatsCalculations.compute(StatsPeriod.CURRENT_WEEK, LocalDate.of(2026, 10, 28), emptyList(), null, null)
        assertNull(data.summary)
        assertTrue(data.chart.isEmpty())
    }

    @Test
    fun shortRangeChartHasOnePointPerDayIncludingDaysOff() {
        val today = LocalDate.of(2026, 10, 28)
        val data = StatsCalculations.compute(StatsPeriod.CURRENT_WEEK, today, listOf(shift("2026-10-27", 20_000.0)), null, null)
        assertFalse(data.chartByMonth)
        assertEquals(7, data.chart.size)
        assertEquals(20_000.0, data.chart[2].netIncome, 0.001) // 25, 26, 27
        assertEquals(0.0, data.chart[0].netIncome, 0.001)
    }

    @Test
    fun longRangeChartIsGroupedByMonth() {
        val today = LocalDate.of(2026, 10, 28)
        val shifts = listOf(shift("2026-01-05", 1_000.0), shift("2026-01-20", 2_000.0), shift("2026-03-02", 4_000.0))
        val data = StatsCalculations.compute(StatsPeriod.CURRENT_YEAR, today, shifts, null, null)
        assertTrue(data.chartByMonth)
        assertEquals(listOf("01/26", "02/26", "03/26"), data.chart.map { it.label })
        assertEquals(listOf(3_000.0, 0.0, 4_000.0), data.chart.map { it.netIncome })
    }

    @Test
    fun weekdayInsightsAverageOnlyWorkedDays() {
        // Hai thu bay (24/10 va 31/10) va mot chu nhat (25/10).
        val shifts = listOf(
            shift("2026-10-24", 30_000.0),
            shift("2026-10-31", 50_000.0),
            shift("2026-10-25", 20_000.0)
        )
        val days = shifts.groupBy { it.localDate }.map { (d, s) -> StatsCalculations.summarizeDay(d, s) }
        val weekdays = StatsCalculations.weekdayStats(days)

        val saturday = weekdays.first { it.day == DayOfWeek.SATURDAY }
        assertEquals(2, saturday.workedDays)
        assertEquals(40_000.0, saturday.avgNetPerDay, 0.001)
        assertEquals(20_000.0, saturday.hourlyRate, 0.001) // 80k / 4h
        assertEquals(0, weekdays.first { it.day == DayOfWeek.MONDAY }.workedDays)
        assertEquals(DayOfWeek.MONDAY, weekdays.first().day)
    }
}
