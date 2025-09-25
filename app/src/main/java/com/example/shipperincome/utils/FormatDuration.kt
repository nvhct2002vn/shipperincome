package com.example.shipperincome.utils // Hoặc package của bạn

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Định dạng tổng số giờ (Double) thành chuỗi "X giờ Y phút".
 * Ví dụ: 8.5 -> "8 giờ 30 phút", 7.0 -> "7 giờ 0 phút", 0.75 -> "0 giờ 45 phút"
 * @param totalHours Tổng số giờ làm việc dạng Double.
 * @return Chuỗi đã định dạng.
 */
fun FormatDuration(totalHours: Double): String {
    // Kiểm tra đầu vào không hợp lệ
    if (totalHours < 0) return "0 giờ 0 phút"

    // Lấy phần giờ nguyên
    val hours = floor(totalHours).toInt()

    // Tính số phút còn lại từ phần thập phân của totalHours và làm tròn
    val minutes = ((totalHours - hours) * 60).roundToInt()

    // Xử lý trường hợp làm tròn phút thành 60
    if (minutes == 60) {
        return "${hours + 1} giờ 0 phút"
    }

    return "${hours} giờ ${minutes} phút"
}