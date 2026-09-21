package com.wledclimb.app.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wledclimb.app.R
import com.wledclimb.app.grid.Wall

@Composable
fun WallScreen(
    state: WallUiState,
    onToggle: () -> Unit,
    onHoldTap: (segmentIndex: Int) -> Unit,
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
            is WallUiState.Connecting -> ConnectingContent()
            is WallUiState.Connected -> ConnectedContent(
                state = state,
                onToggle = onToggle,
                onHoldTap = onHoldTap
            )
            is WallUiState.Error -> ErrorContent(problem = state.problem, onRetry = onRetry)
        }
        // Outside the when: reachable from every state, including when the
        // controller can't be reached and changing it is the way out.
        TextButton(onClick = onChangeController, modifier = Modifier.padding(top = 32.dp)) {
            Text(text = stringResource(R.string.wall_change_controller))
        }
    }
}

@Composable
private fun ColumnScope.ConnectingContent() {
    CircularProgressIndicator()
    Text(
        text = stringResource(R.string.wall_connecting),
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun ColumnScope.ConnectedContent(
    state: WallUiState.Connected,
    onToggle: () -> Unit,
    onHoldTap: (segmentIndex: Int) -> Unit
) {
    val statusColor = if (state.on) WallStatusColors.on else WallStatusColors.off
    val statusText = stringResource(if (state.on) R.string.wall_is_on else R.string.wall_is_off)

    // Read as one phrase by a screen reader: the dot repeats what the text
    // already says, so it's hidden rather than announced.
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
    WallGrid(
        wall = state.wall,
        litHolds = state.litHolds,
        onHoldTap = onHoldTap,
        modifier = Modifier
            .weight(1f, fill = false)
            .padding(top = 16.dp)
    )
    Button(
        onClick = onToggle,
        enabled = !state.busy,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(
            text = stringResource(if (state.on) R.string.wall_turn_off else R.string.wall_turn_on)
        )
    }
}

@Composable
private fun ColumnScope.ErrorContent(problem: WallProblem, onRetry: () -> Unit) {
    Text(text = stringResource(R.string.wall_error_title))
    Text(
        text = stringResource(
            when (problem) {
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

/**
 * The wall's holds, sized so the whole wall is visible at once - reading a
 * route end to end matters more than per-cell precision, and on a phone that
 * can put cells below the 48dp touch minimum, which is what zoom (still to
 * come) is for.
 */
@Composable
private fun WallGrid(
    wall: Wall,
    litHolds: Map<Int, String>,
    onHoldTap: (segmentIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val holdCount = wall.cells.sumOf { row -> row.count { it != null } }
    val description = stringResource(R.string.wall_grid_description, holdCount, wall.width, wall.height)

    BoxWithConstraints(modifier) {
        // Fit whichever dimension runs out first: on a landscape tablet a
        // 12x12 wall is limited by height, on a phone by width. Fitting only
        // to width pushes the controls below the grid off the screen.
        val cellSize = if (wall.width > 0 && wall.height > 0) {
            minOf(maxWidth / wall.width, maxHeight / wall.height)
        } else {
            0.dp
        }

        Column(modifier = Modifier.semantics { contentDescription = description }) {
            for (y in 0 until wall.height) {
                Row {
                    for (x in 0 until wall.width) {
                        // Grid position, not position along the strip: this is
                        // what WLED's per-pixel commands address.
                        val segmentIndex = wall.segmentIndexAt(x, y)
                        HoldCell(
                            segmentIndex = if (wall.hasHoldAt(x, y)) segmentIndex else null,
                            color = litHolds[segmentIndex],
                            size = cellSize,
                            onTap = onHoldTap
                        )
                    }
                }
            }
        }
    }
}

/**
 * One cell. A gap (no LED behind it) takes up its space but isn't tappable -
 * there's nothing there to light.
 */
@Composable
private fun HoldCell(
    segmentIndex: Int?,
    color: String?,
    size: Dp,
    onTap: (segmentIndex: Int) -> Unit
) {
    val unlitColor = WallStatusColors.gridCell
    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                when {
                    segmentIndex == null -> Color.Transparent
                    color != null -> hexToColor(color)
                    else -> unlitColor
                }
            )
            .then(
                if (segmentIndex == null) Modifier
                else Modifier.clickable { onTap(segmentIndex) }
            )
    )
}

/** "RRGGBB" as WLED writes it, with full opacity added. */
private fun hexToColor(hex: String): Color = Color(hex.toLong(16) or 0xFF000000L)
