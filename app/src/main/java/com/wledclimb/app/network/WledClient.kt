package com.wledclimb.app.network

/**
 * Talks to a WLED controller's JSON HTTP API.
 *
 * An interface so ViewModels depending on it can be tested against a fake
 * instead of real sockets; [HttpWledClient] is the real implementation.
 */
interface WledClient {

    /** Current power state of the wall, read from WLED. */
    suspend fun getOn(): Boolean

    /** Turns the whole wall on or off, returning the state WLED confirms. */
    suspend fun setOn(on: Boolean): Boolean

    /** Raw JSON from `/json/cfg` (grid layout, LED count, segments). */
    suspend fun getConfig(): String

    /**
     * Raw JSON array from `/2d-gaps.json`, or null if no gap file is
     * configured - WLED serves this only if one was uploaded via its own 2D
     * setup UI, so its absence is the normal case, not an error.
     */
    suspend fun getGaps(): String?

    /**
     * Lights individual holds: [lit] maps an LED index to an `RRGGBB` colour,
     * and every other LED below [ledCount] is turned off.
     *
     * Sends the whole desired state rather than just what changed, so the wall
     * can't drift out of sync with the app - WLED keeps previously set pixels
     * when it receives further updates, so an incremental message would leave
     * a hold lit after it had been cleared in the app.
     */
    suspend fun setHoldColors(ledCount: Int, lit: Map<Int, String>)
}
