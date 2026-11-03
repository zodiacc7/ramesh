package com.ramesh.sms.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramesh.sms.R
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.model.SenderTab
import com.ramesh.sms.data.util.PhoneNumberKeys
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.SquircleCardShape

/**
 * Telegram-style tab editor: one screen that holds everything about a tab – its name, the
 * senders that belong to it and the "show only" content filters.
 *
 * [initialTab] is `null` when a new tab is being created.
 */
@Composable
fun TabEditorDialog(
    initialTab: SenderTab?,
    allThreads: List<ConversationThread>,
    onDismiss: () -> Unit,
    onSave: (name: String, addresses: List<String>, filterMask: Int) -> Unit
) {
    // Keyed on the tab id only, so background refreshes of the inbox never reset what the
    // user has typed or toggled in the dialog.
    val stableKey = initialTab?.id ?: -1L

    var name by remember(stableKey) { mutableStateOf(initialTab?.name.orEmpty()) }
    var mask by remember(stableKey) { mutableStateOf(initialTab?.filterMask ?: 0) }
    var searchQuery by remember(stableKey) { mutableStateOf("") }

    val senders = remember(allThreads) {
        allThreads.distinctBy { PhoneNumberKeys.canonical(it.address) }
    }

    val selection = remember(stableKey, senders) {
        val map = mutableStateMapOf<String, Boolean>()
        val existing = initialTab?.senderAddresses.orEmpty()
        for (sender in senders) {
            val canonical = PhoneNumberKeys.canonical(sender.address)
            map[canonical] = canonical in existing || PhoneNumberKeys.keys(sender.address).any { it in existing }
        }
        map
    }

    val visibleSenders = remember(senders, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) senders
        else senders.filter {
            it.address.contains(query, ignoreCase = true) ||
                it.contactName?.contains(query, ignoreCase = true) == true
        }
    }

    fun toggleFilter(maskToSet: Int, opposite: Int) {
        mask = if (mask and maskToSet != 0) {
            mask and maskToSet.inv()
        } else {
            (mask and opposite.inv()) or maskToSet
        }
    }

    val selectedCount = selection.count { it.value }
    val canSave = name.isNotBlank() && (selectedCount > 0 || mask != 0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Cap the dialog to the screen so the Cancel/Save row can never be pushed off-screen;
        // the body scrolls instead when there are many senders.
        val maxDialogHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.88f)
        Surface(
            shape = SquircleCardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .heightIn(max = maxDialogHeight)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                ) {
                Text(
                    text = stringResource(
                        if (initialTab == null) R.string.tab_new_title else R.string.tab_edit_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tab_name_label)) },
                    singleLine = true,
                    shape = SquircleCardShape,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.tab_show_only),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorChip(
                        label = stringResource(R.string.tab_filter_spam),
                        selected = mask and SenderTab.MASK_SPAM_ONLY != 0,
                        onClick = {
                            toggleFilter(SenderTab.MASK_SPAM_ONLY, SenderTab.MASK_NON_SPAM_ONLY)
                        }
                    )
                    EditorChip(
                        label = stringResource(R.string.tab_filter_not_spam),
                        selected = mask and SenderTab.MASK_NON_SPAM_ONLY != 0,
                        onClick = {
                            toggleFilter(SenderTab.MASK_NON_SPAM_ONLY, SenderTab.MASK_SPAM_ONLY)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorChip(
                        label = stringResource(R.string.tab_filter_contacts),
                        selected = mask and SenderTab.MASK_CONTACTS_ONLY != 0,
                        onClick = {
                            toggleFilter(SenderTab.MASK_CONTACTS_ONLY, SenderTab.MASK_NON_CONTACTS_ONLY)
                        }
                    )
                    EditorChip(
                        label = stringResource(R.string.tab_filter_unknown),
                        selected = mask and SenderTab.MASK_NON_CONTACTS_ONLY != 0,
                        onClick = {
                            toggleFilter(SenderTab.MASK_NON_CONTACTS_ONLY, SenderTab.MASK_CONTACTS_ONLY)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tab_senders_selected, selectedCount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedCount > 0) {
                        TextButton(onClick = { selection.replaceAll(false) }) {
                            Text(stringResource(R.string.tab_clear))
                        }
                    }
                }

                Surface(shape = PillShape, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.tab_search_senders)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (visibleSenders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.tab_no_senders),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(visibleSenders, key = { it.threadId }) { sender ->
                            val canonical = PhoneNumberKeys.canonical(sender.address)
                            val checked = selection[canonical] == true
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = sender.contactName ?: sender.address,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = if (sender.contactName != null) {
                                    { Text(sender.address, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                } else null,
                                leadingContent = {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier.clickable {
                                    selection[canonical] = !(selection[canonical] ?: false)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (canSave) R.string.tab_editor_hint_ok else R.string.tab_editor_hint_need
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (canSave) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.tab_cancel)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val addresses = senderAddresses(
                                senders = senders,
                                selection = selection,
                                existing = initialTab?.senderAddresses.orEmpty()
                            )
                            onSave(name.trim(), addresses, mask)
                        },
                        enabled = canSave
                    ) {
                        Text(stringResource(R.string.tab_save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun senderAddresses(
    senders: List<ConversationThread>,
    selection: Map<String, Boolean>,
    existing: Set<String>
): List<String> {
    val canonicals = senders.map { PhoneNumberKeys.canonical(it.address) }
    val selected = canonicals.filter { selection[it] == true }
    // Senders that belong to the tab but have no conversation right now must survive the save.
    val orphaned = existing.filterNot { it in canonicals }
    return (selected + orphaned).distinct()
}

private fun androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>.replaceAll(
    value: Boolean
) {
    for (key in keys.toList()) this[key] = value
}

@Composable
private fun EditorChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = PillShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/** Bottom sheet shown for a tab: edit everything in one place, or delete it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabOptionsSheet(
    tab: SenderTab,
    sheetState: androidx.compose.material3.SheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    /** True once the delete row has been armed by a first tap. */
    deleteArmed: Boolean = false,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = tab.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            SheetRow(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.tab_row_edit),
                description = stringResource(R.string.tab_row_edit_desc),
                onClick = onEdit
            )
            // The bin fills in on the first tap; the second one deletes the tab. No dialog.
            SheetRow(
                icon = if (deleteArmed) Icons.Default.Delete else Icons.Default.DeleteOutline,
                label = stringResource(
                    if (deleteArmed) R.string.comp_delete_again else R.string.tab_row_delete
                ),
                description = null,
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = description?.let { { Text(it) } },
        leadingContent = { Icon(imageVector = icon, contentDescription = null, tint = tint) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}
