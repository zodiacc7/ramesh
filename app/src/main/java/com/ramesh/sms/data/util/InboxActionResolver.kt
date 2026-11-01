package com.ramesh.sms.data.util

import com.ramesh.sms.data.model.FilterAction

/**
 * Decides which action the inbox should show for a message.
 *
 * `spam_message_meta` is a *cache* written when the message was evaluated, so it can be stale.
 * A sender that was blocked after the message arrived must immediately read as spam, otherwise
 * the conversation keeps its old verdict (and never moves to the spam tab) until the app is
 * restarted.
 *
 * @param storedAction the cached action, or null when the message was never evaluated.
 * @return the action to show, or null when the filter engine has to evaluate the message now.
 */
fun resolveInboxAction(
    storedAction: FilterAction?,
    isSenderBlocked: Boolean,
    isContact: Boolean,
    noSpamForContacts: Boolean
): FilterAction? {
    if (isSenderBlocked) return FilterAction.SPAM
    if (storedAction == null) return null
    if (noSpamForContacts && isContact && storedAction == FilterAction.SPAM) {
        return FilterAction.NORMAL
    }
    return storedAction
}

/** True when [action] means the message is spam the user has not chosen to reveal. */
fun FilterAction.isHiddenSpam(isRevealed: Boolean): Boolean =
    this == FilterAction.SPAM && !isRevealed
