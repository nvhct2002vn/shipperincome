package com.example.shipperincome.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DonutSmall
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shipperincome.ui.components.ColorDot
import com.example.shipperincome.ui.components.EmptyState
import com.example.shipperincome.ui.components.HeroHeader
import com.example.shipperincome.ui.components.HeroStat
import com.example.shipperincome.ui.components.LocalSnackbarHostState
import com.example.shipperincome.ui.components.MetricRow
import com.example.shipperincome.ui.components.RangeDatePickerDialog
import com.example.shipperincome.ui.components.SectionCard
import com.example.shipperincome.ui.components.SingleDatePickerDialog
import com.example.shipperincome.ui.components.StatusBarScrim
import com.example.shipperincome.ui.theme.AppTheme
import com.example.shipperincome.ui.viewmodel.ChartPoint
import com.example.shipperincome.ui.viewmodel.DailySummary
import com.example.shipperincome.ui.viewmodel.GoalProgress
import com.example.shipperincome.ui.viewmodel.ShiftViewModel
import com.example.shipperincome.ui.viewmodel.StatsPeriod
import com.example.shipperincome.ui.viewmodel.StatsSummary
import com.example.shipperincome.ui.viewmodel.WeekdayStat
import java.time.DayOfWeek
import com.example.shipperincome.utils.DateShort
import com.example.shipperincome.utils.DateWithShortWeekday
import com.example.shipperincome.utils.formatCompactCurrency
import com.example.shipperincome.utils.formatCurrency
import com.example.shipperincome.utils.formatDuration
import com.example.shipperincome.utils.formatDurationShort
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

@Composable
fun StatsScreen(viewModel: ShiftViewModel) {
    val stats by viewModel.statisticsData.collectAsStateWithLifecycle()
    val period = viewModel.statsPeriodType
    val snackbar = LocalSnackbarHostState.current

    var showDayPicker by remember { mutableStateOf(false) }
    var showRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            val result = snackbar.showSnackbar(msg.text, msg.actionLabel, withDismissAction = msg.actionLabel == null)
            if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
        }
    }

    val data = stats
    val summary = data?.summary

    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") { StatsHeader(summary, data?.let { "${it.start.format(DateShort)} – ${it.end.format(DateShort)}/${it.end.year}" }) }

        item(key = "periods") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StatsPeriod.entries) { p ->
                    val selected = p == period
                    FilterChip(
                        selected = selected,
                        onClick = {
                            when (p) {
                                StatsPeriod.SPECIFIC_DAY -> showDayPicker = true
                                StatsPeriod.DATE_RANGE -> showRangePicker = true
                                else -> viewModel.updateStatsPeriod(p)
                            }
                        },
                        label = {
                            Text(
                                when {
                                    selected && p == StatsPeriod.SPECIFIC_DAY && data != null -> data.start.format(DateShort)
                                    selected && p == StatsPeriod.DATE_RANGE && data != null ->
                                        "${data.start.format(DateShort)} – ${data.end.format(DateShort)}"
                                    else -> p.label
                                }
                            )
                        },
                        leadingIcon = when {
                            selected -> { { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) } }
                            p == StatsPeriod.SPECIFIC_DAY -> { { Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp)) } }
                            p == StatsPeriod.DATE_RANGE -> { { Icon(Icons.Outlined.DateRange, null, Modifier.size(18.dp)) } }
                            else -> null
                        }
                    )
                }
            }
        }

        // Doc lai muc tieu moi khi du lieu/giai doan doi hoac khi quay lai man hinh (co the vua sua o Cai dat).
        val goal = if (data == null) null else viewModel.goalFor(period)
        if (goal != null) {
            item(key = "goal") {
                GoalCard(
                    goal = goal,
                    title = if (period == StatsPeriod.CURRENT_WEEK) "Mục tiêu tuần" else "Mục tiêu tháng",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (summary == null) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.QueryStats,
                    title = "Chưa có dữ liệu",
                    subtitle = "Không có ca làm nào trong giai đoạn này. Thử chọn giai đoạn khác."
                )
            }
        } else {
            if (data.chart.size > 1) {
                item(key = "chart") {
                    ChartCard(
                        points = data.chart,
                        byMonth = data.chartByMonth,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            if (data.weekdays.isNotEmpty() && data.days.size >= 2) {
                item(key = "weekdays") { WeekdayCard(data.weekdays, Modifier.padding(horizontal = 16.dp)) }
            }
            item(key = "breakdown") {
                if (viewModel.hidePlus && summary.totalPlusGrossIncome <= 0.0) {
                    OverviewCard(summary, Modifier.padding(horizontal = 16.dp))
                } else {
                    BreakdownCard(summary, Modifier.padding(horizontal = 16.dp))
                }
            }
            item(key = "days_title") {
                Text(
                    "Chi tiết từng ngày (${data.days.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            items(data.days, key = { it.date.toString() }) { day ->
                DayRow(day, hidePlus = viewModel.hidePlus, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    StatusBarScrim(visible = scrolled)
    }

    if (showDayPicker) {
        SingleDatePickerDialog(
            // Mac dinh hom nay; chi giu ngay cu khi dang o che do "Chon ngay" (tranh nhay ve ngay dau tien khi dang xem "Tat ca").
            initial = data?.start?.takeIf { period == StatsPeriod.SPECIFIC_DAY } ?: java.time.LocalDate.now(),
            onDismiss = { showDayPicker = false },
            onConfirm = { viewModel.updateStatsPeriod(StatsPeriod.SPECIFIC_DAY, it, it) }
        )
    }
    if (showRangePicker) {
        RangeDatePickerDialog(
            initialStart = data?.start ?: java.time.LocalDate.now().minusDays(6),
            initialEnd = data?.end ?: java.time.LocalDate.now(),
            onDismiss = { showRangePicker = false },
            onConfirm = { start, end -> viewModel.updateStatsPeriod(StatsPeriod.DATE_RANGE, start, end) }
        )
    }
}

@Composable
private fun StatsHeader(summary: StatsSummary?, rangeLabel: String?) {
    val onHero = AppTheme.extended.onHero
    HeroHeader {
        Text("Thống kê", style = MaterialTheme.typography.titleLarge, color = onHero)
        Text(
            summary?.periodLabel ?: rangeLabel ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = onHero.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(20.dp))
        Text("Tổng thực nhận", style = MaterialTheme.typography.labelLarge, color = onHero.copy(alpha = 0.8f))
        val animatedNet by animateFloatAsState(
            targetValue = (summary?.totalNetIncome ?: 0.0).toFloat(),
            animationSpec = tween(700),
            label = "statsNet"
        )
        Text(formatCurrency(animatedNet.toDouble()), style = MaterialTheme.typography.displaySmall, color = onHero)
        Text(
            "Trước trừ ${formatCurrency(summary?.totalGrossIncome ?: 0.0)}",
            style = MaterialTheme.typography.bodyMedium,
            color = onHero.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroStat("Giờ làm", formatDurationShort(summary?.totalHours ?: 0.0), Modifier.weight(1f))
            HeroStat("TB / giờ", formatCurrency(summary?.averageHourlyRate ?: 0.0), Modifier.weight(1.3f))
            HeroStat("Ngày / Ca", "${summary?.totalDays ?: 0} / ${summary?.totalShifts ?: 0}", Modifier.weight(1f))
        }
    }
}

private val ChartLabelsKey = ExtraStore.Key<List<String>>()

@Composable
private fun ChartCard(points: List<ChartPoint>, byMonth: Boolean, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            columnSeries { series(points.map { it.netIncome }) }
            // Nhan di cung model: model cap nhat bat dong bo, neu doc tu `points` thi luc doi giai doan
            // so cot cu va so nhan moi lech nhau -> nhan rong -> Vico nem IllegalStateException.
            extras { it[ChartLabelsKey] = points.map { point -> point.label } }
        }
    }
    val bottomFormatter = remember {
        CartesianValueFormatter { context, x, _ ->
            // Vico cam chuoi rong, nen luon tra ve it nhat mot dau cach.
            context.model.extraStore.getOrNull(ChartLabelsKey)?.getOrNull(x.toInt()) ?: " "
        }
    }
    val startFormatter = remember {
        CartesianValueFormatter { _, y, _ -> formatCompactCurrency(y) }
    }
    val barColor = MaterialTheme.colorScheme.primary

    SectionCard(
        modifier = modifier,
        title = if (byMonth) "Thực nhận theo tháng" else "Thực nhận theo ngày",
        icon = Icons.Outlined.BarChart
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        rememberLineComponent(
                            fill = fill(barColor),
                            thickness = if (points.size > 20) 8.dp else 16.dp,
                            shape = CorneredShape.rounded(allPercent = 40)
                        )
                    )
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = startFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = bottomFormatter)
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = points.size > 14),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }
}

@Composable
private fun BreakdownCard(summary: StatsSummary, modifier: Modifier = Modifier) {
    val extended = AppTheme.extended
    val total = summary.totalGrossIncome
    val plusShare = if (total > 0) (summary.totalPlusGrossIncome / total).toFloat() else 0f
    val animatedShare by animateFloatAsState(plusShare, tween(700), label = "share")

    SectionCard(modifier = modifier, title = "Plus và thường", icon = Icons.Outlined.DonutSmall) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(extended.normal)
        ) {
            if (animatedShare > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedShare.coerceIn(0.001f, 1f))
                        .background(extended.plus)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            LegendItem(extended.plus, "Plus", "${(plusShare * 100).toInt()}%", Modifier.weight(1f))
            LegendItem(extended.normal, "Thường", "${100 - (plusShare * 100).toInt()}%", Modifier.weight(1f))
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        MetricRow("Plus trước trừ", formatCurrency(summary.totalPlusGrossIncome), valueColor = extended.plus)
        MetricRow("Plus thực nhận", formatCurrency(summary.totalPlusNetIncome), valueColor = extended.plus)
        MetricRow("Thường trước trừ", formatCurrency(summary.totalNormalGrossIncome), valueColor = extended.normal)
        MetricRow("Thường thực nhận", formatCurrency(summary.totalNormalNetIncome), valueColor = extended.normal)
        MetricRow("Tổng số đơn Plus", "${summary.totalDeliveries} đơn")
        MetricRow("Tổng giờ làm", formatDuration(summary.totalHours))
    }
}

@Composable
private fun GoalCard(goal: GoalProgress, title: String, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(goal.fraction, tween(700), label = "goal")
    val color = MaterialTheme.colorScheme.primary
    SectionCard(
        modifier = modifier,
        title = title,
        icon = Icons.Outlined.Flag,
        trailing = { Text("${goal.percent}%", style = MaterialTheme.typography.titleMedium, color = color) }
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatCurrency(goal.earned), style = MaterialTheme.typography.headlineSmall, color = color)
            Spacer(Modifier.width(6.dp))
            Text(
                "/ ${formatCurrency(goal.target)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            if (animated > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated.coerceAtLeast(0.02f))
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                goal.isDone -> "Đã đạt mục tiêu" +
                    if (goal.earned > goal.target) ", vượt ${formatCurrency(goal.earned - goal.target)}. Quá đỉnh!" else ". Quá đỉnh!"
                goal.daysRemaining == 0 -> "Còn thiếu ${formatCurrency(goal.remaining)}."
                else -> "Còn ${formatCurrency(goal.remaining)} · cần khoảng ${formatCurrency(goal.perDayNeeded)} mỗi ngày " +
                    "trong ${goal.daysRemaining} ngày còn lại."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Thay cho the "Plus va thuong" khi da an Plus va giai doan khong co tien Plus. */
@Composable
private fun OverviewCard(summary: StatsSummary, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, title = "Tổng quan", icon = Icons.Outlined.Summarize) {
        MetricRow("Tổng trước trừ", formatCurrency(summary.totalGrossIncome))
        MetricRow("Thuế đã trừ", formatCurrency(summary.totalGrossIncome - summary.totalNetIncome))
        MetricRow("Thực nhận", formatCurrency(summary.totalNetIncome), emphasize = true, valueColor = MaterialTheme.colorScheme.primary)
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        MetricRow("Số ngày làm", "${summary.totalDays} ngày")
        MetricRow("Số ca", "${summary.totalShifts} ca")
        MetricRow("Tổng giờ làm", formatDuration(summary.totalHours))
        if (summary.totalDays > 0) {
            MetricRow("TB mỗi ngày làm", formatCurrency(summary.totalNetIncome / summary.totalDays))
        }
    }
}

private val WeekdayShort = mapOf(
    DayOfWeek.MONDAY to "T2", DayOfWeek.TUESDAY to "T3", DayOfWeek.WEDNESDAY to "T4",
    DayOfWeek.THURSDAY to "T5", DayOfWeek.FRIDAY to "T6", DayOfWeek.SATURDAY to "T7", DayOfWeek.SUNDAY to "CN"
)
private val WeekdayFull = mapOf(
    DayOfWeek.MONDAY to "Thứ hai", DayOfWeek.TUESDAY to "Thứ ba", DayOfWeek.WEDNESDAY to "Thứ tư",
    DayOfWeek.THURSDAY to "Thứ năm", DayOfWeek.FRIDAY to "Thứ sáu", DayOfWeek.SATURDAY to "Thứ bảy", DayOfWeek.SUNDAY to "Chủ nhật"
)

@Composable
private fun WeekdayCard(stats: List<WeekdayStat>, modifier: Modifier = Modifier) {
    val worked = stats.filter { it.workedDays > 0 }
    val best = worked.maxByOrNull { it.avgNetPerDay }
    val max = best?.avgNetPerDay ?: 0.0
    SectionCard(modifier = modifier, title = "Theo thứ trong tuần", icon = Icons.Outlined.CalendarViewWeek) {
        if (best != null && worked.size > 1) {
            Text(
                "Chạy được nhiều nhất vào ${WeekdayFull[best.day]?.lowercase()}: TB ${formatCurrency(best.avgNetPerDay)} mỗi ngày.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }
        stats.forEach { s ->
            InsightBarRow(
                label = WeekdayShort.getValue(s.day),
                fraction = if (max > 0) (s.avgNetPerDay / max).toFloat() else 0f,
                highlight = s == best && worked.size > 1,
                value = if (s.workedDays > 0) formatCurrency(s.avgNetPerDay) else "—",
                detail = if (s.workedDays > 0) "${s.workedDays} ngày · ${formatCurrency(s.hourlyRate)}/h" else "Chưa chạy"
            )
        }
    }
}

@Composable
private fun InsightBarRow(
    label: String,
    fraction: Float,
    highlight: Boolean,
    value: String,
    detail: String
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "insightBar")
    val barColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlight) barColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp)
        )
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (animated > 0f) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animated.coerceAtLeast(0.02f))
                            .clip(CircleShape)
                            .background(if (highlight) barColor else barColor.copy(alpha = 0.45f))
                    )
                }
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        // Cot so rong co dinh de cac thanh cung do dai, so sanh bang mat duoc.
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) barColor else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun DayRow(day: DailySummary, hidePlus: Boolean, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(day.date.toString()) { mutableStateOf(false) }
    val extended = AppTheme.extended
    Card(
        onClick = { expanded = !expanded },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        day.date.format(DateWithShortWeekday).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${day.shiftCount} ca · ${formatDurationShort(day.totalHours)} · ${formatCurrency(day.averageHourlyRate)}/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatCurrency(day.totalNetIncome),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(Modifier.padding(bottom = 8.dp))
                    if (hidePlus && day.totalPlusGrossIncome <= 0.0) {
                        MetricRow("Tổng trước trừ", formatCurrency(day.totalGrossIncome))
                        MetricRow("Thuế đã trừ", formatCurrency(day.totalGrossIncome - day.totalNetIncome))
                    } else {
                        MetricRow("Đơn Plus", "${day.totalDeliveries}")
                        MetricRow("Plus trước trừ / thực nhận", "${formatCurrency(day.totalPlusGrossIncome)} / ${formatCurrency(day.totalPlusNetIncome)}", valueColor = extended.plus)
                        MetricRow("Thường trước trừ / thực nhận", "${formatCurrency(day.totalNormalGrossIncome)} / ${formatCurrency(day.totalNormalNetIncome)}", valueColor = extended.normal)
                        MetricRow("Tổng trước trừ", formatCurrency(day.totalGrossIncome))
                    }
                }
            }
        }
    }
}
