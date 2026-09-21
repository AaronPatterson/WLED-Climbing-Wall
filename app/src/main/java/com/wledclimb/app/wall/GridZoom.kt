package com.wledclimb.app.wall

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** Zoomed right out, the whole wall is visible and there's nothing to pan to. */
const val MIN_GRID_SCALE = 1f

/** Far enough in that a hold is a comfortable target even on a phone. */
const val MAX_GRID_SCALE = 4f

/**
 * How far the grid can be dragged before its edge would come away from the
 * edge of the viewport, leaving empty space beside a wall you're trying to
 * read. At [MIN_GRID_SCALE] this is zero: there's nowhere to go.
 */
fun maxGridPan(scale: Float, viewport: Size): Offset {
    val clampedScale = scale.coerceAtLeast(MIN_GRID_SCALE)
    return Offset(
        x = (clampedScale - 1f) * viewport.width / 2f,
        y = (clampedScale - 1f) * viewport.height / 2f
    )
}

/** Keeps a pan offset within [maxGridPan], so the wall can't be dragged away. */
fun clampGridPan(offset: Offset, scale: Float, viewport: Size): Offset {
    val max = maxGridPan(scale, viewport)
    return Offset(
        x = offset.x.coerceIn(-max.x, max.x),
        y = offset.y.coerceIn(-max.y, max.y)
    )
}

/** Applies a pinch, keeping the result within the allowed zoom range. */
fun clampGridScale(scale: Float): Float = scale.coerceIn(MIN_GRID_SCALE, MAX_GRID_SCALE)
