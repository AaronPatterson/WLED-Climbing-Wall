package com.wledclimb.app.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wledclimb.app.R
import com.wledclimb.app.network.MAX_BRIGHTNESS
import com.wledclimb.app.network.MIN_USABLE_BRIGHTNESS

/**
 * Names the wall and carries the two controls that belong to the whole wall
 * rather than to a route: power and brightness.
 *
 * Both used to sit in the content below, where they competed with the grid for
 * attention and for space. Up here they stay reachable from anywhere that shows
 * a wall, and the screen beneath is free to be about routes.
 *
 * Brightness hides behind its icon rather than sitting open. A slider wide
 * enough for a six-year-old costs a row of vertical space, which on a phone in
 * portrait is a row the grid wants more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallTopBar(
    name: String,
    on: Boolean,
    brightness: Int,
    enabled: Boolean,
    onToggle: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onChangeController: () -> Unit
) {
    var brightnessOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }

    if (aboutOpen) {
        AboutDialog(onDismiss = { aboutOpen = false })
    }

    Column {
        TopAppBar(
            title = { Text(text = name, style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                // Everything that is about the app rather than about the wall.
                // Small now, but it is where configuration grows, and keeping it
                // out of the content is what lets the screen below be about
                // routes.
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.wall_menu)
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wall_change_controller)) },
                        onClick = {
                            menuOpen = false
                            onChangeController()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.about_title)) },
                        onClick = {
                            menuOpen = false
                            aboutOpen = true
                        }
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { brightnessOpen = !brightnessOpen },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_brightness),
                        contentDescription = stringResource(R.string.wall_brightness)
                    )
                }
                // The whole button lights up rather than just the glyph. A
                // tinted outline was too quiet to answer "is the wall on?" from
                // across a garage, which is the one question this control exists
                // to answer without being tapped.
                //
                // The label still says which way it will go, because colour
                // alone is not an answer for anyone who cannot see it.
                val statusColour = if (on) WallStatusColors.on else WallStatusColors.off
                IconButton(onClick = onToggle, enabled = enabled) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            // Filled when on, hollow when off: the difference
                            // reads at a glance and does not rely on telling two
                            // colours apart.
                            .background(if (on) statusColour else Color.Transparent)
                            .border(width = 2.dp, color = statusColour, shape = CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_power),
                            contentDescription = stringResource(
                                if (on) R.string.wall_turn_off else R.string.wall_turn_on
                            ),
                            // On a filled circle the glyph has to contrast with
                            // the fill, not match it.
                            tint = if (on) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                statusColour
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        )

        if (brightnessOpen) {
            val usableRange = MAX_BRIGHTNESS - MIN_USABLE_BRIGHTNESS
            val step = usableRange / 10
            // Seeded when the row opens and owned by the slider from then on.
            // Syncing it back from the wall was a race: for a short movement
            // onValueChangeFinished can land in the same frame as
            // onValueChange, before the new value has propagated, so the thumb
            // snapped back to where it started.
            var position by remember(brightnessOpen) { mutableFloatStateOf(brightness.toFloat()) }
            val percent = ((position - MIN_USABLE_BRIGHTNESS) / usableRange * 100).toInt()

            fun nudge(by: Int) {
                val next = (position.toInt() + by)
                    .coerceIn(MIN_USABLE_BRIGHTNESS, MAX_BRIGHTNESS)
                position = next.toFloat()
                onBrightnessChange(next)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // The buttons are not decoration. Dragging is the nicer
                // gesture and the reason the slider is here, but it asks for
                // precision from people who may not have much, and this
                // control has already proved able to refuse a drag. A tap
                // always works.
                IconButton(onClick = { nudge(-step) }, enabled = enabled && position > MIN_USABLE_BRIGHTNESS) {
                    Icon(
                        painter = painterResource(R.drawable.ic_minus),
                        contentDescription = stringResource(R.string.wall_brightness_down)
                    )
                }
                Slider(
                    value = position,
                    onValueChange = {
                        position = it
                        onBrightnessChange(it.toInt())
                    },
                    // Never reaches zero. Brightness rising from zero is what
                    // makes WLED unfreeze its segments and drop the route, and
                    // it would give the wall a second way to be off.
                    valueRange = MIN_USABLE_BRIGHTNESS.toFloat()..MAX_BRIGHTNESS.toFloat(),
                    enabled = enabled,
                    track = { sliderState ->
                        // Thicker than the default 4dp. A taller track is a
                        // taller touch area, which costs nothing here and
                        // matters on a control that has to tolerate being
                        // grabbed by a six-year-old rather than aimed at.
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(14.dp)
                        )
                    },
                    thumb = {
                        // Drawn rather than SliderDefaults.Thumb, which wires
                        // itself to an interaction source for hover and
                        // indication and swallows a press that lands on it: a
                        // drag begun on the track moved, one begun on the thumb
                        // did nothing at all. Nothing here consumes pointer
                        // events, so the press reaches the slider's own drag
                        // handling.
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        // Taller than the default, so a press that lands a
                        // little above or below the track still counts.
                        .height(48.dp)
                        .semantics { contentDescription = "Brightness $percent percent" }
                )
                IconButton(onClick = { nudge(step) }, enabled = enabled && position < MAX_BRIGHTNESS) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = stringResource(R.string.wall_brightness_up)
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    // Fixed, or the slider's own width changes with the number
                    // and slides the thumb out from under the finger.
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
}
