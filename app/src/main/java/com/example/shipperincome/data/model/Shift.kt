package com.example.shipperincome.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.time.Duration

// Enum định nghĩa loại ca làm việc
enum class ShiftType {
    NORMAL, // Ca thường
    PLUS    // Ca Plus (có đơn hàng)
}

@Serializable
data class Shift(
    // ID duy nhất để dễ dàng sửa/xóa
    val id: String = UUID.randomUUID().toString(),
    // Sử dụng LocalDate và LocalTime để xử lý ngày giờ dễ dàng hơn
    // Lưu ý: kotlinx.serialization cần cấu hình thêm để xử lý kiểu Date/Time hoặc lưu dạng String
    // Cách đơn giản là lưu dạng String "yyyy-MM-dd" và "HH:mm"
    val date: String, // Ví dụ: "2024-07-26"
    val startTime: String, // Ví dụ: "08:00"
    val endTime: String, // Ví dụ: "17:30"
    val grossIncome: Double, // Thu nhập trước trừ phí
    val shiftType: ShiftType = ShiftType.NORMAL, // Loại ca làm việc
    val deliveryCount: Int = 0, // Số đơn đã giao (chỉ cho ca Plus)
    // Lưu settings tại thời điểm tạo shift để không bị thay đổi khi settings thay đổi
    val taxRateSnapshot: Double = 0.057, // Thuế tại thời điểm tạo
    val deliveryFeeSnapshot: Double = 100.0 // Phí đơn tại thời điểm tạo
) {
    // --- Các hàm tính toán tiện ích ---

    // Tính thu nhập ròng sau khi trừ thuế và phí đơn hàng
    // Logic: Nếu là Plus -> trừ phí đơn trước, rồi mới trừ thuế
    // Sử dụng snapshot values để không bị ảnh hưởng bởi thay đổi settings
    fun calculateNetIncome(): Double {
        val deliveryFeeAmount = if (shiftType == ShiftType.PLUS) deliveryCount * deliveryFeeSnapshot else 0.0
        val incomeAfterDeliveryFee = grossIncome - deliveryFeeAmount
        val taxAmount = incomeAfterDeliveryFee * taxRateSnapshot
        return incomeAfterDeliveryFee - taxAmount
    }

    // Overload để backward compatibility
    fun calculateNetIncome(taxRate: Double, deliveryFee: Double): Double {
        return calculateNetIncome() // Sử dụng snapshot values
    }

    // Tính thời gian làm việc (trả về số giờ dạng Double)
    fun calculateDurationHours(): Double {
        try {
            val start = LocalTime.parse(startTime)
            val end = LocalTime.parse(endTime)
            
            // Ca làm qua đêm (endTime trước startTime)
            if (end.isBefore(start) || end == start) {
                // Tính từ startTime đến cuối ngày (00:00 ngày hôm sau)
                val minutesToMidnight = Duration.between(start, LocalTime.MAX).toMinutes() + 1
                
                // Tính từ đầu ngày (00:00) đến endTime
                val minutesFromMidnight = Duration.between(LocalTime.MIN, end).toMinutes()
                
                // Tổng thời gian
                return (minutesToMidnight + minutesFromMidnight) / 60.0
            } else {
                // Ca làm trong cùng ngày
                val duration = Duration.between(start, end)
                return duration.toMinutes() / 60.0
            }
        } catch (e: Exception) {
            // Ghi log lỗi nếu parse thời gian thất bại
            println("Error parsing time for shift ID ${this.id} (startTime='$startTime', endTime='$endTime'): ${e.message}")
            return 0.0 // Trả về 0 nếu có lỗi
        }
    }

    // Tính thu nhập trung bình mỗi giờ
    fun calculateHourlyRate(): Double {
        val duration = calculateDurationHours()
        val netIncome = calculateNetIncome()
        // Tránh chia cho 0
        return if (duration > 0) netIncome / duration else 0.0
    }

    // Overload để backward compatibility
    fun calculateHourlyRate(taxRate: Double, deliveryFee: Double): Double {
        return calculateHourlyRate() // Sử dụng snapshot values
    }

    // Tiện ích chuyển đổi string date thành LocalDate
     @kotlinx.serialization.Transient // Bỏ qua trường này khi serialize/deserialize
     val localDate: LocalDate by lazy { // Sử dụng lazy để tính một lần
         try {
             LocalDate.parse(date, DateTimeFormatter.ISO_DATE)
         } catch (e: Exception) {
             println("Error parsing date string '${date}' for shift ID ${this.id}: $e")
             // Trả về một giá trị mặc định hoặc ném lỗi tùy theo logic xử lý
             LocalDate.MIN // Hoặc một giá trị mặc định khác
         }
     }
} 