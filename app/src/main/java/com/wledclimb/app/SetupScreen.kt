package com.wledclimb.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.wledclimb.app.wled.SetupUiState

@Composable
fun SetupScreen(
    state: SetupUiState,
    onIpInputChange: (String) -> Unit,
    onTestAndSave: () -> Unit,
    onContinue: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (state) {
            is SetupUiState.Editing -> {
                Text(text = "Connect to your WLED controller")
                OutlinedTextField(
                    value = state.ipInput,
                    onValueChange = onIpInputChange,
                    label = { Text("Controller IP or hostname") },
                    enabled = !state.testing,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
                if (state.error != null) {
                    Text(text = state.error, modifier = Modifier.padding(top = 8.dp))
                }
                if (state.testing) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                } else {
                    Button(onClick = onTestAndSave, modifier = Modifier.padding(top = 16.dp)) {
                        Text(text = "Test & save")
                    }
                }
            }

            is SetupUiState.Tested -> {
                Text(text = "Connected to ${state.ip}")
                Text(
                    text = state.rawConfig,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .verticalScroll(rememberScrollState())
                )
                Button(
                    onClick = { onContinue(state.ip) },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = "Continue")
                }
            }
        }
    }
}
