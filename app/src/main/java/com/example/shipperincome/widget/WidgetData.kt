package com.example.shipperincome.widget

import android.content.Context
import com.example.shipperincome.data.repository.RankRepository
import com.example.shipperincome.data.repository.SettingsRepository
import com.example.shipperincome.data.repository.ShiftRepository
import com.example.shipperincome.ui.viewmodel.GoalCalculations
import com.example.shipperincome.ui.viewmodel.GoalProgress
import com.example.shipperincome.ui.viewmodel.RankCalculations
import com.example.shipperincome.ui.viewmodel.RankProgress
import com.example.shipperincome.ui.viewmodel.StatsCalculations
import com.example.shipperincome.ui.viewmodel.StatsPeriod
import java.time.LocalDate

/** Du lieu widget doc thang tu file cua app (khong qua ViewModel vi widget chay ca khi app dong). */
data class WidgetData(
    val today: LocalDate,
    val weekNet: Double,
    val monthNet: Double,
    /** null khi chua dat muc tieu tuan/thang trong Cai dat. */
    val weekGoal: GoalProgress?,
    val monthGoal: GoalProgress?,
    /** null khi chua cau hinh chu ky rank trong Cai dat. */
    val rank: RankProgress?
) {
    companion object {
        suspend fun load(context: Context, today: LocalDate = LocalDate.now()): WidgetData {
            val shifts = ShiftRepository(context).loadAllShifts().shifts
            val settings = SettingsRepository(context)

            fun netIn(period: StatsPeriod): Double {
                val (start, end) = StatsCalculations.periodRange(period, today, shifts, null, null)
                return shifts
                    .filter { !it.localDate.isBefore(start) && !it.localDate.isAfter(end) }
                    .sumOf { it.calculateNetIncome() }
            }

            val goals = settings.getIncomeGoals()
            val config = settings.getRankConfig()
            val rank = config.cycleAnchor?.let { anchor ->
                val cycle = RankCalculations.cycleFor(today, anchor, config.cycleDays)
                RankCalculations.progress(cycle, config.targetPoints, RankRepository(context).load(), today)
            }
            return WidgetData(
                today = today,
                weekNet = netIn(StatsPeriod.CURRENT_WEEK),
                monthNet = netIn(StatsPeriod.CURRENT_MONTH),
                weekGoal = GoalCalculations.weekly(goals.weekly, shifts, today),
                monthGoal = GoalCalculations.monthly(goals.monthly, shifts, today),
                rank = rank
            )
        }
    }
}
