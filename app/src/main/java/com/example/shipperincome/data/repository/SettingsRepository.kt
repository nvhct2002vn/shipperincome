package com.example.shipperincome.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        // Khoa cu luu dang Float (bi sai so, vd 0.057 -> 0.0570000000298). Chi con dung de doc lan dau.
        private const val LEGACY_KEY_TAX_RATE = "tax_rate"
        private const val LEGACY_KEY_PLUS_TAX_RATE = "plus_tax_rate"
        private const val LEGACY_KEY_NORMAL_TAX_RATE = "normal_tax_rate"
        private const val LEGACY_KEY_DELIVERY_FEE = "delivery_fee"

        private const val KEY_PLUS_TAX_RATE = "plus_tax_rate_v2"
        private const val KEY_NORMAL_TAX_RATE = "normal_tax_rate_v2"
        private const val KEY_DELIVERY_FEE = "delivery_fee_v2"
        private const val KEY_TOTAL_HOURS_MODE = "total_hours_mode"
        private const val KEY_HIDE_PLUS = "hide_plus"
        private const val KEY_GOAL_WEEK = "income_goal_week"
        private const val KEY_GOAL_MONTH = "income_goal_month"
        private const val KEY_RANK_TARGET = "rank_target_points"
        private const val KEY_RANK_CYCLE_DAYS = "rank_cycle_days"
        private const val KEY_RANK_CYCLE_ANCHOR = "rank_cycle_anchor"

        const val DEFAULT_RANK_TARGET = 7000
        const val DEFAULT_RANK_CYCLE_DAYS = 14

        const val DEFAULT_TAX_RATE = 0.057
        const val DEFAULT_PLUS_TAX_RATE = 0.057
        const val DEFAULT_NORMAL_TAX_RATE = 0.057
        const val DEFAULT_DELIVERY_FEE = 100.0
    }

    fun getPlusTaxRate(): Double =
        readDouble(KEY_PLUS_TAX_RATE, LEGACY_KEY_PLUS_TAX_RATE, LEGACY_KEY_TAX_RATE, DEFAULT_PLUS_TAX_RATE)

    fun setPlusTaxRate(taxRate: Double) = writeDouble(KEY_PLUS_TAX_RATE, taxRate)

    fun getNormalTaxRate(): Double =
        readDouble(KEY_NORMAL_TAX_RATE, LEGACY_KEY_NORMAL_TAX_RATE, LEGACY_KEY_TAX_RATE, DEFAULT_NORMAL_TAX_RATE)

    fun setNormalTaxRate(taxRate: Double) = writeDouble(KEY_NORMAL_TAX_RATE, taxRate)

    fun getDeliveryFee(): Double =
        readDouble(KEY_DELIVERY_FEE, LEGACY_KEY_DELIVERY_FEE, null, DEFAULT_DELIVERY_FEE)

    fun setDeliveryFee(deliveryFee: Double) = writeDouble(KEY_DELIVERY_FEE, deliveryFee)

    /** Chi tieu diem rank va chu ky; [cycleAnchor] null khi nguoi dung chua chon ngay bat dau. */
    fun getRankConfig(): RankConfig = RankConfig(
        targetPoints = prefs.getInt(KEY_RANK_TARGET, DEFAULT_RANK_TARGET),
        cycleDays = prefs.getInt(KEY_RANK_CYCLE_DAYS, DEFAULT_RANK_CYCLE_DAYS),
        cycleAnchor = prefs.getString(KEY_RANK_CYCLE_ANCHOR, null)
            ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
    )

    fun setRankConfig(config: RankConfig) {
        prefs.edit {
            putInt(KEY_RANK_TARGET, config.targetPoints)
            putInt(KEY_RANK_CYCLE_DAYS, config.cycleDays)
            if (config.cycleAnchor != null) putString(KEY_RANK_CYCLE_ANCHOR, config.cycleAnchor.toString())
            else remove(KEY_RANK_CYCLE_ANCHOR)
        }
    }

    /** Lan nhap truoc dung che do "tong so gio" hay "khung gio"; mo app lai van giu lua chon. */
    fun isTotalHoursMode(): Boolean = prefs.getBoolean(KEY_TOTAL_HOURS_MODE, false)

    fun setTotalHoursMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_TOTAL_HOURS_MODE, enabled) }
    }

    /** Muc tieu thuc nhan tuan/thang; 0 = khong dat muc tieu. */
    fun getIncomeGoals(): IncomeGoals = IncomeGoals(
        weekly = prefs.getString(KEY_GOAL_WEEK, null)?.toDoubleOrNull() ?: 0.0,
        monthly = prefs.getString(KEY_GOAL_MONTH, null)?.toDoubleOrNull() ?: 0.0
    )

    fun setIncomeGoals(goals: IncomeGoals) {
        prefs.edit {
            putString(KEY_GOAL_WEEK, goals.weekly.toString())
            putString(KEY_GOAL_MONTH, goals.monthly.toString())
        }
    }

    /** An phan thu nhap Plus khi nguoi dung chi chay don thuong; ca cu co Plus van duoc tinh. */
    fun isPlusHidden(): Boolean = prefs.getBoolean(KEY_HIDE_PLUS, false)

    fun setPlusHidden(hidden: Boolean) {
        prefs.edit { putBoolean(KEY_HIDE_PLUS, hidden) }
    }

    fun resetToDefaults() {
        prefs.edit {
            putString(KEY_PLUS_TAX_RATE, DEFAULT_PLUS_TAX_RATE.toString())
            putString(KEY_NORMAL_TAX_RATE, DEFAULT_NORMAL_TAX_RATE.toString())
            putString(KEY_DELIVERY_FEE, DEFAULT_DELIVERY_FEE.toString())
        }
    }

    private fun readDouble(key: String, legacyKey: String, legacyFallbackKey: String?, default: Double): Double {
        prefs.getString(key, null)?.toDoubleOrNull()?.let { return it }
        val legacy = when {
            prefs.contains(legacyKey) -> prefs.getFloat(legacyKey, default.toFloat())
            legacyFallbackKey != null && prefs.contains(legacyFallbackKey) ->
                prefs.getFloat(legacyFallbackKey, default.toFloat())
            else -> return default
        }
        // Lam tron ve 6 chu so de bo sai so Float, roi luu lai theo dinh dang moi.
        val cleaned = Math.round(legacy.toDouble() * 1_000_000) / 1_000_000.0
        writeDouble(key, cleaned)
        return cleaned
    }

    private fun writeDouble(key: String, value: Double) {
        prefs.edit { putString(key, value.toString()) }
    }
}
