package com.ramesh.sms.data.util

/**
 * Where the Persian (Jalali) calendar is used.
 *
 * [CHAT_ONLY] keeps the conversation list on the Gregorian calendar and only the chat screen
 * follows the user's Jalali preference; [EVERYWHERE] uses Jalali dates in the whole app.
 */
enum class JalaliScope {
    CHAT_ONLY,
    EVERYWHERE;

    companion object {
        fun fromName(name: String?): JalaliScope =
            entries.firstOrNull { it.name == name } ?: CHAT_ONLY
    }
}

/**
 * Whether a screen should print Jalali dates.
 *
 * Two stored preferences can ask for the Persian calendar: the [JalaliScope] picked in Settings, and
 * the older `showShamsiDate` flag that tapping a date inside a chat toggles. The chat screen always
 * honoured both, while the inbox list only looked at the scope - so the same app could show Jalali
 * dates in one place and Gregorian ones in another, which is exactly what made the date format look
 * like it changed from one tab to the next.
 *
 * Every screen calls this now, so the calendar can only be decided one way for a given pair of
 * preferences.
 */
fun showsJalaliDates(scope: JalaliScope, showShamsiDates: Boolean): Boolean =
    scope == JalaliScope.EVERYWHERE || showShamsiDates
