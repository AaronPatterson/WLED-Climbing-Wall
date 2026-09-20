package com.wledclimb.app.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "wled_settings")
private val wledIpKey = stringPreferencesKey("wled_ip")

/** [WledSettings] backed by Jetpack DataStore. */
class DataStoreWledSettings(private val context: Context) : WledSettings {

    override val wledIp: Flow<String?> = context.dataStore.data.map { it[wledIpKey] }

    override suspend fun saveWledIp(ip: String) {
        context.dataStore.edit { it[wledIpKey] = ip }
    }
}
