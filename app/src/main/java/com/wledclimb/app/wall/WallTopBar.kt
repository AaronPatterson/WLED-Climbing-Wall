package com.wledclimb.app.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.systemGestureExclusion
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
            // Seeded when the row opens and owned by the slider from then on.
            // Syncing it back from the wall was a race: for a short movement
            // onValueChangeFinished can land in the same frame as
            // onValueChange, before the new value has propagated, so the thumb
            // snapped back to where it started.
            var position by remember(brightnessOpen) { mutableFloatStateOf(brightness.toFloat()) }
            val percent = ((position - MIN_USABLE_BRIGHTNESS) / usableRange * 100).toInt()


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    // Wider than usual: with nothing flanking the slider its
                    // ends sit near the screen edges, which belong to the
                    // system's back gesture.
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
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
                    thumb = {
                        // A plain circle, because Material3's own thumb stretches
                        // into a pill while it is being dragged and settles back
                        // when released. That is deliberate on their part and it
                        // looks wrong here, on a control that sits still in a
                        // top bar rather than being the focus of a screen.
                        //
                        // Not, as an earlier version of this comment claimed,
                        // because the default thumb swallows the press. It does
                        // not: the drag failure was a touch target too small,
                        // and the size below is what fixed it.
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
                    modifier = Modifier
                        .weight(1f)
                        // Taller than the default, so a press that lands a
                        // little above or below the track still counts.
                        .height(48.dp)
                        // Android drives its back gesture from both screen
                        // edges and wins over whatever is drawn there. The step
                        // buttons used to hold the slider clear of them; with
                        // those gone it has to claim the area itself.
                        .systemGestureExclusion()
                        .semantics { contentDescription = "Brightness $percent percent" }
                )
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
