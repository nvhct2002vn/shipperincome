package com.example.shipperincome.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Import các icons cần thiết
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.shipperincome.ui.theme.* // Import Theme colors
import com.example.shipperincome.ui.viewmodel.DailySummary
import com.example.shipperincome.ui.viewmodel.ShiftViewModel
import com.example.shipperincome.ui.viewmodel.StatsPeriod
import com.example.shipperincome.ui.viewmodel.StatsSummary
import com.example.shipperincome.data.model.ShiftType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import com.example.shipperincome.utils.FormatDuration


@Composable
fun StatsScreen(viewModel: ShiftViewModel, navController: NavHostController) {

    val statsData by viewModel.statisticsData.collectAsState()
    val (overallSummary, dailySummaries) = statsData
    val currentPeriodType = viewModel.statsPeriodType
    val startDate = viewModel.statsStartDate
    val endDate = viewModel.statsEndDate

    // Add LaunchedEffect to track UI updates
    LaunchedEffect(statsData, currentPeriodType, startDate, endDate) {
        println("[DEBUG StatsScreen UI Update] Period=$currentPeriodType, Start=$startDate, End=$endDate, Overall=${overallSummary != null}, DailyCount=${dailySummaries.size}")
    }

    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(colors = listOf(BlueGreenStart, BlueGreenEnd))

     Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp)
    ) {
        Text(
             "Thống Kê Thu Nhập",
             style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold), // Chỉnh size và weight
             color = Color.White,
             modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Chọn Giai Đoạn Thống Kê
         Box { // Box để DropdownMenu neo vào Button
            OutlinedButton(
                onClick = { showPeriodDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                 colors = ButtonDefaults.outlinedButtonColors(
                     backgroundColor = Beige.copy(alpha = 0.1f), // Nền mờ
                     contentColor = Color.White // Màu chữ và icon
                 ),
                 border = BorderStroke(1.dp, Beige.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Chọn giai đoạn", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    generateDisplayPeriodLabel(currentPeriodType, startDate, endDate),
                    fontWeight = FontWeight.SemiBold
                 )
                Spacer(Modifier.weight(1f)) // Đẩy icon dropdown về cuối
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Mở menu")
            }

             // Dropdown Menu chọn loại giai đoạn
            DropdownMenu(
                 expanded = showPeriodDropdown,
                 onDismissRequest = { showPeriodDropdown = false }
            ) {
                 StatsPeriod.values().forEach { period ->
                     DropdownMenuItem(onClick = {
                         showPeriodDropdown = false
                         println("[DEBUG StatsScreen] Selected period type from dropdown: $period")
                         if (period == StatsPeriod.DATE_RANGE || period == StatsPeriod.SPECIFIC_DAY) {
                             // Mở DatePicker nếu chọn tùy chỉnh
                             println("[DEBUG StatsScreen] Opening StartDatePicker for period: $period")
                             // Update the period type temporarily without setting dates yet
                             viewModel.updateStatsPeriod(period) 
                             showStartDatePicker = true // Mở picker cho ngày bắt đầu trước
                         } else {
                             println("[DEBUG StatsScreen] Calling updateStatsPeriod directly for period: $period")
                             viewModel.updateStatsPeriod(period) // Cập nhật ngay nếu là loại cố định
                         }
                     }) {
                         Text(getPeriodDropdownLabel(period)) // Nhãn gọn hơn cho dropdown
                     }
                 }
             }
         } // End Box

         // Hiển thị nút chọn ngày nếu là DATE_RANGE
         if (currentPeriodType == StatsPeriod.DATE_RANGE) {
             Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                 DateSelectorButton("Từ:", startDate) { 
                     println("[DEBUG StatsScreen] From DateSelectorButton clicked for DATE_RANGE")
                     showStartDatePicker = true 
                 }
                 DateSelectorButton("Đến:", endDate) { 
                     println("[DEBUG StatsScreen] To DateSelectorButton clicked for DATE_RANGE")
                     showEndDatePicker = true 
                 }
             }
         }
          // Hiển thị nút chọn ngày nếu là SPECIFIC_DAY
         if (currentPeriodType == StatsPeriod.SPECIFIC_DAY) {
              Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                  DateSelectorButton("Ngày:", startDate) { 
                      println("[DEBUG StatsScreen] Day DateSelectorButton clicked for SPECIFIC_DAY")
                      showStartDatePicker = true 
                  }
              }
         }

        Spacer(Modifier.height(16.dp))

        // 2. Tổng Hợp Giai Đoạn
        if (overallSummary != null) {
            StatsOverallSummaryCard(summary = overallSummary)
        } else {
            Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Beige.copy(alpha = 0.8f)) {
                Text(
                     "Không có dữ liệu cho giai đoạn đã chọn.",
                     modifier = Modifier.padding(16.dp).fillMaxWidth(),
                     textAlign = TextAlign.Center,
                     color = Brown
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 3. Chi Tiết Từng Ngày
         Row(verticalAlignment = Alignment.CenterVertically){
             Icon(Icons.Default.List, contentDescription = "Chi tiết", tint = Color.White, modifier = Modifier.size(24.dp))
             Spacer(Modifier.width(8.dp))
             Text(
                 "Chi Tiết Từng Ngày",
                  style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold), // Chỉnh size và weight
                 color = Color.White
             )
         }
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
             // Thêm header cho bảng chi tiết (Optional)
            /* item {
                 Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                     Text("Ngày", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Beige)
                     Text("Giờ", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Beige, textAlign = TextAlign.End)
                     Text("Ròng", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = Beige, textAlign = TextAlign.End)
                     Text("TB/giờ", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = Beige, textAlign = TextAlign.End)
                 }
                 Divider(color = Beige.copy(alpha = 0.5f))
            } */

            if (dailySummaries.isEmpty() && overallSummary != null) { // Nếu có tổng hợp nhưng không có chi tiết
                 item {
                     Text(
                         "Không có dữ liệu chi tiết cho giai đoạn này.",
                         color = Beige,
                         modifier = Modifier.padding(top=16.dp).fillMaxWidth(),
                         textAlign = TextAlign.Center
                     )
                 }
            } else if (dailySummaries.isNotEmpty()){
                items(dailySummaries, key = { it.date.toString() }) { daily ->
                    DailyDetailItem(summary = daily)
                    Divider(color = BlueGreenEnd.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            }
            // Trường hợp overallSummary == null đã xử lý ở trên
        }
    } // End Column Chính

    // --- Date Picker Dialogs ---
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    if (showStartDatePicker) {
         calendar.set(startDate.year, startDate.monthValue - 1, startDate.dayOfMonth)
         DatePickerDialog(
             context,
             { _, year, month, day ->
                 val newStartDate = LocalDate.of(year, month + 1, day)
                 println("[DEBUG StatsScreen] StartDatePicker selected: $newStartDate") // DEBUG LOG
                 if (currentPeriodType == StatsPeriod.SPECIFIC_DAY) {
                     println("[DEBUG StatsScreen] Calling updateStatsPeriod for SPECIFIC_DAY") // DEBUG LOG
                     viewModel.updateStatsPeriod(StatsPeriod.SPECIFIC_DAY, newStartDate, newStartDate)
                 } else if (currentPeriodType == StatsPeriod.DATE_RANGE) {
                     println("[DEBUG StatsScreen] Updating start date for DATE_RANGE, opening EndDatePicker...") // DEBUG LOG
                     // Quan trọng: Truyền endDate hiện tại để giữ giá trị nếu người dùng hủy picker tiếp theo
                     viewModel.updateStatsPeriod(StatsPeriod.DATE_RANGE, newStartDate, endDate.coerceAtLeast(newStartDate))
                     showEndDatePicker = true // Mở picker ngày kết thúc
                 }
                 showStartDatePicker = false
             },
             startDate.year, startDate.monthValue - 1, startDate.dayOfMonth
         ).apply { setOnDismissListener { showStartDatePicker = false } }.show() // Đóng cờ khi dismiss
    }

    if (showEndDatePicker) {
         calendar.set(endDate.year, endDate.monthValue - 1, endDate.dayOfMonth)
         DatePickerDialog(
             context,
             { _, year, month, day ->
                 val newEndDate = LocalDate.of(year, month + 1, day)
                 println("[DEBUG StatsScreen] EndDatePicker selected: $newEndDate") // DEBUG LOG
                 // Chỉ cập nhật khi đang ở chế độ DATE_RANGE
                 if (currentPeriodType == StatsPeriod.DATE_RANGE) {
                     println("[DEBUG StatsScreen] Calling updateStatsPeriod for DATE_RANGE") // DEBUG LOG
                     // Đảm bảo ngày kết thúc không trước ngày bắt đầu
                     viewModel.updateStatsPeriod(StatsPeriod.DATE_RANGE, startDate, newEndDate.coerceAtLeast(startDate))
                 }
                 showEndDatePicker = false
             },
             endDate.year, endDate.monthValue - 1, endDate.dayOfMonth
         ).apply {
             // Đảm bảo không chọn được ngày trước ngày bắt đầu
              datePicker.minDate = Calendar.getInstance().apply {
                  set(startDate.year, startDate.monthValue - 1, startDate.dayOfMonth)
              }.timeInMillis
              setOnDismissListener { showEndDatePicker = false } // Đóng cờ khi dismiss
         }.show()
    }
}

@Composable
fun DateSelectorButton(label: String, date: LocalDate, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Beige.copy(alpha = 0.8f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = Brown, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yy", Locale("vi", "VN"))), color = Brown, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}


// Composable cho Card tổng hợp cả giai đoạn
@Composable
fun StatsOverallSummaryCard(summary: StatsSummary) {
     Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = Brown.copy(alpha = 0.9f) // Màu nền nổi bật hơn
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Tổng Hợp: ${summary.periodLabel}",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                color = Color.White
             )
             Spacer(Modifier.height(10.dp))
             SummaryRow("Số ngày làm việc:", "${summary.totalDays} ngày")
             SummaryRow("Tổng số ca:", "${summary.totalShifts} ca (${summary.totalPlusShifts} ca Plus)")
             if (summary.totalDeliveries > 0) {
                 SummaryRow("Tổng đơn hàng:", "${summary.totalDeliveries} đơn")
             }
             SummaryRow("Tổng giờ làm:", FormatDuration(summary.totalHours))
             SummaryRow("Tổng tiền (Trước):", formatCurrency(summary.totalGrossIncome))
             SummaryRow("Thu nhập ròng:", formatCurrency(summary.totalNetIncome))
             SummaryRow("Trung bình/giờ:", "${formatCurrency(summary.averageHourlyRate)}/h")
        }
    }
}

// Composable cho một item chi tiết ngày trong danh sách thống kê
@Composable
fun DailyDetailItem(summary: DailySummary) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd/MM/yy", Locale("vi", "VN")) // Thứ, dd/MM/yy
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp) // Tăng padding dọc
    ) {
        Text(
            summary.date.format(dateFormatter),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(4.dp))
         Row(
             modifier = Modifier.fillMaxWidth(),
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
         ) {
             TextInfo("Ca: ${summary.shiftCount}")
             if (summary.plusShiftsCount > 0) {
                 TextInfo("Plus: ${summary.plusShiftsCount}")
             }
             if (summary.totalDeliveries > 0) {
                 TextInfo("Đơn: ${summary.totalDeliveries}")
             }
             TextInfo("Giờ: ${FormatDuration(summary.totalHours)}")
         }
         Row(
             modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
             horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
         ) {
            TextInfo("Trước phí: ${formatCurrency(summary.totalGrossIncome)}")
            TextInfo("TB/giờ: ${formatCurrency(summary.averageHourlyRate)}")
            // Hiển thị thu nhập ròng nổi bật hơn
            Text(
                 "Ròng: ${formatCurrency(summary.totalNetIncome)}",
                 color = Color.White, // Màu trắng cho nổi bật
                 fontWeight = FontWeight.SemiBold,
                 fontSize = 14.sp
             )
        }
    }
}

// Composable nhỏ cho các thông tin phụ trong DailyDetailItem
@Composable
fun RowScope.TextInfo(text: String) {
     Text(
         text = text,
         color = Beige, // Màu nhạt hơn
         fontSize = 13.sp,
         modifier = Modifier.padding(end = 8.dp) // Thêm khoảng cách phải
     )
}


// Hàm lấy nhãn hiển thị cho nút chọn giai đoạn chính
fun generateDisplayPeriodLabel(period: StatsPeriod, start: LocalDate, end: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi", "VN"))
    val monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy", Locale("vi", "VN"))
    return when(period) {
        StatsPeriod.SPECIFIC_DAY -> "Ngày: ${start.format(formatter)}"
        StatsPeriod.DATE_RANGE -> "Khoảng: ${start.format(formatter)} - ${end.format(formatter)}"
        StatsPeriod.CURRENT_WEEK -> "Tuần này"
        StatsPeriod.PREVIOUS_WEEK -> "Tuần trước"
        StatsPeriod.CURRENT_MONTH -> "Tháng ${start.format(monthFormatter)}"
        StatsPeriod.PREVIOUS_MONTH -> "Tháng ${start.format(monthFormatter)}"
        StatsPeriod.CURRENT_YEAR -> "Năm ${start.year}"
        StatsPeriod.ALL_TIME -> "Toàn bộ thời gian"
    }
}

// Hàm lấy nhãn ngắn gọn cho DropdownMenu
fun getPeriodDropdownLabel(period: StatsPeriod): String {
     return when(period) {
         StatsPeriod.SPECIFIC_DAY -> "Chọn ngày cụ thể..."
         StatsPeriod.DATE_RANGE -> "Chọn khoảng ngày..."
         StatsPeriod.CURRENT_WEEK -> "Tuần này"
         StatsPeriod.PREVIOUS_WEEK -> "Tuần trước"
         StatsPeriod.CURRENT_MONTH -> "Tháng này"
         StatsPeriod.PREVIOUS_MONTH -> "Tháng trước"
         StatsPeriod.CURRENT_YEAR -> "Năm nay"
         StatsPeriod.ALL_TIME -> "Toàn bộ thời gian"
     }
} 