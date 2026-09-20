package com.wledclimb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wledclimb.app.settings.DataStoreWledSettings
import com.wledclimb.app.setup.SetupRoute
import com.wledclimb.app.theme.WledClimbTheme
import com.wledclimb.app.wall.WallRoute

class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels {
        LambdaViewModelFactory { RootViewModel(DataStoreWledSettings(applicationContext)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35 draws edge-to-edge whether we ask or not, so opt in
        // explicitly and inset the content rather than letting it slide under
        // the status and navigation bars.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WledClimbTheme {
                // The Surface fills the display so its background reaches the
                // screen edges; only the content inside is inset.
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.safeDrawingPadding()) {
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
}
