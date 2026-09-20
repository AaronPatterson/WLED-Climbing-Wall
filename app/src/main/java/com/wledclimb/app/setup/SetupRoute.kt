package com.wledclimb.app.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wledclimb.app.LambdaViewModelFactory
import com.wledclimb.app.settings.WledSettings
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Owns SetupViewModel and wires it to SetupScreen - the one place allowed to
 * depend on SetupViewModel directly (see the Composables section of
 * docs/kotlin-style.md).
 */
@Composable
fun SetupRoute(currentUrl: String?, onSetupComplete: (String) -> Unit) {
    val context = LocalContext.current.applicationContext
    val setupViewModel: SetupViewModel = viewModel(
        factory = LambdaViewModelFactory { SetupViewModel(WledSettings(context)) }
    )
    // Runs once each time this screen is (re-)entered - e.g. after tapping
    // "Change controller". Resets first (clearing whatever was left over
    // from last visit) before watching for a fresh success, so a leftover
    // Connected from last time can't be seen as a new one.
    LaunchedEffect(Unit) {
        setupViewModel.reset(currentIp = currentUrl)
        setupViewModel.uiState
            .filterIsInstance<SetupUiState.Connected>()
            .collect { connected -> onSetupComplete(connected.ip) }
    }
    val setupState by setupViewModel.uiState.collectAsState()
    SetupScreen(
        state = setupState,
        onIpInputChange = setupViewModel::onIpInputChange,
        onTestAndSave = setupViewModel::testAndSave
    )
}
