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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "WallViewModel"

/**
 * The single colour holds light up in for now. Phase 3 finishes with a small
 * fixed palette of common hold colours (see docs/design.md); one colour first
 * keeps the end-to-end path honest before the picker exists.
 */
private const val HOLD_COLOR = "FF0000"

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
                // The three reads don't depend on each other, and run against a
                // small controller over Wi-Fi - in sequence their connect timeouts
                // stack up, so a dead controller took three timeouts to report.
                coroutineScope {
                    val on = async { client.getOn() }
                    val config = async { client.getConfig() }
                    val gaps = async { client.getGaps() }
                    val wall = buildWall(
                        panels = parsePanels(config.await()),
                        gaps = gaps.await()?.let { parseGaps(it) }
                    )
                    WallUiState.Connected(on = on.await(), wall = wall)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "refresh() failed", e)
                WallUiState.Error(problemFor(e))
            }
        }
    }

    /**
     * Lights or clears one hold and pushes the whole route to the wall.
     *
     * The grid updates before the request completes: on a local network the
     * round trip is short, but waiting for it would make every tap feel
     * sticky. A failed push falls back to the error state, same as a failed
     * on/off toggle, rather than silently leaving the app and the wall
     * showing different things.
     */
    fun toggleHold(ledIndex: Int) {
        val current = _uiState.value as? WallUiState.Connected ?: return

        val updated = current.litHolds.toMutableMap()
        if (updated.remove(ledIndex) == null) {
            updated[ledIndex] = HOLD_COLOR
        }
        _uiState.value = current.copy(litHolds = updated)

        viewModelScope.launch {
            try {
                client.setHoldColors(ledCount = current.wall.ledCount, lit = updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleHold($ledIndex) failed", e)
                _uiState.value = WallUiState.Error(problemFor(e))
            }
        }
    }

    fun toggleWall() {
        val current = _uiState.value as? WallUiState.Connected ?: return
        if (current.busy) return
        _uiState.value = current.copy(busy = true)

        viewModelScope.launch {
            _uiState.value = try {
                val on = client.setOn(on = !current.on)
                // WLED unfreezes every segment when it's switched on (see the
                // "unfreeze all segments when turning on" branch in json.cpp),
                // which drops the per-pixel route from the wall while the app
                // still shows it. Push the route again so the two agree.
                if (on && current.litHolds.isNotEmpty()) {
                    client.setHoldColors(ledCount = current.wall.ledCount, lit = current.litHolds)
                }
                // copy() rather than a fresh Connected, so the route stays put.
                current.copy(on = on, busy = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "toggleWall() failed", e)
                WallUiState.Error(problemFor(e))
            }
        }
    }
}

/**
 * A controller that answered but isn't set up as a wall needs a different fix
 * from one that couldn't be reached at all.
 */
private fun problemFor(e: Exception): WallProblem = when (e) {
    is WledConfigException -> WallProblem.NotAWledMatrix
    else -> WallProblem.Unreachable
}
