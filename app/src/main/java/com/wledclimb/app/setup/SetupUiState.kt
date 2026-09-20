package com.wledclimb.app.setup

/** UI-facing state of the setup screen. */
sealed interface SetupUiState {
    data class Editing(
        val ipInput: String,
        val testing: Boolean = false,
        val error: String? = null
    ) : SetupUiState

    /** Test succeeded and the address was saved; the caller should move on to wall control. */
    data class Connected(val ip: String) : SetupUiState
}
