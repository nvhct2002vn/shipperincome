package com.example.shipperincome

import com.example.shipperincome.data.model.Shift
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftCalculationTest {

    @Test
    fun plusIncomeSubtractsDeliveryFeeBeforeTax() {
        val shift = Shift(
            date = "2026-05-15",
            startTime = "10:00",
            endTime = "12:00",
            deliveryCount = 3,
            plusGrossIncome = 100_000.0,
            normalGrossIncome = 0.0,
            plusTaxRateSnapshot = 0.1,
            normalTaxRateSnapshot = 0.2,
            deliveryFeeSnapshot = 1_000.0
        )

        assertEquals(87_300.0, shift.calculatePlusNetIncome(), 0.001)
        assertEquals(87_300.0, shift.calculateNetIncome(), 0.001)
    }

    @Test
    fun normalIncomeOnlySubtractsNormalTax() {
        val shift = Shift(
            date = "2026-05-15",
            startTime = "10:00",
            endTime = "12:00",
            deliveryCount = 0,
            plusGrossIncome = 0.0,
            normalGrossIncome = 100_000.0,
            plusTaxRateSnapshot = 0.1,
            normalTaxRateSnapshot = 0.2,
            deliveryFeeSnapshot = 1_000.0
        )

        assertEquals(80_000.0, shift.calculateNormalNetIncome(), 0.001)
        assertEquals(80_000.0, shift.calculateNetIncome(), 0.001)
    }

    @Test
    fun combinedIncomeAddsPlusAndNormalNetIncome() {
        val shift = Shift(
            date = "2026-05-15",
            startTime = "10:00",
            endTime = "12:00",
            deliveryCount = 3,
            plusGrossIncome = 100_000.0,
            normalGrossIncome = 100_000.0,
            plusTaxRateSnapshot = 0.1,
            normalTaxRateSnapshot = 0.2,
            deliveryFeeSnapshot = 1_000.0
        )

        assertEquals(167_300.0, shift.calculateNetIncome(), 0.001)
        assertEquals(83_650.0, shift.calculateHourlyRate(), 0.001)
    }

    @Test
    fun plusMissionIncomeIsTaxedWithPlusIncome() {
        val shift = Shift(
            date = "2026-05-15",
            startTime = "10:00",
            endTime = "12:00",
            deliveryCount = 3,
            plusGrossIncome = 100_000.0,
            plusMissionIncome = 20_000.0,
            normalGrossIncome = 0.0,
            plusTaxRateSnapshot = 0.1,
            normalTaxRateSnapshot = 0.2,
            deliveryFeeSnapshot = 1_000.0
        )

        assertEquals(120_000.0, shift.calculatePlusGrossIncome(), 0.001)
        assertEquals(105_300.0, shift.calculatePlusNetIncome(), 0.001)
    }

    @Test
    fun normalMissionIncomeIsTaxedWithNormalIncome() {
        val shift = Shift(
            date = "2026-05-15",
            startTime = "10:00",
            endTime = "12:00",
            deliveryCount = 0,
            plusGrossIncome = 0.0,
            normalGrossIncome = 100_000.0,
            normalMissionIncome = 20_000.0,
            plusTaxRateSnapshot = 0.1,
            normalTaxRateSnapshot = 0.2,
            deliveryFeeSnapshot = 1_000.0
        )

        assertEquals(120_000.0, shift.calculateNormalGrossIncome(), 0.001)
        assertEquals(96_000.0, shift.calculateNormalNetIncome(), 0.001)
    }
}
