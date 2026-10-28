package com.example.shipperincome.deliverynotes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Trang thai that cua [LocationMonitorService], do chinh service cap nhat trong onStartCommand/onDestroy.
 * UI quan sat StateFlow nay thay vi tu doan trang thai sau khi bam nut.
 */
object MonitorStatus {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    @JvmStatic
    fun setRunning(value: Boolean) {
        _running.value = value
    }

    @JvmStatic
    fun isRunning(): Boolean = _running.value
}
