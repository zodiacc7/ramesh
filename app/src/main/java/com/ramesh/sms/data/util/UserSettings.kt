package com.ramesh.sms.data.util

/**
 * Controls how much of an incoming message may be shown while the device is locked.
 * The full notification is always available once the device is unlocked.
 */
enum class LockScreenPrivacy(val label: String) {
    FULL("Sender and text"),
    SENDER_ONLY("Sender only"),
    HIDDEN("Nothing");

    companion object {
        fun fromName(raw: String?): LockScreenPrivacy =
            entries.firstOrNull { it.name == raw } ?: FULL
    }
}

/** UI language. [SYSTEM] follows the device locale. */
enum class AppLanguage(val label: String, val tag: String?) {
    SYSTEM("System default", null),
    ENGLISH("English", "en"),
    PERSIAN("فارسی", "fa");

    /**
     * Whether the UI is Persian, with [SYSTEM] resolved against the device locale.
     *
     * Date formatting used to compare `this == PERSIAN`, so a Persian phone that left the setting on
     * "System default" was formatted as English - Latin digits and "Shahrivar 23" instead of
     * "۲۳ شهریور" - while the rest of the app was Persian.
     */
    fun isPersian(locale: java.util.Locale = java.util.Locale.getDefault()): Boolean = when (this) {
        PERSIAN -> true
        ENGLISH -> false
        SYSTEM -> locale.language.startsWith("fa") || locale.language.startsWith("pe")
    }

    companion object {
        fun fromName(raw: String?): AppLanguage =
            entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}
