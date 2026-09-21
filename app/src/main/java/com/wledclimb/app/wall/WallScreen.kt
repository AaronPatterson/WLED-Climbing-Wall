package com.wledclimb.app.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
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
    onColorSelect: (HoldColor) -> Unit,
    onClearWall: () -> Unit,
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
                onHoldTap = onHoldTap,
                onColorSelect = onColorSelect,
                onClearWall = onClearWall
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
    onHoldTap: (segmentIndex: Int) -> Unit,
    onColorSelect: (HoldColor) -> Unit,
    onClearWall: () -> Unit
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
    var scale by remember { mutableFloatStateOf(MIN_GRID_SCALE) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(Size.Zero) }

    WallGrid(
        wall = state.wall,
        litHolds = state.litHolds,
        onHoldTap = onHoldTap,
        scale = scale,
        pan = pan,
        onTransform = { gesturePan, gestureZoom, gridSize ->
            viewport = gridSize
            scale = clampGridScale(scale * gestureZoom)
            pan = clampGridPan(pan + gesturePan, scale, gridSize)
        },
        modifier = Modifier
            .weight(1f, fill = false)
            .padding(top = 16.dp)
    )
    GridControls(
        scale = scale,
        canClear = state.litHolds.isNotEmpty(),
        onClearWall = onClearWall,
        onZoom = { factor ->
            scale = clampGridScale(scale * factor)
            // Zooming back out has to pull the grid back into view, or it
            // would sit off-centre with empty space beside it.
            pan = clampGridPan(pan, scale, viewport)
        },
        modifier = Modifier.padding(top = 8.dp)
    )
    ColorPalette(
        selected = state.selectedColor,
        onSelect = onColorSelect,
        modifier = Modifier.padding(top = 16.dp)
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
 * route end to end matters more than per-cell precision.
 *
 * The grid owns every gesture itself, taps included. Per-cell `clickable`
 * cannot be used here: children are dispatched pointer events before their
 * parent, and detectTransformGestures abandons the gesture the moment any
 * change is consumed, so clickable cells silently swallowed every pinch and
 * drag. Cells keep an explicit semantics onClick so screen readers can still
 * activate them.
 */
@Composable
private fun WallGrid(
    wall: Wall,
    litHolds: Map<Int, HoldColor>,
    onHoldTap: (segmentIndex: Int) -> Unit,
    scale: Float,
    pan: Offset,
    onTransform: (pan: Offset, zoom: Float, viewport: Size) -> Unit,
    modifier: Modifier = Modifier
) {
    val holdCount = wall.cells.sumOf { row -> row.count { it != null } }
    val description =
        stringResource(R.string.wall_grid_description, holdCount, wall.width, wall.height)

    // Scaling through graphicsLayer doesn't change the layout size, so a
    // zoomed grid would otherwise paint straight over the controls below it.
    BoxWithConstraints(modifier.clipToBounds()) {
        // Fit whichever dimension runs out first: on a landscape tablet a
        // 12x12 wall is limited by height, on a phone by width. Fitting only
        // to width pushes the controls below the grid off the screen.
        val cellSize = if (wall.width > 0 && wall.height > 0) {
            minOf(maxWidth / wall.width, maxHeight / wall.height)
        } else {
            0.dp
        }
        val cellPx = with(LocalDensity.current) { cellSize.toPx() }
        val viewport = Size(cellPx * wall.width, cellPx * wall.height)

        Column(
            modifier = Modifier
                .semantics { contentDescription = description }
                // graphicsLayer rather than re-laying out at a larger cell
                // size, so zooming doesn't re-measure every cell each frame.
                // Pointer input sits inside the layer, so the offsets it
                // reports are already in the grid's own coordinates.
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pan.x
                    translationY = pan.y
                }
                .pointerInput(cellPx, wall) {
                    detectTapGestures { offset ->
                        val x = (offset.x / cellPx).toInt()
                        val y = (offset.y / cellPx).toInt()
                        if (x in 0 until wall.width &&
                            y in 0 until wall.height &&
                            wall.hasHoldAt(x, y)
                        ) {
                            onHoldTap(wall.segmentIndexAt(x, y))
                        }
                    }
                }
                .pointerInput(viewport) {
                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                        onTransform(gesturePan, gestureZoom, viewport)
                    }
                }
        ) {
            for (y in 0 until wall.height) {
                Row {
                    for (x in 0 until wall.width) {
                        // Grid position, not position along the strip: this is
                        // what WLED's per-pixel commands address.
                        val segmentIndex = wall.segmentIndexAt(x, y)
                        HoldCell(
                            segmentIndex = if (wall.hasHoldAt(x, y)) segmentIndex else null,
                            color = litHolds[segmentIndex],
                            column = x,
                            row = y,
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
 * One cell. A gap (no LED behind it) takes up its space but isn't a control -
 * there's nothing there to light.
 *
 * Taps are handled by the grid, not here; the onClick below exists so screen
 * readers still have something to activate.
 */
@Composable
private fun HoldCell(
    segmentIndex: Int?,
    color: HoldColor?,
    column: Int,
    row: Int,
    size: Dp,
    onTap: (segmentIndex: Int) -> Unit
) {
    val unlitColor = WallStatusColors.gridCell
    val holdDescription = if (color == null) {
        stringResource(R.string.wall_hold_description, column + 1, row + 1)
    } else {
        stringResource(
            R.string.wall_hold_lit_description,
            column + 1,
            row + 1,
            stringResource(color.labelRes)
        )
    }

    Box(
        modifier = Modifier
            .size(size)
            .padding(1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                when {
                    segmentIndex == null -> Color.Transparent
                    color != null -> color.displayColor
                    else -> unlitColor
                }
            )
            .then(
                if (segmentIndex == null) {
                    // An empty cell isn't a control; don't announce it at all.
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier.semantics {
                        contentDescription = holdDescription
                        onClick {
                            onTap(segmentIndex)
                            true
                        }
                    }
                }
            )
    )
}

/**
 * Zoom steps, for anyone who would rather not pinch, plus clearing the wall.
 *
 * Clearing sits here rather than next to the palette: there's no undo and no
 * saved routes yet, so it's worth keeping away from where fingers are busy
 * painting. It's disabled when there's nothing lit, so it can't wipe by
 * accident when the wall is already clear.
 */
@Composable
private fun GridControls(
    scale: Float,
    canClear: Boolean,
    onClearWall: () -> Unit,
    onZoom: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { onZoom(1f / ZOOM_STEP) }, enabled = scale > MIN_GRID_SCALE) {
            Text(text = stringResource(R.string.wall_zoom_out))
        }
        TextButton(onClick = { onZoom(ZOOM_STEP) }, enabled = scale < MAX_GRID_SCALE) {
            Text(text = stringResource(R.string.wall_zoom_in))
        }
        TextButton(onClick = onClearWall, enabled = canClear) {
            Text(text = stringResource(R.string.wall_clear))
        }
    }
}

/** The colours a hold can be painted in. Tapping one arms it for the next tap. */
@Composable
private fun ColorPalette(
    selected: HoldColor,
    onSelect: (HoldColor) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (color in HoldColor.entries) {
            val isSelected = color == selected
            val label = stringResource(color.labelRes)
            val swatchDescription =
                if (isSelected) stringResource(R.string.color_selected, label) else label
            Box(
                modifier = Modifier
                    // Comfortably above the 48dp minimum: these get tapped by
                    // six-year-olds.
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.displayColor)
                    .border(
                        width = if (isSelected) 4.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
                    .clickable { onSelect(color) }
                    .semantics { contentDescription = swatchDescription }
            )
        }
    }
}

/** WLED's "RRGGBB" as an opaque Compose colour. */
private val HoldColor.displayColor: Color
    get() = Color(hex.toLong(16) or 0xFF000000L)

private val HoldColor.labelRes: Int
    get() = when (this) {
        HoldColor.Red -> R.string.color_red
        HoldColor.Orange -> R.string.color_orange
        HoldColor.Yellow -> R.string.color_yellow
        HoldColor.Green -> R.string.color_green
        HoldColor.Blue -> R.string.color_blue
        HoldColor.Purple -> R.string.color_purple
    }
