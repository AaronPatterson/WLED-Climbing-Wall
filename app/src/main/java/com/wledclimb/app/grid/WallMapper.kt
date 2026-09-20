package com.wledclimb.app.grid

/**
 * Builds a [Wall] from the controller's panel layout, porting WLED's own
 * `WS2812FX::setUpMatrix()` (wled00/FX_2Dfcn.cpp) so LED indices line up
 * exactly with the physical wiring WLED itself uses for per-LED control.
 *
 * Panels are walked in physical wiring order (matching the order WLED lists
 * them), and each panel's LEDs are assigned sequential indices continuing
 * from the previous panel's last one - that running count *is* the LED index
 * WLED uses for individual-pixel control, not something computed separately.
 *
 * [gaps], if present, is WLED's optional `/2d-gaps.json`: one entry per cell
 * in the matrix's bounding box (row-major), where -1 means no LED is
 * physically there at all (doesn't consume an LED index) and 0 means an LED
 * *is* wired there but is inactive/unusable (consumes an index but isn't
 * mapped) - the two differ in whether every LED index *after* that point
 * shifts by one, so getting this distinction wrong misaligns every
 * subsequent hold, not just the gapped one. A [gaps] list shorter than the
 * matrix's cell count is ignored entirely, matching WLED's own fallback.
 */
fun buildWall(panels: List<Panel>, gaps: List<Int>? = null): Wall {
    val width = panels.maxOfOrNull { it.xOffset + it.width } ?: 0
    val height = panels.maxOfOrNull { it.yOffset + it.height } ?: 0

    val effectiveGaps = gaps?.takeIf { it.size >= width * height }

    val cells = MutableList(height) { MutableList<Int?>(width) { null } }

    var ledIndex = 0
    for (panel in panels) {
        // WLED walks each panel along its "primary" axis (h) nested inside its
        // "secondary" axis (v) - which axis is which swaps when the panel is
        // wired in vertical columns instead of horizontal rows.
        val primaryAxisLength = if (panel.vertical) panel.height else panel.width
        val secondaryAxisLength = if (panel.vertical) panel.width else panel.height

        for (j in 0 until secondaryAxisLength) {
            for (i in 0 until primaryAxisLength) {
                val startsAtFarSecondaryEdge = if (panel.vertical) panel.rightStart else panel.bottomStart
                val localY = if (startsAtFarSecondaryEdge) secondaryAxisLength - j - 1 else j

                val startsAtFarPrimaryEdge = if (panel.vertical) panel.bottomStart else panel.rightStart
                var localX = if (startsAtFarPrimaryEdge) primaryAxisLength - i - 1 else i

                // Serpentine wiring reverses direction on every other pass along
                // the secondary axis, so the strip snakes back and forth instead
                // of jumping back to the start each time.
                if (panel.serpentine && j % 2 == 1) {
                    localX = primaryAxisLength - localX - 1
                }

                val globalX = panel.xOffset + if (panel.vertical) localY else localX
                val globalY = panel.yOffset + if (panel.vertical) localX else localY

                val gapValue = effectiveGaps?.get(globalY * width + globalX)
                if (gapValue == null || gapValue > 0) {
                    cells[globalY][globalX] = ledIndex
                }
                if (gapValue == null || gapValue >= 0) {
                    ledIndex++
                }
            }
        }
    }

    return Wall(width = width, height = height, cells = cells)
}
