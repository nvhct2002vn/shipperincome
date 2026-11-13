package com.example.shipperincome

import com.example.shipperincome.data.model.RankEntry
import com.example.shipperincome.data.repository.RankRepository
import com.example.shipperincome.ui.viewmodel.RankCalculations
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class RankCalculationsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val anchor = LocalDate.of(2026, 10, 19)

    @Test
    fun cycleFromAnchorAndFollowingCycles() {
        val current = RankCalculations.cycleFor(LocalDate.of(2026, 10, 29), anchor, 14)
        assertEquals(LocalDate.of(2026, 10, 19), current.start)
        assertEquals(LocalDate.of(2026, 11, 1), current.end)

        // Chu ky sau tu noi tiep, khong can cau hinh lai.
        val next = RankCalculations.cycleFor(LocalDate.of(2026, 11, 2), anchor, 14)
        assertEquals(LocalDate.of(2026, 11, 2), next.start)
        assertEquals(LocalDate.of(2026, 11, 15), next.end)

        // Ngay truoc anchor van ra dung chu ky truoc.
        val previous = RankCalculations.cycleFor(LocalDate.of(2026, 10, 18), anchor, 14)
        assertEquals(LocalDate.of(2026, 10, 5), previous.start)
        assertEquals(LocalDate.of(2026, 10, 18), previous.end)
    }

    @Test
    fun remainingAndPerDayWhenTodayNotEnteredYet() {
        val cycle = RankCalculations.cycleFor(LocalDate.of(2026, 10, 21), anchor, 14)
        // Chay 3 ngay (19-21) duoc 2100 diem, hom nay la 22 va chua ghi.
        val entries = listOf(RankEntry("2026-10-19", 600), RankEntry("2026-10-21", 2100))
        val p = RankCalculations.progress(cycle, 7000, entries, LocalDate.of(2026, 10, 22))

        assertEquals(2100, p.currentPoints)
        assertEquals(4900, p.remainingPoints)
        assertEquals(11, p.daysRemaining) // 22/10 .. 01/11
        assertEquals(446, p.pointsPerDayNeeded) // 4900 / 11 = 445.4 -> lam tron len
        assertEquals(700.0, p.averagePerDay, 0.001) // 2100 / 3 ngay
        assertEquals(9800, p.projectedPoints)
        assertTrue(p.onTrack)
        assertEquals(listOf(1500, 600), p.history.map { it.gained })
    }

    @Test
    fun paceDropsOnDaysWithoutPoints() {
        val cycle = RankCalculations.cycleFor(LocalDate.of(2026, 10, 27), anchor, 14)
        val entries = listOf(RankEntry("2026-10-27", 5025))

        // Hom nay 29, chua ghi: da qua 10 ngay (19-28), con 4 ngay (29 - 01).
        val p = RankCalculations.progress(cycle, 7000, entries, LocalDate.of(2026, 10, 29))
        assertEquals(4, p.daysRemaining)
        assertEquals(502.5, p.averagePerDay, 0.001)
        assertEquals(7035, p.projectedPoints) // 5025 + 502.5 x 4, khong cong cho ngay 28 da qua

        // Sang ngay 30 van chua co diem moi: trung binh va du bao deu giam.
        val next = RankCalculations.progress(cycle, 7000, entries, LocalDate.of(2026, 10, 30))
        assertEquals(3, next.daysRemaining)
        assertEquals(5025.0 / 11, next.averagePerDay, 0.001)
        assertEquals(6395, next.projectedPoints)
        assertFalse(next.onTrack)
    }

    @Test
    fun todayAlreadyEnteredCountsFromTomorrow() {
        val cycle = RankCalculations.cycleFor(LocalDate.of(2026, 10, 29), anchor, 14)
        val p = RankCalculations.progress(cycle, 7000, listOf(RankEntry("2026-10-29", 5000)), LocalDate.of(2026, 10, 29))
        assertTrue(p.enteredToday)
        assertEquals(3, p.daysRemaining) // 30, 31, 01
        assertEquals(667, p.pointsPerDayNeeded)
    }

    @Test
    fun doneAndPastCycles() {
        val cycle = RankCalculations.cycleFor(LocalDate.of(2026, 10, 25), anchor, 14)
        val done = RankCalculations.progress(cycle, 7000, listOf(RankEntry("2026-10-28", 7200)), LocalDate.of(2026, 10, 29))
        assertTrue(done.isDone)
        assertEquals(0, done.remainingPoints)
        assertEquals(0, done.pointsPerDayNeeded)

        val past = RankCalculations.progress(cycle, 7000, listOf(RankEntry("2026-11-01", 6500)), LocalDate.of(2026, 11, 5))
        assertTrue(past.isPast)
        assertFalse(past.isDone)
        assertEquals(500, past.remainingPoints)
        assertEquals(0, past.daysRemaining)
    }

    @Test
    fun entriesFromOtherCyclesAreIgnored() {
        val cycle = RankCalculations.cycleFor(LocalDate.of(2026, 11, 3), anchor, 14)
        val entries = listOf(RankEntry("2026-10-30", 6900), RankEntry("2026-11-03", 800))
        val p = RankCalculations.progress(cycle, 7000, entries, LocalDate.of(2026, 11, 3))
        assertEquals(800, p.currentPoints)
        assertEquals(1, p.history.size)
    }

    @Test
    fun cumulativePointsCannotGoBackwards() {
        val cycle = RankCalculations.cycleFor(LocalDate.of(2026, 10, 25), anchor, 14)
        val entries = listOf(RankEntry("2026-10-20", 1000), RankEntry("2026-10-24", 3000))
        assertNotNull(RankCalculations.validate(cycle, entries, LocalDate.of(2026, 10, 22), 900))
        assertNotNull(RankCalculations.validate(cycle, entries, LocalDate.of(2026, 10, 22), 3500))
        assertNull(RankCalculations.validate(cycle, entries, LocalDate.of(2026, 10, 22), 2000))
        assertNull(RankCalculations.validate(cycle, entries, LocalDate.of(2026, 10, 24), 3200))
    }

    @Test
    fun repositoryRoundTripsAndKeepsCorruptFile() = runBlocking {
        val repo = RankRepository(tmp.root)
        repo.save(listOf(RankEntry("2026-10-21", 2100), RankEntry("2026-10-19", 600)))
        assertEquals(listOf("2026-10-19", "2026-10-21"), RankRepository(tmp.root).load().map { it.date })

        java.io.File(tmp.root, "rank_data.json").writeText("{broken")
        assertTrue(RankRepository(tmp.root).load().isEmpty())
        assertTrue(tmp.root.listFiles()!!.any { it.name.startsWith("rank_data.corrupt-") })
    }
}
