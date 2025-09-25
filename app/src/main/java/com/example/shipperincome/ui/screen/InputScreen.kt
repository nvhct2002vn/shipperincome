package com.example.shipperincome.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning // Icon cho lỗi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController // Import NavController
import com.example.shipperincome.data.model.Shift
import com.example.shipperincome.data.model.ShiftType
import com.example.shipperincome.ui.theme.* // Import Theme colors
import com.example.shipperincome.ui.viewmodel.DailySummary
import com.example.shipperincome.ui.viewmodel.ShiftViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.text.NumberFormat
import java.lang.Math
import com.example.shipperincome.utils.FormatDuration

@Composable
fun InputScreen(viewModel: ShiftViewModel, navController: NavHostController) { // Nhận NavController

    // Correctly accessing state objects using collectAsState() and State delegation
    val selectedDate = viewModel.selectedDate
    val shiftsForDay by viewModel.shiftsForSelectedDate.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()
    val editingShiftId = viewModel.editingShiftId
    val inputError = viewModel.inputError // Lấy trạng thái lỗi

    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // State cho tab selection

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val calendar = Calendar.getInstance()

    // Định dạng ngày tháng
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi", "VN"))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Hàm hiển thị DatePickerDialog
    val showDatePicker = {
        calendar.timeInMillis = System.currentTimeMillis() // Cập nhật calendar về ngày hiện tại
        val currentYear = selectedDate.year
        val currentMonth = selectedDate.monthValue - 1 // Calendar month is 0-based
        val currentDay = selectedDate.dayOfMonth

        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                viewModel.changeSelectedDate(LocalDate.of(year, month + 1, dayOfMonth))
            },
            currentYear, currentMonth, currentDay
        ).show()
    }

    // Hàm hiển thị TimePickerDialog
    val showTimePicker = { isStartTime: Boolean ->
        val currentTimeString =
            if (isStartTime) viewModel.startTimeInput else viewModel.endTimeInput
        var initialHour: Int
        var initialMinute: Int
        try {
            val parsedTime = LocalTime.parse(currentTimeString, timeFormatter)
            initialHour = parsedTime.hour
            initialMinute = parsedTime.minute
        } catch (e: Exception) {
            // Lấy giờ hiện tại nếu parse lỗi hoặc input rỗng
            val now = LocalTime.now()
            initialHour = now.hour
            initialMinute = now.minute
        }

        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                val selectedTime = LocalTime.of(hour, minute)
                if (isStartTime) {
                    viewModel.startTimeInput = selectedTime.format(timeFormatter)
                } else {
                    viewModel.endTimeInput = selectedTime.format(timeFormatter)
                }
            },
            initialHour, initialMinute, true // true = 24 giờ format
        ).show()
    }

    // Gradient Background
    val gradientBrush = Brush.verticalGradient(colors = listOf(GreenStart, GreenEnd))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientBrush)
            .padding(16.dp)
    ) {
        // 1. Chọn Ngày
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                "Ngày làm việc:",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(Modifier.weight(1f)) // Đẩy nút sang phải
            Button(
                onClick = showDatePicker,
                colors = ButtonDefaults.buttonColors(backgroundColor = Beige.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Chọn ngày", tint = Brown)
                Spacer(Modifier.width(4.dp))
                Text(
                    selectedDate.format(dateFormatter),
                    color = Brown,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 2. Form Nhập/Sửa Ca Làm
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            backgroundColor = Beige.copy(alpha = 0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (editingShiftId == null) "Thêm Ca Làm Mới" else "Sửa Ca Làm",
                    style = MaterialTheme.typography.h6,
                    color = Brown,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))

                // Hiển thị lỗi nếu có
                if (inputError != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Lỗi", tint = Color.Red)
                        Spacer(Modifier.width(4.dp))
                        Text(inputError, color = Color.Red, fontSize = 14.sp)
                    }
                }

                // Radio buttons cho loại ca làm việc - cùng hàng và compact
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Loại ca:",
                        color = Brown,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    // Radio button cho ca Plus
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            viewModel.selectedShiftType = ShiftType.PLUS
                        }
                    ) {
                        RadioButton(
                            selected = viewModel.selectedShiftType == ShiftType.PLUS,
                            onClick = { viewModel.selectedShiftType = ShiftType.PLUS },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Brown,
                                unselectedColor = Brown.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            "Plus",
                            color = Brown,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 2.dp, end = 16.dp)
                        )
                    }

                    // Radio button cho ca Thường
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            viewModel.selectedShiftType = ShiftType.NORMAL
                            viewModel.deliveryCountInput = "" // Clear delivery count khi đổi sang NORMAL
                        }
                    ) {
                        RadioButton(
                            selected = viewModel.selectedShiftType == ShiftType.NORMAL,
                            onClick = {
                                viewModel.selectedShiftType = ShiftType.NORMAL
                                viewModel.deliveryCountInput = ""
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Brown,
                                unselectedColor = Brown.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            "Thường",
                            color = Brown,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Ô nhập số đơn hàng (chỉ hiện khi chọn Plus)
                if (viewModel.selectedShiftType == ShiftType.PLUS) {
                    OutlinedTextField(
                        value = viewModel.deliveryCountInput,
                        onValueChange = {
                            viewModel.deliveryCountInput = it.filter { char -> char.isDigit() }
                        },
                        label = { Text("Số đơn hàng") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Brown,
                            unfocusedBorderColor = Brown.copy(alpha = 0.5f),
                            textColor = Color.DarkGray,
                            focusedLabelColor = Brown,
                            unfocusedLabelColor = Brown.copy(alpha = 0.7f)
                        ),
                        placeholder = { Text("0", color = Color.Gray) }
                    )
                }

                // Ô nhập giờ bắt đầu, kết thúc, thu nhập
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeInput(
                        label = "Bắt đầu",
                        value = viewModel.startTimeInput,
                        onClick = { showTimePicker(true) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TimeInput(
                        label = "Kết thúc",
                        value = viewModel.endTimeInput,
                        onClick = { showTimePicker(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.incomeInput,
                    onValueChange = {
                        viewModel.incomeInput = it.filter { char -> char.isDigit() }
                    },
                    label = { Text("Thu nhập (Trước trừ thuế)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Brown,
                        unfocusedBorderColor = Brown.copy(alpha = 0.5f),
                        textColor = Color.DarkGray,
                        focusedLabelColor = Brown,
                        unfocusedLabelColor = Brown.copy(alpha = 0.7f)
                    ),
                    placeholder = { Text("0", color = Color.Gray) }
                )
                Spacer(Modifier.height(16.dp))
                // Nút Lưu / Hủy sửa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.addOrUpdateShift()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Brown),
                        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            if (editingShiftId == null) "LƯU CA LÀM" else "LƯU THAY ĐỔI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Nút Hủy chỉ hiển thị khi đang sửa
                    if (editingShiftId != null) {
                        Button(
                            onClick = { viewModel.clearInputs() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray),
                            elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
                        ) {
                            Text("HỦY SỬA", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. TabRow để chuyển đổi giữa "Các ca đã nhập" và "Thống kê ngày"
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            backgroundColor = Color.Transparent
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                backgroundColor = Brown.copy(alpha = 0.9f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Các Ca Đã Nhập",
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Thống Kê Ngày",
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 4. Nội dung tab được chọn
        when (selectedTab) {
            0 -> {
                // Tab "Các Ca Đã Nhập"
                ShiftsListTab(
                    shiftsForDay = shiftsForDay,
                    onEditClick = { shift -> viewModel.startEditing(shift) },
                    onDeleteClick = { shiftId -> showDeleteConfirmDialog = shiftId },
                    modifier = Modifier.weight(1f)
                )
            }
            1 -> {
                // Tab "Thống Kê Ngày"
                DailyStatsTab(
                    dailySummary = dailySummary,
                    shiftsForDay = shiftsForDay,
                    dateFormatter = dateFormatter,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } // End Column Chính

    // Dialog Xác nhận Xóa
    if (showDeleteConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Xác nhận xóa", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc muốn xóa ca làm này?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteShift(showDeleteConfirmDialog!!)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) { Text("XÓA", color = Color.White) }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

// Composable cho ô nhập thời gian có thể bấm được
@Composable
fun TimeInput(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {}, // Không cho sửa trực tiếp
        label = { Text(label) },
        modifier = modifier.clickable(onClick = onClick), // Click vào là gọi TimePicker
        readOnly = true, // Ngăn bàn phím hiện lên
        enabled = false, // Vô hiệu hóa nhưng vẫn nhận click
        colors = TextFieldDefaults.outlinedTextFieldColors(
            disabledTextColor = Color.DarkGray, // Màu text khi disabled
            disabledBorderColor = Brown.copy(alpha = 0.5f),
            disabledLabelColor = Brown.copy(alpha = 0.7f),
            backgroundColor = Color.Transparent // Để hiệu ứng click rõ hơn
        )
    )
}


// Composable cho một item ca làm trong danh sách
@Composable
fun ShiftItem(shift: Shift, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Dòng đầu: Chỉ thời gian, không có badge để tránh nhảy dòng
            Text(
                "${shift.startTime} - ${shift.endTime} (${FormatDuration(shift.calculateDurationHours())})",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))

            // Hiển thị số đơn nếu là ca Plus
            if (shift.shiftType == ShiftType.PLUS && shift.deliveryCount > 0) {
                Text(
                    "Số đơn: ${shift.deliveryCount}",
                    color = Beige.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
            }

            Text(
                "Thu nhập: ${formatCurrency(shift.grossIncome)}",
                color = Beige,
                fontSize = 14.sp
            )
            Text(
                "Ròng: ${formatCurrency(shift.calculateNetIncome())} (TB: ${formatCurrency(shift.calculateHourlyRate())}/h)",
                color = Beige.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
        // Badge Plus và Nút Sửa/Xóa
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Badge cho loại ca Plus
            if (shift.shiftType == ShiftType.PLUS) {
                Card(
                    backgroundColor = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        "PLUS",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Nút Sửa và Xóa
            IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Sửa",
                    tint = Beige.copy(alpha = 0.9f)
                )
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = RedDark.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Composable cho Card tổng kết ngày
@Composable
fun DailySummaryCard(summary: DailySummary, dateFormatter: DateTimeFormatter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = Brown.copy(alpha = 0.9f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Tổng kết ngày: ${summary.date.format(dateFormatter)}",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            SummaryRow("Tổng ca:", "${summary.shiftCount} ca")
            SummaryRow("Tổng giờ làm:", FormatDuration(summary.totalHours))
            SummaryRow("Tổng tiền (Trước):", formatCurrency(summary.totalGrossIncome))
            SummaryRow("Thu nhập ròng:", formatCurrency(summary.totalNetIncome))
            SummaryRow("Trung bình/giờ:", "${formatCurrency(summary.averageHourlyRate)}/h")
        }
    }
}

// Composable phụ trợ cho dòng tổng kết
@Composable
fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            color = Beige,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp
        )
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

// Composable cho tab "Các Ca Đã Nhập"
@Composable
fun ShiftsListTab(
    shiftsForDay: List<Shift>,
    onEditClick: (Shift) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        if (shiftsForDay.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    elevation = 4.dp,
                    backgroundColor = Brown.copy(alpha = 0.9f)
                ) {
                    Text(
                        "Chưa có ca làm nào cho ngày này.",
                        color = Beige,
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            items(shiftsForDay, key = { it.id }) { shift ->
                ShiftItem(
                    shift = shift,
                    onEditClick = { onEditClick(shift) },
                    onDeleteClick = { onDeleteClick(shift.id) }
                )
                Divider(color = GreenEnd.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

// Composable cho tab "Thống Kê Ngày"
@Composable
fun DailyStatsTab(
    dailySummary: DailySummary?,
    shiftsForDay: List<Shift>,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            if (dailySummary != null) {
                DailySummaryCard(summary = dailySummary, dateFormatter = dateFormatter)
            } else if (shiftsForDay.isNotEmpty()) {
                // Hiển thị loading hoặc chờ tính toán nếu cần
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    backgroundColor = Brown.copy(alpha = 0.1f)
                ) {
                    Text(
                        "Đang tính toán...",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = Beige
                    )
                }
            } else {
                // Không có dữ liệu
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    backgroundColor = Brown.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Chưa có ca làm nào",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Hãy thêm ca làm để xem thống kê ngày",
                            color = Beige.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Hàm định dạng tiền tệ Hàn Quốc
fun formatCurrency(amount: Double): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    val roundedAmount = Math.round(amount)
    return "${numberFormat.format(roundedAmount)}₩"
} 