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
import androidx.compose.material.icons.outlined.QueryStats
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
import com.example.shipperincome.ui.viewmodel.ShiftViewModel
import com.example.shipperincome.ui.viewmodel.StatsPeriod
import com.example.shipperincome.ui.viewmodel.StatsSummary
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
            item(key = "breakdown") { BreakdownCard(summary, Modifier.padding(horizontal = 16.dp)) }
            item(key = "days_title") {
                Text(
                    "Chi tiết từng ngày (${data.days.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            items(data.days, key = { it.date.toString() }) { day ->
                DayRow(day, Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    StatusBarScrim(visible = scrolled)
    }

    if (showDayPicker) {
        SingleDatePickerDialog(
            initial = data?.start ?: java.time.LocalDate.now(),
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

@Composable
private fun ChartCard(points: List<ChartPoint>, byMonth: Boolean, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            columnSeries { series(points.map { it.netIncome }) }
        }
    }
    val labels = points.map { it.label }
    val bottomFormatter = remember(labels) {
        CartesianValueFormatter { _, x, _ -> labels.getOrNull(x.toInt()) ?: "" }
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
private fun DayRow(day: DailySummary, modifier: Modifier = Modifier) {
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
                    MetricRow("Đơn Plus", "${day.totalDeliveries}")
                    MetricRow("Plus trước trừ / thực nhận", "${formatCurrency(day.totalPlusGrossIncome)} / ${formatCurrency(day.totalPlusNetIncome)}", valueColor = extended.plus)
                    MetricRow("Thường trước trừ / thực nhận", "${formatCurrency(day.totalNormalGrossIncome)} / ${formatCurrency(day.totalNormalNetIncome)}", valueColor = extended.normal)
                    MetricRow("Tổng trước trừ", formatCurrency(day.totalGrossIncome))
                }
            }
        }
    }
}
