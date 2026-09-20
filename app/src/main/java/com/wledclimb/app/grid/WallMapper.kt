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
 */
fun buildWall(panels: List<Panel>): Wall {
    val width = panels.maxOfOrNull { it.xOffset + it.width } ?: 0
    val height = panels.maxOfOrNull { it.yOffset + it.height } ?: 0

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

                cells[globalY][globalX] = ledIndex
                ledIndex++
            }
        }
    }

    return Wall(width = width, height = height, cells = cells)
}
