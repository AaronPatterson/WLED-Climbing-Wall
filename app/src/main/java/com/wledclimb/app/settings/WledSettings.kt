package com.wledclimb.app.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wled_settings")

/** Persists the WLED controller's address across app restarts. */
class WledSettings(private val context: Context) {

    private val wledIpKey = stringPreferencesKey("wled_ip")

    /** The saved controller address, or null if setup hasn't been completed yet. */
    val wledIp: Flow<String?> = context.dataStore.data.map { it[wledIpKey] }

    suspend fun saveWledIp(ip: String) {
        context.dataStore.edit { it[wledIpKey] = ip }
    }
}
