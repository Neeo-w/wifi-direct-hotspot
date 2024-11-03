// HotspotViewModel.kt
package com.example.wifip2photspot

import android.app.Application
import android.content.Context
import android.net.TrafficStats
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.Q)
class HotspotViewModel(
    application: Application,
    private val dataStore: DataStore<Preferences>
) : AndroidViewModel(application) {

    // ----- DataStore Keys -----
    companion object {
        val SSID_KEY = stringPreferencesKey("ssid")
        val PASSWORD_KEY = stringPreferencesKey("password")
    }

    // ----- Wi-Fi P2P Manager and Channel -----
    val wifiManager: WifiP2pManager =
        application.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    val channel: WifiP2pManager.Channel =
        wifiManager.initialize(application, Looper.getMainLooper(), null)

    // ----- StateFlows for UI State -----
    private val _ssid = MutableStateFlow("TetherGuard")
    val ssid: StateFlow<String> = _ssid.asStateFlow()

    private val _password = MutableStateFlow("00000000")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _selectedBand = MutableStateFlow("Auto")
    val selectedBand: StateFlow<String> = _selectedBand.asStateFlow()

    private val _isWifiP2pEnabled = MutableStateFlow(false)
    val isWifiP2pEnabled: StateFlow<Boolean> = _isWifiP2pEnabled.asStateFlow()

    private val _isHotspotEnabled = MutableStateFlow(false)
    val isHotspotEnabled: StateFlow<Boolean> = _isHotspotEnabled.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _uploadSpeed = MutableStateFlow(0) // in kbps
    val uploadSpeed: StateFlow<Int> = _uploadSpeed.asStateFlow()

    private val _downloadSpeed = MutableStateFlow(0) // in kbps
    val downloadSpeed: StateFlow<Int> = _downloadSpeed.asStateFlow()

    private var previousTxBytes = TrafficStats.getTotalTxBytes()
    private var previousRxBytes = TrafficStats.getTotalRxBytes()

    // ----- Log Entries -----
    private val _logEntries = MutableStateFlow<List<String>>(emptyList())
    val logEntries: StateFlow<List<String>> = _logEntries.asStateFlow()

    // ----- UI Events -----
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // ----- Connected Devices -----
    private val _connectedDevices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val connectedDevices: StateFlow<List<WifiP2pDevice>> = _connectedDevices.asStateFlow()

    // ----- Sealed Class for UI Events -----
    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    init {
        // ----- Load SSID and Password from DataStore -----
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

        // ----- Start Monitoring Network Speeds -----
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000) // Update every second

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

    override fun onCleared() {
        super.onCleared()
        // Unregistering is handled in MainActivity
    }

    // ----- Function to Update Log Entries -----
    fun updateLog(message: String) {
        _logEntries.value = _logEntries.value + message
    }

    // ----- Function to Handle Device List Changes -----
    fun onDevicesChanged(deviceList: Collection<WifiP2pDevice>) {
        _connectedDevices.value = deviceList.toList()

        // Log for debugging
        val deviceInfo = deviceList.joinToString(separator = "\n") { device ->
            "Device Name: ${device.deviceName}, Address: ${device.deviceAddress}"
        }
        Log.d("ConnectedDevices", "Connected Devices:\n$deviceInfo")
        updateLog("Connected Devices Updated:\n$deviceInfo")
    }

    // ----- Function to Set Wi-Fi P2P Enabled State -----
    fun setWifiP2pEnabled(enabled: Boolean) {
        _isWifiP2pEnabled.value = enabled
        updateLog("Wi-Fi P2P Enabled: $enabled")
    }

    // ----- Function to Update SSID -----
    fun updateSSID(newSSID: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SSID_KEY] = newSSID
            }
            _ssid.value = newSSID
            updateLog("SSID updated to: $newSSID")
        }
    }

    // ----- Function to Update Password -----
    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PASSWORD_KEY] = newPassword
            }
            _password.value = newPassword
            updateLog("Password updated.")
        }
    }

    // ----- Function to Start the Hotspot -----
    fun onButtonStartTapped(
        ssidInput: String,
        passwordInput: String,
        selectedBand: String
    ) {
        viewModelScope.launch {
            if (!_isWifiP2pEnabled.value) {
                updateLog("Error: Cannot start hotspot. Wi-Fi P2P is not enabled.")
                _eventFlow.emit(UiEvent.ShowToast("Wi-Fi P2P is not enabled."))
                _isHotspotEnabled.value = false
                return@launch
            }

            _isProcessing.value = true // Start processing

            val ssidTrimmed = ssidInput.trim()
            val passwordTrimmed = passwordInput.trim()

            // ----- Input Validation -----
            if (ssidTrimmed.isEmpty()) {
                updateLog("Error: SSID cannot be empty.")
                _eventFlow.emit(UiEvent.ShowToast("SSID cannot be empty."))
                _isHotspotEnabled.value = false
                _isProcessing.value = false
                return@launch
            }

            if (passwordTrimmed.length !in 8..63) {
                updateLog("Error: The length of a passphrase must be between 8 and 63.")
                _eventFlow.emit(UiEvent.ShowToast("Password must be between 8 and 63 characters."))
                _isHotspotEnabled.value = false
                _isProcessing.value = false
                return@launch
            }

            val ssid = "DIRECT-hs-$ssidTrimmed"

            val band = when (selectedBand) {
                "2.4GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
                "5GHz" -> WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
                else -> WifiP2pConfig.GROUP_OWNER_BAND_AUTO
            }

            val config = WifiP2pConfig.Builder()
                .setNetworkName(ssid)
                .setPassphrase(passwordTrimmed)
                .enablePersistentMode(false)
                .setGroupOperatingBand(band)
                .build()

            try {
                wifiManager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        viewModelScope.launch {
                            updateLog("Hotspot started successfully.")
                            _isHotspotEnabled.value = true
                            updateLog("------------------- Hotspot Info -------------------")
                            updateLog("SSID: $ssid")
                            updateLog("Password: $passwordTrimmed")
                            val bandStr = when (band) {
                                WifiP2pConfig.GROUP_OWNER_BAND_2GHZ -> "2.4GHz"
                                WifiP2pConfig.GROUP_OWNER_BAND_5GHZ -> "5GHz"
                                else -> "Auto"
                            }
                            updateLog("Band: $bandStr")
                            updateLog("---------------------------------------------------")
                            _isProcessing.value = false
                            _eventFlow.emit(UiEvent.ShowToast("Hotspot started successfully."))
                        }
                    }

                    override fun onFailure(reason: Int) {
                        val reasonStr = when (reason) {
                            WifiP2pManager.ERROR -> "General error"
                            WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
                            WifiP2pManager.BUSY -> "System is busy"
                            else -> "Unknown error"
                        }
                        viewModelScope.launch {
                            updateLog("Failed to start hotspot. Reason: $reasonStr")
                            _isHotspotEnabled.value = false
                            _isProcessing.value = false
                            _eventFlow.emit(UiEvent.ShowToast("Failed to start hotspot: $reasonStr"))
                        }
                    }
                })
            } catch (e: Exception) {
                viewModelScope.launch {
                    updateLog("Exception: ${e.message}")
                    _isHotspotEnabled.value = false
                    _isProcessing.value = false
                    _eventFlow.emit(UiEvent.ShowToast("Exception occurred: ${e.message}"))
                }
            }
        }
    }

    // ----- Function to Stop the Hotspot -----
    fun onButtonStopTapped() {
        viewModelScope.launch {
            if (!_isHotspotEnabled.value) {
                updateLog("Error: Hotspot is not enabled.")
                _eventFlow.emit(UiEvent.ShowToast("Hotspot is not enabled."))
                return@launch
            }

            _isProcessing.value = true // Start processing

            try {
                wifiManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        viewModelScope.launch {
                            updateLog("Hotspot stopped successfully.")
                            _isHotspotEnabled.value = false
                            _connectedDevices.value = emptyList()
                            _isProcessing.value = false
                            _eventFlow.emit(UiEvent.ShowToast("Hotspot stopped successfully."))
                        }
                    }

                    override fun onFailure(reason: Int) {
                        val reasonStr = when (reason) {
                            WifiP2pManager.ERROR -> "General error"
                            WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
                            WifiP2pManager.BUSY -> "System is busy"
                            else -> "Unknown error"
                        }
                        viewModelScope.launch {
                            updateLog("Failed to stop hotspot. Reason: $reasonStr")
                            _isHotspotEnabled.value = true // Assuming it was enabled
                            _isProcessing.value = false
                            _eventFlow.emit(UiEvent.ShowToast("Failed to stop hotspot: $reasonStr"))
                        }
                    }
                })
            } catch (e: Exception) {
                viewModelScope.launch {
                    updateLog("Exception: ${e.message}")
                    _isHotspotEnabled.value = true // Assuming it was enabled
                    _isProcessing.value = false
                    _eventFlow.emit(UiEvent.ShowToast("Exception occurred: ${e.message}"))
                }
            }
        }
    }

    // ----- Function to Update Selected Band -----
    fun updateSelectedBand(newBand: String) {
        viewModelScope.launch {
            _selectedBand.value = newBand
        }
    }
}
