package com.ramesh.sms.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ramesh.sms.R
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.model.SimInfo
import com.ramesh.sms.data.model.SmsMessage
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.theme.SquircleCardShape
import com.ramesh.sms.ui.util.SmsDateFormats

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
    // Typing narrows the list by contact name, number or the last message, so forward can find a
    // recipient without scrolling a hundred conversations.
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, targets) {
        val needle = query.trim()
        if (needle.isBlank()) {
            targets
        } else {
            targets.filter { thread ->
                thread.contactName?.contains(needle, ignoreCase = true) == true ||
                    thread.address.contains(needle, ignoreCase = true) ||
                    thread.snippet.contains(needle, ignoreCase = true)
            }
        }
    }
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
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.chat_forward_search)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    if (filtered.isEmpty()) {
                        Text(
                            text = stringResource(R.string.chat_forward_no_match),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(items = filtered, key = { it.threadId }) { thread ->
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
 * "Message details", laid out like the screen Textra shows.
 *
 * Everything here is metadata the provider already gave us - the type, which SIM carried the
 * message, the other party and the two timestamps. Nothing is fetched, so it opens instantly and
 * works offline like the rest of the app.
 */
@Composable
fun MessageDetailsDialog(
    message: SmsMessage,
    sim: SimInfo?,
    useShamsi: Boolean,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SquircleCardShape,
        title = {
            Text(
                text = stringResource(R.string.chat_details_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                DetailRow(
                    label = stringResource(R.string.chat_details_type),
                    value = stringResource(R.string.chat_details_type_sms)
                )
                DetailRow(
                    label = stringResource(R.string.chat_details_sim_slot),
                    value = sim?.badgeLabel ?: stringResource(R.string.chat_details_unknown)
                )
                DetailRow(
                    label = stringResource(R.string.chat_details_sim_name),
                    value = sim?.let { info ->
                        listOf(info.displayName, info.carrierName)
                            .firstOrNull { it.isNotBlank() }
                    } ?: stringResource(R.string.chat_details_unknown)
                )
                DetailRow(
                    label = stringResource(R.string.chat_details_to),
                    value = message.address
                )
                DetailRow(
                    label = stringResource(R.string.chat_details_sent),
                    value = SmsDateFormats.formatDateTimeWithYear(
                        message.date,
                        useShamsi,
                        appLanguage
                    )
                )
                // A received message has no delivery report of ours to show: the row only makes
                // sense for something this phone sent, so it is omitted rather than filled with a
                // dash.
                if (message.isSent) {
                    DetailRow(
                        label = stringResource(R.string.chat_details_delivered),
                        value = if (message.isDelivered) {
                            SmsDateFormats.formatDateTimeWithYear(
                                message.date,
                                useShamsi,
                                appLanguage
                            )
                        } else {
                            stringResource(R.string.chat_details_unknown)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_close))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
