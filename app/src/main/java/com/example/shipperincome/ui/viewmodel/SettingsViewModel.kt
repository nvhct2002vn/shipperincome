package com.example.shipperincome.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.shipperincome.data.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.math.BigDecimal
import java.math.RoundingMode

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

    /** True khi o nhap khac gia tri dang luu, de bat/tat nut Luu. */
    val hasChanges: Boolean
        get() = plusTaxRateInput != percentText(repository.getPlusTaxRate()) ||
            normalTaxRateInput != percentText(repository.getNormalTaxRate()) ||
            deliveryFeeInput != feeText(repository.getDeliveryFee())

    init {
        loadSettings()
    }

    private fun loadSettings() {
        plusTaxRateInput = percentText(repository.getPlusTaxRate())
        normalTaxRateInput = percentText(repository.getNormalTaxRate())
        deliveryFeeInput = feeText(repository.getDeliveryFee())
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

        if (hasError || plusTaxRate == null || normalTaxRate == null || deliveryFee == null) return

        repository.setPlusTaxRate(plusTaxRate / 100.0)
        repository.setNormalTaxRate(normalTaxRate / 100.0)
        repository.setDeliveryFee(deliveryFee)
        loadSettings()
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
        loadSettings()
        plusTaxRateError = null
        normalTaxRateError = null
        deliveryFeeError = null
        hideResetDialog()
        _messages.trySend("Đã khôi phục mặc định.")
    }
}
