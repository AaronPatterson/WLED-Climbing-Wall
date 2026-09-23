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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wledclimb.app.BuildConfig
import com.wledclimb.app.R
import com.wledclimb.app.grid.Wall

@Composable
fun WallScreen(
    state: WallUiState,
    onToggle: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onHoldTap: (segmentIndex: Int) -> Unit,
    onColorSelect: (HoldColor) -> Unit,
    onClearWall: () -> Unit,
    onRetry: () -> Unit,
    onChangeController: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Only when connected: with no wall reached there is no name to show,
        // no brightness to report, and nothing the controls could act on.
        if (state is WallUiState.Connected) {
            WallTopBar(
                name = state.name,
                on = state.on,
                brightness = state.brightness,
                enabled = !state.busy,
                onToggle = onToggle,
                onBrightnessChange = onBrightnessChange,
                onChangeController = onChangeController
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        when (state) {
            is WallUiState.Connecting -> ConnectingContent()
            is WallUiState.Connected -> ConnectedContent(
                state = state,
                onHoldTap = onHoldTap,
                onColorSelect = onColorSelect,
                onClearWall = onClearWall
            )
            is WallUiState.Error -> ErrorContent(problem = state.problem, onRetry = onRetry)
        }
        // Not connected, so the top bar and its menu are absent - changing the
        // controller has to stay reachable, because it is the way out of an
        // address that no longer answers.
        if (state !is WallUiState.Connected) {
            TextButton(onClick = onChangeController, modifier = Modifier.padding(top = 32.dp)) {
                Text(text = stringResource(R.string.wall_change_controller))
            }
        }
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
    onHoldTap: (segmentIndex: Int) -> Unit,
    onColorSelect: (HoldColor) -> Unit,
    onClearWall: () -> Unit
) {
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
            pan = clampGridPan(pan + gridPanDelta(gesturePan, scale), scale, gridSize)
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
    val holdCount = wall.holdCount
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Icons rather than labels: three words of chrome under the grid read as
        // a sentence to be parsed, and none of them is the point of the screen.
        // The labels survive as content descriptions, so nothing is lost to a
        // screen reader.
        IconButton(onClick = { onZoom(1f / ZOOM_STEP) }, enabled = scale > MIN_GRID_SCALE) {
            Icon(
                painter = painterResource(R.drawable.ic_zoom_out),
                contentDescription = stringResource(R.string.wall_zoom_out)
            )
        }
        IconButton(onClick = { onZoom(ZOOM_STEP) }, enabled = scale < MAX_GRID_SCALE) {
            Icon(
                painter = painterResource(R.drawable.ic_zoom_in),
                contentDescription = stringResource(R.string.wall_zoom_in)
            )
        }
        IconButton(onClick = onClearWall, enabled = canClear) {
            Icon(
                painter = painterResource(R.drawable.ic_clear_wall),
                contentDescription = stringResource(R.string.wall_clear)
            )
        }
    }
}

/**
 * The colours a hold can be painted in. Tapping one arms it for the next tap.
 *
 * Swatches size themselves to the width available rather than taking a fixed
 * size: six at 56dp don't fit across a phone, and a Row that runs out of room
 * squashes the last swatch that fits and drops the rest off the edge entirely.
 * They never grow past 56dp, and at phone width land on 48dp - the smallest a
 * touch target should be, and these get tapped by six-year-olds.
 */
@Composable
private fun ColorPalette(
    selected: HoldColor,
    onSelect: (HoldColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HoldColor.entries
    val spacing = 8.dp

    BoxWithConstraints(modifier) {
        val swatchSize = minOf(
            MAX_SWATCH_SIZE,
            (maxWidth - spacing * (colors.size - 1)) / colors.size
        )

        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            for (color in colors) {
                val isSelected = color == selected
                val label = stringResource(color.labelRes)
                val swatchDescription =
                    if (isSelected) stringResource(R.string.color_selected, label) else label
                Box(
                    modifier = Modifier
                        .size(swatchSize)
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
}

/** Big enough to tap easily; past this they just look oversized on a tablet. */
private val MAX_SWATCH_SIZE = 56.dp

/** WLED's "RRGGBB" as an opaque Compose colour. */
/**
 * Served from the repository's own GitHub Pages site, so it costs nothing and
 * cannot lapse. Changing what it says needs no store review; changing this
 * address does, since Play holds it as part of the listing.
 */
internal const val PRIVACY_POLICY_URL =
    "https://aaronpatterson.github.io/WLED-Climbing-Wall/privacy.html"

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
