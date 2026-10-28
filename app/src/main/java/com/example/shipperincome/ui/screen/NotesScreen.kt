package com.example.shipperincome.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.provider.Settings
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WrongLocation
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shipperincome.deliverynotes.LocationNote
import com.example.shipperincome.ui.components.EmptyState
import com.example.shipperincome.ui.components.ErrorBanner
import com.example.shipperincome.ui.components.HeroHeader
import com.example.shipperincome.ui.components.LocalSnackbarHostState
import com.example.shipperincome.ui.components.SectionCard
import com.example.shipperincome.ui.components.StatusBarScrim
import com.example.shipperincome.ui.theme.AppTheme
import com.example.shipperincome.ui.viewmodel.GpsState
import com.example.shipperincome.ui.viewmodel.NoteWithDistance
import com.example.shipperincome.ui.viewmodel.NotesViewModel
import com.example.shipperincome.ui.viewmodel.toDistanceLabel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private val QUICK_TAGS = listOf("Ngõ sau", "Kho đỗ xe", "Gọi trước", "Thang máy lâu", "Khó tìm", "Có bảo vệ")

// minSdk 31 < 33: POST_NOTIFICATIONS chi xin duoc tu API 33.
private val LOCATION_PERMISSIONS: Array<String>
    get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

@Composable
fun NotesScreen(notesViewModel: NotesViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbar = LocalSnackbarHostState.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val monitorRunning by notesViewModel.monitorRunning.collectAsStateWithLifecycle()

    var noteToDelete by remember { mutableStateOf<LocationNote?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showMap by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { notesViewModel.startLocationUpdates() }

    // Chi lang nghe GPS khi tab dang hien de khong hao pin o cac tab khac.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> notesViewModel.startLocationUpdates()
                Lifecycle.Event.ON_PAUSE -> notesViewModel.stopLocationUpdates()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        notesViewModel.startLocationUpdates()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            notesViewModel.stopLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        if (!notesViewModel.hasLocationPermission) {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    LaunchedEffect(notesViewModel) {
        notesViewModel.messages.collect { snackbar.showSnackbar(it, withDismissAction = true) }
    }

    val listState = rememberLazyListState()
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
            NotesHeader(
                state = notesViewModel.gpsState,
                location = notesViewModel.currentLocation,
                total = notesViewModel.totalNoteCount,
                onRequestPermission = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                onOpenSettings = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            )
        }

        item(key = "map") {
            MapCard(
                notes = notesViewModel.notes,
                currentLocation = notesViewModel.currentLocation,
                expanded = showMap,
                onToggle = { showMap = !showMap },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "editor") {
            NoteEditorCard(
                notesViewModel = notesViewModel,
                onSave = { notesViewModel.saveNote { focusManager.clearFocus() } },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "monitor") {
            MonitorCard(
                notesViewModel = notesViewModel,
                running = monitorRunning,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "data") {
            DataCard(
                onExport = {
                    scope.launch {
                        notesViewModel.exportJson()?.let { json ->
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, json)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Lưu hoặc chia sẻ dữ liệu"))
                        }
                    }
                },
                onImport = { showImportDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "search") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = notesViewModel.searchQuery,
                    onValueChange = notesViewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (notesViewModel.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { notesViewModel.updateSearchQuery("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Xóa tìm kiếm")
                            }
                        }
                    } else null,
                    placeholder = { Text("Tìm theo tên hoặc ghi chú") }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Địa điểm đã lưu (${notesViewModel.notes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (notesViewModel.notes.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = if (notesViewModel.searchQuery.isBlank()) Icons.Outlined.AddLocationAlt else Icons.Outlined.WrongLocation,
                    title = if (notesViewModel.searchQuery.isBlank()) "Chưa có địa điểm" else "Không tìm thấy",
                    subtitle = if (notesViewModel.searchQuery.isBlank())
                        "Đến điểm giao hàng, nhập ghi chú rồi bấm \"Lưu vị trí hiện tại\"."
                    else "Không có địa điểm nào khớp với từ khóa."
                )
            }
        } else {
            items(notesViewModel.notes, key = { it.note.id }) { item ->
                NoteItem(
                    item = item,
                    onEditClick = { notesViewModel.startEditing(item.note) },
                    onDeleteClick = { noteToDelete = item.note },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem()
                )
            }
        }
    }

    StatusBarScrim(visible = scrolled)
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("Xóa địa điểm?") },
            text = { Text("\"${note.name}\" sẽ bị xóa vĩnh viễn và không nhắc nữa.") },
            confirmButton = {
                Button(
                    onClick = {
                        notesViewModel.deleteNote(note.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Hủy") } }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json ->
                notesViewModel.importJson(json)
                showImportDialog = false
            }
        )
    }
}

@Composable
private fun NotesHeader(
    state: GpsState,
    location: Location?,
    total: Int,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val onHero = AppTheme.extended.onHero
    HeroHeader {
        Text("Ghi chú địa điểm", style = MaterialTheme.typography.titleLarge, color = onHero)
        Text(
            "$total điểm giao đã lưu",
            style = MaterialTheme.typography.bodyMedium,
            color = onHero.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(16.dp))
        Surface(shape = MaterialTheme.shapes.large, color = onHero.copy(alpha = 0.14f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(onHero.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (state) {
                            GpsState.NO_PERMISSION -> Icons.Outlined.LocationOff
                            GpsState.PROVIDER_OFF -> Icons.Outlined.GpsOff
                            GpsState.SEARCHING -> Icons.Outlined.GpsNotFixed
                            GpsState.READY -> Icons.Filled.MyLocation
                        },
                        contentDescription = null,
                        tint = onHero
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (state) {
                            GpsState.NO_PERMISSION -> "Chưa có quyền vị trí"
                            GpsState.PROVIDER_OFF -> "Định vị đang tắt"
                            GpsState.SEARCHING -> "Đang tìm vị trí..."
                            GpsState.READY -> "GPS sẵn sàng"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = onHero
                    )
                    Text(
                        when (state) {
                            GpsState.NO_PERMISSION -> "Cấp quyền để lưu và nhắc địa điểm."
                            GpsState.PROVIDER_OFF -> "Bật Location để lưu chính xác."
                            GpsState.SEARCHING -> "Đứng chỗ thoáng vài giây để GPS ổn định."
                            GpsState.READY -> "Sai số ±${location?.accuracy?.toDistanceLabel() ?: "-"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = onHero.copy(alpha = 0.8f)
                    )
                }
                if (state == GpsState.NO_PERMISSION || state == GpsState.PROVIDER_OFF) {
                    FilledTonalButton(
                        onClick = if (state == GpsState.NO_PERMISSION) onRequestPermission else onOpenSettings
                    ) { Text(if (state == GpsState.NO_PERMISSION) "Cấp quyền" else "Mở cài đặt") }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun MapCard(
    notes: List<NoteWithDistance>,
    currentLocation: Location?,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = AppTheme.extended.isDark
    SectionCard(
        modifier = modifier.animateContentSize(),
        title = "Bản đồ địa điểm",
        icon = Icons.Outlined.Map,
        trailing = {
            TextButton(onClick = onToggle) { Text(if (expanded) "Ẩn" else "Xem") }
        }
    ) {
        if (!expanded) {
            Text(
                "Xem toàn bộ điểm đã lưu và vùng nhắc trên bản đồ.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // GPS bao vi tri moi vai giay; chi ve lai khi du lieu thuc su doi,
            // neu khong WebView bi nap lai lien tuc va ban do khong bao gio hien.
            val mapPayload = remember(
                notes.map { Triple(it.note.id, it.note.radiusMeters, it.note.name) },
                currentLocation?.latitude?.roundTo(4),
                currentLocation?.longitude?.roundTo(4),
                dark
            ) {
                buildMapPayload(notes.map { it.note }, currentLocation, dark)
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // Chi doc file trong assets (file:///android_asset luon duoc phep); tat quyen doc file khac.
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        clipToOutline = true

                        // WebView nam trong LazyColumn: neu khong chan, moi cu vuot deu bi
                        // danh sach ben ngoai cuop mat va khong keo duoc ban do.
                        setOnTouchListener { view, event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN,
                                MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                            false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                view.renderNotes(view.tag as? String)
                            }
                        }
                        loadUrl("file:///android_asset/map.html")
                    }
                },
                update = { webView ->
                    webView.tag = mapPayload
                    webView.renderNotes(mapPayload)
                }
            )
            Text(
                "Chạm vào điểm để xem ghi chú. Bản đồ © OpenStreetMap, CARTO.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** update() co the chay truoc khi trang load xong, luc do renderNotes chua ton tai. */
private fun WebView.renderNotes(payload: String?) {
    if (payload == null) return
    evaluateJavascript("if (typeof renderNotes === 'function') renderNotes($payload);", null)
}

private fun Double.roundTo(decimals: Int): Double {
    val factor = Math.pow(10.0, decimals.toDouble())
    return Math.round(this * factor) / factor
}

/** Dung JSON de truyen du lieu sang map.html — JSONObject tu escape moi ky tu dac biet. */
private fun buildMapPayload(notes: List<LocationNote>, currentLocation: Location?, dark: Boolean): String {
    val root = JSONObject()
    root.put("dark", dark)
    root.put(
        "me",
        currentLocation?.let { JSONObject().put("lat", it.latitude).put("lng", it.longitude) } ?: JSONObject.NULL
    )
    root.put(
        "notes",
        JSONArray().apply {
            notes.forEach { note ->
                put(
                    JSONObject()
                        .put("lat", note.latitude)
                        .put("lng", note.longitude)
                        .put("radius", note.radiusMeters)
                        .put("name", note.name)
                        .put("note", note.note)
                )
            }
        }
    )
    return root.toString()
}

@Composable
private fun NoteEditorCard(notesViewModel: NotesViewModel, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val isEditing = notesViewModel.editingNoteId != null
    val radius = notesViewModel.radiusInput.toIntOrNull() ?: NotesViewModel.DEFAULT_RADIUS

    SectionCard(
        modifier = modifier,
        title = if (isEditing) "Sửa địa điểm" else "Thêm địa điểm mới",
        icon = if (isEditing) Icons.Outlined.EditLocationAlt else Icons.Outlined.AddLocationAlt
    ) {
        Text(
            if (isEditing) "Vị trí GPS đã lưu được giữ nguyên khi sửa."
            else "Ghi chú sẽ được gắn vào vị trí GPS hiện tại của bạn.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ErrorBanner(notesViewModel.inputError)

        OutlinedTextField(
            value = notesViewModel.nameInput,
            onValueChange = { notesViewModel.nameInput = it },
            label = { Text("Tên địa điểm") },
            placeholder = { Text("VD: Tòa A Chung cư 101") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = notesViewModel.noteInput,
            onValueChange = { notesViewModel.noteInput = it },
            label = { Text("Ghi chú chi tiết") },
            placeholder = { Text("VD: Đi ngõ sau, gọi bảo vệ") },
            minLines = 2,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QUICK_TAGS) { tag ->
                SuggestionChip(
                    onClick = { notesViewModel.appendToNote(tag) },
                    label = { Text(tag) },
                    icon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bán kính nhắc", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${radius}m", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = radius.toFloat().coerceIn(NotesViewModel.MIN_RADIUS.toFloat(), NotesViewModel.MAX_RADIUS.toFloat()),
            onValueChange = { notesViewModel.radiusInput = ((it / 10).roundToInt() * 10).toString() },
            valueRange = NotesViewModel.MIN_RADIUS.toFloat()..NotesViewModel.MAX_RADIUS.toFloat()
        )
        Text(
            "Khu chung cư GPS hay lệch, có thể tăng lên 80–100m.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isEditing) {
                OutlinedButton(
                    onClick = { notesViewModel.clearInputs() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Hủy sửa") }
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(if (isEditing) 1.4f else 1f)
                    .height(52.dp)
            ) {
                Icon(if (isEditing) Icons.Filled.Check else Icons.Filled.MyLocation, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isEditing) "Lưu thay đổi" else "Lưu vị trí hiện tại")
            }
        }
    }
}

@Composable
private fun MonitorCard(notesViewModel: NotesViewModel, running: Boolean, modifier: Modifier = Modifier) {
    SectionCard(
        modifier = modifier,
        title = "Nhắc nền",
        icon = Icons.Outlined.NotificationsActive,
        trailing = {
            Switch(checked = running, onCheckedChange = notesViewModel::setMonitorEnabled)
        }
    ) {
        Text(
            if (running) "Đang chạy · ứng dụng sẽ báo khi bạn đến gần địa điểm đã lưu."
            else "Bật để nhận thông báo khi đến gần địa điểm đã lưu, kể cả khi tắt màn hình.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = notesViewModel.timeoutMinutesInput,
                onValueChange = notesViewModel::updateTimeoutInput,
                label = { Text("Chờ giữa 2 lần nhắc") },
                suffix = { Text("phút") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = notesViewModel::saveTimeout) { Text("Lưu") }
        }
    }
}

@Composable
private fun DataCard(onExport: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, title = "Chuyển dữ liệu", icon = Icons.Outlined.SyncAlt) {
        Text(
            "Sao lưu địa điểm sang máy khác bằng đoạn mã JSON.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.CloudUpload, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Xuất")
            }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.CloudDownload, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nhập")
            }
        }
    }
}

@Composable
private fun NoteItem(
    item: NoteWithDistance,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inside = item.isInsideRadius
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (inside) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.note.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                item.distanceMeters?.let { distance ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (inside) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            if (inside) "Bạn đang ở đây" else distance.toDistanceLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (inside) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(item.note.note, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Nhắc trong bán kính ${item.note.radiusMeters}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Sửa")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    val context = LocalContext.current
    var text by remember {
        mutableStateOf(
            runCatching {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)?.text?.toString()
                    ?.takeIf { it.trim().startsWith("[") }
                    .orEmpty()
            }.getOrDefault("")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
        title = { Text("Nhập dữ liệu") },
        text = {
            Column {
                Text("Dán đoạn mã JSON đã xuất từ máy khác vào đây. Điểm trùng sẽ được bỏ qua.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = { onImport(text) }, enabled = text.isNotBlank()) { Text("Nhập") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
