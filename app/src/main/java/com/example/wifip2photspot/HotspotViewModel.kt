// HotspotViewModel.kt
package com.example.wifip2photspot

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.net.Uri
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

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

    //     ----- Wi-Fi P2P Manager and Channel -----
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

    private var sessionStartRxBytes = 0L
    private var sessionStartTxBytes = 0L

    private val DATA_USAGE_KEY = stringPreferencesKey("device_usage_key")


    // ----- Log Entries -----
    private val _logEntries = MutableStateFlow<List<String>>(emptyList())
    val logEntries: StateFlow<List<String>> = _logEntries.asStateFlow()

    // ----- UI Events -----
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // ----- Connected Devices -----
    private val _connectedDevices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val connectedDevices: StateFlow<List<WifiP2pDevice>> = _connectedDevices.asStateFlow()

    private val _connectedDeviceInfos = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDeviceInfos: StateFlow<List<DeviceInfo>> = _connectedDeviceInfos.asStateFlow()

    private var passwordVisible by mutableStateOf(false)

    // ----- Sealed Class for UI Events -----
    sealed class UiEvent {
        data class ShowToast(val message: String) : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
        object StartProxyService : UiEvent()
        object StopProxyService : UiEvent()
    }

    // Proxy server properties
    private val _isProxyRunning = MutableStateFlow(false)
    val isProxyRunning: StateFlow<Boolean> = _isProxyRunning.asStateFlow()

    private val _proxyPort = MutableStateFlow(8080)
    val proxyPort: StateFlow<Int> = _proxyPort.asStateFlow()


    private val BLOCKED_MAC_ADDRESSES_KEY = stringSetPreferencesKey("blocked_mac_addresses")

    // StateFlows to hold the lists
    private val _allowedMacAddresses = MutableStateFlow<Set<String>>(emptySet())

    // Add a new property for blocked devices
    private val _blockedMacAddresses = MutableStateFlow<Set<String>>(emptySet())
    val blockedMacAddresses: StateFlow<Set<String>> = _blockedMacAddresses.asStateFlow()

    private val _blockedDeviceInfos = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val blockedDeviceInfos: StateFlow<List<DeviceInfo>> = _blockedDeviceInfos.asStateFlow()

    // Define DataStore key
    val DEVICE_ALIAS_KEY = stringPreferencesKey("device_aliases")

    // Load aliases in init block
    private val _deviceAliases = MutableStateFlow<Map<String, String>>(emptyMap())
//
//    private val _historicalDataUsage = MutableStateFlow<List<DataUsageRecord>>(emptyList())
//    val historicalDataUsage: StateFlow<List<DataUsageRecord>> = _historicalDataUsage.asStateFlow()

    private val _uploadSpeedEntries = MutableStateFlow<List<Entry>>(emptyList())
    val uploadSpeedEntries: StateFlow<List<Entry>> = _uploadSpeedEntries.asStateFlow()

    private val _downloadSpeedEntries = MutableStateFlow<List<Entry>>(emptyList())
    val downloadSpeedEntries: StateFlow<List<Entry>> = _downloadSpeedEntries.asStateFlow()


    // Variables to store current speeds in Kbps
    private val _uploadSpeedKbps = MutableStateFlow(0f)
    val uploadSpeedKbps: StateFlow<Float> = _uploadSpeedKbps.asStateFlow()

    private val _downloadSpeedKbps = MutableStateFlow(0f)
    val downloadSpeedKbps: StateFlow<Float> = _downloadSpeedKbps.asStateFlow()

    private val _dataUsageThreshold = MutableStateFlow<Long>(0L) // in bytes
    val dataUsageThreshold: StateFlow<Long> = _dataUsageThreshold.asStateFlow()

    private val _notificationEnabled = MutableStateFlow(true)
    val notificationEnabled: StateFlow<Boolean> = _notificationEnabled.asStateFlow()

    private val _notificationSoundEnabled = MutableStateFlow(true)
    val notificationSoundEnabled: StateFlow<Boolean> = _notificationSoundEnabled.asStateFlow()

    private val _notificationVibrationEnabled = MutableStateFlow(true)
    val notificationVibrationEnabled: StateFlow<Boolean> =
        _notificationVibrationEnabled.asStateFlow()


//    private var proxyServer: proxyServer? = null
//
//    // Start Proxy Server
//    fun startProxyServer() {
//        if (_isProxyRunning.value) {
//            updateLog("Proxy server is already running.")
//            return
//        }
//
//        proxyServer = proxyServer(_proxyPort.value)
//        try {
//            proxyServer?.start()
//            _isProxyRunning.value = true
//            updateLog("Proxy server started on port ${_proxyPort.value}")
//        } catch (e: IOException) {
//            updateLog("Failed to start proxy server: ${e.message}")
//        }
//    }

    // Stop Proxy Server
//    fun stopProxyServer() {
//        if (!_isProxyRunning.value) {
//            updateLog("Proxy server is not running.")
//            return
//        }
//
//        proxyServer?.stop()
//        proxyServer = null
//        _isProxyRunning.value = false
//        updateLog("Proxy server stopped.")
//    }

    // Update Proxy Port
    fun updateProxyPort(port: Int) {
        _proxyPort.value = port
    }

//    fun startVpn(context: Context) {
//        val intent = Intent(context, MyVpnService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(intent)
//        } else {
//            context.startService(intent)
//        }
//    }


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
// In the coroutine where you update uploadSpeed and downloadSpeed
        viewModelScope.launch {
            var time = 0f
            while (true) {
                delay(1000) // Update every second
//
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
                // Update entries
                _uploadSpeedEntries.value += Entry(time, uploadSpeedKbps.toFloat())
                _downloadSpeedEntries.value += Entry(time, downloadSpeedKbps.toFloat())
                time += 1f

                // Limit the number of entries to, e.g., 60
                if (_uploadSpeedEntries.value.size > 60) {
                    _uploadSpeedEntries.value = _uploadSpeedEntries.value.drop(1)
                    _downloadSpeedEntries.value = _downloadSpeedEntries.value.drop(1)
                }

            }
        }
        // Load blocked devices
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                val blockedAddresses = preferences[BLOCKED_MAC_ADDRESSES_KEY] ?: emptySet()
                _blockedMacAddresses.value = blockedAddresses
                updateBlockedDevices()
            }
        }
        // Load device aliases from DataStore
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                val aliasesJson = preferences[DEVICE_ALIAS_KEY] ?: "{}"
                _deviceAliases.value = Json.decodeFromString(aliasesJson)
            }
        }
//         Load historical data usage from DataStore
//        viewModelScope.launch {
//            dataStore.data.collect { preferences ->
//                val json = preferences[DATA_USAGE_KEY] ?: "[]"
//                _historicalDataUsage.value = Json.decodeFromString(json)
//            }
//        }
        monitorNetworkSpeeds()
//
//        viewModelScope.launch {
//            dataStore.data.collect { preferences ->
//                _dataUsageThreshold.value = preferences[DATA_USAGE_THRESHOLD_KEY] ?: 0L
//            }
//        }

    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun monitorNetworkSpeeds() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)

                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()

                // Calculate the difference in bytes over the last second
                val rxBytes = currentRxBytes - previousRxBytes
                val txBytes = currentTxBytes - previousTxBytes

                // Update previous bytes for next calculation
                previousRxBytes = currentRxBytes
                previousTxBytes = currentTxBytes

                // Convert bytes to kilobits per second (Kbps)
                val currentDownloadSpeedKbps = (rxBytes * 8) / 1000f
                val currentUploadSpeedKbps = (txBytes * 8) / 1000f

                // Update the StateFlows
                _downloadSpeedKbps.value = currentDownloadSpeedKbps
                _uploadSpeedKbps.value = currentUploadSpeedKbps

                // Update entries for the graph
                val time = (System.currentTimeMillis() / 1000f) // Time in seconds

                _downloadSpeedEntries.value += Entry(time, currentDownloadSpeedKbps)
                _uploadSpeedEntries.value += Entry(time, currentUploadSpeedKbps)

                // Limit the number of entries to, e.g., 60
                if (_downloadSpeedEntries.value.size > 60) {
                    _downloadSpeedEntries.value = _downloadSpeedEntries.value.drop(1)
                    _uploadSpeedEntries.value = _uploadSpeedEntries.value.drop(1)
                }
            }
        }
    }

    ////////*(((((((((((***********))))))))))))///////////////////// //////////////////////////////working fine //////////////////////////////////

    override fun onCleared() {
        super.onCleared()
    }

    // ----- Function to Update Log Entries -----
    fun updateLog(message: String) {
        _logEntries.value += message
    }

    // ----- Function to Handle Device List Changes -----
    fun onDevicesChanged(deviceList: Collection<WifiP2pDevice>) {
        val previousDevices = _connectedDevices.value
        _connectedDevices.value = deviceList.toList()
        enforceAccessControl()

        // Check for new connections
        val newDevices = _connectedDevices.value - previousDevices
        val disconnectedDevices = previousDevices - _connectedDevices.value

        newDevices.forEach { device ->
            sendDeviceConnectionNotification(device, connected = true)
        }
        disconnectedDevices.forEach { device ->
            sendDeviceConnectionNotification(device, connected = false)
        }

        Log.d("HotspotViewModel", "Devices changed: ${deviceList.size} devices")
        val blockedAddresses = _blockedMacAddresses.value
        _connectedDeviceInfos.value = deviceList.map { device ->
            val isBlocked = blockedAddresses.contains(device.deviceAddress)
            val existingInfo =
                _connectedDeviceInfos.value.find { it.device.deviceAddress == device.deviceAddress }
            existingInfo?.copy(isBlocked = isBlocked) ?: DeviceInfo(
                device = device,
                isBlocked = isBlocked
            )
        }
    }


    // ----- Function to Set Wi-Fi P2P Enabled State -----
    fun setWifiP2pEnabled(enabled: Boolean) {
        _isWifiP2pEnabled.value = enabled
        updateLog("Wi-Fi P2P Enabled: $enabled")
    }

    // ----- Function to Update Selected Band -----
    fun updateSelectedBand(newBand: String) {
        viewModelScope.launch {
            _selectedBand.value = newBand
        }
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
        onButtonStopTapped()
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
                            _eventFlow.emit(UiEvent.StartProxyService)
                            showHotspotStatusNotification()

                        }

                        startDataUsageTracking()


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
                            _eventFlow.emit(UiEvent.StopProxyService)
                            removeHotspotStatusNotification()

                        }
//                        saveSessionDataUsage()
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


    fun updatePasswordVisibility(isVisible: Boolean) {
        passwordVisible = isVisible
    }
    //*****************************************************Fine****************************************

//    fun updateDataUsageThreshold(threshold: Long) {
//        _dataUsageThreshold.value = threshold
//        // Save to DataStore
//        viewModelScope.launch {
//            dataStore.edit { preferences ->
//                preferences[DATA_USAGE_THRESHOLD_KEY] = threshold
//            }
//        }
//    }


// In the coroutine where you update uploadSpeed and downloadSpeed
//    viewModelScope.launch {
//        var time = 0f
//        while (true) {
//            delay(1000)
//            // Existing code to update speeds
//
//            // Update entries
//            _uploadSpeedEntries.value = _uploadSpeedEntries.value + Entry(time, uploadSpeedKbps.toFloat())
//            _downloadSpeedEntries.value = _downloadSpeedEntries.value + Entry(time, downloadSpeedKbps.toFloat())
//            time += 1f
//
//            // Limit the number of entries to, e.g., 60
//            if (_uploadSpeedEntries.value.size > 60) {
//                _uploadSpeedEntries.value = _uploadSpeedEntries.value.drop(1)
//                _downloadSpeedEntries.value = _downloadSpeedEntries.value.drop(1)
//            }
////            if (sessionRxBytes + sessionTxBytes >= dataUsageThreshold.value && !thresholdReached) {
////                thresholdReached = true
////                sendDataUsageNotification()
////            }
//        }
//    }


//    fun saveSessionDataUsage() {
//        val (rxBytes, txBytes) = getSessionDataUsage()
//        val today = LocalDate.now()
//        val existingRecord = _historicalDataUsage.value.find { it.date == today }
//        val updatedRecord = existingRecord?.copy(
//            rxBytes = existingRecord.rxBytes + rxBytes,
//            txBytes = existingRecord.txBytes + txBytes
//        )
//            ?: DataUsageRecord(date = today, rxBytes = rxBytes, txBytes = txBytes)
//        val updatedList = _historicalDataUsage.value.filterNot { it.date == today } + updatedRecord
//        _historicalDataUsage.value = updatedList

    // Save to DataStore
//        val json = Json.encodeToString(updatedList)
//        viewModelScope.launch {
//            dataStore.edit { preferences ->
//                preferences[DATA_USAGE_KEY] = json
//            }
//        }
//    }
//
//    fun sendDataUsageNotification() {
//        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val notificationId = 1
//
//        val notification = NotificationCompat.Builder(getApplication<Application>(), "data_usage_channel")
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle("Data Usage Alert")
//            .setContentText("You have reached your data usage threshold.")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .build()
//
//        notificationManager.notify(notificationId, notification)
//    }
    fun contactSupport() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("2024@example.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
        }
        intent.resolveActivity(getApplication<Application>().packageManager)?.let {
            getApplication<Application>().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } ?: run {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("No email client available"))
            }
        }
    }


    fun submitFeedback(feedback: String) {
        // Send feedback via email or to a server
        // For example, open an email intent
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
            putExtra(Intent.EXTRA_SUBJECT, "App Feedback")
            putExtra(Intent.EXTRA_TEXT, feedback)
        }
        intent.resolveActivity(getApplication<Application>().packageManager)?.let {
            getApplication<Application>().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } ?: run {
            // Handle the case where no email client is available
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("No email client available"))
            }
        }
    }

    fun sendDeviceConnectionNotification(device: WifiP2pDevice, connected: Boolean) {
        val notificationManager =
            getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = device.deviceAddress.hashCode()

        val contentText = if (connected) {
            "Device connected: ${device.deviceName}"
        } else {
            "Device disconnected: ${device.deviceName}"
        }

        val notification =
            NotificationCompat.Builder(getApplication<Application>(), "device_connection_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Device Connection")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showHotspotStatusNotification() {
        val notificationManager =
            getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 1000 // Unique ID for the hotspot status notification

        val notification = NotificationCompat.Builder(getApplication<Application>(), "tether_guard")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Title")
            .setContentText("Content")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .apply {
                if (!notificationSoundEnabled.value) {
                    setSound(null)
                }
                if (!notificationVibrationEnabled.value) {
                    setVibrate(null)
                }
            }
            .build()
    }

    fun removeHotspotStatusNotification() {
        val notificationManager =
            getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 1000
        notificationManager.cancel(notificationId)
    }

    fun scheduleHotspotStart(timeInMillis: Long) {
        val delay = timeInMillis - System.currentTimeMillis()
        val workRequest = OneTimeWorkRequestBuilder<StartHotspotWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(getApplication()).enqueue(workRequest)
    }

    fun scheduleHotspotStop(timeInMillis: Long) {
        val delay = timeInMillis - System.currentTimeMillis()
        val workRequest = OneTimeWorkRequestBuilder<StopHotspotWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(getApplication()).enqueue(workRequest)
    }


    fun startDataUsageTracking() {
        sessionStartRxBytes = TrafficStats.getTotalRxBytes()
        sessionStartTxBytes = TrafficStats.getTotalTxBytes()
    }

    fun getSessionDataUsage(): Pair<Long, Long> {
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val rxBytes = currentRxBytes - sessionStartRxBytes
        val txBytes = currentTxBytes - sessionStartTxBytes
        return Pair(rxBytes, txBytes)
    }


    // Function to block a device
    fun blockDevice(deviceAddress: String) {
        // Add to blocked addresses
        val updatedSet = _blockedMacAddresses.value + deviceAddress
        _blockedMacAddresses.value = updatedSet

        // Save to DataStore
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BLOCKED_MAC_ADDRESSES_KEY] = updatedSet
            }
        }

        // Update blocked devices list
        updateBlockedDevices()

        // Enforce access control
        enforceAccessControl()
    }


    // Function to unblock a device
    fun unblockDevice(deviceAddress: String) {
        // Remove from blocked addresses
        val updatedSet = _blockedMacAddresses.value - deviceAddress
        _blockedMacAddresses.value = updatedSet

        // Save to DataStore
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BLOCKED_MAC_ADDRESSES_KEY] = updatedSet
            }
        }

        // Update blocked devices list
        updateBlockedDevices()
    }


    // Function to save blocked devices to DataStore
    private fun saveBlockedDevices() {
        val blockedAddresses = _connectedDeviceInfos.value
            .filter { it.isBlocked }
            .map { it.device.deviceAddress }
            .toSet()

        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[BLOCKED_MAC_ADDRESSES_KEY] = blockedAddresses
            }
        }
    }

    // Function to load blocked devices
    private fun updateBlockedDevices() {
        _blockedDeviceInfos.value = _blockedMacAddresses.value.map { macAddress ->
            DeviceInfo(
                device = WifiP2pDevice().apply { deviceAddress = macAddress },
                isBlocked = true
            )
        }
    }

    // Enforce access control based on blocked devices
    private fun enforceAccessControl() {
        val devicesToDisconnect = _connectedDeviceInfos.value.filter { deviceInfo ->
            deviceInfo.isBlocked
        }

        devicesToDisconnect.forEach { deviceInfo ->
            disconnectDevice(deviceInfo)
        }
    }

    fun disconnectDevice(deviceInfo: DeviceInfo) {
        // Wi-Fi P2P does not provide a direct way to disconnect a single device.
        // However, if you are the group owner, you can remove the group to disconnect all devices
        // and then reform the group. Alternatively, you can attempt to cancel the connection.

        // For illustration purposes, we'll attempt to remove the group.
        wifiManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                updateLog("Disconnected device: ${deviceInfo.device.deviceName}")
                // Remove the device from the connected devices list
                _connectedDeviceInfos.value = _connectedDeviceInfos.value.filterNot {
                    it.device.deviceAddress == deviceInfo.device.deviceAddress
                }
                // Optionally, reinitialize the group to allow other devices to reconnect
                // This is a workaround due to API limitations
//                initializeGroup()
            }

            override fun onFailure(reason: Int) {
                updateLog("Failed to disconnect device: ${deviceInfo.device.deviceName}")
            }
        })
    }

//    // Function to initialize the group again after removing it
//    private fun initializeGroup() {
//        wifiManager.createGroup(channel, object : WifiP2pManager.ActionListener {
//            override fun onSuccess() {
//                updateLog("Group reinitialized after device disconnection.")
//            }
//
//            override fun onFailure(reason: Int) {
//                updateLog("Failed to reinitialize group after device disconnection.")
//            }
//        })
//    }


    //    // Update alias function
    fun updateDeviceAlias(deviceAddress: String, alias: String) {
        // Update aliases map
        val updatedAliases = _deviceAliases.value.toMutableMap()
        updatedAliases[deviceAddress] = alias
        _deviceAliases.value = updatedAliases

        // Save to DataStore
        val aliasesJson = Json.encodeToString(updatedAliases)
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DEVICE_ALIAS_KEY] = aliasesJson
            }
        }

        // Update connected devices
        _connectedDeviceInfos.value = _connectedDeviceInfos.value.map { deviceInfo ->
            if (deviceInfo.device.deviceAddress == deviceAddress) {
                deviceInfo.copy(alias = alias)
            } else {
                deviceInfo
            }
        }
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        _notificationEnabled.value = enabled
        // Save to DataStore
    }

    fun updateNotificationSoundEnabled(enabled: Boolean) {
        _notificationSoundEnabled.value = enabled
        // Save to DataStore
    }

    fun updateNotificationVibrationEnabled(enabled: Boolean) {
        _notificationVibrationEnabled.value = enabled
        // Save to DataStore
    }
}
