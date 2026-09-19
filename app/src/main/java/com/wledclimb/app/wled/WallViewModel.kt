package com.wledclimb.app.wled

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "WallViewModel"

/**
 * Phase 0 walking skeleton: connect to a hardcoded WLED controller and
 * turn the whole wall on/off. Later phases replace the hardcoded IP with
 * the saved connection from a setup screen, and add route control.
 */
class WallViewModel(
    // TODO(phase 1): move this into a setup screen + persisted setting.
    private val client: WledClient = WledClient(baseUrl = "http://192.168.30.49")
) : ViewModel() {

    private val _uiState = MutableStateFlow<WallUiState>(WallUiState.Connecting)
    val uiState: StateFlow<WallUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = WallUiState.Connecting
            _uiState.value = try {
                WallUiState.Connected(on = client.getOn())
            } catch (e: Exception) {
                Log.e(TAG, "refresh() failed to reach WLED", e)
                WallUiState.Error(e.message ?: "Couldn't reach the WLED controller")
            }
        }
    }

    fun toggleWall() {
        val current = _uiState.value as? WallUiState.Connected ?: return
        _uiState.update { current.copy(busy = true) }

        viewModelScope.launch {
            _uiState.value = try {
                WallUiState.Connected(on = client.setOn(on = !current.on))
            } catch (e: Exception) {
                Log.e(TAG, "toggleWall() failed to reach WLED", e)
                WallUiState.Error(e.message ?: "Couldn't reach the WLED controller")
            }
        }
    }
}
