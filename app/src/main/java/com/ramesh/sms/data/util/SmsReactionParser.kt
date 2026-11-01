package com.ramesh.sms.data.util

import com.ramesh.sms.data.model.SmsMessage

/**
 * A reaction that arrived as a plain SMS.
 *
 * Not every messaging client speaks a reaction protocol, so some of them fall back to sending the
 * reaction as text - `Reacted 👍 to "see you at 8"`. Rendered naively that becomes a whole extra
 * bubble in the conversation, which looks like the contact answered with a sentence about a
 * reaction. Ramesh folds it back onto the message it refers to instead.
 */
data class SmsReaction(
    /** The emoji (or short emoji run) that was chosen. */
    val emoji: String,
    /** The text of the message it reacted to, as quoted by the sender. */
    val quoted: String
)

/**
 * Recognises reaction messages from the shapes different clients and keyboards emit.
 *
 * Only the emoji-carrying forms are recognised; a message that merely starts with the word
 * "Reacted" and quotes something is still treated as a reaction, because the quoted text has to
 * match an existing message before anything is folded (see [foldSmsReactions]).
 */
object SmsReactionParser {

    // Straight quotes, typographic quotes and the French/Persian guillemets all show up in the wild.
    private const val OPEN = "[\"“«„”]"
    private const val CLOSE = "[\"”»“„]"

    private val patterns = listOf(
        // Reacted 👍 to "text"   /   Reacted with 👍 to "text"
        Regex("^\\s*Reacted\\s+(?:with\\s+)?(\\S{1,12})\\s+to\\s+$OPEN(.+)$CLOSE\\s*$", RegexOption.IGNORE_CASE),
        // Reacted to "text" with 👍
        Regex("^\\s*Reacted\\s+to\\s+$OPEN(.+?)$CLOSE\\s+(?:with\\s+)?(\\S{1,12})\\s*$", RegexOption.IGNORE_CASE),
        // You reacted 👍 to "text"
        Regex("^\\s*You\\s+reacted\\s+(?:with\\s+)?(\\S{1,12})\\s+to\\s+$OPEN(.+)$CLOSE\\s*$", RegexOption.IGNORE_CASE),
        // واکنش 👍 به «متن»
        Regex("^\\s*واکنش\\s+(\\S{1,12})\\s+به\\s+$OPEN(.+)$CLOSE\\s*$"),
        // واکنش به «متن» 👍
        Regex("^\\s*واکنش\\s+به\\s+$OPEN(.+?)$CLOSE\\s+(\\S{1,12})\\s*$"),
        // «متن» را 👍 پسندید
        Regex("^\\s*$OPEN(.+?)$CLOSE\\s+را\\s+(\\S{1,12})\\s+پسندید\\s*$")
    )

    /** Returns the parsed reaction, or null when [body] is an ordinary message. */
    fun parse(body: String): SmsReaction? {
        if (body.isBlank()) return null
        // Cheap pre-filter: the emoji forms always carry one of these words.
        val lowered = body.lowercase()
        val looksLikeReaction = lowered.contains("reacted") ||
            body.contains("واکنش") ||
            body.contains("پسندید")
        if (!looksLikeReaction) return null

        for (pattern in patterns) {
            val match = pattern.matchEntire(body) ?: continue
            val first = match.groupValues[1]
            val second = match.groupValues[2]
            // The emoji group is whichever capture holds no letters or digits; that keeps the
            // regexes readable while still refusing "Reacted to the plan yesterday".
            val emoji = when {
                isEmojiLike(first) && !isEmojiLike(second) -> first
                isEmojiLike(second) && !isEmojiLike(first) -> second
                else -> continue
            }
            val quoted = if (emoji == first) second else first
            if (quoted.isBlank()) continue
            return SmsReaction(emoji = emoji.trim(), quoted = quoted.trim())
        }
        return null
    }

    /** A reaction payload is punctuation/symbols only - no words, no digits, no spaces. */
    private fun isEmojiLike(value: String): Boolean {
        if (value.isBlank()) return false
        return value.none { it.isLetterOrDigit() || it.isWhitespace() }
    }
}

/**
 * Folds reaction SMS messages into the message they refer to and drops them from the list.
 *
 * Iteration runs oldest first, so a reaction is matched against the closest earlier message whose
 * text equals the quoted text. Anything that cannot be matched (the original message was deleted,
 * or the quote differs) is kept as a normal message: losing a real SMS would be worse than showing
 * one extra bubble.
 *
 * A reaction that already exists on the target (set from the picker inside Ramesh) wins, so the
 * local choice is never overwritten by a network reaction - but the extra SMS is still removed
 * from the list, because the reaction is already on screen.
 */
fun foldSmsReactions(messages: List<SmsMessage>): List<SmsMessage> {
    if (messages.size < 2) return messages
    val result = ArrayList<SmsMessage>(messages.size)
    for (message in messages) {
        val reaction = SmsReactionParser.parse(message.body)
        if (reaction == null) {
            result.add(message)
            continue
        }
        val targetIndex = result.indexOfLast { candidate ->
            matchesQuote(candidate.body, reaction.quoted)
        }
        if (targetIndex < 0) {
            // Nothing to fold onto: keep the SMS rather than silently dropping a real message.
            result.add(message)
            continue
        }
        val target = result[targetIndex]
        // A reaction picked inside Ramesh wins; a second network reaction for the same message is
        // swallowed so it cannot turn into a bubble of its own.
        if (target.reaction.isNullOrBlank()) {
            result[targetIndex] = target.copy(reaction = reaction.emoji)
        }
    }
    return result
}

private fun matchesQuote(body: String, quoted: String): Boolean {
    if (body.isBlank()) return false
    val left = body.trim().replace(Regex("\\s+"), " ").lowercase()
    val right = quoted.trim().replace(Regex("\\s+"), " ").lowercase()
    if (left == right) return true
    // Clients truncate long quotes with an ellipsis.
    val rightTrimmed = right.trimEnd('.', '…', ' ', '-')
    return rightTrimmed.length >= 8 && left.startsWith(rightTrimmed)
}
