package com.example.wifip2photspot


import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wifip2photspot.ui.SettingsScreen
import com.example.wifip2photspot.ui.screens.MainScreen
import com.example.wifip2photspot.viewModel.HotspotViewModel

@Composable
fun WiFiP2PHotspotApp(
    hotspotViewModel: HotspotViewModel,
) {
    val navController = rememberNavController()
    WiFiP2PHotspotNavHost(
        navController = navController,
        hotspotViewModel = hotspotViewModel
    )

}

@Composable
fun WiFiP2PHotspotNavHost(
    navController: NavHostController,
    hotspotViewModel: HotspotViewModel
) {
    NavHost(navController = navController, startDestination = "main_screen") {
        composable("main_screen") {
            MainScreen(
                navController = navController,
                hotspotViewModel = hotspotViewModel
            )

        }
        composable("settings_screen") {
            SettingsScreen(
                navController = navController,
                hotspotViewModel = hotspotViewModel
            )
        }
    }
}

/**
 * Determines if Wi-Fi is enabled.
 */
fun isWifiEnabled(context: Context): Boolean {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    return wifiManager?.isWifiEnabled ?: false
}

/**
 * Determines if Location services are enabled.
 */
fun isLocationEnabled(context: Context): Boolean {
    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true || locationManager?.isProviderEnabled(
        LocationManager.NETWORK_PROVIDER
    ) == true
}

