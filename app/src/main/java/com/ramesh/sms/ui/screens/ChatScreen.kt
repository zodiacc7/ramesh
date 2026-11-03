package com.ramesh.sms.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramesh.sms.ChatNav
import com.ramesh.sms.R
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.model.SimInfo
import com.ramesh.sms.data.model.SmsMessage
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.data.util.findOtpHighlight
import com.ramesh.sms.data.util.foldSmsReactions
import com.ramesh.sms.theme.AppearanceStore
import com.ramesh.sms.theme.InputBarShape
import com.ramesh.sms.theme.MessageAnimation
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.RameshIcons
import com.ramesh.sms.theme.SquircleCardShape
import com.ramesh.sms.theme.messageEnterTransition
import com.ramesh.sms.theme.messageExitTransition
import com.ramesh.sms.ui.components.ChatBackdropBackground
import com.ramesh.sms.ui.components.ChatDateHeader
import com.ramesh.sms.ui.components.ContactProfileDialog
import com.ramesh.sms.ui.components.ConversationAvatar
import com.ramesh.sms.ui.components.ForwardMessageDialog
import com.ramesh.sms.ui.components.MessageActionBar
import com.ramesh.sms.ui.components.MessageBubble
import com.ramesh.sms.ui.components.MessageDetailsDialog
import com.ramesh.sms.ui.components.SimToggleButton
import com.ramesh.sms.ui.components.SmsSegmentCounter
import com.ramesh.sms.ui.components.SpamMessagePill
import com.ramesh.sms.ui.components.UndoIconButton
import com.ramesh.sms.ui.components.rememberUndoAvailable
import com.ramesh.sms.ui.util.ContactUtils
import com.ramesh.sms.ui.util.SmsDateFormats
import com.ramesh.sms.ui.util.contentAware
import com.ramesh.sms.ui.util.senderDisplayName
import com.ramesh.sms.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    nav: ChatNav,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel(
        key = "ChatViewModel_${nav.threadId}_${nav.address}_${nav.initialMessageId}"
    ) {
        ChatViewModel(
            application = context.applicationContext as android.app.Application,
            initialThreadId = nav.threadId,
            initialAddress = nav.address,
            initialContactName = nav.contactName,
            initialMessageId = nav.initialMessageId
        )
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val look = AppearanceStore.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.setScreenResumed(true)
        viewModel.loadMessages()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.setScreenResumed(false)
    }

    var inputText by remember(nav.threadId, nav.address) {
        mutableStateOf(nav.initialBody.orEmpty())
    }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showContactProfileDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val textToolbar = LocalTextToolbar.current
    var selectedMessageForDialog by remember { mutableStateOf<SmsMessage?>(null) }
    // A single tap on a bubble opens the action strip over that bubble; the rest is what it starts.
    var actionBarMessage by remember { mutableStateOf<SmsMessage?>(null) }
    var actionBarAnchor by remember { mutableStateOf<Rect?>(null) }
    var deleteArmed by remember { mutableStateOf(false) }
    // Kept separate from actionBarMessage so the strip can fade out before it is removed.
    var actionBarVisible by remember { mutableStateOf(true) }
    var detailsMessage by remember { mutableStateOf<SmsMessage?>(null) }
    // Bubble bounds, in root coordinates, recorded as the list lays out. A plain map on purpose:
    // this is read once per tap, so it must not trigger a recomposition on every scroll frame.
    val bubbleBounds = remember { mutableMapOf<Long, Rect>() }
    var contentOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var forwardBody by remember { mutableStateOf<String?>(null) }
    var forwardTargets by remember { mutableStateOf<List<ConversationThread>>(emptyList()) }
    // The bin in the selection bar works exactly like the one in the strip: first tap arms it
    // (filled, red), second tap deletes. No confirmation window.
    var selectionDeleteArmed by remember { mutableStateOf(false) }
    val selectedMessageIds = viewModel.selectedMessageIds
    val isSelectionMode = viewModel.isSelectionMode
    val undoAvailable = rememberUndoAvailable(state.undoUntil)
    LaunchedEffect(isSelectionMode, selectedMessageIds.size) {
        if (!isSelectionMode) selectionDeleteArmed = false
    }
    var activeTextBubbleId by remember { mutableStateOf<Long?>(null) }
    var selectionResetTick by remember { mutableStateOf(0) }
    // Copy turns the bubble itself into selectable text (all of it selected) instead of opening a
    // window over the conversation. The tick forces a re-selection when Copy is tapped again.
    var inlineSelectMessageId by remember { mutableStateOf<Long?>(null) }
    var inlineSelectTick by remember { mutableStateOf(0) }
    // Every action closes the strip the same way: fade out, then drop it. Removing it outright made
    // it pop while the fade-in on the way in was still visible.
    val closeActionBar: () -> Unit = {
        if (actionBarVisible || actionBarMessage != null) {
            actionBarVisible = false
            coroutineScope.launch {
                kotlinx.coroutines.delay(150)
                actionBarMessage = null
                deleteArmed = false
                actionBarVisible = true
            }
        }
    }

    // One clipboard path for the strip and the long-press dialog: same label, same toast.
    val copyToClipboard: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(context.getString(R.string.chat_sms_message), text)
        )
        Toast.makeText(context, context.getString(R.string.chat_copied), Toast.LENGTH_SHORT).show()
    }
    var hasInitiallyScrolled by remember(nav.threadId, nav.address, nav.initialMessageId) { mutableStateOf(false) }
    var highlightedMessageId by remember(nav.threadId, nav.address, nav.initialMessageId) {
        mutableStateOf(nav.initialMessageId)
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(highlightedMessageId, hasInitiallyScrolled) {
        if (highlightedMessageId != null && hasInitiallyScrolled) {
            kotlinx.coroutines.delay(2500)
            highlightedMessageId = null
        }
    }

    val oldestMessageId = state.messages.firstOrNull()?.id
    val newestMessageId = state.messages.lastOrNull()?.id
    var previousOldestId by remember(nav.threadId, nav.address) { mutableStateOf<Long?>(null) }
    var previousNewestId by remember(nav.threadId, nav.address) { mutableStateOf<Long?>(null) }

    LaunchedEffect(
        state.isLoading,
        oldestMessageId,
        newestMessageId,
        state.messages.size,
        nav.initialMessageId
    ) {
        if (state.isLoading || state.messages.isEmpty()) return@LaunchedEffect

        if (!hasInitiallyScrolled) {
            val initialId = nav.initialMessageId
            if (initialId != null) {
                val chronologicalIndex = state.messages.indexOfFirst { it.id == initialId }
                if (chronologicalIndex >= 0) {
                    // reverseLayout lists newest first, so invert the chronological index
                    listState.scrollToItem(state.messages.lastIndex - chronologicalIndex)
                }
            }
            hasInitiallyScrolled = true
            previousOldestId = oldestMessageId
            previousNewestId = newestMessageId
            return@LaunchedEffect
        }

        val prepended = oldestMessageId != previousOldestId
        val appended = newestMessageId != previousNewestId
        previousOldestId = oldestMessageId
        previousNewestId = newestMessageId
        if (appended && !prepended && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    val shouldLoadOlder by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= total - 5
        }
    }

    LaunchedEffect(shouldLoadOlder, state.hasMoreOlder, state.isLoadingOlder, state.isLoading, state.messages.size) {
        if (!state.isLoading && shouldLoadOlder && state.hasMoreOlder && !state.isLoadingOlder) {
            viewModel.loadOlderMessages()
        }
    }

    val isScrolledUp by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    val onRevealToggle = remember(viewModel) {
        { id: Long, revealed: Boolean -> viewModel.toggleSpamReveal(id, revealed) }
    }
    val onMarkNotSpam = remember(viewModel) {
        { id: Long -> viewModel.markMessageNotSpam(id) }
    }
    val onDeleteMessage = remember(viewModel) {
        { id: Long -> viewModel.deleteMessage(id) }
    }
    val onLongClickMessage = remember(viewModel) {
        { message: SmsMessage ->
            if (viewModel.isSelectionMode) {
                viewModel.toggleSelectMessage(message.id)
            } else {
                viewModel.enterSelectionMode(message.id)
            }
        }
    }
    val onTapMessage = remember(viewModel) {
        { message: SmsMessage -> viewModel.toggleSelectMessage(message.id) }
    }
    val onClearTextSelection = remember(textToolbar) {
        {
            inlineSelectMessageId = null
            if (textToolbar.status == TextToolbarStatus.Shown) {
                selectionResetTick += 1
                activeTextBubbleId = null
                textToolbar.hide()
            }
        }
    }
    // Reaction SMS ("Reacted 👍 to …") are attached to the message they quote, never shown as a
    // bubble of their own. Computed on the merged list so paging cannot split a reaction from its
    // target.
    val visibleMessages = remember(state.messages, state.pendingDeleteIds) {
        state.messages.filter { it.id !in state.pendingDeleteIds }
    }
    val foldedMessages = remember(visibleMessages) { foldSmsReactions(visibleMessages) }
    var reactionPickerMessageId by remember { mutableStateOf<Long?>(null) }
    val onShowInfoForSelected = remember(viewModel) {
        {
            val id = selectedMessageIds.singleOrNull()
            if (id != null) {
                selectedMessageForDialog = state.messages.find { it.id == id }
            }
        }
    }
    val performSend: () -> Unit = {
        if (inputText.isNotBlank() && !state.isSending) {
            val text = inputText
            inputText = ""
            viewModel.sendMessage(text) { result ->
                if (!result.sent) {
                    inputText = text
                    Toast.makeText(
                        context,
                        "Couldn't send message. Check signal and default SMS app.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (!result.storedInProvider) {
                    Toast.makeText(
                        context,
                        "Ramesh must be the default SMS app for sent messages to appear",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    val onDeleteSelected = remember(viewModel, context) {
        {
            viewModel.deleteSelectedMessages { count ->
                Toast.makeText(
                    context,
                    "Deleted $count message${if (count > 1) "s" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedMessageIds.size,
                    selectedIds = selectedMessageIds,
                    messages = visibleMessages,
                    onClose = { viewModel.clearSelection() },
                    onMarkRead = { viewModel.markSelectedMessagesRead() },
                    onShare = { bodies ->
                        if (bodies.isNotEmpty()) {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, bodies.joinToString("\n\n"))
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(
                                        share,
                                        context.getString(R.string.chat_menu_share)
                                    )
                                )
                            }
                        }
                        viewModel.clearSelection()
                    },
                    onForward = { bodies ->
                        if (bodies.isNotEmpty()) {
                            forwardBody = bodies.joinToString("\n\n")
                            coroutineScope.launch {
                                forwardTargets = viewModel.forwardTargets()
                            }
                        }
                        viewModel.clearSelection()
                    },
                    deleteArmed = selectionDeleteArmed,
                    onDelete = {
                        if (selectionDeleteArmed) {
                            selectionDeleteArmed = false
                            onDeleteSelected()
                        } else {
                            selectionDeleteArmed = true
                        }
                    },
                    onCopy = { bodies ->
                        if (bodies.isNotEmpty()) copyToClipboard(bodies.joinToString("\n\n"))
                        viewModel.clearSelection()
                    },
                    onInfo = onShowInfoForSelected,
                    onSelectAll = { viewModel.selectAllMessages() }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val opened = ContactUtils.openContactInfo(
                                        context = context,
                                        address = state.address,
                                        contactLookupUri = state.contactLookupUri
                                    )
                                    if (!opened) {
                                        showContactProfileDialog = true
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            ConversationAvatar(
                                address = state.address,
                                contactName = state.contactName,
                                photoUri = state.photoUri,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = senderDisplayName(state.contactName, state.address),
                                    style = MaterialTheme.typography.titleMedium.contentAware(),
                                    fontWeight = FontWeight.Bold
                                )
                                if (state.contactName != null) {
                                    Text(
                                        text = state.address,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.chat_back))
                        }
                    },
                    actions = {
                        // The one undo affordance: visible for 20 seconds after any deletion in
                        // this conversation, then gone on its own.
                        UndoIconButton(
                            available = undoAvailable,
                            onClick = { viewModel.undoPendingDeletions() }
                        )
                        // Call Button (visible only if sender address is a callable phone number)
                        if (ContactUtils.isCallable(state.address)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        ContactUtils.openDialer(context, state.address)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = stringResource(R.string.chat_call_contact),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.chat_participant_settings),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (state.pendingSendSecondsLeft > 0) {
                        PendingSendBar(
                            secondsLeft = state.pendingSendSecondsLeft,
                            onCancel = {
                                val restored = viewModel.cancelPendingSend()
                                if (!restored.isNullOrBlank()) {
                                    inputText = restored
                                }
                            }
                        )
                    }
                    if (look.showCharacterCounter) {
                        SmsSegmentCounter(text = inputText)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.chat_composer_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            textStyle = LocalTextStyle.current.contentAware(),
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (look.enterToSend) ImeAction.Send else ImeAction.Default
                            ),
                            keyboardActions = KeyboardActions(onSend = { performSend() }),
                            shape = InputBarShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            maxLines = 5
                        )

                        if (state.availableSims.size > 1) {
                            SimToggleButton(
                                availableSims = state.availableSims,
                                selectedSim = state.selectedSim,
                                onToggleSim = { viewModel.toggleSim() },
                                onSelectSim = { viewModel.selectSim(it) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = { performSend() },
                            enabled = inputText.isNotBlank() && !state.isSending,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                disabledContentColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.size(50.dp)
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.chat_send),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Where this box sits in the window, so a bubble's root-relative bounds can be turned
                // into a position for the action strip.
                .onGloballyPositioned { contentOrigin = it.boundsInRoot().topLeft }
        ) {
            ChatBackdropBackground(
                appearance = look,
                modifier = Modifier.matchParentSize()
            )
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.messages.isEmpty()) {
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
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.chat_no_messages_yet),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.chat_send_to_start),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                ChatMessageList(
                    // Fold reaction SMS onto the message they answer *here*, on the merged list.
                    // The repository folds each provider page separately, so a reaction that is on
                    // a later page than its target would otherwise reappear as its own bubble after
                    // paging or a refresh.
                    messages = foldedMessages,
                    availableSims = state.availableSims,
                    listState = listState,
                    highlightedMessageId = highlightedMessageId,
                    showShamsiDate = state.showShamsiDate,
                    appLanguage = state.appLanguage,
                    isLoadingOlder = state.isLoadingOlder,
                    enableDateTapShamsiToggle = state.enableDateTapShamsiToggle,
                    onToggleDateFormat = { viewModel.toggleShamsiDate() },
                    onRevealToggle = onRevealToggle,
                    onMarkNotSpam = onMarkNotSpam,
                    onDelete = onDeleteMessage,
                    onLongClick = onLongClickMessage,
                    isSelectionMode = isSelectionMode,
                    selectedMessageIds = selectedMessageIds,
                    showNewSpam = state.showNewSpam,
                    onTapMessage = onTapMessage,
                    onTapBubble = { message ->
                        deleteArmed = false
                        actionBarVisible = true
                        actionBarAnchor = bubbleBounds[message.id]?.translate(
                            -contentOrigin.x,
                            -contentOrigin.y
                        )
                        actionBarMessage = message
                    },
                    onBubbleBounds = { id, bounds -> bubbleBounds[id] = bounds },
                    activeTextBubbleId = activeTextBubbleId,
                    selectionResetTick = selectionResetTick,
                    inlineSelectMessageId = inlineSelectMessageId,
                    inlineSelectTick = inlineSelectTick,
                    onBubbleTouched = { id -> activeTextBubbleId = id },
                    onOutsideTap = onClearTextSelection,
                    address = state.address,
                    contactName = state.contactName,
                    photoUri = state.photoUri
                )
            }

            // M3E Scroll to Bottom Floating Action Button
            AnimatedVisibility(
                visible = isScrolledUp,
                enter = fadeIn() + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (state.messages.isNotEmpty()) {
                                listState.animateScrollToItem(0)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.chat_scroll_to_bottom),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // The action strip is a child of the chat itself, not a dialog: it can sit on top of the
            // bubble that was tapped and it never dims the conversation behind it.
            actionBarMessage?.let { message ->
                val anchorRect = actionBarAnchor
                if (anchorRect != null) {
                    MessageActionBar(
                        anchor = anchorRect,
                        visible = actionBarVisible,
                        otpCode = remember(message.id, message.body) {
                            findOtpHighlight(message.body)?.code
                        },
                        canReact = !message.isSent,
                        isSent = message.isSent,
                        deleteArmed = deleteArmed,
                        onCopyCode = {
                            closeActionBar()
                            copyToClipboard(findOtpHighlight(message.body)?.code.orEmpty())
                        },
                        onCopy = {
                            // Copy takes the text and hands the bubble back as selectable text with
                            // everything selected, so a part of a long message can be picked and
                            // copied from the same place. No window opens over the conversation.
                            copyToClipboard(message.body)
                            closeActionBar()
                            inlineSelectMessageId = message.id
                            inlineSelectTick += 1
                        },
                        onReact = {
                            closeActionBar()
                            reactionPickerMessageId = message.id
                        },
                        onShare = {
                            closeActionBar()
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message.body)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(
                                        share,
                                        context.getString(R.string.chat_menu_share)
                                    )
                                )
                            }
                        },
                        onForward = {
                            closeActionBar()
                            forwardBody = message.body
                            coroutineScope.launch { forwardTargets = viewModel.forwardTargets() }
                        },
                        onDelete = {
                            if (deleteArmed) {
                                closeActionBar()
                                viewModel.deleteMessage(message.id)
                            } else {
                                // First tap only opens the bin; the second one deletes.
                                deleteArmed = true
                            }
                        },
                        onInfo = {
                            closeActionBar()
                            detailsMessage = message
                        },
                        onSelect = {
                            closeActionBar()
                            // The double check acts on this message the way it does in Textra:
                            // the message is selected and the selection bar opens.
                            viewModel.enterSelectionMode(message.id)
                        },
                        onDismiss = { closeActionBar() },
                        modifier = Modifier.zIndex(6f)
                    )
                }
            }
        }
    }

    // Scrolling moves the bubble out from under its strip, so the strip goes with it.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && actionBarMessage != null) {
            closeActionBar()
        }
    }

    // "Message details", the way Textra lays it out.
    detailsMessage?.let { message ->
        MessageDetailsDialog(
            message = message,
            sim = state.availableSims.firstOrNull { it.subscriptionId == message.subId },
            useShamsi = state.showShamsiDate,
            appLanguage = state.appLanguage,
            onDismiss = { detailsMessage = null }
        )
    }

    // Contact Profile Dialog (when tapping top bar for non-contact, or avatar quick preview)
    if (showContactProfileDialog) {
        ContactProfileDialog(
            address = state.address,
            contactName = state.contactName,
            photoUri = state.photoUri,
            contactLookupUri = state.contactLookupUri,
            isContact = !state.contactName.isNullOrBlank(),
            onDismiss = { showContactProfileDialog = false },
            onOpenInfo = {
                showContactProfileDialog = false
                showSettingsSheet = true
            }
        )
    }

    // Participant Settings Bottom Sheet
    if (showSettingsSheet) {
        ParticipantSettingsSheet(
            address = state.address,
            contactName = state.contactName,
            senderPreference = state.senderPreference,
            senderRules = state.senderRules,
            sheetState = sheetState,
            availableTabs = state.availableTabs,
            senderTabIds = state.senderTabIds,
            availableSims = state.availableSims,
            onDismiss = { showSettingsSheet = false },
            onUpdateAction = { action ->
                viewModel.updateSenderDefaultAction(action)
            },
            onUpdatePreferredSim = { preferredSubId ->
                viewModel.setPreferredSim(preferredSubId)
            },
            onAddSenderRule = { pattern, isRegex, action, name ->
                viewModel.addSenderRule(pattern, isRegex, action, name)
            },
            onToggleTab = { tabId, enable ->
                viewModel.toggleSenderTab(tabId, enable)
            },
            onCreateTab = { name ->
                viewModel.createTabWithCurrentSender(name)
            }
        )
    }

    forwardBody?.let { body ->
        ForwardMessageDialog(
            targets = forwardTargets,
            onDismiss = { forwardBody = null },
            onPick = { target ->
                forwardBody = null
                viewModel.forwardMessage(body, target.address) { result ->
                    Toast.makeText(
                        context,
                        if (result.sent) {
                            R.string.chat_menu_forward_sent
                        } else {
                            R.string.chat_menu_forward_failed
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Message Long-Click Actions Dialog
    selectedMessageForDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForDialog = null },
            shape = SquircleCardShape,
            title = {
                Text(
                    text = stringResource(R.string.chat_message_options),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = msg.body,
                                style = MaterialTheme.typography.bodyMedium.contentAware(),
                                maxLines = 4,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    if (msg.isSent) {
                        val (statusIcon, statusText, statusTint) = when {
                            msg.isFailed -> Triple(
                                Icons.Default.ErrorOutline,
                                "Failed",
                                MaterialTheme.colorScheme.error
                            )
                            msg.isDelivered -> Triple(
                                Icons.Default.DoneAll,
                                "Delivered",
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> Triple(
                                Icons.Default.Done,
                                "Sent",
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = statusText,
                                tint = statusTint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusTint
                            )
                        }
                    }
                    Text(
                        text = msg.address,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = if (msg.isSent) 4.dp else 8.dp)
                    )
                    Text(
                        text = SmsDateFormats.formatDateTimeWithYear(msg.date, state.showShamsiDate, state.appLanguage),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .then(
                                if (state.enableDateTapShamsiToggle) {
                                    Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .clickable { viewModel.toggleShamsiDate() }
                                } else Modifier
                            )
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                context.getString(R.string.chat_sms_message),
                                msg.body
                            )
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.chat_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.clearSelection()
                        selectedMessageForDialog = null
                    },
                    shape = PillShape
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.chat_copy), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.chat_copy), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(msg.id)
                        viewModel.clearSelection()
                        selectedMessageForDialog = null
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.chat_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.chat_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    // Batch Delete Confirmation Dialog
    val reactionTarget = reactionPickerMessageId
    if (reactionTarget != null) {
        val currentReaction = state.messages.firstOrNull { it.id == reactionTarget }?.reaction
        ReactionPickerDialog(
            currentReaction = currentReaction,
            onPick = { emoji ->
                viewModel.toggleReaction(reactionTarget, emoji)
                viewModel.clearSelection()
                reactionPickerMessageId = null
            },
            onDismiss = { reactionPickerMessageId = null }
        )
    }

}


@Composable
private fun ChatMessageList(
    messages: List<SmsMessage>,
    availableSims: List<SimInfo>,
    listState: LazyListState,
    highlightedMessageId: Long?,
    isLoadingOlder: Boolean,
    showShamsiDate: Boolean,
    appLanguage: AppLanguage,
    enableDateTapShamsiToggle: Boolean = true,
    onToggleDateFormat: () -> Unit,
    onRevealToggle: (Long, Boolean) -> Unit,
    onMarkNotSpam: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onLongClick: (SmsMessage) -> Unit,
    isSelectionMode: Boolean = false,
    selectedMessageIds: Set<Long> = emptySet(),
    showNewSpam: Boolean = true,
    onTapMessage: (SmsMessage) -> Unit = {},
    /** A single tap on a bubble, which opens the action strip. */
    onTapBubble: (SmsMessage) -> Unit = {},
    /** Bubble bounds, in root coordinates, recorded so the strip can sit over its own message. */
    onBubbleBounds: (Long, Rect) -> Unit = { _, _ -> },
    activeTextBubbleId: Long? = null,
    selectionResetTick: Int = 0,
    /** Bubble that Copy turned into selectable text, and a tick that re-selects it each time. */
    inlineSelectMessageId: Long? = null,
    inlineSelectTick: Int = 0,
    onBubbleTouched: (Long) -> Unit = {},
    onOutsideTap: () -> Unit = {},
    address: String = "",
    contactName: String? = null,
    photoUri: String? = null
) {
    val look = AppearanceStore.current
    val simSlotMap = remember(availableSims) {
        availableSims.associate { it.subscriptionId to it.slotNumber }
    }
    val hasMultipleSims = availableSims.size > 1
    val onDateTapAction: (() -> Unit)? = if (enableDateTapShamsiToggle) onToggleDateFormat else null

    // Newest first, because the list is reversed. This is a real copy, not a view: LazyColumn
    // measures and re-measures while the provider list is being replaced underneath, and a reversed
    // view over a mutating list is what threw IndexOutOfBoundsException / ConcurrentModification.
    val reversedMessages = messages.reversed()
    // Opening and closing a conversation is animated by the "message animation" setting (see
    // Navigation.kt). Inside an already open chat a brand-new bubble keeps a short, fixed entrance
    // of its own so an arriving message does not just pop into place; history and re-bound rows
    // start fully visible.
    val alreadyAnimated = remember { mutableSetOf<Long>() }
    // When the chat was opened. Only messages newer than this arrived while it was on screen, so
    // only those animate: the last message of the conversation used to slide in on every visit.
    val openedAt = remember { System.currentTimeMillis() }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        itemsIndexed(
            items = reversedMessages,
            key = { _, message -> message.id },
            contentType = { _, message ->
                when {
                    message.isSpam -> "spam"
                    message.isSent -> "sent"
                    else -> "inbox"
                }
            }
        ) { index, message ->
            val isHighlighted = message.id == highlightedMessageId
            val simSlotNumber = if (hasMultipleSims) simSlotMap[message.subId] else null
            val olderMessage = reversedMessages.getOrNull(index + 1)
            // One avatar per run of incoming messages.
            val newerMessage = reversedMessages.getOrNull(index - 1)
            val showAvatarForMessage = look.showChatAvatars &&
                !message.isSent &&
                (newerMessage == null || newerMessage.isSent)
            val headerText = if (
                look.showDateHeaders &&
                (olderMessage == null || !SmsDateFormats.isSameDay(olderMessage.date, message.date))
            ) {
                SmsDateFormats.dayHeader(message.date, showShamsiDate, appLanguage)
            } else {
                null
            }
            // Only the newest bubble animates, and only the first time it is shown. Everything else
            // (history, rows re-bound after a scroll) starts fully visible, so the list can never
            // come back stuck at alpha 0 or perform a slideshow.
            val animateIn = message.id !in alreadyAnimated &&
                message.date > openedAt
            val bubbleVisible = remember(message.id) {
                MutableTransitionState(!animateIn).apply { targetState = true }
            }
            LaunchedEffect(message.id) { alreadyAnimated.add(message.id) }
            AnimatedVisibility(
                visibleState = bubbleVisible,
                enter = messageEnterTransition(MessageAnimation.SLIDE),
                exit = messageExitTransition(MessageAnimation.SLIDE)
            ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (headerText != null) {
                    ChatDateHeader(text = headerText)
                }
                if (message.isSpam) {
                    SpamMessagePill(
                        message = message,
                        isHighlighted = isHighlighted,
                        simSlotNumber = simSlotNumber,
                        useShamsi = showShamsiDate,
                        appLanguage = appLanguage,
                        onToggleDateFormat = onDateTapAction,
                        onRevealToggle = { revealed -> onRevealToggle(message.id, revealed) },
                        onMarkNotSpam = { onMarkNotSpam(message.id) },
                        onDelete = { onDelete(message.id) },
                        isSelectionMode = isSelectionMode,
                        isSelected = message.id in selectedMessageIds,
                        onLongClick = { onLongClick(message) },
                        onToggleSelect = { onTapMessage(message) },
                        showNewSpam = showNewSpam
                    )
                } else {
                    val isTargetOfReset = (activeTextBubbleId == message.id)
                    val bubbleResetKey = if (isTargetOfReset) selectionResetTick else 0
                    val avatarSlot: (@Composable () -> Unit)? = if (showAvatarForMessage) {
                        {
                            ConversationAvatar(
                                address = message.address,
                                contactName = contactName,
                                photoUri = photoUri,
                                size = 28.dp
                            )
                        }
                    } else {
                        null
                    }
                    MessageBubble(
                        message = message,
                        isHighlighted = isHighlighted,
                        simSlotNumber = simSlotNumber,
                        useShamsi = showShamsiDate,
                        appLanguage = appLanguage,
                        onToggleDateFormat = onDateTapAction,
                        onLongClick = { onLongClick(message) },
                        onTap = { if (!isSelectionMode) onTapBubble(message) },
                        onBubbleBounds = { bounds -> onBubbleBounds(message.id, bounds) },
                        inlineSelectKey = if (message.id == inlineSelectMessageId) {
                            inlineSelectTick
                        } else {
                            0
                        },
                        isSelectionMode = isSelectionMode,
                        isSelected = message.id in selectedMessageIds,
                        onToggleSelect = { onTapMessage(message) },
                        selectionResetKey = bubbleResetKey,
                        onBubbleTouched = { onBubbleTouched(message.id) },
                        onOutsideTap = onOutsideTap,
                        showAvatar = showAvatarForMessage,
                        avatar = avatarSlot
                    )
                }
            }
            }
        }
        if (isLoadingOlder) {
            item(key = "loading-older", contentType = "loading-older") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * The bar that replaces the chat header while messages are selected.
 *
 * Laid out like Textra's: close, the double check that marks the conversation read, how many are
 * selected, then share, forward and delete, with everything else behind one overflow. Reactions are
 * deliberately absent - the strip on the message already carries them, and having them in both
 * places was the confusing part.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    selectedIds: Set<Long>,
    messages: List<SmsMessage>,
    onClose: () -> Unit,
    onMarkRead: () -> Unit,
    onShare: (List<String>) -> Unit,
    onForward: (List<String>) -> Unit,
    deleteArmed: Boolean = false,
    onDelete: () -> Unit,
    onCopy: (List<String>) -> Unit,
    onInfo: () -> Unit,
    onSelectAll: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // Computed per composition rather than remembered: the selection is a snapshot set that the
    // bar mutates in place, so a remembered value would go stale.
    val selectedBodies = messages.filter { it.id in selectedIds }.map { it.body }

    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.chat_selected_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.chat_cancel_selection)
                )
            }
        },
        actions = {
            IconButton(onClick = onMarkRead) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = stringResource(R.string.chat_mark_read)
                )
            }
            IconButton(onClick = { onShare(selectedBodies) }) {
                Icon(
                    // Same glyph as the strip, so "share" looks like one thing across the app.
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.chat_menu_share)
                )
            }
            IconButton(
                onClick = { onForward(selectedBodies) },
                enabled = selectedBodies.isNotEmpty()
            ) {
                Icon(
                    imageVector = RameshIcons.Forward,
                    contentDescription = stringResource(R.string.chat_menu_forward)
                )
            }
            // Two-step delete, same as the strip: the bin fills in and turns red when armed, and
            // the second tap deletes. Nothing else is asked.
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = if (deleteArmed) Icons.Default.Delete else Icons.Default.DeleteOutline,
                    contentDescription = stringResource(
                        if (deleteArmed) R.string.chat_delete_again else R.string.chat_delete_selected
                    ),
                    tint = if (deleteArmed) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.chat_menu_more)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_copy)) },
                        onClick = {
                            menuExpanded = false
                            onCopy(selectedBodies)
                        }
                    )
                    if (selectedCount == 1) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_message_info)) },
                            onClick = {
                                menuExpanded = false
                                onInfo()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_select_all)) },
                        onClick = {
                            menuExpanded = false
                            onSelectAll()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

private val ReactionEmojis = listOf("❤️", "👍", "😂", "😮", "😢", "🙏", "🔥", "🎉")

/** Emoji reaction picker for a single selected message. */
@Composable
private fun ReactionPickerDialog(
    currentReaction: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.chat_react_action),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReactionEmojis.forEach { emoji ->
                    val selected = emoji == currentReaction
                    Surface(
                        onClick = { onPick(emoji) },
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_close)) }
        }
    )
}

/** Countdown bar shown while a send-delay is running; Cancel puts the text back in the composer. */
@Composable
private fun PendingSendBar(secondsLeft: Int, onCancel: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chat_sending_in, secondsLeft),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.chat_cancel), fontWeight = FontWeight.Bold)
            }
        }
    }
}
