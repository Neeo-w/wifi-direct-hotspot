// app/src/main/java/com/example/wifip2photspot/proxy/ServiceStatusRepository.kt

package com.example.wifip2photspot.proxy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ServiceStatusRepository {
    private val _isProxyRunning = MutableStateFlow(false)
    val isProxyRunning: StateFlow<Boolean> = _isProxyRunning

    fun setProxyRunning(isRunning: Boolean) {
        _isProxyRunning.value = isRunning
    }
}
