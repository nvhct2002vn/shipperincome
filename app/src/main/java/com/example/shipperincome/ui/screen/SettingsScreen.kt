package com.example.shipperincome.ui.screen

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
import com.example.shipperincome.ui.components.PickerField
import com.example.shipperincome.ui.components.SingleDatePickerDialog
import com.example.shipperincome.ui.components.SectionCard
import com.example.shipperincome.ui.components.StatusBarScrim
import com.example.shipperincome.ui.components.ThousandsSeparatorTransformation
import com.example.shipperincome.ui.theme.AppTheme
import com.example.shipperincome.ui.viewmodel.NotesViewModel
import com.example.shipperincome.ui.viewmodel.SettingsViewModel
import com.example.shipperincome.ui.viewmodel.ShiftViewModel
import com.example.shipperincome.utils.DateFull
import com.example.shipperincome.widget.QuickEarnWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    shiftViewModel: ShiftViewModel,
    notesViewModel: NotesViewModel
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarHostState.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val shifts by shiftViewModel.allShifts.collectAsStateWithLifecycle()
    var showAnchorPicker by remember { mutableStateOf(false) }
    var showNotesImport by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(notesViewModel) {
        notesViewModel.messages.collect { snackbar.showSnackbar(it, withDismissAction = true) }
    }

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
                "Thông số tính tiền, điểm rank và ghi chú địa điểm",
                style = MaterialTheme.typography.bodyMedium,
                color = onHero.copy(alpha = 0.8f)
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            listOf("Nhập liệu", "Ghi chú").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.titleSmall) }
                )
            }
        }

        if (selectedTab == 1) {
            MonitorCard(
                notesViewModel = notesViewModel,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            DataCard(
                onExport = {
                    scope.launch {
                        notesViewModel.exportJson()?.let { json ->
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                putExtra(android.content.Intent.EXTRA_TEXT, json)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Lưu hoặc chia sẻ dữ liệu"))
                        }
                    }
                },
                onImport = { showNotesImport = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
        SectionCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Hiển thị",
            icon = Icons.Outlined.Visibility
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ẩn thu nhập Plus", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Chỉ nhập thu nhập thường. Ca cũ có Plus vẫn được tính và vẫn hiện khi bạn sửa ca đó.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = shiftViewModel.hidePlus, onCheckedChange = shiftViewModel::updateHidePlus)
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Widget màn hình chính", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Xem thu nhập hôm nay và điểm rank còn cần mà không phải mở app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(onClick = {
                    val manager = AppWidgetManager.getInstance(context)
                    if (manager.isRequestPinAppWidgetSupported) {
                        manager.requestPinAppWidget(ComponentName(context, QuickEarnWidgetReceiver::class.java), null, null)
                    } else {
                        scope.launch {
                            snackbar.showSnackbar("Giữ tay trên màn hình chính → Widget → QuickEarn", withDismissAction = true)
                        }
                    }
                }) {
                    Icon(Icons.Outlined.Widgets, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Thêm")
                }
            }
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
            // Khi an Plus thi chi con o thue thuong; gia tri Plus van giu nguyen va van duoc kiem tra khi luu.
            val showPlusFields = !shiftViewModel.hidePlus
            if (showPlusFields) {
                SettingField(
                    value = settingsViewModel.plusTaxRateInput,
                    onValueChange = settingsViewModel::updatePlusTaxRateInput,
                    label = "Thuế Plus",
                    suffix = "%",
                    error = settingsViewModel.plusTaxRateError,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(Modifier.height(12.dp))
            }
            SettingField(
                value = settingsViewModel.normalTaxRateInput,
                onValueChange = settingsViewModel::updateNormalTaxRateInput,
                label = "Thuế thường",
                suffix = "%",
                error = settingsViewModel.normalTaxRateError,
                keyboardType = KeyboardType.Decimal,
                imeAction = if (showPlusFields) ImeAction.Next else ImeAction.Done
            )
            if (showPlusFields) {
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
            }
            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Outlined.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Điểm rank", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                "Chọn ngày bắt đầu của một chu kỳ (vd. chu kỳ đang chạy). Các chu kỳ sau app tự nối tiếp.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = settingsViewModel.rankTargetInput,
                    onValueChange = settingsViewModel::updateRankTargetInput,
                    label = { Text("Chỉ tiêu") },
                    suffix = { Text("điểm") },
                    isError = settingsViewModel.rankTargetError != null,
                    supportingText = settingsViewModel.rankTargetError?.let { { Text(it) } },
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = settingsViewModel.rankCycleDaysInput,
                    onValueChange = settingsViewModel::updateRankCycleDaysInput,
                    label = { Text("Mỗi chu kỳ") },
                    suffix = { Text("ngày") },
                    isError = settingsViewModel.rankCycleDaysError != null,
                    supportingText = settingsViewModel.rankCycleDaysError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            PickerField(
                label = "Ngày bắt đầu chu kỳ",
                value = settingsViewModel.rankAnchor?.let { start ->
                    val end = settingsViewModel.rankAnchorEnd
                    start.format(DateFull) + (end?.let { " → ${it.format(DateFull)}" } ?: "")
                } ?: "",
                placeholder = "Chạm để chọn ngày",
                leadingIcon = Icons.Outlined.CalendarMonth,
                onClick = { showAnchorPicker = true }
            )

            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Flag, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Mục tiêu thu nhập", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                "Tính theo thực nhận (sau thuế). Để trống nếu không muốn đặt. Xem tiến độ ở Thống kê (Tuần này / Tháng này) và trên widget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = settingsViewModel.weeklyGoalInput,
                    onValueChange = settingsViewModel::updateWeeklyGoalInput,
                    label = { Text("Mỗi tuần") },
                    suffix = { Text("₩") },
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = settingsViewModel.monthlyGoalInput,
                    onValueChange = settingsViewModel::updateMonthlyGoalInput,
                    label = { Text("Mỗi tháng") },
                    suffix = { Text("₩") },
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                )
            }

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
    }

    StatusBarScrim(visible = scrollState.value > 0)
    }

    if (showNotesImport) {
        ImportDialog(
            onDismiss = { showNotesImport = false },
            onImport = { json ->
                notesViewModel.importJson(json)
                showNotesImport = false
            }
        )
    }

    if (showAnchorPicker) {
        SingleDatePickerDialog(
            initial = settingsViewModel.rankAnchor ?: LocalDate.now(),
            onDismiss = { showAnchorPicker = false },
            onConfirm = settingsViewModel::updateRankAnchor
        )
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
