package com.wledclimb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wledclimb.app.settings.WledSettings
import com.wledclimb.app.setup.SetupRoute
import com.wledclimb.app.wall.WallRoute

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

                        is RootUiState.NeedsSetup -> SetupRoute(
                            currentUrl = state.currentUrl,
                            onSetupComplete = rootViewModel::onSetupComplete
                        )

                        is RootUiState.Ready -> WallRoute(
                            wledBaseUrl = state.wledBaseUrl,
                            onChangeController = rootViewModel::onChangeController
                        )
                    }
                }
            }
        }
    }
}
