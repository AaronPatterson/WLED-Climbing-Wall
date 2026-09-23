package com.wledclimb.app.wall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
                // Colour carries the state, so the button reports as well as
                // acts. The label still says which way it will go, because
                // colour alone is not an answer for anyone who cannot see it.
                IconButton(onClick = onToggle, enabled = enabled) {
                    Icon(
                        painter = painterResource(R.drawable.ic_power),
                        contentDescription = stringResource(
                            if (on) R.string.wall_turn_off else R.string.wall_turn_on
                        ),
                        tint = if (on) WallStatusColors.on else WallStatusColors.off,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        )

        if (brightnessOpen) {
            val percent = brightness * 100 / MAX_BRIGHTNESS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    // Never reaches zero. Brightness rising from zero is what
                    // makes WLED unfreeze its segments and drop the route, and
                    // it would also give the wall a second way to be off.
                    valueRange = MIN_USABLE_BRIGHTNESS.toFloat()..MAX_BRIGHTNESS.toFloat(),
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "Brightness $percent percent"
                        }
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
