package com.example.shipperincome.data.repository

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TAX_RATE = "tax_rate"
        private const val KEY_DELIVERY_FEE = "delivery_fee"

        // Giá trị mặc định
        const val DEFAULT_TAX_RATE = 0.057 // 5.7%
        const val DEFAULT_DELIVERY_FEE = 100.0 // 100₩ per đơn
    }

    // Lưu và đọc tỷ lệ thuế
    fun getTaxRate(): Double {
        return prefs.getFloat(KEY_TAX_RATE, DEFAULT_TAX_RATE.toFloat()).toDouble()
    }

    fun setTaxRate(taxRate: Double) {
        prefs.edit().putFloat(KEY_TAX_RATE, taxRate.toFloat()).apply()
    }

    // Lưu và đọc phí đơn hàng
    fun getDeliveryFee(): Double {
        return prefs.getFloat(KEY_DELIVERY_FEE, DEFAULT_DELIVERY_FEE.toFloat()).toDouble()
    }

    fun setDeliveryFee(deliveryFee: Double) {
        prefs.edit().putFloat(KEY_DELIVERY_FEE, deliveryFee.toFloat()).apply()
    }

    // Reset về giá trị mặc định
    fun resetToDefaults() {
        prefs.edit()
            .putFloat(KEY_TAX_RATE, DEFAULT_TAX_RATE.toFloat())
            .putFloat(KEY_DELIVERY_FEE, DEFAULT_DELIVERY_FEE.toFloat())
            .apply()
    }
}