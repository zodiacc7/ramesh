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
    val quoted: String?
)

/**
 * Recognises reaction messages from the shapes different clients and keysboards emit.
 *
 * Parsing is deliberately *not* anchored to the whole string. Persian SMS carry invisible
 * direction marks (RLM/LRM) and Arabic keyboards emit non-breaking spaces, and an anchored pattern
 * silently fails on both - which is exactly how a reaction sentence kept reaching the chat as a
 * normal bubble. Everything is normalised first, and the reaction is then located inside the text.
 */
object SmsReactionParser {

    /** Invisible direction marks, byte-order marks, non-breaking spaces and emoji variation selectors. */
    private val INVISIBLE = Regex("[\\u200b-\\u200f\\u202a-\\u202e\\u2066-\\u2069\\u00a0\\u202f\\u2060\\ufeff\\ufe0e\\ufe0f]")

    private const val OPEN = "\"“«„”"
    private const val CLOSE = "\"”»“„"

    /** `"quoted text"` or `«quoted text»`, unanchored. */
    private val QUOTED = Regex("[$OPEN]([^$OPEN$CLOSE]{1,400})[$CLOSE]")

    /**
     * Removes the invisible characters that make an identical-looking message parse differently.
     * Also applied to message bodies before the quoted text is compared with them.
     */
    fun normalize(text: String): String = INVISIBLE.replace(text, " ").trim()

    /**
     * Returns the parsed reaction, or null when [body] is an ordinary message.
     *
     * A reaction is only recognised when the text also carries one of the reaction words; a plain
     * sentence such as `I reacted badly, sorry` or `Reacted to the plan yesterday` has no emoji-ish
     * token and is therefore left alone.
     */
    fun parse(body: String): SmsReaction? {
        if (body.isBlank()) return null
        val cleaned = normalize(body)
        if (cleaned.isBlank()) return null

        val lowered = cleaned.lowercase()
        val hasReactionWord = lowered.contains("reacted") ||
            lowered.startsWith("react") ||
            cleaned.contains("واکنش") ||
            cleaned.contains("پسندید")
        if (!hasReactionWord) return null

        val quote = QUOTED.find(cleaned)
        // The emoji is the first run of emoji code points in the message. Scanning the whole message
        // (and not only the text around the quote) tolerates word order differences between clients,
        // while insisting on a real emoji keeps a sentence such as "I reacted - sorry" from being
        // swallowed as a reaction.
        val emoji = cleaned
            .split(Regex("\\s+"))
            .firstNotNullOfOrNull { token -> EMOJI_RUN.find(token)?.value }
            ?: return null

        val quoted = quote?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        return SmsReaction(emoji = emoji, quoted = quoted)
    }

    /**
     * One emoji "atom": an astral code point (a surrogate pair, which is how 👍 is stored in UTF-16)
     * or one of the BMP symbols used for emoji. Written as UTF-16 ranges on purpose - a plain
     * `ch.code in 0x1F000..0x1FAFF` check never matches, because such an emoji is two chars.
     */
    private const val ATOM =
        "(?:[\uD800-\uDBFF][\uDC00-\uDFFF]|[\u2600-\u27BF\u2B00-\u2BFF" +
            "\u2190-\u21FF\u2900-\u297F\u2764\uFE30-\uFE4F])"

    /** A run of emoji atoms plus the joiners, variation selectors and skin tones between them. */
    private val EMOJI_RUN = Regex("$ATOM(?:[\uFE0E\uFE0F\u200D]|$ATOM)*")
}

/**
 * Folds reaction SMS messages into the message they refer to and drops them from the list.
 *
 * Iteration runs oldest first. A reaction is matched against the closest earlier message whose text
 * equals the quoted text; when the quote cannot be matched (the original was deleted, or a client
 * quoted something slightly different) it falls back to the closest earlier message, which is what
 * every SMS app that supports reactions does. Only when there is nothing at all to react to does
 * the SMS stay a message, because dropping it would hide a real message for good.
 *
 * A reaction that already exists on the target (set from the picker inside Ramesh) wins, so the
 * local choice is never overwritten by a network reaction.
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
        val quoted = reaction.quoted
        val exactIndex = if (quoted.isNullOrBlank()) {
            -1
        } else {
            result.indexOfLast { candidate -> matchesQuote(candidate.body, quoted) }
        }
        // A quote that matches nothing (deleted original, truncated text) still belongs under the
        // newest earlier message, which is what every SMS app with reaction support does. Without a
        // quote at all the shape is unusual, so it is only folded when there is a plain target.
        val fallbackIndex = if (exactIndex >= 0) {
            exactIndex
        } else if (quoted == null) {
            result.indexOfLast { it.reaction.isNullOrBlank() }
        } else {
            result.indexOfLast { it.reaction.isNullOrBlank() }.takeIf { it >= 0 }
                ?: result.indexOfLast { true }
        }
        if (fallbackIndex < 0) {
            result.add(message)
            continue
        }
        val target = result[fallbackIndex]
        if (target.reaction.isNullOrBlank()) {
            result[fallbackIndex] = target.copy(reaction = reaction.emoji)
        }
    }
    return result
}

private fun matchesQuote(body: String, quoted: String): Boolean {
    if (body.isBlank()) return false
    val left = SmsReactionParser.normalize(body).replace(Regex("\\s+"), " ").lowercase()
    val right = SmsReactionParser.normalize(quoted).replace(Regex("\\s+"), " ").lowercase()
    if (left == right) return true
    // Clients truncate long quotes with an ellipsis.
    val rightTrimmed = right.trimEnd('.', '…', ' ', '-')
    return rightTrimmed.length >= 4 && left.startsWith(rightTrimmed)
}
