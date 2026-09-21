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
     * Lights individual holds. [lit] maps a **segment buffer index**
     * (`x + y * width`, see Wall.segmentIndexAt) to an `RRGGBB` colour, and
     * every other position below [pixelCount] is turned off.
     *
     * These are grid positions, not positions along the LED strip: WLED's "i"
     * command writes into the segment's 2D buffer and applies the ledmap
     * itself when rendering.
     *
     * Sends the whole desired state rather than just what changed, so the wall
     * can't drift out of sync with the app - WLED keeps previously set pixels
     * when it receives further updates, so an incremental message would leave
     * a hold lit after it had been cleared in the app.
     */
    suspend fun setHoldColors(pixelCount: Int, lit: Map<Int, String>)
}
