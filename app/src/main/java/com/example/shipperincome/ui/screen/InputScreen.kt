package com.example.shipperincome.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shipperincome.data.model.Shift
import com.example.shipperincome.ui.components.ColorDot
import com.example.shipperincome.ui.components.EmptyState
import com.example.shipperincome.ui.components.ErrorBanner
import com.example.shipperincome.ui.components.HeroHeader
import com.example.shipperincome.ui.components.HeroStat
import com.example.shipperincome.ui.components.LocalSnackbarHostState
import com.example.shipperincome.ui.components.MetricRow
import com.example.shipperincome.ui.components.MoneyField
import com.example.shipperincome.ui.components.PickerField
import com.example.shipperincome.ui.components.SectionCard
import com.example.shipperincome.ui.components.SingleDatePickerDialog
import com.example.shipperincome.ui.components.StatusBarScrim
import com.example.shipperincome.ui.components.TimePickerDialog
import com.example.shipperincome.ui.theme.AppTheme
import com.example.shipperincome.ui.viewmodel.DailySummary
import com.example.shipperincome.ui.viewmodel.ShiftViewModel
import com.example.shipperincome.utils.DateWithWeekday
import com.example.shipperincome.utils.TimeHm
import com.example.shipperincome.utils.formatCurrency
import com.example.shipperincome.utils.formatDuration
import com.example.shipperincome.utils.formatDurationShort
import com.example.shipperincome.utils.minutesToHoursInput
import com.example.shipperincome.utils.parseHoursToMinutes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun InputScreen(viewModel: ShiftViewModel) {
    val shiftsForDay by viewModel.shiftsForSelectedDate.collectAsStateWithLifecycle()
    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val selectedDate = viewModel.selectedDate

    val snackbar = LocalSnackbarHostState.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var showDatePicker by remember { mutableStateOf(false) }
    var timePickerFor by remember { mutableStateOf<Boolean?>(null) } // true = bat dau, false = ket thuc

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            val result = snackbar.showSnackbar(
                message = msg.text,
                actionLabel = msg.actionLabel,
                withDismissAction = msg.actionLabel == null,
                duration = if (msg.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
        }
    }

    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            DayHeader(
                date = selectedDate,
                summary = dailySummary,
                onPrev = { viewModel.changeSelectedDate(selectedDate.minusDays(1)) },
                onNext = { viewModel.changeSelectedDate(selectedDate.plusDays(1)) },
                onPickDate = { showDatePicker = true },
                onToday = { viewModel.changeSelectedDate(LocalDate.now()) }
            )
        }

        item(key = "form") {
            ShiftForm(
                viewModel = viewModel,
                onPickStart = { timePickerFor = true },
                onPickEnd = { timePickerFor = false },
                onSave = {
                    focusManager.clearFocus()
                    viewModel.addOrUpdateShift()
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "list_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ca làm trong ngày",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (shiftsForDay.isNotEmpty()) {
                    Text(
                        "Vuốt trái để xóa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (shiftsForDay.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.EventBusy,
                    title = "Chưa có ca làm",
                    subtitle = "Nhập giờ làm và thu nhập ở trên để thêm ca đầu tiên cho ngày này."
                )
            }
        } else {
            items(shiftsForDay, key = { it.id }) { shift ->
                SwipeableShiftCard(
                    shift = shift,
                    isEditing = viewModel.editingShiftId == shift.id,
                    onEdit = {
                        viewModel.startEditing(shift)
                        scope.launch { listState.animateScrollToItem(1) }
                    },
                    onDelete = { viewModel.deleteShift(shift.id) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem()
                )
            }
            dailySummary?.let { summary ->
                item(key = "breakdown") {
                    DayBreakdownCard(summary, Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    StatusBarScrim(visible = scrolled)
    }

    if (showDatePicker) {
        SingleDatePickerDialog(
            initial = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { viewModel.changeSelectedDate(it) }
        )
    }

    timePickerFor?.let { isStart ->
        val current = if (isStart) viewModel.startTimeInput else viewModel.endTimeInput
        val initial = runCatching { LocalTime.parse(current, TimeHm) }.getOrElse { LocalTime.now() }
        TimePickerDialog(
            title = if (isStart) "Giờ bắt đầu ca" else "Giờ kết thúc ca",
            initial = initial,
            onDismiss = { timePickerFor = null },
            onConfirm = { time ->
                if (isStart) viewModel.startTimeInput = time.format(TimeHm)
                else viewModel.endTimeInput = time.format(TimeHm)
            }
        )
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    summary: DailySummary?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
    onToday: () -> Unit
) {
    val onHero = AppTheme.extended.onHero
    val isToday = date == LocalDate.now()

    HeroHeader {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("QuickEarn", style = MaterialTheme.typography.titleLarge, color = onHero)
                Text(
                    if (isToday) "Thu nhập hôm nay" else "Thu nhập trong ngày",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onHero.copy(alpha = 0.8f)
                )
            }
            AnimatedVisibility(visible = !isToday, enter = fadeIn(), exit = fadeOut()) {
                AssistChip(
                    onClick = onToday,
                    label = { Text("Hôm nay") },
                    leadingIcon = { Icon(Icons.Outlined.WbTwilight, null, Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = onHero,
                        leadingIconContentColor = onHero
                    ),
                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = onHero.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(shape = MaterialTheme.shapes.extraLarge, color = onHero.copy(alpha = 0.14f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ngày trước", tint = onHero)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onPickDate)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = onHero, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    AnimatedContent(
                        targetState = date,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "date"
                    ) { d ->
                        Text(
                            d.format(DateWithWeekday).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            color = onHero
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Ngày sau", tint = onHero)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Thực nhận", style = MaterialTheme.typography.labelLarge, color = onHero.copy(alpha = 0.8f))
        val animatedNet by animateFloatAsState(
            targetValue = (summary?.totalNetIncome ?: 0.0).toFloat(),
            animationSpec = tween(600),
            label = "net"
        )
        Text(
            formatCurrency(animatedNet.toDouble()),
            style = MaterialTheme.typography.displaySmall,
            color = onHero
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroStat("Giờ làm", formatDurationShort(summary?.totalHours ?: 0.0), Modifier.weight(1f))
            HeroStat("TB / giờ", formatCurrency(summary?.averageHourlyRate ?: 0.0), Modifier.weight(1.4f))
            HeroStat("Đơn", "${summary?.totalDeliveries ?: 0}", Modifier.weight(0.8f))
        }
    }
}

@Composable
private fun ShiftForm(
    viewModel: ShiftViewModel,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = viewModel.editingShiftId != null
    val extended = AppTheme.extended

    var showPlusMission by rememberSaveable { mutableStateOf(false) }
    var showNormalMission by rememberSaveable { mutableStateOf(false) }
    // Chi hien mot loai thu nhap moi luc cho gon; tab con lai van giu so da nhap.
    var plusTab by rememberSaveable { mutableStateOf(true) }
    // Khi bat dau sua mot ca: mo o nhiem vu neu co, va mo tab cua loai thu nhap ca do co.
    LaunchedEffect(viewModel.editingShiftId) {
        showPlusMission = viewModel.plusMissionIncomeInput.isNotBlank()
        showNormalMission = viewModel.normalMissionIncomeInput.isNotBlank()
        if (viewModel.editingShiftId != null) {
            plusTab = viewModel.plusIncomeInput.isNotBlank() || viewModel.plusMissionIncomeInput.isNotBlank()
        }
    }
    LaunchedEffect(viewModel.inputError) {
        if (viewModel.inputError == ShiftViewModel.ERROR_PLUS_NEEDS_DELIVERIES) plusTab = true
    }
    val plusTotal = (viewModel.plusIncomeInput.toDoubleOrNull() ?: 0.0) +
        (viewModel.plusMissionIncomeInput.toDoubleOrNull() ?: 0.0)
    val normalTotal = (viewModel.normalIncomeInput.toDoubleOrNull() ?: 0.0) +
        (viewModel.normalMissionIncomeInput.toDoubleOrNull() ?: 0.0)

    SectionCard(
        modifier = modifier,
        title = if (isEditing) "Sửa ca làm" else "Thêm ca làm",
        icon = if (isEditing) Icons.Filled.Edit else Icons.Filled.Add,
        trailing = if (isEditing) {
            { Text("Đang sửa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        } else null
    ) {
        ErrorBanner(viewModel.inputError)

        WorkTimeSection(viewModel = viewModel, onPickStart = onPickStart, onPickEnd = onPickEnd)

        Spacer(Modifier.height(16.dp))
        IncomeTypeSelector(
            plusSelected = plusTab,
            onSelect = { plusTab = it },
            plusTotal = plusTotal,
            normalTotal = normalTotal
        )
        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = plusTab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "incomeTab"
        ) { isPlus ->
            if (isPlus) {
                Column {
                    OutlinedTextField(
                        value = viewModel.deliveryCountInput,
                        onValueChange = { viewModel.deliveryCountInput = it.filter(Char::isDigit).take(4) },
                        label = { Text("Số đơn") },
                        placeholder = { Text("0") },
                        leadingIcon = { Icon(Icons.Outlined.Inventory2, null) },
                        singleLine = true,
                        isError = viewModel.inputError == ShiftViewModel.ERROR_PLUS_NEEDS_DELIVERIES,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    IncomeBlock(
                        income = viewModel.plusIncomeInput,
                        onIncomeChange = { viewModel.plusIncomeInput = it },
                        mission = viewModel.plusMissionIncomeInput,
                        onMissionChange = { viewModel.plusMissionIncomeInput = it },
                        showMission = showPlusMission,
                        onToggleMission = {
                            if (showPlusMission) viewModel.plusMissionIncomeInput = ""
                            showPlusMission = !showPlusMission
                        }
                    )
                }
            } else {
                IncomeBlock(
                    income = viewModel.normalIncomeInput,
                    onIncomeChange = { viewModel.normalIncomeInput = it },
                    mission = viewModel.normalMissionIncomeInput,
                    onMissionChange = { viewModel.normalMissionIncomeInput = it },
                    showMission = showNormalMission,
                    onToggleMission = {
                        if (showNormalMission) viewModel.normalMissionIncomeInput = ""
                        showNormalMission = !showNormalMission
                    }
                )
            }
        }

        val preview = viewModel.inputPreview
        AnimatedVisibility(
            visible = preview != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ước tính thực nhận",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            formatCurrency(preview?.netIncome ?: 0.0),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if ((preview?.hourlyRate ?: 0.0) > 0) {
                        Text(
                            "${formatCurrency(preview!!.hourlyRate)}/h",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isEditing) {
                OutlinedButton(
                    onClick = { viewModel.clearInputs() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Hủy sửa") }
            }
            Button(
                onClick = onSave,
                enabled = viewModel.isLoaded,
                modifier = Modifier
                    .weight(if (isEditing) 1.4f else 1f)
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.Check, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Lưu thay đổi" else "Lưu ca làm")
            }
        }
    }
}

private val QUICK_HOURS = listOf(4, 6, 8, 10, 12)

/** Gio lam: chon khung gio bat dau - ket thuc, hoac nhap thang tong so gio trong ngay. */
@Composable
private fun WorkTimeSection(viewModel: ShiftViewModel, onPickStart: () -> Unit, onPickEnd: () -> Unit) {
    val totalMode = viewModel.totalHoursMode
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Giờ làm", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        FilterChip(
            selected = !totalMode,
            onClick = { viewModel.updateTotalHoursMode(false) },
            label = { Text("Khung giờ") }
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = totalMode,
            onClick = { viewModel.updateTotalHoursMode(true) },
            label = { Text("Tổng giờ") }
        )
    }
    Spacer(Modifier.height(8.dp))

    AnimatedContent(
        targetState = totalMode,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        label = "timeMode"
    ) { isTotal ->
        if (isTotal) {
            TotalHoursInput(viewModel)
        } else {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PickerField(
                        label = "Bắt đầu",
                        value = viewModel.startTimeInput,
                        placeholder = "--:--",
                        leadingIcon = Icons.Outlined.Schedule,
                        onClick = onPickStart,
                        modifier = Modifier.weight(1f)
                    )
                    PickerField(
                        label = "Kết thúc",
                        value = viewModel.endTimeInput,
                        placeholder = "--:--",
                        leadingIcon = Icons.Outlined.Schedule,
                        onClick = onPickEnd,
                        modifier = Modifier.weight(1f)
                    )
                }
                val start = runCatching { LocalTime.parse(viewModel.startTimeInput, TimeHm) }.getOrNull()
                val end = runCatching { LocalTime.parse(viewModel.endTimeInput, TimeHm) }.getOrNull()
                if (start != null && end != null && start != end) {
                    val minutes = java.time.Duration.between(start, end).toMinutes().let { if (it < 0) it + 1440 else it }
                    Text(
                        "Ca dài ${formatDuration(minutes / 60.0)}" + if (end.isBefore(start)) " · qua đêm" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalHoursInput(viewModel: ShiftViewModel) {
    val minutes = parseHoursToMinutes(viewModel.totalHoursInput)
    Column {
        OutlinedTextField(
            value = viewModel.totalHoursInput,
            onValueChange = { value ->
                viewModel.totalHoursInput = value.filter { it.isDigit() || it in ".,:h" }.take(6)
            },
            label = { Text("Tổng số giờ làm") },
            placeholder = { Text("VD: 8 hoặc 8.5") },
            leadingIcon = { Icon(Icons.Outlined.Schedule, null) },
            suffix = { Text("giờ") },
            supportingText = {
                Text(
                    if (minutes != null && minutes > 0) "= ${formatDuration(minutes / 60.0)}"
                    else "Gõ 8.5 hoặc 8:30 cho 8 giờ 30 phút"
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickTimeChip("−30p", Modifier.weight(1.1f)) {
                val current = minutes ?: 0
                viewModel.totalHoursInput = minutesToHoursInput((current - 30).coerceAtLeast(30))
            }
            QUICK_HOURS.forEach { h ->
                QuickTimeChip("${h}h", Modifier.weight(1f), selected = minutes == h * 60) {
                    viewModel.totalHoursInput = h.toString()
                }
            }
            QuickTimeChip("+30p", Modifier.weight(1.1f)) {
                val current = minutes ?: 0
                viewModel.totalHoursInput = minutesToHoursInput((current + 30).coerceAtMost(24 * 60))
            }
        }
    }
}

@Composable
private fun QuickTimeChip(label: String, modifier: Modifier = Modifier, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** Hai tab Plus / Thuong; moi tab hien so tien da nhap de biet tab kia co du lieu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeTypeSelector(
    plusSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    plusTotal: Double,
    normalTotal: Double
) {
    val extended = AppTheme.extended
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf(true, false).forEachIndexed { index, isPlus ->
            val total = if (isPlus) plusTotal else normalTotal
            SegmentedButton(
                selected = plusSelected == isPlus,
                onClick = { onSelect(isPlus) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                icon = {},
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(if (isPlus) extended.plus else extended.normal, Modifier.size(8.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(if (isPlus) "Plus" else "Thường", style = MaterialTheme.typography.labelLarge)
                            if (total > 0) {
                                Text(
                                    formatCurrency(total),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun IncomeBlock(
    income: String,
    onIncomeChange: (String) -> Unit,
    mission: String,
    onMissionChange: (String) -> Unit,
    showMission: Boolean,
    onToggleMission: () -> Unit,
    imeAction: ImeAction = ImeAction.Done
) {
    Column {
        MoneyField(
            value = income,
            onValueChange = onIncomeChange,
            label = "Thu nhập (trước trừ)",
            imeAction = if (showMission) ImeAction.Next else imeAction
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilterChip(
                selected = showMission,
                onClick = onToggleMission,
                label = { Text("Tiền nhiệm vụ") },
                leadingIcon = {
                    Icon(
                        if (showMission) Icons.Filled.Check else Icons.Outlined.EmojiEvents,
                        null,
                        Modifier.size(18.dp)
                    )
                }
            )
        }
        AnimatedVisibility(
            visible = showMission,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MoneyField(
                value = mission,
                onValueChange = onMissionChange,
                label = "Tiền nhiệm vụ",
                leadingIcon = Icons.Outlined.EmojiEvents,
                imeAction = imeAction,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableShiftCard(
    shift: Shift,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                label = "swipe"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.large)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Outlined.DeleteOutline, "Xóa", tint = MaterialTheme.colorScheme.onError)
            }
        }
    ) {
        ShiftCard(shift = shift, isEditing = isEditing, onClick = onEdit, onDelete = onDelete)
    }
}

@Composable
private fun ShiftCard(shift: Shift, isEditing: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val extended = AppTheme.extended
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isEditing) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(shift.timeLabel(), style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(formatDuration(shift.calculateDurationHours()))
                        if (shift.deliveryCount > 0) append(" · ${shift.deliveryCount} đơn")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                if (shift.calculatePlusGrossIncome() > 0) {
                    IncomeLine(extended.plus, "Plus", shift.calculatePlusGrossIncome(), shift.plusMissionIncome)
                }
                if (shift.calculateNormalGrossIncome() > 0) {
                    IncomeLine(extended.normal, "Thường", shift.calculateNormalGrossIncome(), shift.normalMissionIncome)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatCurrency(shift.calculateNetIncome()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${formatCurrency(shift.calculateHourlyRate())}/h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Xóa ca",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeLine(color: Color, label: String, gross: Double, mission: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        ColorDot(color, Modifier.size(8.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "$label ${formatCurrency(gross)}" + if (mission > 0) " (NV ${formatCurrency(mission)})" else "",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DayBreakdownCard(summary: DailySummary, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val extended = AppTheme.extended
    SectionCard(
        modifier = modifier,
        title = "Chi tiết ngày",
        icon = Icons.Outlined.Payments,
        trailing = {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Thu gọn" else "Mở rộng"
                )
            }
        }
    ) {
        MetricRow("Tổng trước trừ", formatCurrency(summary.totalGrossIncome))
        MetricRow("Thực nhận", formatCurrency(summary.totalNetIncome), emphasize = true, valueColor = MaterialTheme.colorScheme.primary)
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                MetricRow("Số ca", "${summary.shiftCount} ca")
                MetricRow("Tổng giờ làm", formatDuration(summary.totalHours))
                MetricRow("Plus trước trừ", formatCurrency(summary.totalPlusGrossIncome), valueColor = extended.plus)
                MetricRow("Plus thực nhận", formatCurrency(summary.totalPlusNetIncome), valueColor = extended.plus)
                MetricRow("Thường trước trừ", formatCurrency(summary.totalNormalGrossIncome), valueColor = extended.normal)
                MetricRow("Thường thực nhận", formatCurrency(summary.totalNormalNetIncome), valueColor = extended.normal)
                MetricRow("Trung bình / giờ", "${formatCurrency(summary.averageHourlyRate)}/h")
            }
        }
    }
}
