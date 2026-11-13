package com.example.shipperincome.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shipperincome.deliverynotes.LocationMonitorService
import com.example.shipperincome.deliverynotes.LocationNote
import com.example.shipperincome.deliverynotes.MonitorStatus
import com.example.shipperincome.deliverynotes.NotesDb
import com.example.shipperincome.deliverynotes.NotesPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

/** Ca ghi chu kem khoang cach toi vi tri hien tai (null neu chua co GPS). */
data class NoteWithDistance(
    val note: LocationNote,
    val distanceMeters: Float?
) {
    val isInsideRadius: Boolean
        get() = distanceMeters != null && distanceMeters <= note.radiusMeters
}

enum class GpsState { NO_PERMISSION, PROVIDER_OFF, SEARCHING, READY }

class NotesViewModel(application: Application) : AndroidViewModel(application), LocationListener {

    private val db = NotesDb.get(application)
    private val locationManager =
        application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val prefs =
        application.getSharedPreferences(NotesPrefs.NAME, Context.MODE_PRIVATE)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Ban sao trong bo nho cua bang ghi chu: GPS cap nhat lien tuc chi tinh lai khoang cach, khong doc DB. */
    private var allNotes: List<LocationNote> = emptyList()
    private var listening = false

    val monitorRunning: StateFlow<Boolean> = MonitorStatus.running

    var currentLocation by mutableStateOf<Location?>(null)
        private set
    var notes by mutableStateOf<List<NoteWithDistance>>(emptyList())
        private set
    var totalNoteCount by mutableStateOf(0)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var gpsState by mutableStateOf(GpsState.SEARCHING)
        private set
    var timeoutMinutesInput by mutableStateOf(
        prefs.getInt(NotesPrefs.KEY_TIMEOUT_MINS, NotesPrefs.DEFAULT_TIMEOUT_MINS).toString()
    )
        private set

    var nameInput by mutableStateOf("")
    var noteInput by mutableStateOf("")
    var radiusInput by mutableStateOf(DEFAULT_RADIUS.toString())
    var editingNoteId by mutableStateOf<Long?>(null)
        private set
    var inputError by mutableStateOf<String?>(null)

    val hasLocationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    init {
        reloadNotes()
    }

    fun startLocationUpdates() {
        reloadNotes()
        updateGpsState()
        if (!hasLocationPermission || listening) return
        listening = true
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            runCatching {
                locationManager.requestLocationUpdates(provider, 3000L, 2f, this, Looper.getMainLooper())
            }
        }
        bestLastKnownLocation()?.let { onLocationChanged(it) }
    }

    fun stopLocationUpdates() {
        listening = false
        runCatching { locationManager.removeUpdates(this) }
    }

    override fun onLocationChanged(location: Location) {
        val previous = currentLocation
        // Bo qua diem kem chinh xac hon han diem dang co (thuong la vi tri mang cu).
        if (previous != null && location.accuracy > previous.accuracy * 3 &&
            location.time - previous.time < 30_000
        ) return
        currentLocation = location
        updateGpsState()
        rebuildList()
    }

    override fun onProviderEnabled(provider: String) = updateGpsState()
    override fun onProviderDisabled(provider: String) = updateGpsState()

    private fun updateGpsState() {
        gpsState = when {
            !hasLocationPermission -> GpsState.NO_PERMISSION
            !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> GpsState.PROVIDER_OFF
            currentLocation == null -> GpsState.SEARCHING
            else -> GpsState.READY
        }
    }

    private fun bestLastKnownLocation(): Location? {
        if (!hasLocationPermission) return null
        return runCatching {
            locationManager.getProviders(true)
                .mapNotNull { locationManager.getLastKnownLocation(it) }
                .minByOrNull { it.accuracy }
        }.getOrNull()
    }

    fun updateSearchQuery(value: String) {
        searchQuery = value
        rebuildList()
    }

    private fun reloadNotes() {
        viewModelScope.launch {
            allNotes = withContext(Dispatchers.IO) { db.getAllNotes() }
            totalNoteCount = allNotes.size
            rebuildList()
        }
    }

    /** Sap xep theo khoang cach khi co GPS de diem gan nhat luon o dau danh sach. */
    private fun rebuildList() {
        val here = currentLocation
        val query = searchQuery.trim().lowercase()
        notes = allNotes
            .filter {
                query.isEmpty() ||
                    it.name.lowercase().contains(query) ||
                    it.note.lowercase().contains(query)
            }
            .map { note ->
                val distance = here?.let {
                    val result = FloatArray(1)
                    Location.distanceBetween(
                        it.latitude, it.longitude, note.latitude, note.longitude, result
                    )
                    result[0]
                }
                NoteWithDistance(note, distance)
            }
            .let { list ->
                if (here != null) list.sortedBy { it.distanceMeters } else list
            }
    }

    fun saveNote(onSaved: () -> Unit = {}) {
        inputError = null
        val name = nameInput.trim()
        val note = noteInput.trim()

        if (note.isEmpty()) {
            inputError = "Hãy nhập ghi chú cho địa điểm này."
            return
        }

        val radius = radiusInput.toIntOrNull()?.coerceIn(MIN_RADIUS, MAX_RADIUS) ?: DEFAULT_RADIUS
        val editingId = editingNoteId

        if (editingId != null) {
            if (name.isEmpty()) {
                inputError = "Tên địa điểm không được để trống."
                return
            }
            viewModelScope.launch {
                withContext(Dispatchers.IO) { db.updateNote(editingId, name, note, radius) }
                _messages.send("Đã cập nhật địa điểm.")
                reloadNotes()
            }
        } else {
            val location = currentLocation ?: bestLastKnownLocation()
            if (location == null) {
                inputError = "Chưa có GPS. Hãy ra chỗ thoáng vài giây rồi thử lại."
                return
            }
            val finalName = name.ifEmpty { "Điểm lưu lúc ${nowLabel()}" }
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    db.addNote(finalName, note, location.latitude, location.longitude, radius)
                }
                _messages.send("Đã lưu địa điểm tại vị trí hiện tại.")
                reloadNotes()
            }
        }

        clearInputs()
        onSaved()
    }

    fun startEditing(note: LocationNote) {
        editingNoteId = note.id
        nameInput = note.name
        noteInput = note.note
        radiusInput = note.radiusMeters.toString()
        inputError = null
    }

    fun clearInputs() {
        editingNoteId = null
        nameInput = ""
        noteInput = ""
        radiusInput = DEFAULT_RADIUS.toString()
        inputError = null
    }

    fun deleteNote(id: Long) {
        if (editingNoteId == id) clearInputs()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.deleteNote(id) }
            _messages.send("Đã xóa địa điểm.")
            reloadNotes()
        }
    }

    fun appendToNote(text: String) {
        noteInput = if (noteInput.isBlank()) text else "${noteInput.trimEnd().trimEnd(',')}, $text"
    }

    fun updateTimeoutInput(value: String) {
        timeoutMinutesInput = value.filter { it.isDigit() }.take(4)
    }

    fun saveTimeout() {
        val minutes = timeoutMinutesInput.toIntOrNull()
        if (minutes == null || minutes < 1) {
            _messages.trySend("Thời gian chờ phải là số phút lớn hơn 0.")
            return
        }
        prefs.edit { putInt(NotesPrefs.KEY_TIMEOUT_MINS, minutes) }
        _messages.trySend("Đã lưu thời gian chờ: $minutes phút.")
    }

    fun setMonitorEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        if (!enabled) {
            app.startService(
                Intent(app, LocationMonitorService::class.java)
                    .setAction(LocationMonitorService.ACTION_STOP)
            )
            _messages.trySend("Đã tắt nhắc nền.")
            return
        }
        if (!hasLocationPermission) {
            _messages.trySend("Cần cấp quyền vị trí trước khi bật nhắc nền.")
            return
        }
        val intent = Intent(app, LocationMonitorService::class.java)
            .setAction(LocationMonitorService.ACTION_START)
        try {
            app.startForegroundService(intent)
            _messages.trySend("Đã bật nhắc nền.")
        } catch (e: IllegalStateException) {
            _messages.trySend("Không bật được nhắc nền: ${e.message}")
        }
    }

    suspend fun exportJson(): String? {
        val all = withContext(Dispatchers.IO) { db.getAllNotes() }
        if (all.isEmpty()) {
            _messages.send("Không có dữ liệu để xuất.")
            return null
        }
        return JSONArray().apply {
            all.forEach { note ->
                put(
                    JSONObject().apply {
                        put("name", note.name)
                        put("note", note.note)
                        put("latitude", note.latitude)
                        put("longitude", note.longitude)
                        put("radiusMeters", note.radiusMeters)
                    }
                )
            }
        }.toString()
    }

    /**
     * Doc het va kiem tra truoc, chi ghi vao DB khi toan bo hop le (khong nhap do dang).
     * Diem trung ten va toa do (sai lech duoi ~1m) voi diem da co thi bo qua.
     */
    fun importJson(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val parsed = try {
                val array = JSONArray(trimmed)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    LocationNote(
                        0,
                        obj.getString("name"),
                        obj.getString("note"),
                        obj.getDouble("latitude"),
                        obj.getDouble("longitude"),
                        obj.optInt("radiusMeters", DEFAULT_RADIUS).coerceIn(MIN_RADIUS, MAX_RADIUS),
                        0,
                        0
                    )
                }
            } catch (e: JSONException) {
                _messages.send("Dữ liệu không hợp lệ. Vui lòng kiểm tra lại mã.")
                return@launch
            }

            val existingKeys = allNotes.map { it.dedupeKey() }.toMutableSet()
            val fresh = parsed.filter { existingKeys.add(it.dedupeKey()) }
            withContext(Dispatchers.IO) { db.addNotes(fresh) }
            val skipped = parsed.size - fresh.size
            _messages.send(
                if (skipped > 0) "Đã nhập ${fresh.size} địa điểm, bỏ qua $skipped điểm trùng."
                else "Đã nhập ${fresh.size} địa điểm."
            )
            reloadNotes()
        }
    }

    private fun LocationNote.dedupeKey(): String =
        "${name.trim().lowercase()}|${(latitude * 1e5).roundToInt()}|${(longitude * 1e5).roundToInt()}"

    override fun onCleared() {
        stopLocationUpdates()
        super.onCleared()
    }

    private fun nowLabel(): String {
        val formatter = java.text.SimpleDateFormat("HH:mm dd/MM", java.util.Locale.US)
        return formatter.format(java.util.Date())
    }

    companion object {
        const val DEFAULT_RADIUS = 20
        const val MIN_RADIUS = 20
        const val MAX_RADIUS = 500
    }
}

fun Float.toDistanceLabel(): String =
    if (this >= 1000f) String.format(java.util.Locale.US, "%.1fkm", this / 1000f)
    else "${this.roundToInt()}m"
