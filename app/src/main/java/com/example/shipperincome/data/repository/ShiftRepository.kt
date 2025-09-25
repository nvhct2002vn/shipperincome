package com.example.shipperincome.data.repository

import android.content.Context
import com.example.shipperincome.data.model.Shift
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class ShiftRepository(private val context: Context) {

    // Tên file lưu dữ liệu
    private val fileName = "shifts_data.json"

    // Settings repository để lấy default values cho migration
    private val settingsRepository = SettingsRepository(context)

    // Đối tượng Json để encode/decode
    private val json = Json {
        prettyPrint = true // Giúp file JSON dễ đọc hơn
        ignoreUnknownKeys = true // Bỏ qua các trường không biết khi đọc
        encodeDefaults = true // Đảm bảo các giá trị mặc định (như id) được lưu
    }

    // Hàm lưu danh sách tất cả các ca làm vào file
    fun saveAllShifts(shifts: List<Shift>) {
        try {
            val jsonString = json.encodeToString(shifts)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
            println("Shifts saved successfully to $fileName (${shifts.size} items)") // Log để debug
        } catch (e: IOException) {
            println("Error saving shifts to $fileName: ${e.message}")
            // Cân nhắc ném lỗi hoặc thông báo cho người dùng
        } catch (e: Exception) {
            println("Error encoding shifts: ${e.message}")
        }
    }

    // Hàm đọc danh sách tất cả các ca làm từ file
    fun loadAllShifts(): List<Shift> {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists() || file.length() == 0L) { // Kiểm tra file tồn tại và không rỗng
                println("File not found or empty: $fileName. Returning empty list.")
                emptyList() // Trả về danh sách rỗng nếu file không tồn tại hoặc rỗng
            } else {
                val jsonString = context.openFileInput(fileName).bufferedReader().use { it.readText() }
                 // Kiểm tra lại nội dung trước khi decode
                if (jsonString.isBlank()) {
                     println("File content is blank: $fileName. Returning empty list.")
                     emptyList()
                } else {
                     val shifts = json.decodeFromString<List<Shift>>(jsonString)
                     println("Shifts loaded successfully from $fileName: ${shifts.size} items.") // Log

                     // Migrate data cũ không có snapshot values
                     val migratedShifts = migrateOldShifts(shifts)

                     // Lưu lại nếu có migration
                     if (migratedShifts.any { it.taxRateSnapshot != shifts.find { old -> old.id == it.id }?.taxRateSnapshot }) {
                         saveAllShifts(migratedShifts)
                         println("Migrated ${migratedShifts.size} shifts with default snapshot values")
                     }

                     migratedShifts
                }
            }
        } catch (e: IOException) {
            println("Error loading shifts from $fileName: ${e.message}")
            emptyList() // Trả về danh sách rỗng nếu có lỗi đọc file
        } catch (e: Exception) {
             // Có thể là lỗi SerializationException nếu cấu trúc file JSON cũ không khớp model mới
             println("Error decoding shifts from $fileName: ${e.message}")
             // Có thể cân nhắc xóa file cũ hoặc thông báo lỗi nghiêm trọng
             emptyList()
        }
    }

    // Hàm migrate data cũ không có snapshot values
    private fun migrateOldShifts(shifts: List<Shift>): List<Shift> {
        return shifts.map { shift ->
            // Check nếu shift có snapshot values đúng (khác default hoặc 0)
            // Data cũ sẽ có default values 0.057 và 100.0
            val needsMigration = (shift.taxRateSnapshot == 0.0 && shift.deliveryFeeSnapshot == 0.0)

            if (needsMigration) {
                shift.copy(
                    taxRateSnapshot = SettingsRepository.DEFAULT_TAX_RATE, // 0.057
                    deliveryFeeSnapshot = SettingsRepository.DEFAULT_DELIVERY_FEE // 100.0
                )
            } else {
                shift // Giữ nguyên nếu đã có snapshot values
            }
        }
    }
} 