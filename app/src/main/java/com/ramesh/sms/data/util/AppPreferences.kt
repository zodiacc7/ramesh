package com.ramesh.sms.data.util

import android.content.Context
import android.content.SharedPreferences

interface UserPreferences {
    var showContactsOnly: Boolean
    var showShamsiDate: Boolean
    var enableDateTapShamsiToggle: Boolean
    var showNotificationDeleteAction: Boolean
    var showTabs: Boolean
    var showSimFilter: Boolean
    var noSpamForContacts: Boolean

    /** Position of the "All" tab inside the tab strip (0 = first). */
    var allTabIndex: Int

    /** Ask the network for a delivery report for every outgoing message. */
    var deliveryReports: Boolean

    /** Seconds to wait before actually dispatching an outgoing message (0 = send now). */
    var sendDelaySeconds: Int

    /** Keep at most this many messages per conversation (0 = unlimited). */
    var autoDeleteLimit: Int

    /** How much of a message may appear on the lock screen. */
    var lockScreenPrivacy: LockScreenPrivacy

    /** UI language; [AppLanguage.SYSTEM] follows the device. */
    var appLanguage: AppLanguage

    /** Show the SIM slot badge next to conversations when the phone has more than one SIM. */
    var showSimBadge: Boolean

    /** Whether the Jalali calendar is used only inside chats or everywhere in the app. */
    var jalaliScope: JalaliScope

    /**
     * The number the "report spam" action sends to.
     *
     * Empty by default on purpose: there is no universal short code. `7726` is the British and
     * American one, while Iranian operators publish their own, so the user provides it once.
     */
    var spamReportNumber: String
}

class AppPreferences(
    private val prefs: SharedPreferences
) : UserPreferences {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override var showContactsOnly: Boolean
        get() = prefs.getBoolean(KEY_SHOW_CONTACTS_ONLY, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_CONTACTS_ONLY, value).apply()
        }

    override var showShamsiDate: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SHAMSI_DATE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_SHAMSI_DATE, value).apply()
        }

    override var enableDateTapShamsiToggle: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_DATE_TAP_SHAMSI_TOGGLE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLE_DATE_TAP_SHAMSI_TOGGLE, value).apply()
        }

    override var showNotificationDeleteAction: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NOTIFICATION_DELETE_ACTION, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_NOTIFICATION_DELETE_ACTION, value).apply()
        }

    override var showTabs: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TABS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_TABS, value).apply()
        }

    override var showSimFilter: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SIM_FILTER, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_SIM_FILTER, value).apply()
        }

    override var noSpamForContacts: Boolean
        get() = prefs.getBoolean(KEY_NO_SPAM_FOR_CONTACTS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_NO_SPAM_FOR_CONTACTS, value).apply()
        }

    override var allTabIndex: Int
        get() = prefs.getInt(KEY_ALL_TAB_INDEX, 0)
        set(value) {
            prefs.edit().putInt(KEY_ALL_TAB_INDEX, value).apply()
        }

    override var deliveryReports: Boolean
        get() = prefs.getBoolean(KEY_DELIVERY_REPORTS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DELIVERY_REPORTS, value).apply()
        }

    override var sendDelaySeconds: Int
        get() = prefs.getInt(KEY_SEND_DELAY_SECONDS, 0)
        set(value) {
            prefs.edit().putInt(KEY_SEND_DELAY_SECONDS, value.coerceIn(0, 30)).apply()
        }

    override var autoDeleteLimit: Int
        get() = prefs.getInt(KEY_AUTO_DELETE_LIMIT, 0)
        set(value) {
            prefs.edit().putInt(KEY_AUTO_DELETE_LIMIT, value.coerceAtLeast(0)).apply()
        }

    override var lockScreenPrivacy: LockScreenPrivacy
        get() = LockScreenPrivacy.fromName(prefs.getString(KEY_LOCK_SCREEN_PRIVACY, null))
        set(value) {
            prefs.edit().putString(KEY_LOCK_SCREEN_PRIVACY, value.name).apply()
        }

    override var appLanguage: AppLanguage
        get() = AppLanguage.fromName(prefs.getString(KEY_APP_LANGUAGE, null))
        set(value) {
            prefs.edit().putString(KEY_APP_LANGUAGE, value.name).apply()
        }

    override var showSimBadge: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SIM_BADGE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_SIM_BADGE, value).apply()
        }

    override var jalaliScope: JalaliScope
        get() = JalaliScope.fromName(prefs.getString(KEY_JALALI_SCOPE, null))
        set(value) {
            prefs.edit().putString(KEY_JALALI_SCOPE, value.name).apply()
        }

    override var spamReportNumber: String
        get() = prefs.getString(KEY_SPAM_REPORT_NUMBER, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_SPAM_REPORT_NUMBER, value.trim()).apply()
        }

    companion object {
        const val PREFS_NAME = "ramesh_preferences"
        const val KEY_SHOW_CONTACTS_ONLY = "show_contacts_only"
        const val KEY_SHOW_SHAMSI_DATE = "show_shamsi_date"
        const val KEY_ENABLE_DATE_TAP_SHAMSI_TOGGLE = "enable_date_tap_shamsi_toggle"
        const val KEY_SHOW_NOTIFICATION_DELETE_ACTION = "show_notification_delete_action"
        const val KEY_SHOW_TABS = "show_tabs"
        const val KEY_SHOW_SIM_FILTER = "show_sim_filter"
        const val KEY_NO_SPAM_FOR_CONTACTS = "no_spam_for_contacts"
        const val KEY_ALL_TAB_INDEX = "all_tab_index"
        const val KEY_DELIVERY_REPORTS = "delivery_reports"
        const val KEY_SEND_DELAY_SECONDS = "send_delay_seconds"
        const val KEY_AUTO_DELETE_LIMIT = "auto_delete_limit"
        const val KEY_LOCK_SCREEN_PRIVACY = "lock_screen_privacy"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_SHOW_SIM_BADGE = "show_sim_badge"
        const val KEY_JALALI_SCOPE = "jalali_scope"
        const val KEY_SPAM_REPORT_NUMBER = "spam_report_number"
    }
}
