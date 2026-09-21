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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wledclimb.app.R
import com.wledclimb.app.grid.Wall

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
                Text(
                    text = stringResource(R.string.wall_connecting),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            is WallUiState.Connected -> {
                val statusColor = if (state.on) WallStatusColors.on else WallStatusColors.off
                val statusText =
                    stringResource(if (state.on) R.string.wall_is_on else R.string.wall_is_off)

                // Read as one phrase by a screen reader: the dot repeats what
                // the text already says, so it's hidden rather than announced.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(color = statusColor, shape = CircleShape)
                            .clearAndSetSemantics { }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                WallGrid(wall = state.wall, modifier = Modifier.padding(top = 16.dp))
                Button(
                    onClick = onToggle,
                    enabled = !state.busy,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (state.on) R.string.wall_turn_off else R.string.wall_turn_on
                        )
                    )
                }
            }

            is WallUiState.Error -> {
                Text(text = stringResource(R.string.wall_error_title))
                Text(
                    text = stringResource(
                        when (state.problem) {
                            WallProblem.Unreachable -> R.string.wall_error_unreachable
                            WallProblem.NotAWledMatrix -> R.string.wall_error_not_wled
                        }
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.wall_retry))
                }
            }
        }
        TextButton(onClick = onChangeController, modifier = Modifier.padding(top = 32.dp)) {
            Text(text = stringResource(R.string.wall_change_controller))
        }
    }
}

/**
 * Read-only preview of the wall's LED grid: a filled square for each cell a
 * panel covers, blank for gaps. Phase 3 makes this interactive (tap to light
 * a hold); for now it's just confirmation the grid layout came through.
 */
@Composable
private fun WallGrid(wall: Wall, modifier: Modifier = Modifier) {
    val cellColor = WallStatusColors.gridCell
    // Individual cells mean nothing to a screen reader and there can be
    // hundreds, so the grid is described once as a whole instead.
    val description = stringResource(
        R.string.wall_grid_description,
        wall.cells.sumOf { row -> row.count { it != null } },
        wall.width,
        wall.height
    )

    Column(modifier = modifier.semantics { contentDescription = description }) {
        for (row in wall.cells) {
            Row {
                for (cell in row) {
                    Box(
                        modifier = Modifier
                            .padding(1.dp)
                            .size(16.dp)
                            .background(
                                color = if (cell != null) cellColor else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
