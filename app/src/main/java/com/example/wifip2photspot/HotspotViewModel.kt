// HotspotViewModel.kt
package com.example.wifip2photspot

import android.net.TrafficStats
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.IOException

class HotspotViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    companion object {
        val SSID_KEY = stringPreferencesKey("ssid")
        val PASSWORD_KEY = stringPreferencesKey("password")
    }

    private val _ssid = MutableStateFlow("TetherGuard")
    val ssid: StateFlow<String> = _ssid

    private val _password = MutableStateFlow("00000000")
    val password: StateFlow<String> = _password

    private val _uploadSpeed = MutableStateFlow(0) // in kbps
    val uploadSpeed: StateFlow<Int> = _uploadSpeed

    private val _downloadSpeed = MutableStateFlow(0) // in kbps
    val downloadSpeed: StateFlow<Int> = _downloadSpeed

    private var previousTxBytes = TrafficStats.getTotalTxBytes()
    private var previousRxBytes = TrafficStats.getTotalRxBytes()
    // StateFlow for Total Download
    private val _totalDownload = MutableStateFlow(0) // in KB
    val totalDownload: StateFlow<Int> = _totalDownload

    private val _isHotspotEnabled = MutableStateFlow(false)
    val isHotspotEnabled: StateFlow<Boolean> = _isHotspotEnabled.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        // Load SSID and Password from DataStore
        viewModelScope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .collect { preferences ->
                    _ssid.value = preferences[SSID_KEY] ?: "TetherGuard"
                    _password.value = preferences[PASSWORD_KEY] ?: "00000000"
                }
        }

        // Start monitoring network speeds
        viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second

                val currentTxBytes = TrafficStats.getTotalTxBytes()
                val currentRxBytes = TrafficStats.getTotalRxBytes()

                val txBytesDiff = currentTxBytes - previousTxBytes
                val rxBytesDiff = currentRxBytes - previousRxBytes

                previousTxBytes = currentTxBytes
                previousRxBytes = currentRxBytes

                // Convert bytes to kilobits per second (kbps)
                val uploadSpeedKbps = (txBytesDiff * 8) / 1000
                val downloadSpeedKbps = (rxBytesDiff * 8) / 1000

                _uploadSpeed.value = uploadSpeedKbps.toInt()
                _downloadSpeed.value = downloadSpeedKbps.toInt()
            }
        }
    }



    fun updateSSID(newSSID: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SSID_KEY] = newSSID
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PASSWORD_KEY] = newPassword
            }
        }
    }
}
