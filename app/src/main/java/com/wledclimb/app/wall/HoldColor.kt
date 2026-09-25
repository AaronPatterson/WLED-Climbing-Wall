package com.wledclimb.app.wall

/**
 * The colours a hold can be lit in.
 *
 * A small fixed set rather than a colour wheel, per docs/design.md: the target
 * user is six, and picking from a handful of obvious colours is far easier
 * than steering a gradient. [hex] is the `RRGGBB` WLED expects.
 *
 * The values are WLED's own quick-select swatches, from `wled00/data/index.htm`,
 * rather than colours chosen to look right on a screen. The two are not the
 * same thing: an LED is additive and far brighter than a display, so a hex that
 * reads as orange in a design tool can reach the wall looking red. Orange is
 * where that showed - `FF6A00` has green at 106 and lit as a warm red, while
 * WLED's `FFA000` has it at 160 and actually reads orange.
 *
 * Purple is ours. WLED's quick set has no purple; the nearest is magenta
 * (`FF00FF`), which is a different colour rather than a better-tuned one.
 *
 * Nothing about these values is persisted - see [RouteHolds], which stores a
 * hold's [slot] - so the palette can be retuned, or swapped wholesale, without
 * touching a saved route.
 */
enum class HoldColor(val hex: String) {
    Red("FF0000"),
    Orange("FFA000"),
    Yellow("FFC800"),
    Green("08FF00"),
    Blue("0000FF"),
    Purple("AA00FF");

    /**
     * Position in the palette, and the only thing a saved route records.
     *
     * A route stores which slot a hold uses rather than which colour, so the
     * palette supplies the colour when the route is drawn and pushed. That is
     * what lets a future palette change re-skin every saved route for nothing
     * - see Phase 15 in docs/design.md. It is the enum's ordinal today because
     * the enum *is* the palette; when palettes become data this is the one
     * place that has to learn where a slot really comes from.
     */
    val slot: Int get() = ordinal

    companion object {
        /** The colour in [slot], or null if the palette has no such slot. */
        fun atSlot(slot: Int): HoldColor? = entries.getOrNull(slot)
    }
}
