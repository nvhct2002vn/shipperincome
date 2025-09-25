package com.example.shipperincome.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipperincome.data.repository.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    // State cho tax rate (tỷ lệ thuế)
    var taxRateInput by mutableStateOf("")
        private set

    // State cho delivery fee (phí đơn hàng)
    var deliveryFeeInput by mutableStateOf("")
        private set

    // State cho error messages
    var taxRateError by mutableStateOf<String?>(null)
        private set
    var deliveryFeeError by mutableStateOf<String?>(null)
        private set

    // State để hiển thị confirmation dialog
    var showResetConfirmDialog by mutableStateOf(false)
        private set

    init {
        loadSettings()
    }

    // Load settings từ repository
    private fun loadSettings() {
        val taxRate = repository.getTaxRate()
        val deliveryFee = repository.getDeliveryFee()

        // Convert thành phần trăm và format cho UI với Locale US để dùng dấu chấm
        taxRateInput = String.format(java.util.Locale.US, "%.1f", taxRate * 100) // Ví dụ: 5.7
        deliveryFeeInput = deliveryFee.toInt().toString() // Ví dụ: 100
    }

    // Update tax rate input
    fun updateTaxRateInput(value: String) {
        taxRateInput = value.filter { it.isDigit() || it == '.' }
        taxRateError = null // Clear error khi user nhập
    }

    // Update delivery fee input
    fun updateDeliveryFeeInput(value: String) {
        deliveryFeeInput = value.filter { it.isDigit() }
        deliveryFeeError = null // Clear error khi user nhập
    }

    // Validate và save settings
    fun saveSettings() {
        var hasError = false

        // Validate tax rate
        val taxRate = taxRateInput.toDoubleOrNull()
        if (taxRate == null || taxRate < 0 || taxRate > 100) {
            taxRateError = "Thuế phải từ 0% đến 100%"
            hasError = true
        }

        // Validate delivery fee
        val deliveryFee = deliveryFeeInput.toDoubleOrNull()
        if (deliveryFee == null || deliveryFee < 0) {
            deliveryFeeError = "Phí đơn hàng phải là số không âm"
            hasError = true
        }

        if (hasError) return

        // Save to repository
        viewModelScope.launch {
            repository.setTaxRate(taxRate!! / 100.0) // Convert từ % về decimal
            repository.setDeliveryFee(deliveryFee!!)
        }
    }

    // Show reset confirmation dialog
    fun showResetDialog() {
        showResetConfirmDialog = true
    }

    // Hide reset confirmation dialog
    fun hideResetDialog() {
        showResetConfirmDialog = false
    }

    // Reset về giá trị mặc định
    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
            loadSettings() // Reload UI
            hideResetDialog()
        }
    }

    // Get current settings để sử dụng trong các ViewModel khác
    fun getCurrentSettings(): Pair<Double, Double> {
        return Pair(repository.getTaxRate(), repository.getDeliveryFee())
    }
}