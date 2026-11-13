package com.example.shipperincome.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.shipperincome.data.repository.IncomeGoals
import com.example.shipperincome.data.repository.RankConfig
import com.example.shipperincome.data.repository.SettingsRepository
import com.example.shipperincome.widget.QuickEarnWidget
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    var plusTaxRateInput by mutableStateOf("")
        private set
    var normalTaxRateInput by mutableStateOf("")
        private set
    var deliveryFeeInput by mutableStateOf("")
        private set

    var plusTaxRateError by mutableStateOf<String?>(null)
        private set
    var normalTaxRateError by mutableStateOf<String?>(null)
        private set
    var deliveryFeeError by mutableStateOf<String?>(null)
        private set

    var showResetConfirmDialog by mutableStateOf(false)
        private set

    // Diem rank: chi tieu, so ngay moi chu ky, ngay bat dau mot chu ky.
    var rankTargetInput by mutableStateOf("")
        private set
    var rankCycleDaysInput by mutableStateOf("")
        private set
    var rankAnchor by mutableStateOf<LocalDate?>(null)
        private set
    var rankTargetError by mutableStateOf<String?>(null)
        private set
    var rankCycleDaysError by mutableStateOf<String?>(null)
        private set

    // Muc tieu thuc nhan; de trong = khong dat.
    var weeklyGoalInput by mutableStateOf("")
        private set
    var monthlyGoalInput by mutableStateOf("")
        private set

    /** Ngay ket thuc chu ky dau tien tinh tu [rankAnchor], de nguoi dung doi chieu (vd. 19/10 -> 01/11). */
    val rankAnchorEnd: LocalDate?
        get() {
            val days = rankCycleDaysInput.toIntOrNull()?.takeIf { it in 1..MAX_CYCLE_DAYS } ?: return null
            return rankAnchor?.plusDays(days - 1L)
        }

    /** True khi o nhap khac gia tri dang luu, de bat/tat nut Luu. */
    val hasChanges: Boolean
        get() = plusTaxRateInput != percentText(repository.getPlusTaxRate()) ||
            normalTaxRateInput != percentText(repository.getNormalTaxRate()) ||
            deliveryFeeInput != feeText(repository.getDeliveryFee()) ||
            rankChanged || goalsChanged

    private val goalsChanged: Boolean
        get() {
            val saved = repository.getIncomeGoals()
            return weeklyGoalInput != goalText(saved.weekly) || monthlyGoalInput != goalText(saved.monthly)
        }

    private val rankChanged: Boolean
        get() {
            val saved = repository.getRankConfig()
            return rankTargetInput != saved.targetPoints.toString() ||
                rankCycleDaysInput != saved.cycleDays.toString() ||
                rankAnchor != saved.cycleAnchor
        }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        plusTaxRateInput = percentText(repository.getPlusTaxRate())
        normalTaxRateInput = percentText(repository.getNormalTaxRate())
        deliveryFeeInput = feeText(repository.getDeliveryFee())
        val rank = repository.getRankConfig()
        rankTargetInput = rank.targetPoints.toString()
        rankCycleDaysInput = rank.cycleDays.toString()
        rankAnchor = rank.cycleAnchor
        val goals = repository.getIncomeGoals()
        weeklyGoalInput = goalText(goals.weekly)
        monthlyGoalInput = goalText(goals.monthly)
    }

    private fun goalText(value: Double): String = if (value <= 0.0) "" else feeText(value)

    fun updateWeeklyGoalInput(value: String) {
        weeklyGoalInput = value.filter { it.isDigit() }.trimStart('0').take(9)
    }

    fun updateMonthlyGoalInput(value: String) {
        monthlyGoalInput = value.filter { it.isDigit() }.trimStart('0').take(10)
    }

    fun updateRankTargetInput(value: String) {
        rankTargetInput = value.filter { it.isDigit() }.take(7)
        rankTargetError = null
    }

    fun updateRankCycleDaysInput(value: String) {
        rankCycleDaysInput = value.filter { it.isDigit() }.take(2)
        rankCycleDaysError = null
    }

    fun updateRankAnchor(date: LocalDate) {
        rankAnchor = date
    }

    /** 0.057 -> "5.7", 0.1 -> "10" (bo so 0 thua, toi da 3 chu so thap phan). */
    private fun percentText(rate: Double): String =
        BigDecimal(rate * 100).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private fun feeText(fee: Double): String =
        BigDecimal(fee).setScale(0, RoundingMode.HALF_UP).toPlainString()

    fun updatePlusTaxRateInput(value: String) {
        plusTaxRateInput = sanitizeDecimal(value)
        plusTaxRateError = null
    }

    fun updateNormalTaxRateInput(value: String) {
        normalTaxRateInput = sanitizeDecimal(value)
        normalTaxRateError = null
    }

    fun updateDeliveryFeeInput(value: String) {
        deliveryFeeInput = value.filter { it.isDigit() }.take(7)
        deliveryFeeError = null
    }

    /** Chi giu chu so va mot dau thap phan; chap nhan ca dau phay. */
    private fun sanitizeDecimal(value: String): String {
        val normalized = value.replace(',', '.')
        val sb = StringBuilder()
        var hasDot = false
        normalized.forEach { c ->
            when {
                c.isDigit() -> sb.append(c)
                c == '.' && !hasDot -> { sb.append(c); hasDot = true }
            }
        }
        return sb.toString().take(6)
    }

    fun saveSettings() {
        var hasError = false

        val plusTaxRate = plusTaxRateInput.toDoubleOrNull()
        if (plusTaxRate == null || plusTaxRate < 0 || plusTaxRate > 100) {
            plusTaxRateError = "Thuế Plus phải từ 0% đến 100%"
            hasError = true
        }

        val normalTaxRate = normalTaxRateInput.toDoubleOrNull()
        if (normalTaxRate == null || normalTaxRate < 0 || normalTaxRate > 100) {
            normalTaxRateError = "Thuế thường phải từ 0% đến 100%"
            hasError = true
        }

        val deliveryFee = deliveryFeeInput.toDoubleOrNull()
        if (deliveryFee == null || deliveryFee < 0) {
            deliveryFeeError = "Phí đơn hàng phải là số không âm"
            hasError = true
        }

        val rankTarget = rankTargetInput.toIntOrNull()
        if (rankTarget == null || rankTarget <= 0) {
            rankTargetError = "Chỉ tiêu điểm phải lớn hơn 0"
            hasError = true
        }
        val cycleDays = rankCycleDaysInput.toIntOrNull()
        if (cycleDays == null || cycleDays !in 1..MAX_CYCLE_DAYS) {
            rankCycleDaysError = "Số ngày mỗi chu kỳ từ 1 đến $MAX_CYCLE_DAYS"
            hasError = true
        }

        if (hasError || plusTaxRate == null || normalTaxRate == null || deliveryFee == null ||
            rankTarget == null || cycleDays == null
        ) return

        repository.setRankConfig(RankConfig(rankTarget, cycleDays, rankAnchor))
        repository.setIncomeGoals(
            IncomeGoals(
                weekly = weeklyGoalInput.toDoubleOrNull() ?: 0.0,
                monthly = monthlyGoalInput.toDoubleOrNull() ?: 0.0
            )
        )

        repository.setPlusTaxRate(plusTaxRate / 100.0)
        repository.setNormalTaxRate(normalTaxRate / 100.0)
        repository.setDeliveryFee(deliveryFee)
        loadSettings()
        QuickEarnWidget.refresh(getApplication())
        _messages.trySend("Đã lưu cài đặt. Áp dụng cho ca làm mới.")
    }

    fun showResetDialog() {
        showResetConfirmDialog = true
    }

    fun hideResetDialog() {
        showResetConfirmDialog = false
    }

    fun resetToDefaults() {
        repository.resetToDefaults()
        // Khoi phuc mac dinh chi ap dung cho thue/phi; giu nguyen cac o diem rank va muc tieu dang nhap.
        val keepTarget = rankTargetInput
        val keepDays = rankCycleDaysInput
        val keepAnchor = rankAnchor
        val keepWeeklyGoal = weeklyGoalInput
        val keepMonthlyGoal = monthlyGoalInput
        loadSettings()
        rankTargetInput = keepTarget
        rankCycleDaysInput = keepDays
        rankAnchor = keepAnchor
        weeklyGoalInput = keepWeeklyGoal
        monthlyGoalInput = keepMonthlyGoal
        plusTaxRateError = null
        normalTaxRateError = null
        deliveryFeeError = null
        hideResetDialog()
        _messages.trySend("Đã khôi phục mặc định.")
    }

    companion object {
        const val MAX_CYCLE_DAYS = 60
    }
}
