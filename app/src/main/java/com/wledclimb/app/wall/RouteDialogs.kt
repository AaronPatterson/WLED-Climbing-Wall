package com.wledclimb.app.wall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wledclimb.app.R

/**
 * Names a route on the way to saving it.
 *
 * [canUpdate] is true when a saved route is open, which is what makes two
 * buttons necessary: editing a route and wanting to keep both versions is as
 * ordinary as wanting to replace it, and a single "save" would have to guess.
 * With nothing open there is only one thing it can mean.
 */
@Composable
fun SaveRouteDialog(
    initialName: String,
    canUpdate: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, asNew: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val named = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.routes_save_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.routes_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (canUpdate) {
                TextButton(enabled = named, onClick = { onSave(name.trim(), false) }) {
                    Text(stringResource(R.string.routes_update))
                }
            } else {
                TextButton(enabled = named, onClick = { onSave(name.trim(), true) }) {
                    Text(stringResource(R.string.routes_save))
                }
            }
        },
        dismissButton = {
            if (canUpdate) {
                TextButton(enabled = named, onClick = { onSave(name.trim(), true) }) {
                    Text(stringResource(R.string.routes_save_new))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.routes_cancel))
                }
            }
        }
    )
}

@Composable
fun RenameRouteDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.routes_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.routes_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onRename(name.trim()) }) {
                Text(stringResource(R.string.routes_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.routes_cancel)) }
        }
    )
}

/**
 * Confirms a delete.
 *
 * Asked for because deleting is the one route action that cannot be undone -
 * there is no bin to fish it back out of, and the holds are not enough to
 * rebuild it from once the wall moves on.
 */
@Composable
fun DeleteRouteDialog(
    name: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.routes_delete_title, name)) },
        text = { Text(stringResource(R.string.routes_delete_body)) },
        confirmButton = {
            TextButton(onClick = onDelete) { Text(stringResource(R.string.routes_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.routes_cancel)) }
        }
    )
}
