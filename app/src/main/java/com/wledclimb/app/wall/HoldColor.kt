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
 * Only the names are persisted - see RouteHolds - so these values can be
 * retuned without touching a saved route.
 */
enum class HoldColor(val hex: String) {
    Red("FF0000"),
    Orange("FFA000"),
    Yellow("FFC800"),
    Green("08FF00"),
    Blue("0000FF"),
    Purple("AA00FF")
}
