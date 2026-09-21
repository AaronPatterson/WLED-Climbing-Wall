package com.wledclimb.app.wall

import com.wledclimb.app.grid.Wall

/** UI-facing state of the wall connection. */
sealed interface WallUiState {
    data object Connecting : WallUiState

    /**
     * [litHolds] maps an LED index to the `RRGGBB` colour it's showing; holds
     * absent from it are off. This is the route currently on the wall.
     */
    data class Connected(
        val on: Boolean,
        val wall: Wall,
        val litHolds: Map<Int, String> = emptyMap(),
        val busy: Boolean = false
    ) : WallUiState

    data class Error(val problem: WallProblem) : WallUiState
}
