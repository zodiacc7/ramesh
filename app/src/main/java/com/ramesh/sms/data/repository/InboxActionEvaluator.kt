package com.ramesh.sms.data.repository

import com.ramesh.sms.data.db.SpamMessageMeta
import com.ramesh.sms.data.db.SpamMetaWrite
import com.ramesh.sms.data.model.FilterAction
import com.ramesh.sms.data.model.SenderPreference
import com.ramesh.sms.data.util.resolveInboxAction
import com.ramesh.sms.engine.PreparedFilterRules
import com.ramesh.sms.engine.SmsFilterEngine

/**
 * Decides the filter action of a message while the inbox is being assembled.
 *
 * Extracted from `SmsRepository.loadThreads`, where it lived as a closure inside a 300-line
 * function: it mutated two lists that the caller then persisted, and it could not be tested without
 * a content provider. The inputs are plain values, so a unit test can pin the rules - a stored
 * verdict wins unless the sender was blocked since, contacts can be exempted, and anything that was
 * never evaluated is evaluated once and remembered for the caller to persist.
 */
class InboxActionEvaluator(
    private val storedMeta: Map<Long, SpamMessageMeta>,
    private val senderPreferences: Map<String, SenderPreference>,
    private val preparedRules: PreparedFilterRules,
    private val noSpamForContacts: Boolean,
    private val normalizeAddress: (String) -> String
) {
    private val evaluated = HashMap<Long, FilterAction>()

    /** Verdicts reached here that are not in [storedMeta] and therefore have to be persisted. */
    val pendingMarks = mutableListOf<SpamMetaWrite>()

    /**
     * The action to show for one message.
     *
     * The cached rules and sender preferences live in memory (see `FilterRulesCache`), so a sender
     * blocked a second ago wins over a stale verdict stored for this message.
     */
    fun actionFor(
        messageId: Long,
        address: String,
        body: String,
        isContact: Boolean
    ): FilterAction {
        evaluated[messageId]?.let { return it }
        val preferred = senderPreferences[normalizeAddress(address)]
        val decided = resolveInboxAction(
            storedAction = storedMeta[messageId]?.action,
            isSenderBlocked = preferred?.isBlocked == true,
            isContact = isContact,
            noSpamForContacts = noSpamForContacts
        )
        val action = decided ?: run {
            val eval = SmsFilterEngine.evaluateMessage(
                sender = address,
                body = body,
                prepared = preparedRules,
                senderPreference = preferred,
                isContact = isContact,
                noSpamForContacts = noSpamForContacts
            )
            if (eval.action != FilterAction.NORMAL) {
                pendingMarks.add(
                    SpamMetaWrite(
                        messageId = messageId,
                        address = address,
                        matchedRuleName = eval.matchedRuleName,
                        action = eval.action
                    )
                )
            }
            eval.action
        }
        evaluated[messageId] = action
        return action
    }
}
