package com.example.shipperincome.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipperincome.data.model.RankEntry
import com.example.shipperincome.data.repository.RankRepository
import com.example.shipperincome.data.repository.SettingsRepository
import com.example.shipperincome.widget.QuickEarnWidget
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class RankViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RankRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    var config by mutableStateOf(settingsRepository.getRankConfig())
        private set
    var entries by mutableStateOf<List<RankEntry>>(emptyList())
        private set
    var isLoaded by mutableStateOf(false)
        private set

    /** 0 = chu ky hien tai, -1 = chu ky truoc... */
    var cycleOffset by mutableStateOf(0)
        private set
    var selectedDate by mutableStateOf(LocalDate.now())
        private set
    var pointsInput by mutableStateOf("")
    var inputError by mutableStateOf<String?>(null)

    val viewedCycle: RankCycle?
        get() {
            val anchor = config.cycleAnchor ?: return null
            val current = RankCalculations.cycleFor(LocalDate.now(), anchor, config.cycleDays)
            return RankCalculations.cycleAt(current.index + cycleOffset, anchor, config.cycleDays)
        }

    val progress: RankProgress?
        get() = viewedCycle?.let { RankCalculations.progress(it, config.targetPoints, entries, LocalDate.now()) }

    init {
        viewModelScope.launch {
            entries = repository.load()
            isLoaded = true
            prefillForSelectedDate()
        }
    }

    /** Doc lai cau hinh moi lan mo tab, vi nguoi dung co the vua sua o Cai dat. */
    fun refreshConfig() {
        val latest = settingsRepository.getRankConfig()
        if (latest != config) {
            config = latest
            cycleOffset = 0
            selectedDate = LocalDate.now()
            prefillForSelectedDate()
        }
    }

    fun showCycle(offset: Int) {
        cycleOffset = offset.coerceAtMost(0)
        val cycle = viewedCycle ?: return
        val today = LocalDate.now()
        selectedDate = if (today in cycle) today else cycle.end
        prefillForSelectedDate()
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
        prefillForSelectedDate()
    }

    private fun prefillForSelectedDate() {
        inputError = null
        pointsInput = entries.firstOrNull { it.date == selectedDate.toString() }?.totalPoints?.toString() ?: ""
    }

    fun saveEntry() {
        inputError = null
        val cycle = viewedCycle ?: return
        val points = pointsInput.toIntOrNull()
        if (points == null) {
            inputError = "Nhập tổng điểm đã chạy được"
            return
        }
        if (selectedDate !in cycle || selectedDate.isAfter(LocalDate.now())) {
            inputError = "Chọn ngày nằm trong chu kỳ và không sau hôm nay"
            return
        }
        RankCalculations.validate(cycle, entries, selectedDate, points)?.let {
            inputError = it
            return
        }
        val key = selectedDate.toString()
        val previous = entries.firstOrNull { it.date == key }
        persist(entries.filterNot { it.date == key } + RankEntry(key, points))
        val remaining = (config.targetPoints - points).coerceAtLeast(0)
        _messages.trySend(
            UiMessage(
                if (remaining == 0) "Đã lưu $points điểm · đủ chỉ tiêu rồi!"
                else "Đã lưu $points điểm · còn thiếu $remaining điểm",
                actionLabel = if (previous != null) "Hoàn tác" else null,
                onAction = previous?.let { old -> { persist(entries.filterNot { it.date == key } + old) } }
            )
        )
    }

    fun deleteEntry(date: LocalDate) {
        val key = date.toString()
        val removed = entries.firstOrNull { it.date == key } ?: return
        persist(entries.filterNot { it.date == key })
        if (date == selectedDate) pointsInput = ""
        _messages.trySend(
            UiMessage("Đã xóa điểm ngày ${date.dayOfMonth}/${date.monthValue}", "Hoàn tác") {
                if (entries.none { it.date == key }) persist(entries + removed)
            }
        )
    }

    private fun persist(newEntries: List<RankEntry>) {
        entries = newEntries.sortedBy { it.date }
        viewModelScope.launch {
            try {
                repository.save(entries)
                QuickEarnWidget.refresh(getApplication())
            } catch (e: Exception) {
                _messages.send(UiMessage("Không lưu được điểm: ${e.message}"))
            }
        }
    }

    fun hasConfig(): Boolean = config.cycleAnchor != null
}
