package com.ramesh.sms.ui.util

import androidx.compose.ui.unit.LayoutDirection

/**
 * Decides which way the message composer should read.
 *
 * Persian, Arabic and Hebrew are written right to left, Latin and digits left to right. A message
 * that mixes the two is treated as right to left as soon as it holds one letter of an RTL script:
 * in "سلام 123" the sentence runs from the right, and only the digits inside it stay LTR, which is
 * what the bidi algorithm already does for the characters themselves.
 *
 * A message with no letters at all (only digits, punctuation or emoji) carries no direction of its
 * own, so [fallback] - the app's own layout direction - is kept instead of guessing.
 */
fun resolveTextDirection(text: String, fallback: LayoutDirection): LayoutDirection {
    var hasRtl = false
    var hasLtr = false
    for (ch in text) {
        when {
            isRtlLetter(ch) -> {
                hasRtl = true
            }
            isLtrLetter(ch) -> {
                hasLtr = true
            }
        }
        if (hasRtl) return LayoutDirection.Rtl
    }
    return when {
        hasLtr -> LayoutDirection.Ltr
        else -> fallback
    }
}

/** Persian/Arabic/Hebrew letter blocks. Digits are deliberately not included. */
private fun isRtlLetter(ch: Char): Boolean {
    val code = ch.code
    return (code in 0x0590..0x05FF) || // Hebrew
        (code in 0x0600..0x06FF) || // Arabic + Persian letters
        (code in 0x0750..0x077F) || // Arabic supplement
        (code in 0x08A0..0x08FF) || // Arabic extended-A
        (code in 0xFB50..0xFDFF) || // Arabic presentation forms-A
        (code in 0xFE70..0xFEFF) // Arabic presentation forms-B
}

private fun isLtrLetter(ch: Char): Boolean =
    (ch in 'a'..'z') || (ch in 'A'..'Z') ||
        (ch.code in 0x00C0..0x024F) || // Latin-1 supplement + extended
        (ch.code in 0x0370..0x03FF) || // Greek
        (ch.code in 0x0400..0x04FF) // Cyrillic
