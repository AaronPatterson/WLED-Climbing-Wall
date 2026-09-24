package com.wledclimb.app.wall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wledclimb.app.BuildConfig
import com.wledclimb.app.R

/**
 * Version and privacy policy, which used to sit at the bottom of the wall
 * screen competing with the grid for space.
 *
 * The version still matters: builds get sideloaded onto several devices, so
 * "which one is this?" needs an answer that does not involve a cable, and it is
 * the tell for an update that did not take. It just does not need to be on
 * screen at all times to do that job.
 *
 * Play's Families policy requires the privacy policy to be reachable from
 * inside the app. Two taps through a menu still counts as reachable; hidden
 * behind a gesture would not.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.about_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.wall_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(text = stringResource(R.string.about_privacy_policy))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.about_close))
            }
        }
    )
}
