package com.example.shipperincome.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shipperincome.BuildConfig
import com.example.shipperincome.ui.components.HeroHeader
import com.example.shipperincome.ui.components.LocalSnackbarHostState
import com.example.shipperincome.ui.components.MetricRow
import com.example.shipperincome.ui.components.SectionCard
import com.example.shipperincome.ui.components.StatusBarScrim
import com.example.shipperincome.ui.components.ThousandsSeparatorTransformation
import com.example.shipperincome.ui.theme.AppTheme
import com.example.shipperincome.ui.viewmodel.SettingsViewModel
import com.example.shipperincome.ui.viewmodel.ShiftViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, shiftViewModel: ShiftViewModel) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarHostState.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val shifts by shiftViewModel.allShifts.collectAsStateWithLifecycle()

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.messages.collect { snackbar.showSnackbar(it, withDismissAction = true) }
    }
    LaunchedEffect(shiftViewModel) {
        shiftViewModel.messages.collect { msg ->
            val result = snackbar.showSnackbar(msg.text, msg.actionLabel, withDismissAction = msg.actionLabel == null)
            if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = shiftViewModel.exportShiftsJson()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                        ?: error("no stream")
                }.isSuccess
            }
            snackbar.showSnackbar(if (ok) "Đã sao lưu ${shifts.size} ca làm." else "Không ghi được file sao lưu.")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (raw == null) snackbar.showSnackbar("Không đọc được file.")
            else shiftViewModel.importShiftsJson(raw)
        }
    }

    val scrollState = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val onHero = AppTheme.extended.onHero
        HeroHeader {
            Text("Cài đặt", style = MaterialTheme.typography.titleLarge, color = onHero)
            Text(
                "Thông số tính tiền và sao lưu dữ liệu",
                style = MaterialTheme.typography.bodyMedium,
                color = onHero.copy(alpha = 0.8f)
            )
        }

        SectionCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Thông số tính toán",
            icon = Icons.Outlined.Tune
        ) {
            Text(
                "Áp dụng cho ca làm mới hoặc khi bạn sửa lại ca đã có; ca cũ giữ nguyên giá trị đã lưu tại thời điểm nhập.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SettingField(
                value = settingsViewModel.plusTaxRateInput,
                onValueChange = settingsViewModel::updatePlusTaxRateInput,
                label = "Thuế Plus",
                suffix = "%",
                error = settingsViewModel.plusTaxRateError,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(12.dp))
            SettingField(
                value = settingsViewModel.normalTaxRateInput,
                onValueChange = settingsViewModel::updateNormalTaxRateInput,
                label = "Thuế thường",
                suffix = "%",
                error = settingsViewModel.normalTaxRateError,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(12.dp))
            SettingField(
                value = settingsViewModel.deliveryFeeInput,
                onValueChange = settingsViewModel::updateDeliveryFeeInput,
                label = "Phí mỗi đơn Plus",
                suffix = "₩",
                error = settingsViewModel.deliveryFeeError,
                keyboardType = KeyboardType.Number,
                money = true,
                imeAction = ImeAction.Done
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = settingsViewModel::showResetDialog,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Restore, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mặc định")
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        settingsViewModel.saveSettings()
                    },
                    enabled = settingsViewModel.hasChanges,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Lưu")
                }
            }
        }

        SectionCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Cách tính thực nhận",
            icon = Icons.Outlined.Calculate
        ) {
            FormulaLine("Plus", "(Thu nhập + nhiệm vụ − số đơn × phí đơn) × (1 − thuế Plus)")
            Spacer(Modifier.height(8.dp))
            FormulaLine("Thường", "(Thu nhập + nhiệm vụ) × (1 − thuế thường)")
        }

        SectionCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Sao lưu ca làm",
            icon = Icons.Outlined.Backup
        ) {
            Text(
                "Lưu toàn bộ ca làm ra file JSON (Drive, Downloads...) để chuyển máy hoặc phòng khi mất dữ liệu. Khôi phục sẽ gộp với dữ liệu hiện có.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { exportLauncher.launch("quickearn-backup-${LocalDate.now()}.json") },
                    enabled = shifts.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.FileUpload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sao lưu")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Khôi phục")
                }
            }
        }

        SectionCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Thông tin",
            icon = Icons.Outlined.Info
        ) {
            MetricRow("Phiên bản", BuildConfig.VERSION_NAME)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            MetricRow("Số ca đã lưu", "${shifts.size}")
        }
    }

    StatusBarScrim(visible = scrollState.value > 0)
    }

    if (settingsViewModel.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = settingsViewModel::hideResetDialog,
            icon = { Icon(Icons.Outlined.Restore, contentDescription = null) },
            title = { Text("Khôi phục mặc định?") },
            text = { Text("Thuế Plus và thuế thường về 5.7%, phí đơn về 100₩. Ca làm đã lưu không bị ảnh hưởng.") },
            confirmButton = { Button(onClick = settingsViewModel::resetToDefaults) { Text("Khôi phục") } },
            dismissButton = { TextButton(onClick = settingsViewModel::hideResetDialog) { Text("Hủy") } }
        )
    }
}

@Composable
private fun SettingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    error: String?,
    keyboardType: KeyboardType,
    money: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = if (!money) { { Icon(Icons.Outlined.Percent, null) } } else null,
        suffix = { Text(suffix) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (money) ThousandsSeparatorTransformation else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FormulaLine(label: String, formula: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(formula, style = MaterialTheme.typography.bodyMedium)
    }
}
