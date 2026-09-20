package com.wledclimb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.filterIsInstance
import com.wledclimb.app.settings.WledSettings
import com.wledclimb.app.wled.SetupUiState
import com.wledclimb.app.wled.SetupViewModel
import com.wledclimb.app.wled.WallViewModel
import com.wledclimb.app.wled.WledClient

class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels {
        LambdaViewModelFactory { RootViewModel(WledSettings(applicationContext)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val rootState by rootViewModel.uiState.collectAsState()
                    when (val state = rootState) {
                        is RootUiState.Loading -> LoadingScreen()

                        is RootUiState.NeedsSetup -> {
                            val setupViewModel: SetupViewModel = viewModel(
                                factory = LambdaViewModelFactory { SetupViewModel(WledSettings(applicationContext)) }
                            )
                            // Runs once each time this screen is (re-)entered - e.g. after
                            // tapping "Change controller". Resets first (clearing whatever was
                            // left over from last visit) before watching for a fresh success,
                            // so a leftover Connected from last time can't be seen as a new one.
                            LaunchedEffect(Unit) {
                                setupViewModel.reset(currentIp = state.currentUrl)
                                setupViewModel.uiState
                                    .filterIsInstance<SetupUiState.Connected>()
                                    .collect { connected -> rootViewModel.onSetupComplete(connected.ip) }
                            }
                            val setupState by setupViewModel.uiState.collectAsState()
                            SetupScreen(
                                state = setupState,
                                onIpInputChange = setupViewModel::onIpInputChange,
                                onTestAndSave = setupViewModel::testAndSave
                            )
                        }

                        is RootUiState.Ready -> {
                            // Keyed on the address so switching controllers gets a WallViewModel
                            // (and WledClient) pointed at the new one, not the previous instance.
                            val wallViewModel: WallViewModel = viewModel(
                                key = state.wledBaseUrl,
                                factory = LambdaViewModelFactory {
                                    WallViewModel(WledClient(baseUrl = state.wledBaseUrl))
                                }
                            )
                            val wallState by wallViewModel.uiState.collectAsState()
                            WallScreen(
                                state = wallState,
                                onToggle = wallViewModel::toggleWall,
                                onRetry = wallViewModel::refresh,
                                onChangeController = rootViewModel::onChangeController
                            )
                        }
                    }
                }
            }
        }
    }
}
