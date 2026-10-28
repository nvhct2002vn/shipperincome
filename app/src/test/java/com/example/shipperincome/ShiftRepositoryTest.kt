package com.example.shipperincome

import com.example.shipperincome.data.model.Shift
import com.example.shipperincome.data.repository.ShiftRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ShiftRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun shift(id: String, income: Double = 50_000.0) = Shift(
        id = id,
        date = "2026-05-15",
        startTime = "10:00",
        endTime = "12:00",
        normalGrossIncome = income
    )

    @Test
    fun saveThenLoadRoundTrips() = runBlocking {
        val repo = ShiftRepository(tmp.root)
        repo.saveAllShifts(listOf(shift("a"), shift("b", 70_000.0)))

        val result = ShiftRepository(tmp.root).loadAllShifts()

        assertNull(result.corruptBackup)
        assertEquals(listOf("a", "b"), result.shifts.map { it.id })
        assertEquals(70_000.0, result.shifts[1].normalGrossIncome, 0.001)
        assertFalse(File(tmp.root, "shifts_data.json.tmp").exists())
    }

    @Test
    fun corruptFileIsBackedUpInsteadOfBeingOverwritten() = runBlocking {
        val original = "[{\"id\": \"a\", \"date\": \"2026-05-15\", BROKEN"
        File(tmp.root, "shifts_data.json").writeText(original)
        val repo = ShiftRepository(tmp.root)

        val result = repo.loadAllShifts()
        assertTrue(result.shifts.isEmpty())
        val backup = result.corruptBackup
        assertNotNull(backup)
        assertEquals(original, backup!!.readText())

        // Luu ca moi khong duoc lam mat ban sao luu cua du lieu cu.
        repo.saveAllShifts(listOf(shift("new")))
        assertEquals(original, backup.readText())
        assertEquals(listOf("new"), repo.loadAllShifts().shifts.map { it.id })
    }

    @Test
    fun legacyFormatIsMigrated() = runBlocking {
        File(tmp.root, "shifts_data.json").writeText(
            """
            [
              {"id": "p", "date": "2025-01-01", "startTime": "09:00", "endTime": "11:00",
               "deliveryCount": 2, "grossIncome": 30000.0, "shiftType": "PLUS", "taxRateSnapshot": 0.033},
              {"id": "n", "date": "2025-01-01", "startTime": "12:00", "endTime": "13:00",
               "grossIncome": 10000.0}
            ]
            """.trimIndent()
        )

        val shifts = ShiftRepository(tmp.root).loadAllShifts().shifts

        val plus = shifts.first { it.id == "p" }
        assertEquals(30_000.0, plus.plusGrossIncome, 0.001)
        assertEquals(0.0, plus.normalGrossIncome, 0.001)
        assertEquals(0.033, plus.plusTaxRateSnapshot, 0.0001)
        val normal = shifts.first { it.id == "n" }
        assertEquals(10_000.0, normal.normalGrossIncome, 0.001)
        // Da ghi lai theo dinh dang moi.
        assertFalse(File(tmp.root, "shifts_data.json").readText().contains("shiftType"))
    }

    @Test
    fun missingFileLoadsEmpty() = runBlocking {
        val result = ShiftRepository(tmp.root).loadAllShifts()
        assertTrue(result.shifts.isEmpty())
        assertNull(result.corruptBackup)
    }

    @Test
    fun decodeRejectsGarbage() {
        val repo = ShiftRepository(tmp.root)
        val failed = runCatching { repo.decode("not json") }.isFailure
        assertTrue(failed)
        assertEquals(listOf("x"), repo.decode(repo.encode(listOf(shift("x")))).map { it.id })
    }
}
