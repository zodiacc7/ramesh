package com.ramesh.sms.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ramesh.sms.R
import kotlinx.coroutines.delay

/**
 * True while the undo window opened at [undoUntil] is still running.
 *
 * The clock ticks on its own so the icon disappears by itself after the window closes, without the
 * screen having to schedule a removal of its own.
 */
@Composable
fun rememberUndoAvailable(undoUntil: Long): Boolean {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(undoUntil) {
        if (undoUntil <= 0L) return@LaunchedEffect
        while (System.currentTimeMillis() < undoUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    return undoUntil > 0L && now < undoUntil
}

/**
 * The single undo affordance of the app.
 *
 * Every deletion opens one 20-second window and this icon is the only way back: there is no undo
 * action in a snackbar, and no second, differently-worded "restore" anywhere else.
 */
@Composable
fun UndoIconButton(
    available: Boolean,
    onClick: () -> Unit,
    tint: Color? = null,
    contentDescription: String = stringResource(R.string.inbox_undo)
) {
    if (!available) return
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = contentDescription,
            tint = tint ?: MaterialTheme.colorScheme.primary
        )
    }
}
