package com.example.shipperincome.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Tong diem rank cong don tinh den het ngay [date] (khong phai diem rieng cua ngay do). */
@Serializable
data class RankEntry(
    val date: String,
    val totalPoints: Int
) {
    val localDate: LocalDate
        get() = LocalDate.parse(date)
}
