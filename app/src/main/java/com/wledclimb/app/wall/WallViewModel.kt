package com.wledclimb.app.wall

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wledclimb.app.grid.buildWall
import com.wledclimb.app.grid.parsePanels
import com.wledclimb.app.network.WledClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "WallViewModel"

/**
 * Connects to the WLED controller saved during setup: turns the whole wall
 * on/off, and loads its grid layout (from `/json/cfg`) for display. Later
 * phases add per-hold route control.
 */
class WallViewModel(private val client: WledClient) : ViewModel() {

    private val _uiState = MutableStateFlow<WallUiState>(WallUiState.Connecting)
    val uiState: StateFlow<WallUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = WallUiState.Connecting
            _uiState.value = try {
                val on = client.getOn()
                val wall = buildWall(parsePanels(client.getConfig()))
                WallUiState.Connected(on = on, wall = wall)
            } catch (e: Exception) {
                Log.e(TAG, "refresh() failed to reach WLED", e)
                WallUiState.Error("Couldn't reach the WLED controller. Check that it's on and on the same Wi-Fi.")
            }
        }
    }

    fun toggleWall() {
        val current = _uiState.value as? WallUiState.Connected ?: return
        _uiState.update { current.copy(busy = true) }

        viewModelScope.launch {
            _uiState.value = try {
                WallUiState.Connected(on = client.setOn(on = !current.on), wall = current.wall)
            } catch (e: Exception) {
                Log.e(TAG, "toggleWall() failed to reach WLED", e)
                WallUiState.Error("Couldn't reach the WLED controller. Check that it's on and on the same Wi-Fi.")
            }
        }
    }
}
