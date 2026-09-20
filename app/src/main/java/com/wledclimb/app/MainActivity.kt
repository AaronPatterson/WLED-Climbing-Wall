package com.wledclimb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wledclimb.app.settings.WledSettings
import com.wledclimb.app.wled.SetupUiState
import com.wledclimb.app.wled.SetupViewModel
import com.wledclimb.app.wled.WallUiState
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
                            val context = LocalContext.current
                            // Keyed on visitId so returning here to change an existing
                            // address gets a fresh ViewModel instead of the one left
                            // over (already "Connected", stale text field) from last time.
                            val setupViewModel: SetupViewModel = viewModel(
                                key = state.visitId.toString(),
                                factory = LambdaViewModelFactory {
                                    SetupViewModel(WledSettings(context), initialIp = state.currentUrl)
                                }
                            )
                            val setupState by setupViewModel.uiState.collectAsState()
                            LaunchedEffect(setupState) {
                                val connected = setupState as? SetupUiState.Connected
                                if (connected != null) {
                                    rootViewModel.onSetupComplete(connected.ip)
                                }
                            }
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

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun WallScreen(
    state: WallUiState,
    onToggle: () -> Unit,
    onRetry: () -> Unit,
    onChangeController: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is WallUiState.Connecting -> {
                CircularProgressIndicator()
                Text(text = "Connecting to the wall…", modifier = Modifier.padding(top = 16.dp))
            }

            is WallUiState.Connected -> {
                Text(text = if (state.on) "Wall is ON" else "Wall is OFF")
                Button(
                    onClick = onToggle,
                    enabled = !state.busy,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = if (state.on) "Turn wall off" else "Turn wall on")
                }
            }

            is WallUiState.Error -> {
                Text(text = "Couldn't reach the wall")
                Text(text = state.message, modifier = Modifier.padding(top = 8.dp))
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = "Retry")
                }
            }
        }
        TextButton(onClick = onChangeController, modifier = Modifier.padding(top = 32.dp)) {
            Text(text = "Change controller")
        }
    }
}
