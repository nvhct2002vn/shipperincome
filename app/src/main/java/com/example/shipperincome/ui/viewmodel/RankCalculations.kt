package com.example.shipperincome.ui.viewmodel

import com.example.shipperincome.data.model.RankEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

data class RankCycle(val index: Long, val start: LocalDate, val end: LocalDate) {
    val days: Int get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

/** Mot lan ghi diem kem so diem tang them so voi lan ghi truoc trong cung chu ky. */
data class RankHistoryItem(val date: LocalDate, val totalPoints: Int, val gained: Int)

data class RankProgress(
    val cycle: RankCycle,
    val targetPoints: Int,
    val currentPoints: Int,
    val remainingPoints: Int,
    /** So ngay con co the chay: tinh ca hom nay neu hom nay chua ghi diem. */
    val daysRemaining: Int,
    /** Trung binh moi ngay can chay de du chi tieu (lam tron len). */
    val pointsPerDayNeeded: Int,
    /** Trung binh moi ngay da chay, tinh tren moi ngay da qua cua chu ky (ca ngay nghi). */
    val averagePerDay: Double,
    /** Diem hien tai + trung binh x so ngay con lai: neu giu nhip nay thi het chu ky dat khoang bao nhieu. */
    val projectedPoints: Int,
    val enteredToday: Boolean,
    val isPast: Boolean,
    val isFuture: Boolean,
    val history: List<RankHistoryItem>
) {
    val isDone: Boolean get() = currentPoints >= targetPoints
    val fraction: Float get() = if (targetPoints > 0) (currentPoints.toFloat() / targetPoints).coerceIn(0f, 1f) else 0f
    val onTrack: Boolean get() = isDone || projectedPoints >= targetPoints
}

object RankCalculations {

    /** Chu ky chua ngay [date], suy ra tu mot ngay bat dau bat ky [anchor] (ke ca ngay truoc anchor). */
    fun cycleFor(date: LocalDate, anchor: LocalDate, cycleDays: Int): RankCycle =
        cycleAt(Math.floorDiv(ChronoUnit.DAYS.between(anchor, date), cycleDays.toLong()), anchor, cycleDays)

    fun cycleAt(index: Long, anchor: LocalDate, cycleDays: Int): RankCycle {
        val start = anchor.plusDays(index * cycleDays)
        return RankCycle(index, start, start.plusDays(cycleDays - 1L))
    }

    fun progress(cycle: RankCycle, targetPoints: Int, entries: List<RankEntry>, today: LocalDate): RankProgress {
        val inCycle = entries
            .mapNotNull { e -> runCatching { e.localDate }.getOrNull()?.let { it to e.totalPoints } }
            .filter { (d, _) -> d in cycle && !d.isAfter(today) }
            .sortedBy { it.first }

        var previous = 0
        val history = inCycle.map { (date, total) ->
            RankHistoryItem(date, total, total - previous).also { previous = total }
        }

        val last = inCycle.lastOrNull()
        val current = last?.second ?: 0
        val remaining = (targetPoints - current).coerceAtLeast(0)
        val isPast = cycle.end.isBefore(today)
        val isFuture = cycle.start.isAfter(today)
        val enteredToday = last?.first == today

        val daysRemaining = when {
            isPast -> 0
            isFuture -> cycle.days
            else -> {
                val from = if (enteredToday) today.plusDays(1) else today
                (ChronoUnit.DAYS.between(from, cycle.end) + 1).toInt().coerceAtLeast(0)
            }
        }
        val perDay = when {
            remaining == 0 -> 0
            daysRemaining == 0 -> remaining
            else -> ceil(remaining.toDouble() / daysRemaining).toInt()
        }

        // Tinh theo so ngay da troi qua den hom nay (khong phai den lan ghi cuoi): ngay khong di lam/khong ghi
        // diem van keo trung binh xuong, va du bao chi cong them cho nhung ngay con lai thuc su.
        val daysElapsed = if (isFuture) 0 else cycle.days - daysRemaining
        val average = if (daysElapsed > 0) current.toDouble() / daysElapsed else 0.0
        val projected = current + (average * daysRemaining).roundToInt()

        return RankProgress(
            cycle = cycle,
            targetPoints = targetPoints,
            currentPoints = current,
            remainingPoints = remaining,
            daysRemaining = daysRemaining,
            pointsPerDayNeeded = perDay,
            averagePerDay = average,
            projectedPoints = projected,
            enteredToday = enteredToday,
            isPast = isPast,
            isFuture = isFuture,
            history = history.reversed()
        )
    }

    /**
     * Kiem tra tong diem moi cho ngay [date]: diem cong don nen khong duoc nho hon lan ghi truoc,
     * cung khong duoc lon hon lan ghi sau trong cung chu ky. Tra ve thong bao loi hoac null.
     */
    fun validate(cycle: RankCycle, entries: List<RankEntry>, date: LocalDate, points: Int): String? {
        val inCycle = entries.filter { runCatching { it.localDate in cycle }.getOrDefault(false) }
        val before = inCycle.filter { it.localDate.isBefore(date) }.maxByOrNull { it.date }
        val after = inCycle.filter { it.localDate.isAfter(date) }.minByOrNull { it.date }
        return when {
            before != null && points < before.totalPoints ->
                "Tổng điểm không được nhỏ hơn ngày ${before.localDate.dayOfMonth}/${before.localDate.monthValue} (${before.totalPoints})"
            after != null && points > after.totalPoints ->
                "Tổng điểm không được lớn hơn ngày ${after.localDate.dayOfMonth}/${after.localDate.monthValue} (${after.totalPoints})"
            else -> null
        }
    }
}
