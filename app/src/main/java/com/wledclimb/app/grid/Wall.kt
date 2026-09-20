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
}
