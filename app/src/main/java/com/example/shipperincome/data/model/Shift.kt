package com.example.shipperincome.data.model

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class Shift(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val startTime: String,
    val endTime: String,
    val deliveryCount: Int = 0,
    val plusGrossIncome: Double = 0.0,
    val normalGrossIncome: Double = 0.0,
    val plusMissionIncome: Double = 0.0,
    val normalMissionIncome: Double = 0.0,
    val plusTaxRateSnapshot: Double = 0.057,
    val normalTaxRateSnapshot: Double = 0.057,
    val deliveryFeeSnapshot: Double = 100.0
) {
    fun calculatePlusGrossIncome(): Double = plusGrossIncome + plusMissionIncome

    fun calculateNormalGrossIncome(): Double = normalGrossIncome + normalMissionIncome

    fun calculateGrossIncome(): Double = calculatePlusGrossIncome() + calculateNormalGrossIncome()

    fun calculatePlusNetIncome(): Double {
        val taxableIncome = calculatePlusGrossIncome()
        if (taxableIncome <= 0.0) return 0.0

        val incomeAfterDeliveryFee = taxableIncome - (deliveryCount * deliveryFeeSnapshot)
        return incomeAfterDeliveryFee - (incomeAfterDeliveryFee * plusTaxRateSnapshot)
    }

    fun calculateNormalNetIncome(): Double {
        val taxableIncome = calculateNormalGrossIncome()
        if (taxableIncome <= 0.0) return 0.0

        return taxableIncome - (taxableIncome * normalTaxRateSnapshot)
    }

    fun calculateNetIncome(): Double = calculatePlusNetIncome() + calculateNormalNetIncome()

    fun calculateDurationHours(): Double {
        try {
            val start = LocalTime.parse(startTime)
            val end = LocalTime.parse(endTime)

            return if (end.isBefore(start) || end == start) {
                val minutesToMidnight = Duration.between(start, LocalTime.MAX).toMinutes() + 1
                val minutesFromMidnight = Duration.between(LocalTime.MIN, end).toMinutes()
                (minutesToMidnight + minutesFromMidnight) / 60.0
            } else {
                Duration.between(start, end).toMinutes() / 60.0
            }
        } catch (e: Exception) {
            return 0.0
        }
    }

    fun calculateHourlyRate(): Double {
        val duration = calculateDurationHours()
        return if (duration > 0) calculateNetIncome() / duration else 0.0
    }

    val localDate: LocalDate by lazy {
        try {
            LocalDate.parse(date, DateTimeFormatter.ISO_DATE)
        } catch (e: Exception) {
            LocalDate.MIN
        }
    }
}
