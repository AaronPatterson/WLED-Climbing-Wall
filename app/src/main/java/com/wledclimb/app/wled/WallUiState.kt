package com.wledclimb.app.wled

/** UI-facing state of the wall connection. Kept deliberately small for Phase 0. */
sealed interface WallUiState {
    data object Connecting : WallUiState
    data class Connected(val on: Boolean, val busy: Boolean = false) : WallUiState
    data class Error(val message: String) : WallUiState
}
