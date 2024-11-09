// DataStoreManager.kt
package com.example.wifip2photspot

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map

object DataStoreManager {
    private const val DATASTORE_NAME = "hotspot_prefs"

    private val Context.dataStore by preferencesDataStore(
        name = DATASTORE_NAME
    )

    // Define keys
    private val SSID_KEY = stringPreferencesKey("ssid_key")
    private val PASSWORD_KEY = stringPreferencesKey("password_key")

    // Function to get SSID
    fun getSsid(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SSID_KEY] ?: "TetherGuard" // Default SSID
        }
    }

    // Function to get Password
    fun getPassword(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PASSWORD_KEY] ?: "00000000" // Default Password
        }
    }

    // Function to save SSID
    suspend fun saveSsid(context: Context, ssid: String) {
        context.dataStore.edit { preferences ->
            preferences[SSID_KEY] = ssid
        }
    }

    // Function to save Password
    suspend fun savePassword(context: Context, password: String) {
        context.dataStore.edit { preferences ->
            preferences[PASSWORD_KEY] = password
        }
    }
}
