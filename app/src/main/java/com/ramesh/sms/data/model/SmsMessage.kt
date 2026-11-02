package com.ramesh.sms.data.model

import android.provider.Telephony
import android.telephony.SubscriptionManager
import androidx.compose.runtime.Immutable

@Immutable
data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int, // 1 = INBOX, 2 = SENT, 3 = DRAFT, etc.
    val read: Boolean,
    val isSpam: Boolean = false,
    val matchedRuleName: String? = null,
    val isRevealed: Boolean = false,
    val status: Int = Telephony.Sms.STATUS_NONE,
    val subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
    /** Emoji reaction attached locally to this message, if any. */
    val reaction: String? = null,
    /**
     * A filtered spam message nobody has opened yet.
     *
     * This is the same flag the inbox badge counts (`spam_message_meta.is_new`), carried down to the
     * message itself: without it the badge said "2" while nothing on screen showed which two
     * messages it meant.
     */
    val isNewSpam: Boolean = false
) {
    val isInbox: Boolean get() = type == 1
    val isSent: Boolean get() = type == 2
    val isDelivered: Boolean
        get() = isSent && status == Telephony.Sms.STATUS_COMPLETE
    val isFailed: Boolean
        get() = isSent && (status == Telephony.Sms.STATUS_FAILED || type == Telephony.Sms.MESSAGE_TYPE_FAILED)
}

@Immutable
data class ConversationThread(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val snippet: String,
    val date: Long,
    val messageCount: Int,
    val unreadCount: Int,
    /**
     * Spam messages from this sender the user has not opened yet. Spam arrives silently and is
     * stored read, so it never shows up in [unreadCount]; the tab badges add the two together.
     */
    val newSpamCount: Int = 0,
    val hasSpam: Boolean = false,
    val isUnreadSpam: Boolean = false,
    val lastMessageAction: FilterAction = FilterAction.NORMAL,
    /**
     * The newest filtered-spam message in this conversation, when the newest received message is the
     * spam one. The inbox uses it to open a spam row *at* the message that was filtered instead of
     * leaving the user to hunt for it. Not persisted in the cached snapshot: a stale id there is
     * worse than none, so it is filled only on a live provider read and falls back to null.
     */
    val newestSpamMessageId: Long? = null,
    /** True when every received message in this thread is spam, so the thread holds no content. */
    val spamOnly: Boolean = false,
    val subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
    val photoUri: String? = null,
    val contactLookupUri: String? = null
) {
    val isContact: Boolean get() = !contactName.isNullOrBlank()
    val isLastReceivedSpam: Boolean get() = lastMessageAction == FilterAction.SPAM
}

@Immutable
data class SearchMessageResult(
    val messageId: Long,
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val body: String,
    val date: Long,
    val read: Boolean,
    val type: Int,
    val isSpam: Boolean = false,
    val subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
    val photoUri: String? = null,
    val contactLookupUri: String? = null
)
