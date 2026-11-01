package com.ramesh.sms.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Single source of truth for the look & feel settings.
 *
 * The value is a Compose snapshot state, so every screen that reads
 * [AppearanceStore.current] recomposes as soon as the user changes a setting.
 */
object AppearanceStore {

    private const val PREFS_NAME = "ramesh_appearance"

    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT = "accent_color"
    private const val KEY_BUBBLE_STYLE = "bubble_style"
    private const val KEY_BUBBLE_TAIL = "bubble_tail"
    private const val KEY_SENT_TONE = "sent_bubble_tone"
    private const val KEY_RECEIVED_TONE = "received_bubble_tone"
    private const val KEY_BACKDROP = "chat_backdrop"
    private const val KEY_WALLPAPER = "chat_wallpaper_uri"
    private const val KEY_FONT_SIZE = "chat_font_size"
    private const val KEY_FONT_FAMILY = "chat_font_family"
    private const val KEY_CHAT_AVATARS = "chat_avatars"
    private const val KEY_DATE_HEADERS = "date_headers"
    private const val KEY_INBOX_AVATARS = "inbox_avatars"
    private const val KEY_ENTER_TO_SEND = "enter_to_send"
    private const val KEY_CHAR_COUNTER = "character_counter"
    private const val KEY_OTP_ACTION = "otp_copy_action"
    private const val KEY_SWIPE_DELETE = "swipe_to_delete"
    private const val KEY_MESSAGE_ANIMATION = "message_animation"

    private var prefs: SharedPreferences? = null

    var current by mutableStateOf(Appearance())
        private set

    fun init(context: Context) {
        val shared = prefs ?: context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { prefs = it }
        current = read(shared)
    }

    fun update(context: Context, transform: (Appearance) -> Appearance) {
        val shared = prefs ?: context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { prefs = it }
        val updated = transform(current)
        if (updated == current) return
        write(shared, updated)
        current = updated
    }

    fun reset(context: Context) {
        update(context) { Appearance() }
    }

    /**
     * Notification code paths (broadcast receivers) are not composables, so they
     * read the flag straight from storage instead of the snapshot state.
     */
    fun isOtpCopyEnabled(context: Context): Boolean {
        val shared = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return shared.getBoolean(KEY_OTP_ACTION, true)
    }

    private fun read(shared: SharedPreferences): Appearance {
        val defaults = Appearance()
        return Appearance(
            themeMode = shared.readEnum(KEY_THEME_MODE, defaults.themeMode),
            accent = shared.readEnum(KEY_ACCENT, defaults.accent),
            bubbleStyle = shared.readEnum(KEY_BUBBLE_STYLE, defaults.bubbleStyle),
            bubbleTail = shared.getBoolean(KEY_BUBBLE_TAIL, defaults.bubbleTail),
            sentTone = shared.readEnum(KEY_SENT_TONE, defaults.sentTone),
            receivedTone = shared.readEnum(KEY_RECEIVED_TONE, defaults.receivedTone),
            backdrop = shared.readEnum(KEY_BACKDROP, defaults.backdrop),
            wallpaperUri = shared.getString(KEY_WALLPAPER, null),
            chatFontSize = shared.readEnum(KEY_FONT_SIZE, defaults.chatFontSize),
            fontFamily = shared.readEnum(KEY_FONT_FAMILY, defaults.fontFamily),
            showChatAvatars = shared.getBoolean(KEY_CHAT_AVATARS, defaults.showChatAvatars),
            showDateHeaders = shared.getBoolean(KEY_DATE_HEADERS, defaults.showDateHeaders),
            showInboxAvatars = shared.getBoolean(KEY_INBOX_AVATARS, defaults.showInboxAvatars),
            enterToSend = shared.getBoolean(KEY_ENTER_TO_SEND, defaults.enterToSend),
            showCharacterCounter = shared.getBoolean(KEY_CHAR_COUNTER, defaults.showCharacterCounter),
            otpCopyAction = shared.getBoolean(KEY_OTP_ACTION, defaults.otpCopyAction),
            swipeToDelete = shared.getBoolean(KEY_SWIPE_DELETE, defaults.swipeToDelete),
            messageAnimation = shared.readEnum(KEY_MESSAGE_ANIMATION, defaults.messageAnimation)
        )
    }

    private fun write(shared: SharedPreferences, appearance: Appearance) {
        shared.edit()
            .putString(KEY_THEME_MODE, appearance.themeMode.name)
            .putString(KEY_ACCENT, appearance.accent.name)
            .putString(KEY_BUBBLE_STYLE, appearance.bubbleStyle.name)
            .putBoolean(KEY_BUBBLE_TAIL, appearance.bubbleTail)
            .putString(KEY_SENT_TONE, appearance.sentTone.name)
            .putString(KEY_RECEIVED_TONE, appearance.receivedTone.name)
            .putString(KEY_BACKDROP, appearance.backdrop.name)
            .putString(KEY_WALLPAPER, appearance.wallpaperUri)
            .putString(KEY_FONT_SIZE, appearance.chatFontSize.name)
            .putString(KEY_FONT_FAMILY, appearance.fontFamily.name)
            .putBoolean(KEY_CHAT_AVATARS, appearance.showChatAvatars)
            .putBoolean(KEY_DATE_HEADERS, appearance.showDateHeaders)
            .putBoolean(KEY_INBOX_AVATARS, appearance.showInboxAvatars)
            .putBoolean(KEY_ENTER_TO_SEND, appearance.enterToSend)
            .putBoolean(KEY_CHAR_COUNTER, appearance.showCharacterCounter)
            .putBoolean(KEY_OTP_ACTION, appearance.otpCopyAction)
            .putBoolean(KEY_SWIPE_DELETE, appearance.swipeToDelete)
            .putString(KEY_MESSAGE_ANIMATION, appearance.messageAnimation.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.readEnum(key: String, fallback: T): T {
        val raw = getString(key, null) ?: return fallback
        return try {
            enumValueOf<T>(migrateEnumName(raw, fallback))
        } catch (_: IllegalArgumentException) {
            fallback
        }
    }

    /**
     * Stored enums are written by [Enum.name], so renaming a constant silently resets the user's
     * choice to the default. `BubbleStyle.TEXTRA` was renamed to `CLASSIC` in 1.0.0; map the old
     * name here instead of losing the setting.
     */
    private fun <T : Enum<T>> migrateEnumName(raw: String, fallback: T): String =
        if (raw == "TEXTRA" && fallback is BubbleStyle) BubbleStyle.CLASSIC.name else raw
}
