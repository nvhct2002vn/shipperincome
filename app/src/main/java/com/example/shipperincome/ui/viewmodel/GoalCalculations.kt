package com.example.shipperincome.ui.viewmodel

import com.example.shipperincome.data.model.Shift
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class GoalProgress(
    val target: Double,
    val earned: Double,
    val start: LocalDate,
    val end: LocalDate,
    /** So ngay con co the chay: tinh ca hom nay neu hom nay chua co ca nao (giong cach tinh rank). */
    val daysRemaining: Int
) {
    val isDone: Boolean get() = earned >= target
    val remaining: Double get() = (target - earned).coerceAtLeast(0.0)
    val fraction: Float get() = if (target > 0) (earned / target).coerceIn(0.0, 1.0).toFloat() else 0f
    val percent: Int get() = if (target > 0) (earned / target * 100).toInt() else 0
    val perDayNeeded: Double
        get() = when {
            isDone -> 0.0
            daysRemaining > 0 -> remaining / daysRemaining
            else -> remaining
        }
}

object GoalCalculations {

    /** null khi chua dat muc tieu (target <= 0). */
    fun progress(target: Double, start: LocalDate, end: LocalDate, shifts: List<Shift>, today: LocalDate): GoalProgress? {
        if (target <= 0.0) return null
        val earned = shifts
            .filter { !it.localDate.isBefore(start) && !it.localDate.isAfter(end) }
            .sumOf { it.calculateNetIncome() }
        val workedToday = shifts.any { it.localDate == today }
        val daysRemaining = when {
            today.isAfter(end) -> 0
            today.isBefore(start) -> (ChronoUnit.DAYS.between(start, end) + 1).toInt()
            else -> {
                val from = if (workedToday) today.plusDays(1) else today
                (ChronoUnit.DAYS.between(from, end) + 1).toInt().coerceAtLeast(0)
            }
        }
        return GoalProgress(target, earned, start, end, daysRemaining)
    }

    fun weekly(target: Double, shifts: List<Shift>, today: LocalDate): GoalProgress? {
        val (start, end) = StatsCalculations.periodRange(StatsPeriod.CURRENT_WEEK, today, shifts, null, null)
        return progress(target, start, end, shifts, today)
    }

    fun monthly(target: Double, shifts: List<Shift>, today: LocalDate): GoalProgress? {
        val (start, end) = StatsCalculations.periodRange(StatsPeriod.CURRENT_MONTH, today, shifts, null, null)
        return progress(target, start, end, shifts, today)
    }
}
