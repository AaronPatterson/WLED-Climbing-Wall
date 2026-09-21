package com.wledclimb.app.wall

import com.wledclimb.app.grid.Wall

/** UI-facing state of the wall connection. */
sealed interface WallUiState {
    data object Connecting : WallUiState
    data class Connected(val on: Boolean, val wall: Wall, val busy: Boolean = false) : WallUiState
    data class Error(val problem: WallProblem) : WallUiState
}
