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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /**
     * Pending brightness, at most one waiting.
     *
     * A Slider reports every pixel of a drag, and firing a request per report
     * put dozens of them at an ESP32 in a second - which answered by dropping
     * connections ("unexpected end of stream"). Conflating means one request is
     * in flight at a time and newer values replace the waiting one rather than
     * queueing behind it, so the wall still tracks the finger but is asked at a
     * rate it can answer.
     */
    private val brightnessRequests = Channel<Int>(Channel.CONFLATED)

    /**
     * The most recent brightness asked for, which is not necessarily the one
     * being confirmed: conflation means a reply can describe a value the user
     * has already moved past.
     */
    private var requestedBrightness: Int? = null

    init {
        refresh()
        viewModelScope.launch {
            for (target in brightnessRequests) {
                applyBrightness(target)
            }
        }
    }

    private suspend fun applyBrightness(brightness: Int) {
        val current = _uiState.value as? WallUiState.Connected ?: return
        try {
            val status = client.setBrightness(brightness = brightness, on = current.on)
            val latest = _uiState.value as? WallUiState.Connected ?: return
            // Power is always worth taking from the reply. Brightness only when
            // nothing newer has been asked for, or a slow reply would drag the
            // value back to where the finger has already left.
            val superseded = requestedBrightness != brightness
            _uiState.value = latest.copy(
                brightness = if (superseded) latest.brightness else status.brightness,
                on = status.on
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Deliberately not an error state. A dropped brightness request is
            // not evidence the wall has gone: it is one request among many
            // during a drag, and tearing down the screen over it loses the
            // grid, the route and the user's place. The displayed value stands
            // until the next successful set or refresh corrects it, and a wall
            // that really is unreachable will say so on the next toggle or tap.
            Log.e(TAG, "setBrightness($brightness) failed", e)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = WallUiState.Connecting
            _uiState.value = try {
                // The three reads don't depend on each other, and run against a
                // small controller over Wi-Fi - in sequence their connect timeouts
                // stack up, so a dead controller took three timeouts to report.
                coroutineScope {
                    val status = async { client.getStatus() }
                    val name = async { client.getName() }
                    val config = async { client.getConfig() }
                    val gaps = async { client.getGaps() }
                    val wall = buildWall(
                        panels = parsePanels(config.await()),
                        gaps = gaps.await()?.let { parseGaps(it) }
                    )
                    WallUiState.Connected(
                        on = status.await().on,
                        brightness = status.await().brightness,
                        name = name.await(),
                        wall = wall
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "refresh() failed", e)
                WallUiState.Error(problemFor(e))
            }
        }
    }

    /** Picks the colour the next tapped hold will be painted in. */
    fun selectColor(color: HoldColor) {
        val current = _uiState.value as? WallUiState.Connected ?: return
        _uiState.value = current.copy(selectedColor = color)
    }

    /**
     * Paints, repaints or clears one hold, then pushes the whole route.
     *
     * Tapping a hold that's already the selected colour turns it off, so the
     * same gesture both paints and erases and there's no separate eraser mode
     * to explain. Tapping one showing a different colour repaints it.
     */
    fun toggleHold(segmentIndex: Int) {
        val current = _uiState.value as? WallUiState.Connected ?: return

        val updated = current.litHolds.toMutableMap()
        if (updated[segmentIndex] == current.selectedColor) {
            updated.remove(segmentIndex)
        } else {
            updated[segmentIndex] = current.selectedColor
        }
        showAndPush(current, updated, "toggleHold($segmentIndex)")
    }

    /**
     * Turns every hold off, to start a fresh route.
     *
     * Without this, clearing a route means tapping each lit hold in turn - one
     * request per hold, and a lot of tapping for anything but a short route.
     */
    fun clearWall() {
        val current = _uiState.value as? WallUiState.Connected ?: return
        if (current.litHolds.isEmpty()) return

        showAndPush(current, emptyMap(), "clearWall()")
    }

    /**
     * Shows [holds] straight away and pushes them to the wall in the background.
     *
     * The grid updates before the request completes: on a local network the
     * round trip is short, but waiting for it would make every tap feel
     * sticky. A failed push falls back to the error state, same as a failed
     * on/off toggle, rather than silently leaving the app and the wall
     * showing different things.
     */
    private fun showAndPush(
        current: WallUiState.Connected,
        holds: Map<Int, HoldColor>,
        description: String
    ) {
        _uiState.value = current.copy(litHolds = holds)

        viewModelScope.launch {
            try {
                client.setHoldColors(pixelCount = current.wall.segmentSize, lit = holds.toHex())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "$description failed", e)
                _uiState.value = WallUiState.Error(problemFor(e))
            }
        }
    }

    /**
     * Sets master brightness.
     *
     * The wall's power state is passed along deliberately: WLED derives power
     * from brightness when the field is absent, so a bare brightness change
     * would switch a sleeping wall back on. The client also clamps away from
     * zero, because brightness rising from zero is what makes WLED unfreeze
     * its segments and drop the route.
     *
     * Unlike a hold tap this does not need the route re-pushing - the route
     * survives a brightness change, so long as brightness never reaches zero.
     */
    fun setBrightness(brightness: Int) {
        val current = _uiState.value as? WallUiState.Connected ?: return
        // Moves with the finger. The request that follows is conflated, so the
        // slider stays smooth whatever the controller is keeping up with.
        _uiState.value = current.copy(brightness = brightness)
        requestedBrightness = brightness
        brightnessRequests.trySend(brightness)
    }

    fun toggleWall() {
        val current = _uiState.value as? WallUiState.Connected ?: return
        if (current.busy) return
        _uiState.value = current.copy(busy = true)

        viewModelScope.launch {
            _uiState.value = try {
                val status = client.setOn(on = !current.on)
                // WLED unfreezes every segment when it's switched on (see the
                // "unfreeze all segments when turning on" branch in json.cpp),
                // which drops the per-pixel route from the wall while the app
                // still shows it. Push the route again so the two agree.
                if (status.on && current.litHolds.isNotEmpty()) {
                    client.setHoldColors(pixelCount = current.wall.segmentSize, lit = current.litHolds.toHex())
                }
                // copy() rather than a fresh Connected, so the route stays put.
                current.copy(on = status.on, brightness = status.brightness, busy = false)
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
/** The wire format WLED wants, from the colours the UI works in. */
private fun Map<Int, HoldColor>.toHex(): Map<Int, String> = mapValues { it.value.hex }

private fun problemFor(e: Exception): WallProblem = when (e) {
    is WledConfigException -> WallProblem.NotAWledMatrix
    else -> WallProblem.Unreachable
}
