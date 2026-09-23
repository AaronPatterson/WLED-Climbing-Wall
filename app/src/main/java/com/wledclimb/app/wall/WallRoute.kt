package com.wledclimb.app.wall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wledclimb.app.LambdaViewModelFactory
import com.wledclimb.app.network.HttpWledClient

/**
 * Owns WallViewModel and wires it to WallScreen - the one place allowed to
 * depend on WallViewModel directly (see the Composables section of
 * docs/kotlin-style.md).
 */
@Composable
fun WallRoute(wledBaseUrl: String, onChangeController: () -> Unit) {
    // Keyed on the address so switching controllers gets a WallViewModel
    // (and WledClient) pointed at the new one, not the previous instance.
    val wallViewModel: WallViewModel = viewModel(
        key = wledBaseUrl,
        factory = LambdaViewModelFactory { WallViewModel(HttpWledClient(baseUrl = wledBaseUrl)) }
    )
    val wallState by wallViewModel.uiState.collectAsState()
    WallScreen(
        state = wallState,
        onToggle = wallViewModel::toggleWall,
        onHoldTap = wallViewModel::toggleHold,
        onBrightnessChange = wallViewModel::setBrightness,
        onColorSelect = wallViewModel::selectColor,
        onClearWall = wallViewModel::clearWall,
        onRetry = wallViewModel::refresh,
        onChangeController = onChangeController
    )
}
