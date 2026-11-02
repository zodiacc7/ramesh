package com.ramesh.sms.data.util

/**
 * A part of a message body that should behave like a link.
 *
 * [start] is inclusive and [end] exclusive, both indices into the *original* body, so a caller can
 * build an `AnnotatedString` from them without copying the text.
 */
data class MessageSpan(
    val start: Int,
    val end: Int,
    val kind: Kind,
    /** The address to open: the URL itself, or the phone number without separators. */
    val value: String
) {
    enum class Kind { URL, PHONE, DATE }
}

/**
 * Finds the pieces of an SMS body that the bubble should make tappable.
 *
 * SMS arrive as flat text: a link, a phone number and a date look exactly like the rest of the
 * sentence. Textra underlines all three and offers the matching action on tap, and that is what
 * this scanner feeds.
 *
 * The order matters. URLs are collected first and their ranges are then excluded from the phone and
 * date passes, because a URL such as `https://bank.example/1404/03/23` contains something that
 * looks like a Jalali date, and a link such as `https://t.me/joinchat/989121234567` ends in
 * something that looks like a phone number. Overlapping spans would give one piece of text two
 * actions.
 *
 * Nothing here touches `android.*`, so the whole scanner is a JVM unit test.
 */
object MessageLinkScanner {

    /** `https://…`, `http://…`, or a bare `www.…` host, up to the next space. */
    private val URL = Regex("""\b(?:https?://|www\.)[^\s<>"']+""", RegexOption.IGNORE_CASE)

    /**
     * A run of 7-15 digits that may carry the usual separators, optionally with a leading `+`.
     *
     * Seven digits is the minimum so short codes (`1000`) and amounts stay plain text, and fifteen
     * the maximum so a 16-digit card number does not become dialable.
     */
    private val PHONE = Regex("""(?<![0-9+])\+?[0-9][0-9\s\-()]{5,18}[0-9](?![0-9])""")

    /** `1404/03/23`, `1404-03-23` and `2026/11/02`. */
    private val DATE = Regex("""(?<![0-9])(?:1[34][0-9]{2}|20[0-9]{2})[/-][01]?[0-9][/-][0-3]?[0-9](?![0-9])""")

    /** Punctuation a sentence glues to a URL: `…/page.` or `…/page,`. */
    private const val TRAILING_PUNCTUATION = ".,;:!?)]}»›…"

    /** Persian and Arabic digit blocks, so a body typed on a Persian keyboard still dials. */
    private const val PERSIAN_ZERO = '\u06F0'
    private const val ARABIC_ZERO = '\u0660'

    fun scan(body: String): List<MessageSpan> {
        if (body.isBlank()) return emptyList()
        // Persian and Arabic digits are folded to Latin *before* matching, one character for one
        // character, so every index found here still points at the same place in the original body.
        // Without this a number typed on a Persian keyboard ("۰۹۱۲ …") was invisible to the patterns
        // while a card number in Latin digits was not.
        val searchable = toLatinDigits(body)
        val claimed = ArrayList<IntRange>(8)
        val spans = ArrayList<MessageSpan>(4)

        for (match in URL.findAll(searchable)) {
            var end = match.range.last + 1
            while (end > match.range.first && searchable[end - 1] in TRAILING_PUNCTUATION) end--
            if (end - match.range.first < 8) continue
            val raw = searchable.substring(match.range.first, end)
            val value = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
            spans.add(MessageSpan(match.range.first, end, MessageSpan.Kind.URL, value))
            claimed.add(match.range.first until end)
        }

        for (match in PHONE.findAll(searchable)) {
            if (claimed.any { overlaps(it, match.range) }) continue
            if (match.value.count { it.isDigit() } !in 7..15) continue
            spans.add(
                MessageSpan(
                    start = match.range.first,
                    end = match.range.last + 1,
                    kind = MessageSpan.Kind.PHONE,
                    value = normalisePhone(match.value)
                )
            )
        }

        for (match in DATE.findAll(searchable)) {
            if (claimed.any { overlaps(it, match.range) }) continue
            if (spans.any { it.kind == MessageSpan.Kind.PHONE && overlaps(it, match.range) }) continue
            spans.add(
                MessageSpan(
                    start = match.range.first,
                    end = match.range.last + 1,
                    kind = MessageSpan.Kind.DATE,
                    value = match.value
                )
            )
        }

        // Sorted by position, because the caller walks the spans once to build an AnnotatedString.
        return spans.sortedBy { it.start }
    }

    /** Keeps a leading `+` and the digits; drops everything a dialer does not need. */
    private fun normalisePhone(raw: String): String {
        val builder = StringBuilder(raw.length)
        raw.forEachIndexed { index, ch ->
            when {
                ch.isDigit() -> builder.append(toLatinDigit(ch))
                ch == '+' && index == 0 -> builder.append('+')
            }
        }
        return builder.toString()
    }

    /** Same-length copy of [text] with Persian and Arabic digits replaced by Latin ones. */
    private fun toLatinDigits(text: String): String {
        var needsWork = false
        for (ch in text) {
            if (ch in PERSIAN_ZERO..'\u06F9' || ch in ARABIC_ZERO..'\u0669') {
                needsWork = true
                break
            }
        }
        if (!needsWork) return text
        val builder = StringBuilder(text.length)
        for (ch in text) builder.append(toLatinDigit(ch))
        return builder.toString()
    }

    private fun toLatinDigit(ch: Char): Char = when (ch) {
        in PERSIAN_ZERO..'\u06F9' -> '0' + (ch - PERSIAN_ZERO)
        in ARABIC_ZERO..'\u0669' -> '0' + (ch - ARABIC_ZERO)
        else -> ch
    }

    private fun overlaps(range: IntRange, other: IntRange): Boolean =
        range.first <= other.last && other.first <= range.last

    private fun overlaps(span: MessageSpan, other: IntRange): Boolean =
        span.start <= other.last && other.first < span.end
}

/**
 * The one-time code inside a body, with the range it occupies, so a menu can highlight it.
 *
 * Wraps [com.ramesh.sms.engine.OtpDetector] so the UI does not have to know the detection
 * heuristic - and so a test can pin "the highlighted range really is the code" without a device.
 */
data class OtpHighlight(val code: String, val start: Int, val end: Int)

fun findOtpHighlight(
    body: String,
    detector: (String) -> String? = com.ramesh.sms.engine.OtpDetector::findCode
): OtpHighlight? {
    val code = detector(body) ?: return null
    // The detector normalises Persian digits before matching, so the code may appear in the body in
    // either script; both forms are tried before giving up.
    val latinIndex = body.indexOf(code)
    if (latinIndex >= 0) return OtpHighlight(code, latinIndex, latinIndex + code.length)
    val persian = buildString { code.forEach { append(toPersian(it)) } }
    val persianIndex = body.indexOf(persian)
    if (persianIndex >= 0) return OtpHighlight(code, persianIndex, persianIndex + persian.length)
    return null
}

private fun toPersian(ch: Char): Char = if (ch in '0'..'9') PERSIAN_ZERO_FOR_CODE + (ch - '0') else ch

private const val PERSIAN_ZERO_FOR_CODE = '\u06F0'
