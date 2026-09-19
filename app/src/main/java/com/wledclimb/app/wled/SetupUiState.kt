package com.wledclimb.app.wled

/** UI-facing state of the setup screen. */
sealed interface SetupUiState {
    data class Editing(
        val ipInput: String,
        val testing: Boolean = false,
        val error: String? = null
    ) : SetupUiState

    data class Tested(val ip: String, val rawConfig: String) : SetupUiState
}
