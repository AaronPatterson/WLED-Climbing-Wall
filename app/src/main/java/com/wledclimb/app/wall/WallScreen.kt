package com.wledclimb.app.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val wallOnColor = Color(0xFF2E7D32)
private val wallOffColor = Color(0xFF616161)

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
                val statusColor = if (state.on) wallOnColor else wallOffColor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color = statusColor, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.on) "Wall is ON" else "Wall is OFF",
                        color = statusColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
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
