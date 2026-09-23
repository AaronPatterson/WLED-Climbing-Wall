package com.wledclimb.app.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
            // While a finger is down the slider owns its position, and the
            // wall's reported brightness is ignored.
            //
            // Requests are conflated, so a reply confirming an older value
            // arrives while the finger has already moved on. Feeding the
            // reported value straight back into the slider let those replies
            // drag it backwards - a visible stutter mid-drag, and at the top of
            // the range a deadlock, because every reply reset it to maximum
            // faster than a drag could move it away.
            // Seeded when the row opens and owned by the slider from then on.
            //
            // Earlier versions kept syncing this back from the wall's reported
            // brightness whenever a drag was thought to have ended, which was a
            // race: for a short movement onValueChangeFinished can land in the
            // same frame as onValueChange, before the new value has propagated,
            // so the thumb snapped back to where it started. A quick jab did
            // nothing at all while a slow deliberate drag sometimes survived -
            // which is exactly how it behaved.
            //
            // Nothing syncs it now. A brightness changed elsewhere while this
            // row is open will not be picked up until it is reopened, which is
            // the right trade: the value under someone's finger should not move
            // on its own.
            var position by remember(brightnessOpen) { mutableFloatStateOf(brightness.toFloat()) }
            // Reported across the usable range rather than against 255, so the
            // ends read 0% and 100%. The wall never actually reaches zero - see
            // MIN_USABLE_BRIGHTNESS - but a slider whose bottom says 3% looks
            // broken, and "as dim as this goes" is what the control means.
            val usableRange = (MAX_BRIGHTNESS - MIN_USABLE_BRIGHTNESS).toFloat()
            val percent = ((position - MIN_USABLE_BRIGHTNESS) / usableRange * 100).toInt()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    // Wider than the usual 16dp on purpose. At either end of its
                    // travel the thumb sits near a screen edge, and those edges
                    // belong to the system's back gesture.
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
                    // it would also give the wall a second way to be off.
                    valueRange = MIN_USABLE_BRIGHTNESS.toFloat()..MAX_BRIGHTNESS.toFloat(),
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        // Android's back gesture is driven from both screen
                        // edges, and it wins over whatever is drawn there. With
                        // the thumb at either end of its travel it sat inside
                        // that zone, so a drag became a back gesture and the
                        // control appeared stuck - most obviously at the top,
                        // where it could not be moved away at all without first
                        // tapping elsewhere on the track.
                        .systemGestureExclusion()
                        .semantics {
                            contentDescription = "Brightness $percent percent"
                        }
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    // Fixed width, or the slider's own width changes as the
                    // number does. The slider takes the space this label leaves,
                    // so dropping a digit widens it and slides the thumb out
                    // from under the finger - worst at the top of the range,
                    // where the first movement goes from four characters to
                    // three and shunts the thumb right as the drag pulls left.
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
}
