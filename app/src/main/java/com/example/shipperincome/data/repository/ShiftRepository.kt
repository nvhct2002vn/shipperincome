package com.example.shipperincome.data.repository

import android.content.Context
import android.util.Log
import com.example.shipperincome.data.model.Shift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Ket qua doc file: neu file hong, ban goc duoc sao luu sang [corruptBackup] truoc khi tra ve list rong. */
data class ShiftLoadResult(
    val shifts: List<Shift>,
    val corruptBackup: File? = null
)

class ShiftRepository(private val dataDir: File) {

    constructor(context: Context) : this(context.filesDir)

    private val file = File(dataDir, FILE_NAME)
    private val writeLock = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun saveAllShifts(shifts: List<Shift>) = withContext(Dispatchers.IO) {
        writeLock.withLock { writeAtomically(json.encodeToString(ListSerializer(Shift.serializer()), shifts)) }
    }

    suspend fun loadAllShifts(): ShiftLoadResult = withContext(Dispatchers.IO) {
        writeLock.withLock { loadLocked() }
    }

    /** Dung cho sao luu: tra ve JSON cua danh sach hien tai. */
    fun encode(shifts: List<Shift>): String =
        json.encodeToString(ListSerializer(Shift.serializer()), shifts)

    /** Dung cho khoi phuc: nem exception neu chuoi khong hop le. */
    fun decode(raw: String): List<Shift> =
        json.decodeFromString(ListSerializer(StoredShift.serializer()), raw).map { it.toShift() }

    private fun loadLocked(): ShiftLoadResult {
        if (!file.exists() || file.length() == 0L) return ShiftLoadResult(emptyList())

        val text = try {
            file.readText()
        } catch (e: IOException) {
            Log.e(TAG, "Cannot read $FILE_NAME", e)
            return ShiftLoadResult(emptyList(), backupCorruptFile())
        }
        if (text.isBlank()) return ShiftLoadResult(emptyList())

        return try {
            val stored = json.decodeFromString(ListSerializer(StoredShift.serializer()), text)
            val shifts = stored.map { it.toShift() }
            if (stored.any { it.isLegacy() }) {
                writeAtomically(json.encodeToString(ListSerializer(Shift.serializer()), shifts))
            }
            ShiftLoadResult(shifts)
        } catch (e: Exception) {
            // Khong tra ve list rong roi de lan luu sau ghi de: giu lai ban goc de con cuu duoc.
            Log.e(TAG, "Cannot decode $FILE_NAME", e)
            ShiftLoadResult(emptyList(), backupCorruptFile())
        }
    }

    private fun backupCorruptFile(): File? = try {
        val backup = File(dataDir, "shifts_data.corrupt-${System.currentTimeMillis()}.json")
        Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
        backup
    } catch (e: IOException) {
        Log.e(TAG, "Cannot back up corrupt $FILE_NAME", e)
        null
    }

    private fun writeAtomically(content: String) = writeTextAtomically(dataDir, FILE_NAME, content)

    @Serializable
    private data class StoredShift(
        val id: String = "",
        val date: String = "",
        val startTime: String = "",
        val endTime: String = "",
        val deliveryCount: Int = 0,
        val plusGrossIncome: Double? = null,
        val normalGrossIncome: Double? = null,
        val plusMissionIncome: Double? = null,
        val normalMissionIncome: Double? = null,
        val plusTaxRateSnapshot: Double? = null,
        val normalTaxRateSnapshot: Double? = null,
        val deliveryFeeSnapshot: Double? = null,
        val grossIncome: Double? = null,
        val shiftType: String? = null,
        val taxRateSnapshot: Double? = null,
        val durationMinutes: Int? = null
    ) {
        fun isLegacy(): Boolean = grossIncome != null || shiftType != null || taxRateSnapshot != null

        fun toShift(): Shift {
            val legacyIncome = grossIncome ?: 0.0
            val legacyTaxRate = taxRateSnapshot ?: SettingsRepository.DEFAULT_TAX_RATE

            val migratedPlusIncome = when {
                plusGrossIncome != null -> plusGrossIncome
                shiftType == "PLUS" -> legacyIncome
                else -> 0.0
            }
            val migratedNormalIncome = when {
                normalGrossIncome != null -> normalGrossIncome
                shiftType == "NORMAL" -> legacyIncome
                shiftType == null -> legacyIncome
                else -> 0.0
            }

            return Shift(
                id = id.ifBlank { java.util.UUID.randomUUID().toString() },
                date = date,
                startTime = startTime,
                endTime = endTime,
                deliveryCount = deliveryCount,
                plusGrossIncome = migratedPlusIncome,
                normalGrossIncome = migratedNormalIncome,
                plusMissionIncome = plusMissionIncome ?: 0.0,
                normalMissionIncome = normalMissionIncome ?: 0.0,
                plusTaxRateSnapshot = plusTaxRateSnapshot ?: legacyTaxRate,
                normalTaxRateSnapshot = normalTaxRateSnapshot ?: legacyTaxRate,
                deliveryFeeSnapshot = deliveryFeeSnapshot ?: SettingsRepository.DEFAULT_DELIVERY_FEE,
                durationMinutes = durationMinutes
            )
        }
    }

    companion object {
        private const val TAG = "ShiftRepository"
        private const val FILE_NAME = "shifts_data.json"
    }
}
