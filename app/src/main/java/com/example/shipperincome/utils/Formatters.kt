package com.example.shipperincome.utils

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val VI = Locale("vi", "VN")

val DateFull: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", VI)
val DateShort: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", VI)
val DateWithWeekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", VI)
val DateWithShortWeekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd/MM", VI)
val TimeHm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Dinh dang tien Han Quoc: 12345.6 -> "12,346₩". */
fun formatCurrency(amount: Double): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    return "${numberFormat.format(amount.roundToLong())}₩"
}

/** Ban rut gon cho truc bieu do: 1_250_000 -> "1.3tr", 85_000 -> "85k". */
fun formatCompactCurrency(amount: Double): String = when {
    amount >= 1_000_000 -> String.format(Locale.US, "%.1ftr", amount / 1_000_000)
    amount >= 1_000 -> "${(amount / 1_000).roundToInt()}k"
    else -> amount.roundToInt().toString()
}

/**
 * Dinh dang tong so gio thanh "X giờ Y phút".
 * Vi du: 8.5 -> "8 giờ 30 phút", 0.75 -> "0 giờ 45 phút".
 */
fun formatDuration(totalHours: Double): String {
    if (totalHours < 0) return "0 giờ 0 phút"
    var hours = floor(totalHours).toInt()
    var minutes = ((totalHours - hours) * 60).roundToInt()
    if (minutes == 60) {
        hours += 1
        minutes = 0
    }
    return "$hours giờ $minutes phút"
}

/** Ban ngan cho the nho: 8.5 -> "8h30". */
fun formatDurationShort(totalHours: Double): String {
    if (totalHours <= 0) return "0h"
    var hours = floor(totalHours).toInt()
    var minutes = ((totalHours - hours) * 60).roundToInt()
    if (minutes == 60) {
        hours += 1
        minutes = 0
    }
    return if (minutes == 0) "${hours}h" else "${hours}h%02d".format(minutes)
}

/** Them dau phay ngan cach hang nghin vao chuoi chi gom chu so: "1234567" -> "1,234,567". */
fun groupDigits(digits: String): String {
    if (digits.length <= 3) return digits
    val sb = StringBuilder(digits.length + digits.length / 3)
    digits.forEachIndexed { index, c ->
        if (index > 0 && (digits.length - index) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return sb.toString()
}

fun Double.toPlainInput(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
