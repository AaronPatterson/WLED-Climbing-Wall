package com.wledclimb.app.grid

/**
 * The climbing wall's grid: [width] x [height] cells, where each either has an
 * LED behind it or is empty. `cells[y][x]` holds that LED's position in the
 * physical wiring, or null where there's no hold.
 *
 * Two different indices are in play here and confusing them lights the wrong
 * holds, so they're named apart deliberately:
 *
 * - [ledIndexAt] is where the LED sits **along the strip**, which is what
 *   WLED's own ledmap is built from.
 * - [segmentIndexAt] is where the hold sits **in the grid** (`x + y * width`),
 *   which is what WLED's per-pixel JSON commands actually address - `"i"`
 *   writes into the segment's 2D buffer and WLED applies the ledmap itself
 *   when it renders (see `setPixelColorXYRaw` in FX.h).
 */
data class Wall(
    val width: Int,
    val height: Int,
    val cells: List<List<Int?>>
) {
    /** Position along the physical strip, or null where there's no hold. */
    fun ledIndexAt(x: Int, y: Int): Int? = cells.getOrNull(y)?.getOrNull(x)

    /** True where a hold can actually be lit. */
    fun hasHoldAt(x: Int, y: Int): Boolean = ledIndexAt(x, y) != null

    /** Position in WLED's 2D segment buffer - what per-pixel commands address. */
    fun segmentIndexAt(x: Int, y: Int): Int = x + y * width

    /** Size of that buffer, i.e. the range a "clear the whole wall" must cover. */
    val segmentSize: Int get() = width * height
}
