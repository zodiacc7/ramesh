package com.ramesh.sms.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramesh.sms.R
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.data.util.JalaliScope
import com.ramesh.sms.data.util.LockScreenPrivacy
import com.ramesh.sms.engine.NotificationHelper
import com.ramesh.sms.theme.AccentColor
import com.ramesh.sms.theme.Appearance
import com.ramesh.sms.theme.AppearanceStore
import com.ramesh.sms.theme.BubbleStyle
import com.ramesh.sms.theme.BubbleTone
import com.ramesh.sms.theme.ChatBackdrop
import com.ramesh.sms.theme.ChatFontFamily
import com.ramesh.sms.theme.ChatFontSize
import com.ramesh.sms.theme.MessageAnimation
import com.ramesh.sms.theme.SquircleCardShape
import com.ramesh.sms.theme.ThemeMode
import com.ramesh.sms.theme.backdropColorOrNull
import com.ramesh.sms.theme.bubbleBackground
import com.ramesh.sms.theme.bubbleContent
import com.ramesh.sms.theme.bubbleContentBottomPadding
import com.ramesh.sms.theme.chatBubbleShape
import com.ramesh.sms.theme.messageEnterTransition
import com.ramesh.sms.theme.messageExitTransition
import com.ramesh.sms.ui.components.AccentSwatchPicker
import com.ramesh.sms.ui.components.ChatDateHeader
import com.ramesh.sms.ui.components.SettingsActionRow
import com.ramesh.sms.ui.components.SettingsChoiceRow
import com.ramesh.sms.ui.components.SettingsColorChoiceRow
import com.ramesh.sms.ui.components.SettingsGroup
import com.ramesh.sms.ui.components.SettingsRowDivider
import com.ramesh.sms.ui.components.SettingsSectionHeader
import com.ramesh.sms.ui.components.SettingsSliderRow
import com.ramesh.sms.ui.components.SettingsSwitchRow
import com.ramesh.sms.ui.components.autoDeleteLimitForProgress
import com.ramesh.sms.ui.components.hasPersistedReadAccess
import com.ramesh.sms.ui.components.persistWallpaperAccess
import com.ramesh.sms.ui.components.progressForAutoDeleteLimit
import com.ramesh.sms.ui.viewmodel.ConversationsViewModel
import kotlin.math.roundToInt

private val ToneDotColors: List<Pair<String, androidx.compose.ui.graphics.Color?>> =
    BubbleTone.values().map { tone ->
        tone.label to when (tone) {
            BubbleTone.ACCENT, BubbleTone.SURFACE -> null
            else -> tone.lightBg
        }
    }

private val BackdropDotColors: List<Pair<String, androidx.compose.ui.graphics.Color?>> =
    ChatBackdrop.values().map { backdrop ->
        backdrop.label to backdrop.light
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onOpenFilterStudio: () -> Unit,
    inboxViewModel: ConversationsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val look = AppearanceStore.current
    val inboxState by inboxViewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Sliders keep their own value while dragging so the preference is only written once the
    // user lets go; that avoids a disk write on every pixel.
    var sendDelay by remember(inboxState.sendDelaySeconds) {
        mutableFloatStateOf(inboxState.sendDelaySeconds.toFloat().coerceIn(0f, 9f))
    }
    var autoDeleteProgress by remember(inboxState.autoDeleteLimit) {
        mutableFloatStateOf(progressForAutoDeleteLimit(inboxState.autoDeleteLimit))
    }

    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    fun sendTestNotification() {
        val shown = NotificationHelper.getInstance(context).showTestNotification()
        Toast.makeText(
            context,
            context.getString(
                if (shown) R.string.settings_test_notification_sent
                else R.string.settings_test_notification_blocked
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sendTestNotification()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.settings_test_notification_blocked),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun update(transform: (Appearance) -> Appearance) {
        AppearanceStore.update(context, transform)
    }

    // Remembers that the last picked wallpaper could not be granted persistent access, so the row
    // can warn instead of pretending everything is fine until the next reboot.
    var wallpaperNeedsNewPick by remember { mutableStateOf(false) }

    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val persisted = persistWallpaperAccess(context, uri)
            wallpaperNeedsNewPick = !persisted
            if (!persisted) {
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_wallpaper_needs_pick),
                    Toast.LENGTH_LONG
                ).show()
            }
            update { it.copy(wallpaperUri = uri.toString()) }
        }
    }

    // A stored wallpaper whose grant was lost (app data cleared, provider removed) cannot be
    // decoded any more; surface that instead of showing an empty chat background.
    LaunchedEffect(look.wallpaperUri) {
        val stored = look.wallpaperUri
        wallpaperNeedsNewPick = if (stored.isNullOrBlank()) {
            false
        } else {
            val parsed = runCatching { Uri.parse(stored) }.getOrNull()
            parsed == null || !hasPersistedReadAccess(context, parsed)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(R.string.settings_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(key = "preview") {
                BubblePreviewCard(look)
            }

            item(key = "theme-header") {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_theme_header),
                    subtitle = stringResource(R.string.settings_theme_sub)
                )
            }
            item(key = "theme-group") {
                SettingsGroup {
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_theme_mode),
                        subtitle = stringResource(R.string.settings_theme_mode_sub),
                        options = ThemeMode.values().map { it.label },
                        selectedIndex = look.themeMode.ordinal,
                        onSelect = { index ->
                            update { it.copy(themeMode = ThemeMode.values()[index]) }
                        },
                        showDivider = true
                    )
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_accent),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_accent_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AccentSwatchPicker(
                            selected = look.accent,
                            onSelect = { accent -> update { it.copy(accent = accent) } }
                        )
                        if (look.accent == AccentColor.SYSTEM) {
                            Text(
                                text = stringResource(R.string.settings_accent_system),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            item(key = "bubbles-header") {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_bubbles_header),
                    subtitle = stringResource(R.string.settings_bubbles_sub)
                )
            }
            item(key = "bubbles-group") {
                SettingsGroup {
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_bubble_style),
                        subtitle = stringResource(R.string.settings_bubble_style_sub),
                        options = BubbleStyle.values().map { it.label },
                        selectedIndex = look.bubbleStyle.ordinal,
                        onSelect = { index ->
                            update { it.copy(bubbleStyle = BubbleStyle.values()[index]) }
                        }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = stringResource(R.string.settings_bubble_tail),
                        subtitle = stringResource(R.string.settings_bubble_tail_sub),
                        checked = look.bubbleTail,
                        onCheckedChange = { enabled -> update { it.copy(bubbleTail = enabled) } },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.settings_swipe_delete),
                        subtitle = stringResource(R.string.settings_swipe_delete_sub),
                        checked = look.swipeToDelete,
                        onCheckedChange = { enabled -> update { it.copy(swipeToDelete = enabled) } },
                        showDivider = true
                    )
                    SettingsColorChoiceRow(
                        title = stringResource(R.string.settings_sent_color),
                        options = ToneDotColors,
                        selectedIndex = look.sentTone.ordinal,
                        onSelect = { index ->
                            update { it.copy(sentTone = BubbleTone.values()[index]) }
                        }
                    )
                    SettingsColorChoiceRow(
                        title = stringResource(R.string.settings_received_color),
                        options = ToneDotColors,
                        selectedIndex = look.receivedTone.ordinal,
                        onSelect = { index ->
                            update { it.copy(receivedTone = BubbleTone.values()[index]) }
                        }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_text_size),
                        options = ChatFontSize.values().map { it.label },
                        selectedIndex = look.chatFontSize.ordinal,
                        onSelect = { index ->
                            update { it.copy(chatFontSize = ChatFontSize.values()[index]) }
                        }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_message_font),
                        options = ChatFontFamily.values().map { it.label },
                        selectedIndex = look.fontFamily.ordinal,
                        onSelect = { index ->
                            update { it.copy(fontFamily = ChatFontFamily.values()[index]) }
                        },
                        showDivider = true
                    )
                }
            }

            item(key = "background-header") {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_background_header),
                    subtitle = stringResource(R.string.settings_background_sub)
                )
            }
            item(key = "background-group") {
                SettingsGroup {
                    SettingsColorChoiceRow(
                        title = stringResource(R.string.settings_background),
                        options = BackdropDotColors,
                        selectedIndex = look.backdrop.ordinal,
                        onSelect = { index ->
                            update { it.copy(backdrop = ChatBackdrop.values()[index]) }
                        }
                    )
                    SettingsActionRow(
                        title = if (look.wallpaperUri == null) stringResource(R.string.settings_choose_image) else stringResource(R.string.settings_change_image),
                        subtitle = stringResource(R.string.settings_pick_photo),
                        icon = Icons.Default.Wallpaper,
                        onClick = { wallpaperPicker.launch(arrayOf("image/*")) }
                    )
                    if (look.wallpaperUri != null && wallpaperNeedsNewPick) {
                        // The file is still remembered but no longer readable, so the chat shows a
                        // plain background. Say it out loud instead of leaving the row silent.
                        SettingsActionRow(
                            title = stringResource(R.string.settings_wallpaper_needs_pick),
                            subtitle = stringResource(R.string.settings_wallpaper_needs_pick_sub),
                            icon = Icons.Default.Warning,
                            onClick = { wallpaperPicker.launch(arrayOf("image/*")) }
                        )
                    }
                    if (look.wallpaperUri != null) {
                        SettingsActionRow(
                            title = stringResource(R.string.settings_remove_wallpaper),
                            subtitle = stringResource(R.string.settings_remove_wallpaper_sub),
                            icon = Icons.Default.DeleteOutline,
                            onClick = { update { it.copy(wallpaperUri = null) } }
                        )
                    }
                }
            }

            item(key = "chat-header") {
                SettingsSectionHeader(title = stringResource(R.string.settings_chat_header), subtitle = stringResource(R.string.settings_chat_sub))
            }
            item(key = "chat-group") {
                SettingsGroup {
                    SettingsSwitchRow(
                        icon = Icons.Default.People,
                        title = stringResource(R.string.settings_show_photos),
                        subtitle = stringResource(R.string.settings_show_photos_sub),
                        checked = look.showChatAvatars,
                        onCheckedChange = { enabled -> update { it.copy(showChatAvatars = enabled) } },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.CalendarMonth,
                        title = stringResource(R.string.settings_date_dividers),
                        subtitle = stringResource(R.string.settings_date_dividers_sub),
                        checked = look.showDateHeaders,
                        onCheckedChange = { enabled -> update { it.copy(showDateHeaders = enabled) } },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Keyboard,
                        title = stringResource(R.string.settings_enter_sends),
                        subtitle = stringResource(R.string.settings_enter_sends_sub),
                        checked = look.enterToSend,
                        onCheckedChange = { enabled -> update { it.copy(enterToSend = enabled) } },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.TextSnippet,
                        title = stringResource(R.string.settings_counter),
                        subtitle = stringResource(R.string.settings_counter_sub),
                        checked = look.showCharacterCounter,
                        onCheckedChange = { enabled -> update { it.copy(showCharacterCounter = enabled) } },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.CalendarMonth,
                        title = stringResource(R.string.settings_date_tap),
                        subtitle = stringResource(R.string.settings_date_tap_sub),
                        checked = inboxState.enableDateTapShamsiToggle,
                        onCheckedChange = { enabled -> inboxViewModel.setDateTapShamsiEnabled(enabled) },
                        showDivider = true
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_jalali_scope),
                        subtitle = stringResource(R.string.settings_jalali_scope_sub),
                        options = listOf(
                            stringResource(R.string.settings_jalali_chat_only),
                            stringResource(R.string.settings_jalali_everywhere)
                        ),
                        selectedIndex = if (inboxState.jalaliScope == JalaliScope.EVERYWHERE) 1 else 0,
                        onSelect = { index ->
                            inboxViewModel.setJalaliScope(
                                if (index == 1) JalaliScope.EVERYWHERE else JalaliScope.CHAT_ONLY
                            )
                        },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.SimCard,
                        title = stringResource(R.string.settings_sim_badge),
                        subtitle = stringResource(R.string.settings_sim_badge_sub),
                        checked = inboxState.showSimBadge,
                        onCheckedChange = { enabled -> inboxViewModel.setShowSimBadgeEnabled(enabled) },
                        showDivider = true
                    )
                    val delaySeconds = sendDelay.roundToInt().coerceIn(0, 9)
                    SettingsSliderRow(
                        title = stringResource(R.string.settings_send_delay),
                        subtitle = stringResource(R.string.settings_send_delay_sub),
                        value = sendDelay,
                        valueRange = 0f..9f,
                        steps = 8,
                        valueLabel = if (delaySeconds == 0) {
                            stringResource(R.string.settings_off)
                        } else {
                            stringResource(R.string.settings_seconds_value, delaySeconds)
                        },
                        onValueChange = { sendDelay = it.roundToInt().toFloat() },
                        onValueChangeFinished = {
                            inboxViewModel.setSendDelaySeconds(delaySeconds)
                        }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_message_animation),
                        subtitle = stringResource(R.string.settings_message_animation_sub),
                        options = listOf(
                            stringResource(R.string.settings_animation_fade),
                            stringResource(R.string.settings_animation_slide),
                            stringResource(R.string.settings_animation_pop),
                            stringResource(R.string.settings_animation_none)
                        ),
                        selectedIndex = look.messageAnimation.ordinal,
                        onSelect = { index ->
                            update { it.copy(messageAnimation = MessageAnimation.entries[index]) }
                        },
                        // The full chat preview lives at the top of a long scrolling screen, so it
                        // is not even composed when the user taps this row. A preview right here is
                        // the only way the choice visibly does something.
                        footer = { AnimationPreviewRow(look) }
                    )
                }
            }

            item(key = "inbox-header") {
                SettingsSectionHeader(title = stringResource(R.string.settings_inbox_header), subtitle = stringResource(R.string.settings_inbox_sub))
            }
            item(key = "inbox-group") {
                SettingsGroup {
                    SettingsSwitchRow(
                        icon = Icons.Default.Tab,
                        title = stringResource(R.string.settings_tabs),
                        subtitle = stringResource(R.string.settings_tabs_sub),
                        checked = inboxState.showTabs,
                        onCheckedChange = { enabled -> inboxViewModel.setShowTabsEnabled(enabled) },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.SimCard,
                        title = stringResource(R.string.settings_sim_filter),
                        subtitle = stringResource(R.string.settings_sim_filter_sub),
                        checked = inboxState.showSimFilter,
                        onCheckedChange = { enabled -> inboxViewModel.setShowSimFilterEnabled(enabled) },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.People,
                        title = stringResource(R.string.settings_contact_photos),
                        subtitle = stringResource(R.string.settings_contact_photos_sub),
                        checked = look.showInboxAvatars,
                        onCheckedChange = { enabled -> update { it.copy(showInboxAvatars = enabled) } },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.settings_contacts_only),
                        subtitle = stringResource(R.string.settings_contacts_only_sub),
                        checked = inboxState.showContactsOnly,
                        onCheckedChange = { inboxViewModel.toggleContactsOnly() }
                    )
                }
            }

            item(key = "notifications-header") {
                SettingsSectionHeader(title = stringResource(R.string.settings_notifications_header), subtitle = stringResource(R.string.settings_notifications_sub))
            }
            item(key = "notifications-group") {
                SettingsGroup {
                    SettingsSwitchRow(
                        icon = Icons.Default.DeleteOutline,
                        title = stringResource(R.string.settings_delete_action),
                        subtitle = stringResource(R.string.settings_delete_action_sub),
                        checked = inboxState.showNotificationDeleteAction,
                        onCheckedChange = { enabled ->
                            inboxViewModel.setNotificationDeleteActionEnabled(enabled)
                        },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.ContentCopy,
                        title = stringResource(R.string.settings_copy_codes),
                        subtitle = stringResource(R.string.settings_copy_codes_sub),
                        checked = look.otpCopyAction,
                        onCheckedChange = { enabled -> update { it.copy(otpCopyAction = enabled) } },
                        showDivider = true
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.CheckCircle,
                        title = stringResource(R.string.settings_delivery),
                        subtitle = stringResource(R.string.settings_delivery_sub),
                        checked = inboxState.deliveryReports,
                        onCheckedChange = { enabled -> inboxViewModel.setDeliveryReportsEnabled(enabled) },
                        showDivider = true
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.settings_test_notification),
                        subtitle = stringResource(R.string.settings_test_notification_sub),
                        icon = Icons.Default.NotificationsActive,
                        showDivider = true,
                        onClick = {
                            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                sendTestNotification()
                            }
                        }
                    )
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_lock_screen),
                        subtitle = stringResource(R.string.settings_lock_screen_sub),
                        options = listOf(
                            LockScreenPrivacy.FULL.label,
                            LockScreenPrivacy.SENDER_ONLY.label,
                            LockScreenPrivacy.HIDDEN.label
                        ),
                        selectedIndex = inboxState.lockScreenPrivacy.ordinal,
                        onSelect = { index ->
                            inboxViewModel.setLockScreenPrivacy(LockScreenPrivacy.entries[index])
                        }
                    )
                }
            }

            item(key = "filtering-header") {
                SettingsSectionHeader(title = stringResource(R.string.settings_filtering_header), subtitle = stringResource(R.string.settings_filtering_sub))
            }
            item(key = "filtering-group") {
                SettingsGroup {
                    SettingsSwitchRow(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.settings_no_spam_contacts),
                        subtitle = stringResource(R.string.settings_no_spam_contacts_sub),
                        checked = inboxState.noSpamForContacts,
                        onCheckedChange = { enabled -> inboxViewModel.setNoSpamForContactsEnabled(enabled) },
                        showDivider = true
                    )
                    SettingsActionRow(
                        title = stringResource(R.string.settings_filter_studio),
                        subtitle = stringResource(R.string.settings_filter_studio_sub),
                        icon = Icons.Default.FilterAlt,
                        onClick = onOpenFilterStudio
                    )
                }
            }

            item(key = "language-header") {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_language_header),
                    subtitle = stringResource(R.string.settings_language_sub)
                )
            }
            item(key = "language-group") {
                SettingsGroup {
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_app_language),
                        subtitle = stringResource(R.string.settings_app_language_sub),
                        options = AppLanguage.entries.map { it.label },
                        selectedIndex = inboxState.appLanguage.ordinal,
                        onSelect = { index ->
                            inboxViewModel.setAppLanguage(AppLanguage.entries[index])
                            (context as? Activity)?.recreate()
                        }
                    )
                }
            }

            item(key = "storage-header") {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_storage_header),
                    subtitle = stringResource(R.string.settings_storage_sub)
                )
            }
            item(key = "storage-group") {
                SettingsGroup {
                    val pendingAutoDelete = autoDeleteLimitForProgress(autoDeleteProgress)
                    SettingsSliderRow(
                        title = stringResource(R.string.settings_auto_delete),
                        subtitle = stringResource(R.string.settings_auto_delete_sub),
                        value = autoDeleteProgress,
                        valueRange = 0f..100f,
                        valueLabel = if (pendingAutoDelete == 0) {
                            stringResource(R.string.settings_off)
                        } else {
                            stringResource(
                                R.string.settings_messages_count,
                                java.text.NumberFormat.getIntegerInstance()
                                    .format(pendingAutoDelete.toLong())
                            )
                        },
                        onValueChange = { autoDeleteProgress = it },
                        onValueChangeFinished = {
                            inboxViewModel.setAutoDeleteLimit(
                                autoDeleteLimitForProgress(autoDeleteProgress)
                            )
                        }
                    )
                }
            }

            item(key = "about-header") {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_about_header),
                    subtitle = stringResource(R.string.settings_about_sub)
                )
            }
            item(key = "about-group") {
                SettingsGroup {
                    SettingsActionRow(
                        title = stringResource(R.string.settings_about_app),
                        subtitle = stringResource(R.string.settings_about_version, appVersion),
                        icon = Icons.Default.Info,
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            item(key = "reset-header") {
                SettingsSectionHeader(title = stringResource(R.string.settings_reset_header))
            }
            item(key = "reset-group") {
                SettingsGroup {
                    SettingsActionRow(
                        title = stringResource(R.string.settings_restore_look),
                        subtitle = stringResource(R.string.settings_restore_look_sub),
                        icon = Icons.Default.RestartAlt,
                        onClick = { showResetDialog = true }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            shape = SquircleCardShape,
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.settings_about_version, appVersion),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.settings_about_source),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.settings_about_close), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = SquircleCardShape,
            title = {
                Text(
                    text = stringResource(R.string.settings_restore_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_restore_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        AppearanceStore.reset(context)
                    }
                ) {
                    Text(stringResource(R.string.settings_restore), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }
}

/**
 * Plays the chosen message animation next to the picker itself.
 *
 * Changing the setting used to do nothing visible: the conversation preview sits at the top of the
 * settings list and is recycled while the user is scrolled down to this row. Tap the row to replay.
 */
@Composable
private fun AnimationPreviewRow(look: Appearance) {
    var replay by remember { mutableIntStateOf(0) }
    // Replay on every change, including the very first composition, so the row always demonstrates
    // the current style instead of showing a static bubble.
    LaunchedEffect(look.messageAnimation) { replay++ }
    val state = remember(replay, look.messageAnimation) {
        MutableTransitionState(false).apply { targetState = true }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { replay++ },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedVisibility(
                visibleState = state,
                enter = messageEnterTransition(look.messageAnimation),
                exit = messageExitTransition(look.messageAnimation)
            ) {
                Surface(
                    shape = chatBubbleShape(false, look.bubbleStyle, look.bubbleTail),
                    color = look.receivedTone.bubbleBackground(false)
                ) {
                    Text(
                        text = stringResource(R.string.settings_animation_preview_bubble),
                        style = MaterialTheme.typography.bodyMedium,
                        color = look.receivedTone.bubbleContent(false),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_animation_replay),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BubblePreviewCard(look: Appearance) {
    // Replay the chosen animation whenever the style or the backdrop changes, so the picker shows a
    // result instead of asking the user to send themselves an SMS to see it.
    var replay by remember { mutableIntStateOf(0) }
    LaunchedEffect(look.messageAnimation, look.backdrop) { replay++ }
    val previewBackground = look.backdrop.backdropColorOrNull()
        ?: MaterialTheme.colorScheme.surfaceContainerLow

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            // Tapping the card replays the animation, so the style can be checked on demand.
            .clickable { replay++ },
        shape = SquircleCardShape,
        colors = CardDefaults.cardColors(containerColor = previewBackground),
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            ChatDateHeader(text = stringResource(R.string.settings_today))
            PreviewAnimatedSlot(replay, look) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = chatBubbleShape(false, look.bubbleStyle, look.bubbleTail),
                        color = look.receivedTone.bubbleBackground(false),
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.settings_preview_in),
                            style = MaterialTheme.typography.bodyMedium,
                            color = look.receivedTone.bubbleContent(false),
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 9.dp,
                                    bottom = 9.dp + bubbleContentBottomPadding(
                                        look.bubbleStyle,
                                        look.bubbleTail
                                    )
                                )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            PreviewAnimatedSlot(replay, look) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = chatBubbleShape(true, look.bubbleStyle, look.bubbleTail),
                        color = look.sentTone.bubbleBackground(true)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_preview_out),
                            style = MaterialTheme.typography.bodyMedium,
                            color = look.sentTone.bubbleContent(true),
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 9.dp,
                                    bottom = 9.dp + bubbleContentBottomPadding(
                                        look.bubbleStyle,
                                        look.bubbleTail
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Plays the selected message animation inside the settings preview.
 *
 * The chat only animates messages younger than 15 seconds, so without this the picker looked like
 * it did nothing until the next SMS arrived.
 */
@Composable
private fun PreviewAnimatedSlot(replay: Int, look: Appearance, content: @Composable () -> Unit) {
    val state = remember(replay) {
        MutableTransitionState(false).apply { targetState = true }
    }
    AnimatedVisibility(
        visibleState = state,
        enter = messageEnterTransition(look.messageAnimation),
        exit = messageExitTransition(look.messageAnimation)
    ) {
        content()
    }
}
