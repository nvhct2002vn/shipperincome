package com.example.shipperincome.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipperincome.data.model.Shift
import com.example.shipperincome.data.model.ShiftType
import com.example.shipperincome.data.repository.ShiftRepository
import com.example.shipperincome.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

// Enum định nghĩa các loại khoảng thời gian thống kê
enum class StatsPeriod {
    SPECIFIC_DAY, DATE_RANGE, CURRENT_WEEK, PREVIOUS_WEEK, CURRENT_MONTH, PREVIOUS_MONTH, CURRENT_YEAR, ALL_TIME
}

// Data class cho tổng kết ngày
data class DailySummary(
    val date: LocalDate,
    val shiftCount: Int,
    val totalHours: Double,
    val totalGrossIncome: Double,
    val totalNetIncome: Double,
    val averageHourlyRate: Double,
    val plusShiftsCount: Int = 0, // Số ca Plus trong ngày
    val totalDeliveries: Int = 0 // Tổng số đơn hàng trong ngày
)

// Data class cho tổng hợp thống kê cả giai đoạn
data class StatsSummary(
    val periodLabel: String, // Mô tả giai đoạn (vd: "Tuần 30", "Tháng 7/2024")
    val totalDays: Long, // Số ngày có dữ liệu trong giai đoạn
    val totalShifts: Int, // Tổng số ca làm
    val totalHours: Double,
    val totalGrossIncome: Double,
    val totalNetIncome: Double,
    val averageHourlyRate: Double, // Trung bình giờ của cả giai đoạn
    val totalPlusShifts: Int = 0, // Tổng số ca Plus trong giai đoạn
    val totalDeliveries: Int = 0 // Tổng số đơn hàng trong giai đoạn
)


class ShiftViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShiftRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val _allShifts = MutableStateFlow<List<Shift>>(emptyList())
    val allShifts: StateFlow<List<Shift>> = _allShifts.asStateFlow()


    // === Input Screen State ===
    var selectedDate by mutableStateOf(LocalDate.now())
        private set
    var startTimeInput by mutableStateOf("")
    var endTimeInput by mutableStateOf("")
    var incomeInput by mutableStateOf("")
    var selectedShiftType by mutableStateOf(ShiftType.PLUS) // Loại ca làm việc - mặc định Plus
    var deliveryCountInput by mutableStateOf("") // Số đơn hàng (chỉ cho ca Plus)
    var editingShiftId by mutableStateOf<String?>(null)
    var inputError by mutableStateOf<String?>(null) // State cho thông báo lỗi nhập liệu

    // === Stats Screen State ===
    var statsPeriodType by mutableStateOf(StatsPeriod.CURRENT_WEEK)
        private set
    var statsStartDate by mutableStateOf(getFirstDayOfWeek(LocalDate.now()))
        private set
    var statsEndDate by mutableStateOf(getLastDayOfWeek(LocalDate.now()))
        private set

    // === Derived State (Tính toán từ State gốc) ===

    // Ca làm cho ngày đang chọn (InputScreen)
    val shiftsForSelectedDate: StateFlow<List<Shift>> = _allShifts.combine(snapshotFlow { selectedDate }) { shifts, date ->
        shifts.filter { it.getLocalDate() == date }.sortedBy { it.startTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tổng kết ngày cho ngày đang chọn (InputScreen)
    val dailySummary: StateFlow<DailySummary?> = shiftsForSelectedDate.map { dailyShifts ->
        if (dailyShifts.isEmpty()) null else {
            calculateDailySummary(dailyShifts.first().getLocalDate(), dailyShifts)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dữ liệu cho màn hình Thống kê
    val statisticsData: StateFlow<Pair<StatsSummary?, List<DailySummary>>> =
        combine(
            _allShifts,
            snapshotFlow { statsStartDate },
            snapshotFlow { statsEndDate }
        ) { allShifts, start, end ->
            val filteredShifts = allShifts.filter { shift ->
                val shiftDate = shift.getLocalDate()
                !shiftDate.isBefore(start) && !shiftDate.isAfter(end)
            }

            val overallSummary = if (filteredShifts.isEmpty()) null else calculateStatsSummary(filteredShifts, start, end)

            val dailySummaries = filteredShifts
                .groupBy { it.getLocalDate() }
                .map { (date, shiftsInDay) -> calculateDailySummary(date, shiftsInDay) }
                .sortedByDescending { it.date }

            Pair(overallSummary, dailySummaries)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(null, emptyList()))

    // Định nghĩa formatter một lần để tái sử dụng
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        loadShifts()
        updateStatsPeriod(StatsPeriod.CURRENT_WEEK) // Khởi tạo giá trị mặc định
    }

    // --- Actions ---

    private fun loadShifts() {
        viewModelScope.launch { _allShifts.value = repository.loadAllShifts() }
    }

    private fun saveAllShifts() {
        viewModelScope.launch { repository.saveAllShifts(_allShifts.value) }
    }

    fun addOrUpdateShift() {
        inputError = null // Reset lỗi cũ

        // Validate Input
        val startTime = parseTime(startTimeInput)
        val endTime = parseTime(endTimeInput)
        val income = incomeInput.toDoubleOrNull()

        if (startTime == null) { inputError = "Giờ bắt đầu không hợp lệ (HH:mm)"; return }
        if (endTime == null) { inputError = "Giờ kết thúc không hợp lệ (HH:mm)"; return }

        if (startTime == endTime) { inputError = "Giờ bắt đầu và kết thúc không được giống nhau"; return }

        if (income == null || income <= 0) { inputError = "Thu nhập phải là số dương"; return }

        // Validate delivery count cho ca Plus
        val deliveryCount = if (selectedShiftType == ShiftType.PLUS) {
            val count = deliveryCountInput.toIntOrNull()
            if (count == null || count < 0) {
                inputError = "Số đơn hàng phải là số không âm"
                return
            }
            count
        } else {
            0
        }

        viewModelScope.launch {
            val currentShifts = _allShifts.value.toMutableList()
            val dateString = selectedDate.format(DateTimeFormatter.ISO_DATE)
            val formattedStartTime = startTime.format(DateTimeFormatter.ISO_TIME)
            val formattedEndTime = endTime.format(DateTimeFormatter.ISO_TIME)

            if (editingShiftId == null) { // Thêm mới
                // Lấy current settings để snapshot vào shift
                val currentTaxRate = settingsRepository.getTaxRate()
                val currentDeliveryFee = settingsRepository.getDeliveryFee()

                val newShift = Shift(
                    date = dateString,
                    startTime = formattedStartTime,
                    endTime = formattedEndTime,
                    grossIncome = income,
                    shiftType = selectedShiftType,
                    deliveryCount = deliveryCount,
                    taxRateSnapshot = currentTaxRate,
                    deliveryFeeSnapshot = currentDeliveryFee
                )
                currentShifts.add(newShift)
            } else { // Cập nhật - sử dụng settings hiện tại
                // Lấy current settings để snapshot vào shift được sửa
                val currentTaxRate = settingsRepository.getTaxRate()
                val currentDeliveryFee = settingsRepository.getDeliveryFee()

                val index = currentShifts.indexOfFirst { it.id == editingShiftId }
                if (index != -1) {
                    currentShifts[index] = currentShifts[index].copy(
                        date = dateString, // Cập nhật ngày nếu người dùng đổi ngày khi đang sửa
                        startTime = formattedStartTime,
                        endTime = formattedEndTime,
                        grossIncome = income,
                        shiftType = selectedShiftType,
                        deliveryCount = deliveryCount,
                        // Cập nhật snapshot values theo settings hiện tại khi sửa
                        taxRateSnapshot = currentTaxRate,
                        deliveryFeeSnapshot = currentDeliveryFee
                    )
                } else {
                    inputError = "Lỗi: Không tìm thấy ca làm để cập nhật."
                    return@launch
                }
            }
            _allShifts.value = currentShifts
            saveAllShifts()
            clearInputs()
        }
    }

    fun deleteShift(shiftId: String) {
        viewModelScope.launch {
            val updatedShifts = _allShifts.value.filterNot { it.id == shiftId }
            if (updatedShifts.size < _allShifts.value.size) { // Kiểm tra xem có xóa được không
                _allShifts.value = updatedShifts
                saveAllShifts()
            }
        }
    }

    fun startEditing(shift: Shift) {
        editingShiftId = shift.id
        selectedDate = shift.getLocalDate()
        selectedShiftType = shift.shiftType
        deliveryCountInput = if (shift.shiftType == ShiftType.PLUS) shift.deliveryCount.toString() else ""

        try {
            // Parse thời gian từ Shift object
            val startTimeParsed = LocalTime.parse(shift.startTime)
            val endTimeParsed = LocalTime.parse(shift.endTime)

            // Định dạng lại thành "HH:mm" trước khi gán vào state cho UI
            startTimeInput = startTimeParsed.format(timeFormatter)
            endTimeInput = endTimeParsed.format(timeFormatter)
        } catch (e: DateTimeParseException) {
            // Xử lý lỗi nếu parse thất bại
            println("[ERROR] Failed to parse time when starting edit for shift ID ${shift.id}: ${e.message}")
            startTimeInput = shift.startTime // Giữ giá trị gốc nếu lỗi
            endTimeInput = shift.endTime
        } catch (e: Exception) {
            // Các lỗi khác có thể xảy ra
            println("[ERROR] Unexpected error when parsing time for shift ID ${shift.id}: ${e.message}")
            startTimeInput = shift.startTime
            endTimeInput = shift.endTime
        }

        incomeInput = shift.grossIncome.toString()
        inputError = null // Xóa lỗi khi bắt đầu sửa
    }

    fun clearInputs() {
        startTimeInput = ""
        endTimeInput = ""
        incomeInput = ""
        selectedShiftType = ShiftType.PLUS
        deliveryCountInput = ""
        editingShiftId = null
        inputError = null
    }

    fun changeSelectedDate(newDate: LocalDate) {
        selectedDate = newDate
        clearInputs() // Xóa input khi đổi ngày
    }

     fun updateStatsPeriod(period: StatsPeriod, customStart: LocalDate? = null, customEnd: LocalDate? = null) {
        println("[DEBUG ViewModel] updateStatsPeriod called: type=$period, start=$customStart, end=$customEnd") // DEBUG LOG ở đầu hàm
        val oldStartDate = statsStartDate // Lưu lại giá trị cũ để so sánh
        val oldEndDate = statsEndDate
        
        statsPeriodType = period
        val today = LocalDate.now()

        when (period) {
            StatsPeriod.SPECIFIC_DAY -> {
                if (customStart != null) {
                    statsStartDate = customStart
                    statsEndDate = customStart
                } else {
                    println("[ERROR ViewModel] customStart is null for SPECIFIC_DAY") 
                    statsStartDate = today
                    statsEndDate = today
                }
            }
            StatsPeriod.DATE_RANGE -> {
                if (customStart != null && customEnd != null) {
                    statsStartDate = customStart
                    statsEndDate = customEnd
                } else if (customStart != null) {
                    // Nếu chỉ có start date, thì dùng end date hiện tại hoặc mặc định 7 ngày
                    statsStartDate = customStart
                    statsEndDate = customEnd ?: today
                } else {
                    println("[ERROR ViewModel] customStart and customEnd are null for DATE_RANGE")
                    statsStartDate = today.minusDays(6)
                    statsEndDate = today
                }
            }
            StatsPeriod.CURRENT_WEEK -> {
                statsStartDate = getFirstDayOfWeek(today)
                statsEndDate = getLastDayOfWeek(today)
            }
             StatsPeriod.PREVIOUS_WEEK -> {
                val prevWeekStart = getFirstDayOfWeek(today).minusWeeks(1)
                statsStartDate = prevWeekStart
                statsEndDate = getLastDayOfWeek(prevWeekStart)
            }
            StatsPeriod.CURRENT_MONTH -> {
                statsStartDate = today.withDayOfMonth(1)
                statsEndDate = today.with(TemporalAdjusters.lastDayOfMonth())
            }
            StatsPeriod.PREVIOUS_MONTH -> {
                val prevMonth = today.minusMonths(1)
                statsStartDate = prevMonth.withDayOfMonth(1)
                statsEndDate = prevMonth.with(TemporalAdjusters.lastDayOfMonth())
             }
            StatsPeriod.CURRENT_YEAR -> {
                statsStartDate = today.withDayOfYear(1)
                statsEndDate = today.with(TemporalAdjusters.lastDayOfYear())
             }
             StatsPeriod.ALL_TIME -> {
                 // Tìm ngày bắt đầu và kết thúc trong dữ liệu nếu có, nếu không thì là hôm nay
                 val firstDate = _allShifts.value.minOfOrNull { it.getLocalDate() } ?: today
                 val lastDate = _allShifts.value.maxOfOrNull { it.getLocalDate() } ?: today
                 statsStartDate = firstDate
                 statsEndDate = lastDate
             }
        }
        // Đảm bảo EndDate không trước StartDate
        if (statsEndDate.isBefore(statsStartDate)) {
            statsEndDate = statsStartDate
        }

        // Chỉ in log nếu có sự thay đổi thực sự
        if (oldStartDate != statsStartDate || oldEndDate != statsEndDate) {
            println("[DEBUG ViewModel] ViewModel state updated: statsStartDate=$statsStartDate, statsEndDate=$statsEndDate") // DEBUG LOG ở cuối hàm
        } else {
            println("[DEBUG ViewModel] ViewModel state unchanged.")
        }
    }


    // --- Helper Functions ---

    private fun parseTime(timeString: String): LocalTime? {
        return try {
            // Ưu tiên parse định dạng HH:mm
            LocalTime.parse(timeString.trim(), timeFormatter)
        } catch (e: DateTimeParseException) {
            try {
                // Thử parse định dạng mặc định nếu HH:mm thất bại (có thể bao gồm giây)
                LocalTime.parse(timeString.trim())
            } catch (e2: DateTimeParseException) {
                println("[ERROR] Failed to parse time string: '$timeString'")
                null // Trả về null nếu cả hai định dạng đều không đúng
            }
        }
    }

    private fun calculateDailySummary(date: LocalDate, shiftsInDay: List<Shift>, taxRate: Double = 0.0, deliveryFee: Double = 0.0): DailySummary {
        val totalHours = shiftsInDay.sumOf { it.calculateDurationHours() }
        val totalGross = shiftsInDay.sumOf { it.grossIncome }
        val totalNet = shiftsInDay.sumOf { it.calculateNetIncome() } // Sử dụng snapshot values
        val avgHourly = if (totalHours > 0) totalNet / totalHours else 0.0

        // Tính toán thông tin về đơn hàng
        val plusShifts = shiftsInDay.filter { it.shiftType == ShiftType.PLUS }
        val plusShiftsCount = plusShifts.size
        val totalDeliveries = plusShifts.sumOf { it.deliveryCount }

        return DailySummary(date, shiftsInDay.size, totalHours, totalGross, totalNet, avgHourly, plusShiftsCount, totalDeliveries)
    }

    private fun calculateStatsSummary(shifts: List<Shift>, periodStart: LocalDate, periodEnd: LocalDate, taxRate: Double = 0.0, deliveryFee: Double = 0.0): StatsSummary {
        val totalHours = shifts.sumOf { it.calculateDurationHours() }
        val totalGross = shifts.sumOf { it.grossIncome }
        val totalNet = shifts.sumOf { it.calculateNetIncome() } // Sử dụng snapshot values
        val avgHourly = if (totalHours > 0) totalNet / totalHours else 0.0
        val periodLabel = generatePeriodLabel(statsPeriodType, periodStart, periodEnd)
        val totalDays = shifts.map { it.getLocalDate() }.distinct().count().toLong()
        val totalShiftsCount = shifts.size

        // Tính toán thông tin về đơn hàng
        val plusShifts = shifts.filter { it.shiftType == ShiftType.PLUS }
        val totalPlusShifts = plusShifts.size
        val totalDeliveries = plusShifts.sumOf { it.deliveryCount }

        return StatsSummary(periodLabel, totalDays, totalShiftsCount, totalHours, totalGross, totalNet, avgHourly, totalPlusShifts, totalDeliveries)
    }

    private fun getFirstDayOfWeek(date: LocalDate): LocalDate {
        // Sử dụng Locale.KOREA để xác định ngày đầu tuần theo chuẩn Hàn Quốc
        val firstDayOfWeek = WeekFields.of(Locale.KOREA).firstDayOfWeek
        return date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    }

    private fun getLastDayOfWeek(date: LocalDate): LocalDate {
        val firstDayOfWeek = WeekFields.of(Locale.KOREA).firstDayOfWeek
        return date.with(TemporalAdjusters.nextOrSame(firstDayOfWeek.plus(6))) // +6 ngày từ ngày đầu tuần
    }

     private fun generatePeriodLabel(period: StatsPeriod, start: LocalDate, end: LocalDate): String {
        val formatterShort = DateTimeFormatter.ofPattern("dd/MM")
         val formatterFull = DateTimeFormatter.ofPattern("dd/MM/yyyy")
         val formatterMonth = DateTimeFormatter.ofPattern("MM/yyyy")
         val formatterYear = DateTimeFormatter.ofPattern("yyyy")
        val weekFields = WeekFields.of(Locale.KOREA) // Tuần theo Hàn Quốc

        return when(period) {
            StatsPeriod.SPECIFIC_DAY -> "Ngày ${start.format(formatterFull)}"
            StatsPeriod.DATE_RANGE -> "${start.format(formatterShort)} - ${end.format(formatterShort)}"
            StatsPeriod.CURRENT_WEEK, StatsPeriod.PREVIOUS_WEEK -> {
                val weekNum = start.get(weekFields.weekOfWeekBasedYear())
                 "Tuần $weekNum (${start.format(formatterShort)} - ${end.format(formatterShort)})"
            }
            StatsPeriod.CURRENT_MONTH, StatsPeriod.PREVIOUS_MONTH -> "Tháng ${start.format(formatterMonth)}"
            StatsPeriod.CURRENT_YEAR -> "Năm ${start.format(formatterYear)}"
            StatsPeriod.ALL_TIME -> "Toàn bộ (${start.format(formatterShort)} - ${end.format(formatterShort)})"
        }
    }
    
    // Extension function for Shift to make code more readable
    private fun Shift.getLocalDate(): LocalDate = this.localDate
} 