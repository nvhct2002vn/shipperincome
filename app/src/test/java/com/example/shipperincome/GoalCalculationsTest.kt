package com.example.shipperincome

import com.example.shipperincome.data.model.Shift
import com.example.shipperincome.ui.viewmodel.GoalCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalCalculationsTest {

    private fun shift(date: String, net: Double) = Shift(
        date = date,
        startTime = "17:00",
        endTime = "23:00",
        normalGrossIncome = net,
        normalTaxRateSnapshot = 0.0
    )

    @Test
    fun noGoalMeansNoProgress() {
        assertNull(GoalCalculations.monthly(0.0, listOf(shift("2026-10-01", 100_000.0)), LocalDate.of(2026, 10, 29)))
    }

    @Test
    fun monthlyCountsOnlyThisMonthAndRemainingDaysIncludeToday() {
        val shifts = listOf(
            shift("2026-09-30", 999_999.0), // thang truoc, khong tinh
            shift("2026-10-05", 1_000_000.0),
            shift("2026-10-27", 500_000.0)
        )
        // Hom nay 29/10 chua co ca: con 29, 30, 31 = 3 ngay.
        val p = GoalCalculations.monthly(3_000_000.0, shifts, LocalDate.of(2026, 10, 29))!!
        assertEquals(1_500_000.0, p.earned, 0.001)
        assertEquals(1_500_000.0, p.remaining, 0.001)
        assertEquals(3, p.daysRemaining)
        assertEquals(500_000.0, p.perDayNeeded, 0.001)
        assertEquals(50, p.percent)
    }

    @Test
    fun todayAlreadyWorkedCountsFromTomorrowAndDoneStopsPace() {
        val today = LocalDate.of(2026, 10, 29)
        val worked = GoalCalculations.monthly(3_000_000.0, listOf(shift("2026-10-29", 600_000.0)), today)!!
        assertEquals(2, worked.daysRemaining) // 30, 31

        val done = GoalCalculations.weekly(500_000.0, listOf(shift("2026-10-27", 600_000.0)), today)!!
        assertTrue(done.isDone)
        assertEquals(0.0, done.perDayNeeded, 0.001)
        assertEquals(1f, done.fraction)
    }
}
