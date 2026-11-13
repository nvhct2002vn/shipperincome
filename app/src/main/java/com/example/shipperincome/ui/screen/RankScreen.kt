package com.example.shipperincome.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.shipperincome.ui.components.EmptyState
import com.example.shipperincome.ui.components.ErrorBanner
import com.example.shipperincome.ui.components.HeroHeader
import com.example.shipperincome.ui.components.HeroStat
import com.example.shipperincome.ui.components.LocalSnackbarHostState
import com.example.shipperincome.ui.components.MetricRow
import com.example.shipperincome.ui.components.PickerField
import com.example.shipperincome.ui.components.SectionCard
import com.example.shipperincome.ui.components.SingleDatePickerDialog
import com.example.shipperincome.ui.components.StatusBarScrim
import com.example.shipperincome.ui.components.ThousandsSeparatorTransformation
import com.example.shipperincome.ui.theme.AppTheme
import com.example.shipperincome.ui.viewmodel.RankHistoryItem
import com.example.shipperincome.ui.viewmodel.RankProgress
import com.example.shipperincome.ui.viewmodel.RankViewModel
import com.example.shipperincome.utils.DateShort
import com.example.shipperincome.utils.DateWithShortWeekday
import com.example.shipperincome.utils.DateWithWeekday
import com.example.shipperincome.utils.groupDigits
import java.time.LocalDate
import kotlin.math.roundToInt

private fun points(value: Int): String = groupDigits(value.toString())

@Composable
fun RankScreen(viewModel: RankViewModel, onOpenSettings: () -> Unit) {
    val snackbar = LocalSnackbarHostState.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
    var showDatePicker by remember { mutableStateOf(false) }

    // Cau hinh co the vua doi o tab Cai dat: doc lai moi khi quay lai tab nay.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshConfig()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshConfig()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    val progress = viewModel.progress

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (progress == null) {
                item(key = "header") {
                    HeroHeader {
                        Text("Điểm rank", style = MaterialTheme.typography.titleLarge, color = AppTheme.extended.onHero)
                        Text(
                            "Theo dõi chỉ tiêu điểm theo chu kỳ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.extended.onHero.copy(alpha = 0.8f)
                        )
                    }
                }
                item(key = "setup") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.EmojiEvents,
                            title = "Chưa cấu hình chu kỳ",
                            subtitle = "Vào Cài đặt, nhập chỉ tiêu điểm và chọn ngày bắt đầu một chu kỳ. Các chu kỳ sau app tự tính."
                        )
                        Button(onClick = onOpenSettings) { Text("Mở Cài đặt") }
                    }
                }
            } else {
                item(key = "header") {
                    RankHeader(
                        progress = progress,
                        canGoNext = viewModel.cycleOffset < 0,
                        onPrev = { viewModel.showCycle(viewModel.cycleOffset - 1) },
                        onNext = { viewModel.showCycle(viewModel.cycleOffset + 1) }
                    )
                }
                item(key = "pace") { PaceCard(progress, Modifier.padding(horizontal = 16.dp)) }
                item(key = "entry") {
                    EntryCard(
                        viewModel = viewModel,
                        onPickDate = { showDatePicker = true },
                        onSave = {
                            focusManager.clearFocus()
                            viewModel.saveEntry()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item(key = "history_title") {
                    Text(
                        "Lịch sử chu kỳ (${progress.history.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                if (progress.history.isEmpty()) {
                    item(key = "history_empty") {
                        Text(
                            "Chưa ghi điểm nào trong chu kỳ này.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    items(progress.history, key = { it.date.toString() }) { item ->
                        HistoryRow(
                            item = item,
                            onClick = { viewModel.selectDate(item.date) },
                            onDelete = { viewModel.deleteEntry(item.date) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        )
                    }
                }
            }
        }
        StatusBarScrim(visible = scrolled)
    }

    val cycle = viewModel.viewedCycle
    if (showDatePicker && cycle != null) {
        val today = LocalDate.now()
        SingleDatePickerDialog(
            initial = viewModel.selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { viewModel.selectDate(it) },
            isSelectable = { it in cycle && !it.isAfter(today) }
        )
    }
}

@Composable
private fun RankHeader(progress: RankProgress, canGoNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    val onHero = AppTheme.extended.onHero
    val cycle = progress.cycle
    HeroHeader {
        Text("Điểm rank", style = MaterialTheme.typography.titleLarge, color = onHero)
        Spacer(Modifier.height(12.dp))
        Surface(shape = MaterialTheme.shapes.extraLarge, color = onHero.copy(alpha = 0.14f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Chu kỳ trước", tint = onHero)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${cycle.start.format(DateShort)} – ${cycle.end.format(DateShort)}/${cycle.end.year}",
                        style = MaterialTheme.typography.titleSmall,
                        color = onHero
                    )
                    Text(
                        when {
                            progress.isPast -> "Chu kỳ đã kết thúc"
                            progress.isFuture -> "Chưa bắt đầu"
                            else -> "Chu kỳ hiện tại · ${cycle.days} ngày"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = onHero.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onNext, enabled = canGoNext) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Chu kỳ sau",
                        tint = if (canGoNext) onHero else onHero.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Đã chạy được", style = MaterialTheme.typography.labelLarge, color = onHero.copy(alpha = 0.8f))
        val animated by animateFloatAsState(progress.currentPoints.toFloat(), tween(600), label = "points")
        Row(verticalAlignment = Alignment.Bottom) {
            Text(points(animated.roundToInt()), style = MaterialTheme.typography.displaySmall, color = onHero)
            Text(
                " / ${points(progress.targetPoints)} điểm",
                style = MaterialTheme.typography.titleMedium,
                color = onHero.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        val animatedFraction by animateFloatAsState(progress.fraction, tween(700), label = "fraction")
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = onHero,
            trackColor = onHero.copy(alpha = 0.2f),
            drawStopIndicator = {}
        )
        Text(
            "${(progress.fraction * 100).roundToInt()}% chỉ tiêu",
            style = MaterialTheme.typography.labelMedium,
            color = onHero.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroStat(
                "Còn thiếu",
                if (progress.isDone) "Đủ rồi" else points(progress.remainingPoints),
                Modifier.weight(1f)
            )
            HeroStat(
                "Cần / ngày",
                if (progress.isDone || progress.isPast) "—" else points(progress.pointsPerDayNeeded),
                Modifier.weight(1f)
            )
            HeroStat("Còn lại", "${progress.daysRemaining} ngày", Modifier.weight(1f))
        }
    }
}

@Composable
private fun PaceCard(progress: RankProgress, modifier: Modifier = Modifier) {
    val (title, detail) = when {
        progress.isDone -> "Đã đạt chỉ tiêu 🎉" to
            "Bạn đã có ${points(progress.currentPoints)} điểm, vượt ${points(progress.currentPoints - progress.targetPoints)} điểm."
        progress.isPast -> "Không đạt chỉ tiêu" to
            "Chu kỳ kết thúc với ${points(progress.currentPoints)} điểm, thiếu ${points(progress.remainingPoints)} điểm."
        progress.isFuture -> "Chu kỳ chưa bắt đầu" to "Cần ${points(progress.targetPoints)} điểm trong ${progress.cycle.days} ngày."
        progress.daysRemaining == 0 -> "Hết ngày trong chu kỳ" to
            "Còn thiếu ${points(progress.remainingPoints)} điểm."
        else -> "Cần trung bình ${points(progress.pointsPerDayNeeded)} điểm/ngày" to
            "Để đủ ${points(progress.targetPoints)} điểm trong ${progress.daysRemaining} ngày còn lại" +
            if (progress.enteredToday) " (từ ngày mai)." else " (tính cả hôm nay)."
    }
    SectionCard(modifier = modifier, title = "Nhịp độ", icon = Icons.Outlined.Speed) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (progress.history.isNotEmpty() && !progress.isFuture) {
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            MetricRow("Trung bình từ đầu chu kỳ", "${points(progress.averagePerDay.roundToInt())} điểm/ngày")
            if (!progress.isPast && !progress.isDone) {
                MetricRow(
                    "Giữ nhịp này, hết chu kỳ đạt",
                    "${points(progress.projectedPoints)} điểm",
                    valueColor = if (progress.onTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EntryCard(
    viewModel: RankViewModel,
    onPickDate: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val cycle = viewModel.viewedCycle
    val canEdit = cycle != null && !cycle.start.isAfter(today)
    SectionCard(modifier = modifier, title = "Cập nhật điểm", icon = Icons.Outlined.Stars) {
        Text(
            "Nhập TỔNG điểm đã chạy được từ đầu chu kỳ đến hết ngày đã chọn (không phải điểm riêng của ngày đó).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        ErrorBanner(viewModel.inputError)
        PickerField(
            label = "Ngày",
            value = viewModel.selectedDate.format(DateWithWeekday).replaceFirstChar { it.uppercase() } +
                if (viewModel.selectedDate == today) " (hôm nay)" else "",
            placeholder = "",
            leadingIcon = Icons.Outlined.CalendarMonth,
            onClick = { if (canEdit) onPickDate() }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.pointsInput,
            onValueChange = { viewModel.pointsInput = it.filter(Char::isDigit).take(7) },
            label = { Text("Tổng điểm đã chạy được") },
            placeholder = { Text("VD: 2100") },
            suffix = { Text("điểm") },
            leadingIcon = { Icon(Icons.Outlined.EmojiEvents, null) },
            singleLine = true,
            enabled = canEdit,
            visualTransformation = ThousandsSeparatorTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSave,
            enabled = canEdit && viewModel.isLoaded,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Filled.Check, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Lưu điểm")
        }
    }
}

@Composable
private fun HistoryRow(item: RankHistoryItem, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.date.format(DateWithShortWeekday).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Tổng ${points(item.totalPoints)} điểm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "+${points(item.gained)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
