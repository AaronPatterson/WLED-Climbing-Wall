package com.wledclimb.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colours that carry meaning about the wall itself, which Material's scheme has
 * no slot for ("this is on" isn't primary, secondary or error).
 *
 * Each has a dark-theme variant: the light-theme green is too dark to read
 * against a dark surface.
 */
object WallStatusColors {

    /** The wall's LEDs are lit. */
    val on: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)

    /**
     * The wall's LEDs are off. Deliberately neutral rather than red - off is a
     * normal state, not a fault, and red is reserved for actual errors.
     */
    val off: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFFBDBDBD) else Color(0xFF616161)

    /** A grid cell that has an LED behind it. */
    val gridCell: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF90A4AE) else Color(0xFF546E7A)
}
