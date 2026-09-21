package com.wledclimb.app.wall

import com.wledclimb.app.grid.Wall

/** UI-facing state of the wall connection. */
sealed interface WallUiState {
    data object Connecting : WallUiState

    /**
     * [litHolds] maps a grid position (`Wall.segmentIndexAt`) to the colour it's
     * showing; holds absent from it are off. This is the route currently on the
     * wall. Keyed by grid position, not by position along the LED strip - that's
     * what WLED's per-pixel commands address.
     *
     * [selectedColor] is what the next tapped hold will be painted in.
     */
    data class Connected(
        val on: Boolean,
        val wall: Wall,
        val litHolds: Map<Int, HoldColor> = emptyMap(),
        val selectedColor: HoldColor = HoldColor.Red,
        val busy: Boolean = false
    ) : WallUiState

    data class Error(val problem: WallProblem) : WallUiState
}
