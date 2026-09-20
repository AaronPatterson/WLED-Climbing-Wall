package com.wledclimb.app.wled

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wledclimb.app.settings.WledSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "SetupViewModel"

/**
 * Phase 1 setup screen: enter a WLED controller address, test it by pulling
 * `/json/cfg`, and save it for future launches. Later phases parse the raw
 * config into a real `Wall` model instead of just displaying it.
 */
class SetupViewModel(private val settings: WledSettings) : ViewModel() {

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Editing(ipInput = ""))
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onIpInputChange(ip: String) {
        _uiState.value = SetupUiState.Editing(ipInput = ip)
    }

    fun testAndSave() {
        val current = _uiState.value as? SetupUiState.Editing ?: return
        val ip = current.ipInput.trim()
        if (ip.isEmpty()) {
            _uiState.value = current.copy(error = "Enter the controller's IP address or hostname")
            return
        }

        _uiState.value = current.copy(testing = true, error = null)
        viewModelScope.launch {
            val baseUrl = if (ip.startsWith("http://") || ip.startsWith("https://")) ip else "http://$ip"
            _uiState.value = try {
                val rawConfig = WledClient(baseUrl = baseUrl).getConfig()
                Log.d(TAG, "WLED config for $baseUrl: $rawConfig")
                settings.saveWledIp(baseUrl)
                SetupUiState.Connected(ip = baseUrl)
            } catch (e: Exception) {
                Log.e(TAG, "testAndSave() failed to reach WLED", e)
                SetupUiState.Editing(ipInput = ip, error = e.message ?: "Couldn't reach the WLED controller")
            }
        }
    }
}
