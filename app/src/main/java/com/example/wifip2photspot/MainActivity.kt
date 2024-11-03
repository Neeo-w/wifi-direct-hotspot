// MainActivity.kt
package com.example.wifip2photspot

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme

private val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HotspotViewModel
    private lateinit var receiver: WifiDirectBroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize DataStore
        val dataStore = applicationContext.dataStore

        // Initialize ViewModel with Factory
        viewModel = ViewModelProvider(
            this,
            HotspotViewModelFactory(application, dataStore)
        ).get(HotspotViewModel::class.java)

        // Initialize and Register BroadcastReceiver
        receiver = WifiDirectBroadcastReceiver(
            manager = viewModel.wifiManager,
            channel = viewModel.channel,
            viewModel = viewModel
        )
        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            // Add other actions if necessary
        }

        // Log current permission statuses
        logPermissionsStatus()

        // Request necessary permissions
        if (!allPermissionsGranted()) {
            requestPermissions()
        }

        setContent {
            MaterialTheme {
                WiFiP2PHotspotApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        // Check if we should show a rationale
        val shouldShowRationale = REQUIRED_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }

        if (shouldShowRationale) {
            // Show a dialog explaining why the permissions are needed
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires location and Wi-Fi permissions to function correctly.")
                .setPositiveButton("OK") { dialog, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE
                    )
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    Toast.makeText(
                        this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    finish()
                }
                .create()
                .show()
        } else {
            // Directly request permissions
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Denied")
            .setMessage("You have denied some permissions. Allow all permissions at [Settings] > [Permissions]")
            .setPositiveButton("Open Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Exit") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .create()
            .show()
    }

    private fun logPermissionsStatus() {
        REQUIRED_PERMISSIONS.forEach { permission ->
            val status = ContextCompat.checkSelfPermission(this, permission)
            Toast.makeText(
                this,
                "$permission: ${if (status == PackageManager.PERMISSION_GRANTED) "Granted" else "Denied"}",
                Toast.LENGTH_SHORT
            ).show()
            // Alternatively, use Logcat
            // Log.d("PermissionsStatus", "$permission: $status")
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            // Add NEARBY_WIFI_DEVICES if targeting Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.NEARBY_WIFI_DEVICES
            } else {
                null
            }
        ).filterNotNull().toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                // Permissions granted, proceed as normal
                Toast.makeText(
                    this,
                    "Permissions granted.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Permissions denied
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                // Optionally, direct the user to app settings
                showPermissionDeniedDialog()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

