package com.wledclimb.app.wall

/**
 * The colours a hold can be lit in.
 *
 * A small fixed set rather than a colour wheel, per docs/design.md: the target
 * user is six, and picking from a handful of obvious colours is far easier
 * than steering a gradient. [hex] is the `RRGGBB` WLED expects.
 */
enum class HoldColor(val hex: String) {
    Red("FF0000"),
    Orange("FF6A00"),
    Yellow("FFD500"),
    Green("00C853"),
    Blue("2979FF"),
    Purple("AA00FF")
}
