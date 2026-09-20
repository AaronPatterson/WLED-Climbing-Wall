package com.wledclimb.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wledclimb.app.wled.WallUiState

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
