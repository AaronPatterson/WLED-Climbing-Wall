package com.wledclimb.app.wall

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wledclimb.app.grid.WledConfigException
import com.wledclimb.app.grid.buildWall
import com.wledclimb.app.grid.parseGaps
import com.wledclimb.app.grid.parsePanels
import com.wledclimb.app.network.WledClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "WallViewModel"

private const val UNREACHABLE_MESSAGE =
    "Couldn't reach the WLED controller. Check that it's on and on the same Wi-Fi."

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
                val panels = parsePanels(client.getConfig())
                val gaps = client.getGaps()?.let { parseGaps(it) }
                WallUiState.Connected(on = on, wall = buildWall(panels, gaps))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "refresh() failed", e)
                WallUiState.Error(messageFor(e))
            }
        }
    }

    fun toggleWall() {
        val current = _uiState.value as? WallUiState.Connected ?: return
        if (current.busy) return
        _uiState.value = current.copy(busy = true)

        viewModelScope.launch {
            _uiState.value = try {
                WallUiState.Connected(on = client.setOn(on = !current.on), wall = current.wall)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleWall() failed", e)
                WallUiState.Error(messageFor(e))
            }
        }
    }
}

/**
 * A controller that answered but isn't set up as a wall needs a different fix
 * from one that couldn't be reached, so the two don't share a message.
 */
private fun messageFor(e: Exception): String = when (e) {
    is WledConfigException -> e.message ?: UNREACHABLE_MESSAGE
    else -> UNREACHABLE_MESSAGE
}
