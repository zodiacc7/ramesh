package com.ramesh.sms.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.MutableTransitionState
import com.ramesh.sms.theme.messageEnterTransition
import com.ramesh.sms.theme.messageExitTransition
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramesh.sms.ChatNav
import com.ramesh.sms.data.model.SimInfo
import com.ramesh.sms.data.util.AppLanguage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.ramesh.sms.data.model.SmsMessage
import com.ramesh.sms.R
import com.ramesh.sms.theme.AppearanceStore
import com.ramesh.sms.theme.InputBarShape
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.SquircleCardShape
import com.ramesh.sms.ui.components.ChatBackdropBackground
import com.ramesh.sms.ui.components.ChatDateHeader
import com.ramesh.sms.ui.components.ContactProfileDialog
import com.ramesh.sms.ui.components.ConversationAvatar
import com.ramesh.sms.ui.components.MessageBubble
import com.ramesh.sms.ui.components.SimToggleButton
import com.ramesh.sms.ui.components.SmsSegmentCounter
import com.ramesh.sms.ui.components.SpamMessagePill
import com.ramesh.sms.ui.util.ContactUtils
import com.ramesh.sms.ui.util.senderDisplayName
import com.ramesh.sms.ui.util.contentAware
import com.ramesh.sms.ui.util.SmsDateFormats
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
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }
    val selectedMessageIds = viewModel.selectedMessageIds
    val isSelectionMode = viewModel.isSelectionMode
    var activeTextBubbleId by remember { mutableStateOf<Long?>(null) }
    var selectionResetTick by remember { mutableStateOf(0) }
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
            if (textToolbar.status == TextToolbarStatus.Shown) {
                selectionResetTick += 1
                activeTextBubbleId = null
                textToolbar.hide()
            }
        }
    }
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
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.chat_selected_count, selectedMessageIds.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.chat_cancel_selection))
                        }
                    },
                    actions = {
                        if (selectedMessageIds.size == 1) {
                            val selectedId = selectedMessageIds.first()
                            val activeReaction = state.messages.firstOrNull { it.id == selectedId }?.reaction
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Quick reactions right in the selection bar: tapping a message
                                // used to hide the only react entry point behind a dialog.
                                ReactionEmojis.take(3).forEach { emoji ->
                                    val isActive = emoji == activeReaction
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                            .clickable {
                                                viewModel.toggleReaction(selectedId, emoji)
                                                viewModel.clearSelection()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 15.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { reactionPickerMessageId = selectedId },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddReaction,
                                        contentDescription = stringResource(R.string.chat_react),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                // Text is no longer selectable inside the bubble (that gesture is
                                // the message selection now), so copying lives here.
                                IconButton(
                                    onClick = {
                                        val body = state.messages.firstOrNull { it.id == selectedId }?.body
                                        if (!body.isNullOrEmpty()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("message", body))
                                            Toast.makeText(context, R.string.chat_copied, Toast.LENGTH_SHORT).show()
                                        }
                                        viewModel.clearSelection()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(R.string.chat_copy),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                IconButton(onClick = onShowInfoForSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = stringResource(R.string.chat_message_info),
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
                            IconButton(onClick = { showBatchDeleteConfirmDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.chat_delete_selected),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
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
                                    "Text message (SMS)...",
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
                    messages = state.messages,
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
                    onTapMessage = onTapMessage,
                    activeTextBubbleId = activeTextBubbleId,
                    selectionResetTick = selectionResetTick,
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
        }
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

    if (showBatchDeleteConfirmDialog) {
        val count = selectedMessageIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            shape = SquircleCardShape,
            title = {
                Text(
                    text = stringResource(R.string.chat_delete_messages_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.chat_delete_messages_body, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchDeleteConfirmDialog = false
                        onDeleteSelected()
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.chat_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.chat_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
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
    onTapMessage: (SmsMessage) -> Unit = {},
    activeTextBubbleId: Long? = null,
    selectionResetTick: Int = 0,
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

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(
            count = messages.size,
            key = { index -> messages[messages.lastIndex - index].id },
            contentType = { index ->
                val message = messages[messages.lastIndex - index]
                when {
                    message.isSpam -> "spam"
                    message.isSent -> "sent"
                    else -> "inbox"
                }
            }
        ) { index ->
            val message = messages[messages.lastIndex - index]
            val isHighlighted = message.id == highlightedMessageId
            val simSlotNumber = if (hasMultipleSims) simSlotMap[message.subId] else null
            val olderMessage = messages.getOrNull(messages.lastIndex - index - 1)
            // Like Telegram/Textra: one avatar per run of incoming messages.
            val newerMessage = messages.getOrNull(messages.lastIndex - index + 1)
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
            // Each fresh bubble animates in with the style picked in Settings. Older messages
            // (history, scroll-backs) start fully visible so the list never performs a slideshow.
            val bubbleVisible = remember(message.id) {
                val isFresh = System.currentTimeMillis() - message.date < 15_000L
                MutableTransitionState(!isFresh).apply { targetState = true }
            }
            AnimatedVisibility(
                visibleState = bubbleVisible,
                enter = messageEnterTransition(look.messageAnimation),
                exit = messageExitTransition(look.messageAnimation)
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
                        onToggleSelect = { onTapMessage(message) }
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
