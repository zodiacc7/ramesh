package com.ramesh.sms.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ramesh.sms.R
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.theme.SquircleCardShape

/**
 * "Forward to …" picker.
 *
 * The list is the cached conversations - the same snapshot the inbox paints from - so it opens
 * instantly and needs no provider query of its own. A forward is a normal SMS to another thread,
 * because the receiving side is another SMS app.
 */
@Composable
fun ForwardMessageDialog(
    targets: List<ConversationThread>,
    onDismiss: () -> Unit,
    onPick: (ConversationThread) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SquircleCardShape,
        title = {
            Text(
                text = stringResource(R.string.chat_menu_forward_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (targets.isEmpty()) {
                Text(
                    text = stringResource(R.string.chat_menu_forward_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(items = targets, key = { it.threadId }) { thread ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(thread) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Forward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = thread.contactName?.takeIf { it.isNotBlank() }
                                        ?: thread.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = thread.snippet,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_cancel))
            }
        }
    )
}

/**
 * "Report spam" confirmation, with the operator's report number.
 *
 * The number is stored the first time and reused afterwards; it stays editable because a user who
 * changes operator has to change it. Nothing is sent without this dialog, so a mis-tap on a long
 * message cannot quietly file a report in the user's name.
 */
@Composable
fun ReportSpamDialog(
    initialNumber: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var number by remember(initialNumber) { mutableStateOf(initialNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SquircleCardShape,
        title = {
            Text(
                text = stringResource(R.string.chat_menu_report_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.chat_menu_report_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.chat_menu_report_number)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(number) },
                enabled = number.isNotBlank()
            ) {
                Text(stringResource(R.string.chat_menu_report_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_cancel))
            }
        }
    )
}
