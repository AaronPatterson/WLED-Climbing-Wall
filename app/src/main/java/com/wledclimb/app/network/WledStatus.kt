package com.wledclimb.app.network

/**
 * The parts of `/json/state` the app cares about, read in one request because
 * they arrive together.
 *
 * [brightness] is WLED's master brightness, 0-255. It is independent of
 * [on]: switching the wall off leaves brightness where it was, which is why
 * both are needed to describe the wall rather than either alone.
 */
data class WledStatus(
    val on: Boolean,
    val brightness: Int
)
