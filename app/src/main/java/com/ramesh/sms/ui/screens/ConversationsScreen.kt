package com.ramesh.sms.ui.screens

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState as animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.ramesh.sms.ChatNav
import com.ramesh.sms.R
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.data.util.JalaliScope
import com.ramesh.sms.data.model.SearchMessageResult
import androidx.compose.ui.semantics.Role
import com.ramesh.sms.data.model.SenderTab
import com.ramesh.sms.theme.AppearanceStore
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.SquircleCardShape
import com.ramesh.sms.ui.components.AddToTabDialog
import com.ramesh.sms.ui.components.ContactProfileDialog
import com.ramesh.sms.ui.components.ConversationAvatar
import com.ramesh.sms.ui.components.CustomizationSheet
import com.ramesh.sms.ui.components.DefaultSmsBanner
import com.ramesh.sms.ui.components.DeleteTabDialog
import com.ramesh.sms.ui.components.SenderTabRow
import com.ramesh.sms.ui.components.SimBadge
import com.ramesh.sms.ui.components.SimFilterRow
import com.ramesh.sms.ui.components.TabEditorDialog
import com.ramesh.sms.ui.components.TabOptionsSheet
import com.ramesh.sms.ui.util.SmsDateFormats
import com.ramesh.sms.ui.util.senderDisplayName
import com.ramesh.sms.ui.util.contentAware
import com.ramesh.sms.ui.viewmodel.ConversationsViewModel
import com.ramesh.sms.ui.viewmodel.UNDO_DELETE_WINDOW_MILLIS
import com.ramesh.sms.util.DefaultSmsAppHelper
import java.util.Locale

private val ListBottomFabPadding = 88.dp
private val AvatarDividerInset = 78.dp
private val NoSelectionIds: Set<Long> = emptySet()

/** Sentinel used by the tab editor to mean "creating a brand new tab". */
private const val NEW_TAB_ID = -1L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onNavigateToChat: (ChatNav) -> Unit,
    onNavigateToFilterStudio: () -> Unit,
    onNavigateToCompose: () -> Unit,
    onNavigateToTestLab: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ConversationsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedThreadIds = viewModel.selectedThreadIds
    val isSelectionMode = viewModel.isSelectionMode
    val context = LocalContext.current

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDefaultSmsStatus()
        viewModel.loadThreads(force = true)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onInboxResumed()
    }

    val openChat: (ChatNav) -> Unit = { nav ->
        viewModel.clearUnreadForThread(nav.threadId)
        onNavigateToChat(nav)
    }

    val simSlotMap = remember(state.availableSims) {
        state.availableSims.associate { it.subscriptionId to it.slotNumber }
    }
    val hasMultipleSims = state.availableSims.size > 1

    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    // null = editor closed, NEW_TAB_ID = creating a tab, otherwise the id of the tab being edited.
    var tabEditorId by remember { mutableStateOf<Long?>(null) }
    var tabToDelete by remember { mutableStateOf<SenderTab?>(null) }
    var tabForOptionsMenu by remember { mutableStateOf<SenderTab?>(null) }
    var showAddToTabDialog by remember { mutableStateOf(false) }
    var activeProfileDialogInfo by remember { mutableStateOf<ProfileDialogData?>(null) }
    var showCustomizationSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    // The search field is hidden behind a magnifier button until the user asks for it. A query
    // that survived a trip to a chat keeps the field open, so results are never shown without it.
    var isSearchOpen by remember { mutableStateOf(state.searchQuery.isNotBlank()) }
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            // Let the field enter the composition before asking for focus.
            delay(120)
            runCatching { searchFocusRequester.requestFocus() }
        }
    }
    val swipeToDelete = AppearanceStore.current.swipeToDelete
    val undoScope = rememberCoroutineScope()
    // Long press opens the same actions the swipe reveals, so they stay reachable on devices
    // where the system back gesture eats edge swipes.
    var threadOptions by remember { mutableStateOf<ConversationThread?>(null) }

    // A refused provider write used to look like "the swipe does nothing"; surface it instead.
    LaunchedEffect(state.deleteFailureTick) {
        if (state.deleteFailureTick > 0L) {
            Toast.makeText(context, R.string.inbox_delete_failed, Toast.LENGTH_LONG).show()
        }
    }

    // Deleting waits five seconds so the undo bar stays usable; nothing is lost if the user
    // changes their mind.
    val requestDeleteConversation: (ConversationThread) -> Unit = { thread ->
        viewModel.requestDeleteConversation(thread.threadId)
        undoScope.launch {
            val dismissJob = launch {
                delay(UNDO_DELETE_WINDOW_MILLIS)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.inbox_conversation_deleted),
                actionLabel = context.getString(R.string.inbox_undo),
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                dismissJob.cancel()
                viewModel.undoDeleteConversation(thread.threadId)
            }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }
    BackHandler(enabled = isSearchOpen && !isSelectionMode) {
        viewModel.onSearchQueryChanged("")
        isSearchOpen = false
    }

    // "Contacts only" is a filter of the All tab; custom tabs keep their own scope.
    val contactsOnlyActive = state.showContactsOnly && state.selectedTabId == null
    val currentlyVisibleThreads = remember(state.filteredThreads, contactsOnlyActive, state.pendingDeleteIds) {
        val visible = state.filteredThreads.filter { it.threadId !in state.pendingDeleteIds }
        if (contactsOnlyActive) visible.filter { it.isContact } else visible
    }
    val isDebuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    Scaffold(
        topBar = {
            ConversationsTopBar(
                isSelectionMode = isSelectionMode,
                selectedThreadIds = selectedThreadIds,
                visibleThreads = currentlyVisibleThreads,
                showContactsOnly = contactsOnlyActive,
                showContactsToggle = state.selectedTabId == null,
                threads = state.threads,
                isDebuggable = isDebuggable,
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAllThreads() },
                onMarkSelectedRead = {
                    viewModel.markSelectedConversationsRead { count ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.inbox_marked_read_count, count),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onDeleteSelected = { showBatchDeleteDialog = true },
                showTabs = state.showTabs,
                onAddToTab = { showAddToTabDialog = true },
                onOpenSearch = { isSearchOpen = true },
                onToggleContactsOnly = {
                    viewModel.toggleContactsOnly()
                    val msg = if (!contactsOnlyActive) {
                        context.getString(R.string.inbox_showing_contacts_only)
                    } else {
                        context.getString(R.string.inbox_showing_all)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onMarkAllRead = {
                    val hasUnread = state.threads.any { it.unreadCount > 0 }
                    if (hasUnread) {
                        viewModel.markAllConversationsRead {
                            Toast.makeText(
                                context,
                                context.getString(R.string.inbox_all_marked_read),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.inbox_all_already_read),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onNavigateToTestLab = onNavigateToTestLab,
                onNavigateToFilterStudio = onNavigateToFilterStudio,
                onOpenCustomization = { showCustomizationSheet = true },
                onOpenSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SmallFloatingActionButton(
                    onClick = onNavigateToCompose,
                    content = { Icon(Icons.Default.Edit, contentDescription = null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = !isSelectionMode && isSearchOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = {
                                Text(
                                    stringResource(R.string.inbox_search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.inbox_clear_search),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    // Closes the search and returns to the plain inbox.
                                    IconButton(
                                        onClick = {
                                            viewModel.onSearchQueryChanged("")
                                            isSearchOpen = false
                                        }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.inbox_close_search),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.contentAware(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isSelectionMode && state.showTabs,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SenderTabRow(
                    tabs = state.tabs,
                    selectedTabId = state.selectedTabId,
                    threads = state.threads,
                    allTabIndex = state.allTabIndex,
                    onSelectTab = { viewModel.selectTab(it) },
                    onCreateTabClick = { tabEditorId = NEW_TAB_ID },
                    onTabOptionsClick = { tabForOptionsMenu = it },
                    onReorderTabs = { viewModel.reorderTabs(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = !isSelectionMode && hasMultipleSims && state.showSimFilter,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SimFilterRow(
                    availableSims = state.availableSims,
                    selectedSubId = state.selectedSimFilterSubId,
                    threads = state.threads,
                    onSelectSubId = { viewModel.selectSimFilter(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!state.hasSmsPermission) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sms_permission_needed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                val settingsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(settingsIntent)
                            }
                        ) {
                            Text(stringResource(R.string.open_settings))
                        }
                    }
                }
            }

            val loadError = state.error
            if (loadError != null && state.hasSmsPermission) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = loadError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.loadThreads(force = true) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            if (!state.isDefaultSmsApp) {
                DefaultSmsBanner(
                    onRequestDefault = {
                        DefaultSmsAppHelper.requestDefaultSmsApp(context) { intent ->
                            defaultSmsLauncher.launch(intent)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val isSearchActive = state.searchQuery.isNotBlank()
            val displayedThreads = currentlyVisibleThreads
            val displayedMessages = remember(state.matchingMessages, contactsOnlyActive) {
                if (contactsOnlyActive) {
                    state.matchingMessages.filter { !it.contactName.isNullOrBlank() }
                } else {
                    state.matchingMessages
                }
            }

            if (state.isLoading && state.threads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (isSearchActive) {
                val hasResults = displayedThreads.isNotEmpty() || displayedMessages.isNotEmpty()

                if (state.isSearching && !hasResults) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (!hasResults) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.inbox_no_messages),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (contactsOnlyActive) "No contact messages match \"${state.searchQuery}\"" else "No messages match \"${state.searchQuery}\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (contactsOnlyActive) "Try a different keyword or toggle off the contacts filter" else "Try searching for a different keyword or contact name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = ListBottomFabPadding)
                    ) {
                        if (displayedThreads.isNotEmpty()) {
                            item(key = "header_conversations") {
                                SearchSectionHeader(
                                    title = "CONVERSATIONS",
                                    count = displayedThreads.size
                                )
                            }
                            items(
                                items = displayedThreads,
                                key = { "thread_${it.threadId}" },
                                contentType = { "thread" }
                            ) { thread ->
                                SwipeToDeleteContainer(
                                    enabled = swipeToDelete,
                                    onDelete = { requestDeleteConversation(thread) },
                                    onMarkRead = { viewModel.clearUnreadForThread(thread.threadId) }
                                ) {
                                    ConversationItem(
                                        thread = thread,
                                        selectedIds = NoSelectionIds,
                                        isSelectionMode = false,
                                        simSlotNumber = if (hasMultipleSims && state.showSimBadge) simSlotMap[thread.subId] else null,
                                        useShamsi = state.jalaliScope == JalaliScope.EVERYWHERE,
                                        onClick = {
                                            openChat(
                                                ChatNav(
                                                    threadId = thread.threadId,
                                                    address = thread.address,
                                                    contactName = thread.contactName
                                                )
                                            )
                                        },
                                        onLongClick = { threadOptions = thread },
                                        onAvatarClick = {
                                            activeProfileDialogInfo = ProfileDialogData(
                                                threadId = thread.threadId,
                                                address = thread.address,
                                                contactName = thread.contactName,
                                                photoUri = thread.photoUri,
                                                contactLookupUri = thread.contactLookupUri,
                                                isContact = thread.isContact
                                            )
                                        }
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = AvatarDividerInset, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            }
                        }

                        if (displayedMessages.isNotEmpty()) {
                            item(key = "header_messages") {
                                SearchSectionHeader(
                                    title = "MESSAGES",
                                    count = displayedMessages.size
                                )
                            }
                            items(
                                items = displayedMessages,
                                key = { "msg_${it.messageId}" },
                                contentType = { "search_msg" }
                            ) { msg ->
                                SearchMessageResultItem(
                                    item = msg,
                                    searchQuery = state.searchQuery,
                                    simSlotNumber = if (hasMultipleSims) simSlotMap[msg.subId] else null,
                                    useShamsi = state.jalaliScope == JalaliScope.EVERYWHERE,
                                    appLanguage = state.appLanguage,
                                    onClick = {
                                        openChat(
                                            ChatNav(
                                                threadId = msg.threadId,
                                                address = msg.address,
                                                contactName = msg.contactName,
                                                initialMessageId = msg.messageId
                                            )
                                        )
                                    },
                                    onAvatarClick = {
                                        activeProfileDialogInfo = ProfileDialogData(
                                            threadId = msg.threadId,
                                            address = msg.address,
                                            contactName = msg.contactName,
                                            photoUri = msg.photoUri,
                                            contactLookupUri = msg.contactLookupUri,
                                            isContact = !msg.contactName.isNullOrBlank()
                                        )
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = AvatarDividerInset, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }
            } else if (displayedThreads.isEmpty()) {
                val selectedTab = if (state.showTabs) state.selectedTabId?.let { id -> state.tabs.find { it.id == id } } else null
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedTab != null) Icons.Default.FolderOpen
                                    else if (contactsOnlyActive) Icons.Default.Contacts
                                    else Icons.Default.Shield,
                                    contentDescription = stringResource(R.string.inbox_no_messages),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab != null) "No conversations in \"${selectedTab.name}\""
                            else if (contactsOnlyActive) "No contacts found"
                            else "No SMS messages found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedTab != null) {
                                if (selectedTab.senderAddresses.isEmpty()) {
                                    "No senders have been added to this tab yet"
                                } else {
                                    "None of the senders in this tab have messages in your inbox"
                                }
                            }
                            else if (contactsOnlyActive) "Conversations with saved contacts will appear here"
                            else "Incoming SMS messages will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (selectedTab != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(
                                onClick = { tabEditorId = selectedTab.id },
                                shape = PillShape
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.inbox_manage_senders), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                ConversationsInboxList(
                    threads = displayedThreads,
                    selectedIds = selectedThreadIds,
                    isSelectionMode = isSelectionMode,
                    simSlotMap = simSlotMap,
                    hasMultipleSims = hasMultipleSims,
                    showSimBadge = state.showSimBadge,
                    useShamsi = state.jalaliScope == JalaliScope.EVERYWHERE,
                    appLanguage = state.appLanguage,
                    onOpenChat = openChat,
                    onToggleSelect = viewModel::toggleSelectThread,
                    onEnterSelection = viewModel::enterSelectionMode,
                    onAvatarClick = { thread ->
                        activeProfileDialogInfo = ProfileDialogData(
                            threadId = thread.threadId,
                            address = thread.address,
                            contactName = thread.contactName,
                            photoUri = thread.photoUri,
                            contactLookupUri = thread.contactLookupUri,
                            isContact = thread.isContact
                        )
                    }
                )
            }
        }
    }

    threadOptions?.let { thread ->
        val optionsTitle = senderDisplayName(thread.contactName, thread.address)
        AlertDialog(
            onDismissRequest = { threadOptions = null },
            shape = SquircleCardShape,
            title = {
                Text(
                    text = optionsTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    ConversationActionRow(
                        icon = Icons.Default.MarkChatRead,
                        label = stringResource(R.string.inbox_mark_read)
                    ) {
                        threadOptions = null
                        viewModel.clearUnreadForThread(thread.threadId)
                    }
                    ConversationActionRow(
                        icon = Icons.Default.Delete,
                        label = stringResource(R.string.inbox_delete_conversation),
                        isDestructive = true
                    ) {
                        threadOptions = null
                        requestDeleteConversation(thread)
                    }
                    ConversationActionRow(
                        icon = Icons.Default.SelectAll,
                        label = stringResource(R.string.inbox_select_messages)
                    ) {
                        threadOptions = null
                        viewModel.enterSelectionMode(thread.threadId)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { threadOptions = null }) {
                    Text(stringResource(R.string.inbox_cancel))
                }
            }
        )
    }

    if (showBatchDeleteDialog) {
        BatchDeleteConversationsDialog(
            selectedThreadIds = selectedThreadIds,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = { deletedCount ->
                Toast.makeText(
                    context,
                    context.getString(R.string.inbox_deleted_count, deletedCount),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDelete = { onComplete -> viewModel.deleteSelectedConversations(onComplete) }
        )
    }

    tabEditorId?.let { editingId ->
        val liveTab = if (editingId == NEW_TAB_ID) null else state.tabs.find { it.id == editingId }
        if (editingId == NEW_TAB_ID || liveTab != null) {
            TabEditorDialog(
                initialTab = liveTab,
                allThreads = state.threads,
                onDismiss = { tabEditorId = null },
                onSave = { name, addresses, filterMask ->
                    if (liveTab == null) {
                        viewModel.createTab(name, addresses, filterMask) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.inbox_tab_created, name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        viewModel.saveTab(liveTab.id, name, addresses, filterMask)
                        Toast.makeText(
                            context,
                            context.getString(R.string.inbox_tab_saved, name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    tabEditorId = null
                }
            )
        }
    }

    tabForOptionsMenu?.let { tab ->
        TabOptionsSheet(
            tab = tab,
            onDismiss = { tabForOptionsMenu = null },
            onEdit = {
                tabEditorId = tab.id
                tabForOptionsMenu = null
            },
            onDelete = {
                tabToDelete = tab
                tabForOptionsMenu = null
            }
        )
    }

    tabToDelete?.let { tab ->
        DeleteTabDialog(
            tabName = tab.name,
            onDismiss = { tabToDelete = null },
            onConfirm = {
                viewModel.deleteTab(tab.id)
                Toast.makeText(
                    context,
                    context.getString(R.string.inbox_tab_deleted, tab.name),
                    Toast.LENGTH_SHORT
                ).show()
                tabToDelete = null
            }
        )
    }

    if (showAddToTabDialog) {
        AddToTabDialog(
            selectedCount = selectedThreadIds.size,
            tabs = state.tabs,
            onDismiss = { showAddToTabDialog = false },
            onAssignToTabs = { tabIds ->
                viewModel.addSelectedToTabs(tabIds) { count ->
                    Toast.makeText(
                        context,
                        context.getString(R.string.inbox_added_to_tab_count, count),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showAddToTabDialog = false
            },
            onCreateNewTab = {
                showAddToTabDialog = false
                tabEditorId = NEW_TAB_ID
            }
        )
    }

    activeProfileDialogInfo?.let { profile ->
        ContactProfileDialog(
            address = profile.address,
            contactName = profile.contactName,
            photoUri = profile.photoUri,
            contactLookupUri = profile.contactLookupUri,
            isContact = profile.isContact,
            onDismiss = { activeProfileDialogInfo = null },
            onOpenInfo = {
                activeProfileDialogInfo = null
                openChat(
                    ChatNav(
                        threadId = profile.threadId,
                        address = profile.address,
                        contactName = profile.contactName
                    )
                )
            }
        )
    }

    if (showCustomizationSheet) {
        CustomizationSheet(
            showTabs = state.showTabs,
            onToggleShowTabs = { enabled ->
                viewModel.setShowTabsEnabled(enabled)
            },
            showSimFilter = state.showSimFilter,
            onToggleShowSimFilter = { enabled ->
                viewModel.setShowSimFilterEnabled(enabled)
            },
            noSpamForContacts = state.noSpamForContacts,
            onToggleNoSpamForContacts = { enabled ->
                viewModel.setNoSpamForContactsEnabled(enabled)
            },
            enableDateTapShamsiToggle = state.enableDateTapShamsiToggle,
            onToggleDateTapShamsi = { enabled ->
                viewModel.setDateTapShamsiEnabled(enabled)
            },
            showNotificationDeleteAction = state.showNotificationDeleteAction,
            onToggleNotificationDeleteAction = { enabled ->
                viewModel.setNotificationDeleteActionEnabled(enabled)
            },
            onOpenSettings = {
                showCustomizationSheet = false
                onNavigateToSettings()
            },
            onDismiss = { showCustomizationSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationsTopBar(
    isSelectionMode: Boolean,
    selectedThreadIds: Set<Long>,
    visibleThreads: List<ConversationThread>,
    showContactsOnly: Boolean,
    showContactsToggle: Boolean = true,
    onOpenSearch: () -> Unit = {},
    threads: List<ConversationThread>,
    isDebuggable: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onMarkSelectedRead: () -> Unit,
    onDeleteSelected: () -> Unit,
    showTabs: Boolean = true,
    onAddToTab: () -> Unit = {},
    onToggleContactsOnly: () -> Unit,
    onMarkAllRead: () -> Unit,
    onNavigateToTestLab: () -> Unit,
    onNavigateToFilterStudio: () -> Unit,
    onOpenCustomization: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (isSelectionMode) {
        val selectedCount = selectedThreadIds.size
        val allFilteredSelected = visibleThreads.isNotEmpty() &&
                selectedThreadIds.containsAll(visibleThreads.map { it.threadId })
        TopAppBar(
            title = {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.inbox_cancel_selection),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = if (allFilteredSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = if (allFilteredSelected) "Deselect all" else "Select all",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (showTabs) {
                    IconButton(onClick = onAddToTab) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = stringResource(R.string.inbox_add_to_tab),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onMarkSelectedRead) {
                    Icon(
                        imageVector = Icons.Default.MarkChatRead,
                        contentDescription = stringResource(R.string.inbox_mark_as_read),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.inbox_delete_selected),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            windowInsets = TopAppBarDefaults.windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    } else {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.sms_inbox),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            actions = {
                val contactsButtonBg by animateColorAsState(
                    targetValue = if (showContactsOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    animationSpec = spring(),
                    label = "contactsToggleBg"
                )
                val contactsIconTint by animateColorAsState(
                    targetValue = if (showContactsOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    animationSpec = spring(),
                    label = "contactsToggleTint"
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.inbox_search),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showContactsToggle) {
                    Surface(
                        shape = CircleShape,
                        color = contactsButtonBg,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        IconButton(onClick = onToggleContactsOnly) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = if (showContactsOnly) "Showing contacts only (Tap to show all)" else "Filter by contacts only",
                                tint = contactsIconTint
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    IconButton(onClick = onNavigateToFilterStudio) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = stringResource(R.string.inbox_filter_studio),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Textra keeps its top bar tidy: everything else lives in an overflow menu.
                var showOverflowMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.padding(start = 2.dp, end = 4.dp)) {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.inbox_more_options),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.inbox_customize)) },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenCustomization()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.inbox_settings)) },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSettings()
                            }
                        )
                        if (threads.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.inbox_mark_all_read)) },
                                leadingIcon = {
                                    Icon(Icons.Default.MarkChatRead, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onMarkAllRead()
                                }
                            )
                        }
                        if (isDebuggable) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.inbox_test_lab)) },
                                leadingIcon = {
                                    Icon(Icons.Default.BugReport, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToTestLab()
                                }
                            )
                        }
                    }
                }
            },
            windowInsets = TopAppBarDefaults.windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun ConversationsInboxList(
    threads: List<ConversationThread>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    simSlotMap: Map<Int, Int> = emptyMap(),
    hasMultipleSims: Boolean = false,
    showSimBadge: Boolean = true,
    useShamsi: Boolean = false,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    onOpenChat: (ChatNav) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onAvatarClick: (ConversationThread) -> Unit
) {
    val showAvatars = AppearanceStore.current.showInboxAvatars
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ListBottomFabPadding)
    ) {
        items(
            items = threads,
            key = { it.threadId },
            contentType = { "thread" }
        ) { thread ->
            ConversationItem(
                thread = thread,
                selectedIds = selectedIds,
                isSelectionMode = isSelectionMode,
                showAvatar = showAvatars,
                simSlotNumber = if (hasMultipleSims && showSimBadge) simSlotMap[thread.subId] else null,
                useShamsi = useShamsi,
                appLanguage = appLanguage,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect(thread.threadId)
                    } else {
                        onOpenChat(
                            ChatNav(
                                threadId = thread.threadId,
                                address = thread.address,
                                contactName = thread.contactName
                            )
                        )
                    }
                },
                onLongClick = {
                    if (isSelectionMode) {
                        onToggleSelect(thread.threadId)
                    } else {
                        onEnterSelection(thread.threadId)
                    }
                },
                onAvatarClick = {
                    onAvatarClick(thread)
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(
                    start = if (showAvatars) AvatarDividerInset else 16.dp,
                    end = 16.dp
                ),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun BatchDeleteConversationsDialog(
    selectedThreadIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onDelete: ((Int) -> Unit) -> Unit
) {
    val count = selectedThreadIds.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.inbox_delete_confirm_title, count),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.inbox_delete_confirm_body, count),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onDelete { deletedCount -> onConfirm(deletedCount) }
                }
            ) {
                Text(
                    stringResource(R.string.inbox_delete),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.inbox_cancel))
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    thread: ConversationThread,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showAvatar: Boolean = true,
    simSlotNumber: Int? = null,
    useShamsi: Boolean = false,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
    onAvatarClick: (() -> Unit)? = null
) {
    val isSelected by remember(thread.threadId) {
        derivedStateOf { thread.threadId in selectedIds }
    }
    val displayName = senderDisplayName(thread.contactName, thread.address)

    val itemBgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
    } else {
        Color.Transparent
    }
    val dateText = remember(thread.date, useShamsi, appLanguage) {
        SmsDateFormats.conversationList(thread.date, useShamsi, appLanguage)
    }
    val isLastReceivedSpam = thread.isLastReceivedSpam

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLastReceivedSpam) 0.46f else 1f)
            .background(itemBgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAvatar || isSelectionMode) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.inbox_selected),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                ) {
                    ConversationAvatar(
                        address = thread.address,
                        contactName = thread.contactName,
                        photoUri = thread.photoUri,
                        size = 48.dp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (onAvatarClick != null) {
                                Modifier.clickable(
                                    role = Role.Button,
                                    onClick = onAvatarClick
                                )
                            } else Modifier
                        )
                ) {
                    ConversationAvatar(
                        address = thread.address,
                        contactName = thread.contactName,
                        photoUri = thread.photoUri,
                        size = 48.dp
                    )
                }
            }
        }
        }

        if (showAvatar || isSelectionMode) {
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.contentAware(),
                    fontWeight = if (thread.unreadCount > 0 && !thread.isUnreadSpam) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // The timestamp block owns the end of the row, so it stays flush with the edge in
                // both LTR and RTL layouts.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (simSlotNumber != null) {
                        SimBadge(slotNumber = simSlotNumber)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (thread.unreadCount > 0 && !thread.isUnreadSpam) FontWeight.Bold else FontWeight.Normal,
                        color = if (thread.unreadCount > 0 && !thread.isUnreadSpam) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = thread.snippet,
                    style = MaterialTheme.typography.bodyMedium.contentAware(),
                    color = if (thread.unreadCount > 0 && !thread.isUnreadSpam) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (thread.unreadCount > 0 && !thread.isUnreadSpam) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (thread.unreadCount > 0 && !isSelectionMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (thread.isUnreadSpam) {
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = stringResource(R.string.comp_spam),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = thread.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SearchMessageResultItem(
    item: SearchMessageResult,
    searchQuery: String,
    onClick: () -> Unit,
    simSlotNumber: Int? = null,
    onAvatarClick: (() -> Unit)? = null,
    useShamsi: Boolean = false,
    appLanguage: AppLanguage = AppLanguage.SYSTEM
) {
    val displayName = senderDisplayName(item.contactName, item.address)

    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val annotatedSnippet = remember(item.body, searchQuery, primaryColor, textColor) {
        buildAnnotatedSearchSnippet(item.body, searchQuery, primaryColor, textColor)
    }
    val dateText = remember(item.date, useShamsi, appLanguage) {
        SmsDateFormats.conversationList(item.date, useShamsi, appLanguage)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (onAvatarClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onAvatarClick)
                    } else Modifier
                )
        ) {
            ConversationAvatar(
                address = item.address,
                contactName = item.contactName,
                photoUri = item.photoUri,
                size = 48.dp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.contentAware(),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (simSlotNumber != null) {
                        SimBadge(slotNumber = simSlotNumber)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = annotatedSnippet,
                    style = MaterialTheme.typography.bodyMedium.contentAware(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (item.isSpam) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = stringResource(R.string.comp_spam),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

private fun buildAnnotatedSearchSnippet(
    body: String,
    query: String,
    primaryColor: Color,
    textColor: Color
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(body)

    val builder = AnnotatedString.Builder()
    val lowerBody = body.lowercase(Locale.getDefault())
    val lowerQuery = query.trim().lowercase(Locale.getDefault())

    var currentIndex = 0
    var matchIndex = lowerBody.indexOf(lowerQuery, currentIndex)

    while (matchIndex != -1) {
        if (matchIndex > currentIndex) {
            builder.append(body.substring(currentIndex, matchIndex))
        }
        val endIndex = matchIndex + lowerQuery.length
        builder.pushStyle(
            SpanStyle(
                color = primaryColor,
                fontWeight = FontWeight.ExtraBold,
                background = primaryColor.copy(alpha = 0.15f)
            )
        )
        builder.append(body.substring(matchIndex, endIndex))
        builder.pop()
        currentIndex = endIndex
        matchIndex = lowerBody.indexOf(lowerQuery, currentIndex)
    }

    if (currentIndex < body.length) {
        builder.append(body.substring(currentIndex))
    }

    return builder.toAnnotatedString()
}

private data class ProfileDialogData(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val photoUri: String? = null,
    val contactLookupUri: String? = null,
    val isContact: Boolean = !contactName.isNullOrBlank()
)

/**
 * Reveals an action button when a conversation row is dragged sideways: dragging towards one
 * side offers "mark as read", dragging towards the other offers "delete". Nothing is applied
 * until the button itself is tapped, so a mistimed gesture never destroys anything.
 */
@Composable
private fun SwipeToDeleteContainer(
    enabled: Boolean,
    onDelete: () -> Unit,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val revealPx = with(LocalDensity.current) { 84.dp.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    var targetOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloat(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 170),
        label = "swipeReveal"
    )
    val revealed = abs(animatedOffset) > 1f
    // Negative offset slides the row towards the physical left, so the exposed strip sits on the
    // physical right. Alignment.End/Start are direction aware, therefore the mapping flips in RTL.
    val showDelete = animatedOffset < 0f
    val actionAlignment = when {
        showDelete && isRtl -> Alignment.CenterStart
        showDelete -> Alignment.CenterEnd
        isRtl -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(modifier.fillMaxWidth()) {
        if (revealed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 10.dp)
                    .clickable { targetOffset = 0f },
                contentAlignment = actionAlignment
            ) {
                val container = if (showDelete) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
                val onContainer = if (showDelete) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
                Surface(
                    shape = CircleShape,
                    color = container,
                    contentColor = onContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    IconButton(
                        onClick = {
                            targetOffset = 0f
                            if (showDelete) onDelete() else onMarkRead()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = if (showDelete) Icons.Default.Delete else Icons.Default.MarkChatRead,
                            contentDescription = stringResource(
                                if (showDelete) R.string.inbox_delete_conversation else R.string.inbox_mark_read
                            ),
                            tint = onContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedOffset }
                .draggable(
                    state = rememberDraggableState { delta ->
                        targetOffset = (targetOffset + delta).coerceIn(-revealPx, revealPx)
                    },
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { },
                    onDragStopped = {
                        targetOffset = when {
                            abs(targetOffset) < revealPx / 2f -> 0f
                            targetOffset > 0f -> revealPx
                            else -> -revealPx
                        }
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }
        }
    }
}

/** One tappable row inside the conversation long-press sheet. */
@Composable
private fun ConversationActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
