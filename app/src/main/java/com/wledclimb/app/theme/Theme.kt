package com.wledclimb.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Material 3's baseline palette, which is what the app was already getting by
// calling MaterialTheme with no arguments - the difference is that there's now
// a dark scheme too. Picking real brand colours is a design decision that can
// be made by overriding slots here without touching any screen.
private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

/**
 * App theme. Deliberately not using dynamic colour: the wall's on/off and hold
 * colours carry meaning, so a palette that changes with the user's wallpaper
 * would work against them.
 */
@Composable
fun WledClimbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
