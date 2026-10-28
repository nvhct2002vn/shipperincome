package com.example.shipperincome.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipperincome.data.model.Shift
import com.example.shipperincome.data.repository.SettingsRepository
import com.example.shipperincome.data.repository.ShiftRepository
import com.example.shipperincome.utils.TimeHm
import com.example.shipperincome.utils.minutesToHoursInput
import com.example.shipperincome.utils.parseHoursToMinutes
import com.example.shipperincome.utils.toPlainInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Thong bao mot lan cho Snackbar, co the kem nut hanh dong (vd. Hoan tac). */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/** Uoc tinh thu nhap cua ca dang nhap, hien ngay duoi form. */
data class InputPreview(
    val grossIncome: Double,
    val netIncome: Double,
    val durationHours: Double,
    val hourlyRate: Double
)

class ShiftViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShiftRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val _allShifts = MutableStateFlow<List<Shift>>(emptyList())
    val allShifts: StateFlow<List<Shift>> = _allShifts.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    var isLoaded by mutableStateOf(false)
        private set

    var selectedDate by mutableStateOf(LocalDate.now())
        private set
    /** true: nhap thang tong so gio trong ngay; false: chon gio bat dau - ket thuc. */
    var totalHoursMode by mutableStateOf(settingsRepository.isTotalHoursMode())
        private set
    var totalHoursInput by mutableStateOf("")
    var startTimeInput by mutableStateOf("")
    var endTimeInput by mutableStateOf("")
    var deliveryCountInput by mutableStateOf("")
    var plusIncomeInput by mutableStateOf("")
    var normalIncomeInput by mutableStateOf("")
    var plusMissionIncomeInput by mutableStateOf("")
    var normalMissionIncomeInput by mutableStateOf("")
    var editingShiftId by mutableStateOf<String?>(null)
        private set
    var inputError by mutableStateOf<String?>(null)

    var statsPeriodType by mutableStateOf(StatsPeriod.CURRENT_WEEK)
        private set
    private var statsCustomStart by mutableStateOf<LocalDate?>(null)
    private var statsCustomEnd by mutableStateOf<LocalDate?>(null)

    val shiftsForSelectedDate: StateFlow<List<Shift>> =
        _allShifts.combine(snapshotFlow { selectedDate }) { shifts, date ->
            shifts.filter { it.localDate == date }.sortedBy { it.startTime }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummary: StateFlow<DailySummary?> = shiftsForSelectedDate.map { dailyShifts ->
        if (dailyShifts.isEmpty()) null
        else StatsCalculations.summarizeDay(dailyShifts.first().localDate, dailyShifts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val statisticsData: StateFlow<StatsData?> =
        combine(
            _allShifts,
            snapshotFlow { Triple(statsPeriodType, statsCustomStart, statsCustomEnd) }
        ) { shifts, (period, customStart, customEnd) ->
            StatsCalculations.compute(period, LocalDate.now(), shifts, customStart, customEnd)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Uoc tinh theo thong so hien tai trong Cai dat; null khi chua nhap gi. */
    val inputPreview: InputPreview?
        get() {
            val plus = (plusIncomeInput.toDoubleOrNull() ?: 0.0)
            val normal = (normalIncomeInput.toDoubleOrNull() ?: 0.0)
            val plusMission = (plusMissionIncomeInput.toDoubleOrNull() ?: 0.0)
            val normalMission = (normalMissionIncomeInput.toDoubleOrNull() ?: 0.0)
            if (plus + normal + plusMission + normalMission <= 0.0) return null
            val start = parseTime(startTimeInput)
            val end = parseTime(endTimeInput)
            val totalMinutes = if (totalHoursMode) parseHoursToMinutes(totalHoursInput)?.takeIf { it in 1..MAX_MINUTES } else null
            val shift = Shift(
                date = selectedDate.toString(),
                startTime = start?.toString() ?: "00:00",
                endTime = end?.toString() ?: "00:00",
                durationMinutes = totalMinutes,
                deliveryCount = deliveryCountInput.toIntOrNull() ?: 0,
                plusGrossIncome = plus,
                normalGrossIncome = normal,
                plusMissionIncome = plusMission,
                normalMissionIncome = normalMission,
                plusTaxRateSnapshot = settingsRepository.getPlusTaxRate(),
                normalTaxRateSnapshot = settingsRepository.getNormalTaxRate(),
                deliveryFeeSnapshot = settingsRepository.getDeliveryFee()
            )
            val hours = when {
                totalHoursMode -> totalMinutes?.div(60.0) ?: 0.0
                start != null && end != null && start != end -> shift.calculateDurationHours()
                else -> 0.0
            }
            val net = shift.calculateNetIncome()
            return InputPreview(
                grossIncome = shift.calculateGrossIncome(),
                netIncome = net,
                durationHours = hours,
                hourlyRate = if (hours > 0) net / hours else 0.0
            )
        }

    init {
        loadShifts()
    }

    private fun loadShifts() {
        viewModelScope.launch {
            val result = repository.loadAllShifts()
            _allShifts.value = result.shifts
            isLoaded = true
            result.corruptBackup?.let {
                _messages.send(UiMessage("File dữ liệu bị lỗi nên đã được sao lưu riêng (${it.name}). Dữ liệu mới sẽ lưu vào file mới."))
            }
        }
    }

    private fun persist(shifts: List<Shift>) {
        _allShifts.value = shifts
        viewModelScope.launch {
            try {
                repository.saveAllShifts(shifts)
            } catch (e: Exception) {
                _messages.send(UiMessage("Không lưu được dữ liệu: ${e.message}"))
            }
        }
    }

    fun addOrUpdateShift() {
        inputError = null
        if (!isLoaded) {
            inputError = "Đang tải dữ liệu, vui lòng thử lại sau giây lát."
            return
        }

        val startTime = parseTime(startTimeInput)
        val endTime = parseTime(endTimeInput)
        val totalMinutes = parseHoursToMinutes(totalHoursInput)
        val deliveryCount = deliveryCountInput.toIntOrNull() ?: 0
        val plusIncome = plusIncomeInput.toDoubleOrNull() ?: 0.0
        val normalIncome = normalIncomeInput.toDoubleOrNull() ?: 0.0
        val plusMissionIncome = plusMissionIncomeInput.toDoubleOrNull() ?: 0.0
        val normalMissionIncome = normalMissionIncomeInput.toDoubleOrNull() ?: 0.0
        val totalPlusIncome = plusIncome + plusMissionIncome
        val totalNormalIncome = normalIncome + normalMissionIncome

        if (totalHoursMode) {
            if (totalMinutes == null) { inputError = "Nhập số giờ làm, ví dụ 8 hoặc 8:30"; return }
            if (totalMinutes !in 1..MAX_MINUTES) { inputError = "Số giờ phải lớn hơn 0 và không quá 24 giờ"; return }
        } else {
            if (startTime == null) { inputError = "Chọn giờ bắt đầu"; return }
            if (endTime == null) { inputError = "Chọn giờ kết thúc"; return }
            if (startTime == endTime) { inputError = "Giờ bắt đầu và kết thúc không được giống nhau"; return }
        }
        if (deliveryCount < 0) { inputError = "Số đơn hàng phải là số không âm"; return }
        if (plusIncome < 0 || normalIncome < 0 || plusMissionIncome < 0 || normalMissionIncome < 0) {
            inputError = "Thu nhập không được âm"
            return
        }
        if (totalPlusIncome > 0.0 && deliveryCount <= 0) {
            inputError = ERROR_PLUS_NEEDS_DELIVERIES
            return
        }
        if (totalPlusIncome <= 0.0 && totalNormalIncome <= 0.0) {
            inputError = "Nhập thu nhập Plus hoặc thu nhập thường"
            return
        }

        val currentShifts = _allShifts.value.toMutableList()
        val dateString = selectedDate.format(DateTimeFormatter.ISO_DATE)
        val formattedStartTime = if (totalHoursMode) "" else startTime!!.format(DateTimeFormatter.ISO_TIME)
        val formattedEndTime = if (totalHoursMode) "" else endTime!!.format(DateTimeFormatter.ISO_TIME)
        val durationMinutes = if (totalHoursMode) totalMinutes else null
        val currentPlusTaxRate = settingsRepository.getPlusTaxRate()
        val currentNormalTaxRate = settingsRepository.getNormalTaxRate()
        val currentDeliveryFee = settingsRepository.getDeliveryFee()
        val editingId = editingShiftId

        if (editingId == null) {
            currentShifts.add(
                Shift(
                    date = dateString,
                    startTime = formattedStartTime,
                    endTime = formattedEndTime,
                    deliveryCount = deliveryCount,
                    plusGrossIncome = plusIncome,
                    normalGrossIncome = normalIncome,
                    plusMissionIncome = plusMissionIncome,
                    normalMissionIncome = normalMissionIncome,
                    plusTaxRateSnapshot = currentPlusTaxRate,
                    normalTaxRateSnapshot = currentNormalTaxRate,
                    deliveryFeeSnapshot = currentDeliveryFee,
                    durationMinutes = durationMinutes
                )
            )
        } else {
            val index = currentShifts.indexOfFirst { it.id == editingId }
            if (index == -1) {
                inputError = "Không tìm thấy ca làm để cập nhật."
                return
            }
            currentShifts[index] = currentShifts[index].copy(
                date = dateString,
                startTime = formattedStartTime,
                endTime = formattedEndTime,
                deliveryCount = deliveryCount,
                plusGrossIncome = plusIncome,
                normalGrossIncome = normalIncome,
                plusMissionIncome = plusMissionIncome,
                normalMissionIncome = normalMissionIncome,
                plusTaxRateSnapshot = currentPlusTaxRate,
                normalTaxRateSnapshot = currentNormalTaxRate,
                deliveryFeeSnapshot = currentDeliveryFee,
                durationMinutes = durationMinutes
            )
        }

        persist(currentShifts)
        clearInputs()
        _messages.trySend(UiMessage(if (editingId == null) "Đã lưu ca làm" else "Đã cập nhật ca làm"))
    }

    fun deleteShift(shiftId: String) {
        val removed = _allShifts.value.firstOrNull { it.id == shiftId } ?: return
        if (editingShiftId == shiftId) clearInputs()
        persist(_allShifts.value.filterNot { it.id == shiftId })
        _messages.trySend(
            UiMessage("Đã xóa ca ${removed.timeLabel()}", "Hoàn tác") {
                if (_allShifts.value.none { it.id == removed.id }) {
                    persist(_allShifts.value + removed)
                }
            }
        )
    }

    fun startEditing(shift: Shift) {
        editingShiftId = shift.id
        selectedDate = shift.localDate
        deliveryCountInput = if (shift.deliveryCount > 0) shift.deliveryCount.toString() else ""
        plusIncomeInput = if (shift.plusGrossIncome > 0) shift.plusGrossIncome.toPlainInput() else ""
        normalIncomeInput = if (shift.normalGrossIncome > 0) shift.normalGrossIncome.toPlainInput() else ""
        plusMissionIncomeInput = if (shift.plusMissionIncome > 0) shift.plusMissionIncome.toPlainInput() else ""
        normalMissionIncomeInput = if (shift.normalMissionIncome > 0) shift.normalMissionIncome.toPlainInput() else ""

        val minutes = shift.durationMinutes
        if (minutes != null) {
            totalHoursMode = true
            totalHoursInput = minutesToHoursInput(minutes)
            startTimeInput = ""
            endTimeInput = ""
        } else {
            totalHoursMode = false
            totalHoursInput = ""
            try {
                startTimeInput = LocalTime.parse(shift.startTime).format(TimeHm)
                endTimeInput = LocalTime.parse(shift.endTime).format(TimeHm)
            } catch (e: DateTimeParseException) {
                startTimeInput = shift.startTime
                endTimeInput = shift.endTime
            }
        }

        inputError = null
    }

    fun updateTotalHoursMode(enabled: Boolean) {
        totalHoursMode = enabled
        settingsRepository.setTotalHoursMode(enabled)
        inputError = null
    }

    fun clearInputs() {
        totalHoursInput = ""
        startTimeInput = ""
        endTimeInput = ""
        deliveryCountInput = ""
        plusIncomeInput = ""
        normalIncomeInput = ""
        plusMissionIncomeInput = ""
        normalMissionIncomeInput = ""
        editingShiftId = null
        inputError = null
    }

    fun changeSelectedDate(newDate: LocalDate) {
        if (newDate == selectedDate) return
        selectedDate = newDate
        clearInputs()
    }

    fun updateStatsPeriod(period: StatsPeriod, customStart: LocalDate? = null, customEnd: LocalDate? = null) {
        statsPeriodType = period
        statsCustomStart = customStart
        statsCustomEnd = customEnd
    }

    /** JSON toan bo ca lam de sao luu ra file. */
    suspend fun exportShiftsJson(): String = withContext(Dispatchers.Default) {
        repository.encode(_allShifts.value)
    }

    /** Gop du lieu sao luu: ca trung id duoc thay the, ca moi duoc them vao. */
    fun importShiftsJson(raw: String) {
        viewModelScope.launch {
            val imported = try {
                withContext(Dispatchers.Default) { repository.decode(raw) }
            } catch (e: Exception) {
                _messages.send(UiMessage("File sao lưu không hợp lệ."))
                return@launch
            }
            val byId = _allShifts.value.associateBy { it.id }.toMutableMap()
            var added = 0
            imported.forEach { shift ->
                if (byId.put(shift.id, shift) == null) added++
            }
            persist(byId.values.toList())
            _messages.send(UiMessage("Đã khôi phục ${imported.size} ca ($added ca mới)."))
        }
    }

    companion object {
        private const val MAX_MINUTES = 24 * 60

        /** Loi gan voi tab Plus: man hinh dua vao day de tu chuyen sang tab Plus. */
        const val ERROR_PLUS_NEEDS_DELIVERIES = "Đã nhập thu nhập Plus thì phải nhập số đơn hàng"
    }

    private fun parseTime(timeString: String): LocalTime? {
        val trimmed = timeString.trim()
        if (trimmed.isEmpty()) return null
        return try {
            LocalTime.parse(trimmed, TimeHm)
        } catch (e: DateTimeParseException) {
            try {
                LocalTime.parse(trimmed)
            } catch (e2: DateTimeParseException) {
                null
            }
        }
    }
}
