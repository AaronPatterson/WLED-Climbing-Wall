package com.wledclimb.app.grid

/**
 * The climbing wall's LED grid: a [width] x [height] logical grid where each
 * cell either maps to a physical LED index (usable with WLED's per-LED JSON
 * control) or is empty (no panel covers that cell). `cells[y][x]` is the LED
 * index at that position, or null.
 */
data class Wall(
    val width: Int,
    val height: Int,
    val cells: List<List<Int?>>
) {
    fun ledIndexAt(x: Int, y: Int): Int? = cells.getOrNull(y)?.getOrNull(x)

    /**
     * One past the highest LED index in the grid - the range that covers every
     * hold, which is what a "clear the whole wall" command needs. Derived, so
     * it deliberately doesn't take part in equals/hashCode.
     */
    val ledCount: Int by lazy {
        cells.flatten().filterNotNull().maxOrNull()?.plus(1) ?: 0
    }
}
