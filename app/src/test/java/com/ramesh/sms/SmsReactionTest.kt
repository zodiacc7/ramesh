package com.ramesh.sms

import android.provider.Telephony
import com.ramesh.sms.data.model.SmsMessage
import com.ramesh.sms.data.util.SmsReactionParser
import com.ramesh.sms.data.util.foldSmsReactions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsReactionTest {

    private fun message(
        id: Long,
        body: String,
        date: Long = id,
        sent: Boolean = false
    ) = SmsMessage(
        id = id,
        threadId = 1L,
        address = "09121234567",
        body = body,
        date = date,
        type = if (sent) Telephony.Sms.MESSAGE_TYPE_SENT else Telephony.Sms.MESSAGE_TYPE_INBOX,
        read = true
    )

    // --- Parsing --------------------------------------------------------------------------------

    @Test
    fun parsesTheShapeAnotherClientSentInTheScreenshot() {
        val parsed = SmsReactionParser.parse("Reacted 👍 to \u201Cگوشی دستت باشه\u201D")

        assertEquals("👍", parsed?.emoji)
        assertEquals("گوشی دستت باشه", parsed?.quoted)
    }

    @Test
    fun parsesStraightQuotesAndTheWithVariants() {
        assertEquals("❤️", SmsReactionParser.parse("Reacted ❤️ to \"see you at 8\"")?.emoji)
        assertEquals("👍", SmsReactionParser.parse("Reacted with 👍 to \"ok\"")?.emoji)
        assertEquals("😂", SmsReactionParser.parse("Reacted to \"that was funny\" with 😂")?.emoji)
        assertEquals(
            "that was funny",
            SmsReactionParser.parse("Reacted to \"that was funny\" with 😂")?.quoted
        )
    }

    @Test
    fun parsesPersianWording() {
        val parsed = SmsReactionParser.parse("واکنش 👍 به «باشه فردا»")

        assertEquals("👍", parsed?.emoji)
        assertEquals("باشه فردا", parsed?.quoted)
    }

    @Test
    fun invisibleBidiMarksFromPersianKeyboardsDoNotBreakParsing() {
        // Persian SMS carry RLM/LRM marks and non-breaking spaces, which made the anchored pattern
        // used before fail on the exact message from the report.
        val variants = listOf(
            "Reacted 👍 to \"گوشی دستت باشه\"",
            "Reacted 👍 to \u200F\"گوشی دستت باشه\"",
            "Reacted 👍 to \"\u200Fگوشی دستت باشه\u200F\"",
            "\u200FReacted 👍 to \"گوشی دستت باشه\"",
            "Reacted\u00a0👍 to \"گوشی دستت باشه\"",
            "Reacted 👍\ufe0f to \"گوشی دستت باشه\"",
            "Reacted 👍 to \"گوشی دستت باشه\"\u200F"
        )

        for (variant in variants) {
            val parsed = SmsReactionParser.parse(variant)
            assertEquals("failed for: $variant", "👍", parsed?.emoji)
            assertEquals("failed for: $variant", "گوشی دستت باشه", parsed?.quoted)
        }
    }

    @Test
    fun aBodyWithInvisibleMarksStillMatchesTheQuote() {
        val original = message(1L, "\u200Fگوشی دستت باشه")
        val reaction = message(2L, "Reacted 👍 to \"گوشی دستت باشه\"")

        val folded = foldSmsReactions(listOf(original, reaction))

        assertEquals(1, folded.size)
        assertEquals("👍", folded[0].reaction)
    }

    @Test
    fun ordinaryMessagesAreNotReactions() {
        assertNull(SmsReactionParser.parse("Reacted to the plan yesterday"))
        assertNull(SmsReactionParser.parse("I reacted badly, sorry"))
        assertNull(SmsReactionParser.parse("سلام، خوبی؟"))
        assertNull(SmsReactionParser.parse(""))
        // Quotes without a reaction word must stay a normal message.
        assertNull(SmsReactionParser.parse("she said \"hello\""))
        // Words, no emoji: nothing to fold.
        assertNull(SmsReactionParser.parse("I reacted - sorry"))
        assertNull(SmsReactionParser.parse("Reacted to the plan yesterday"))
    }

    // --- Folding --------------------------------------------------------------------------------

    @Test
    fun reactionIsAttachedToTheMessageItQuotesAndRemovedFromTheList() {
        val original = message(1L, "گوشی دستت باشه")
        val reaction = message(2L, "Reacted 👍 to \u201Cگوشی دستت باشه\u201D")

        val folded = foldSmsReactions(listOf(original, reaction))

        assertEquals(1, folded.size)
        assertEquals("👍", folded[0].reaction)
        assertEquals("گوشی دستت باشه", folded[0].body)
    }

    @Test
    fun reactionGoesToTheClosestEarlierMatch() {
        val older = message(1L, "ok")
        val newer = message(3L, "ok")
        val reaction = message(4L, "Reacted ❤️ to \"ok\"")

        val folded = foldSmsReactions(listOf(older, newer, reaction))

        assertEquals(2, folded.size)
        assertNull(folded[0].reaction)
        assertEquals("❤️", folded[1].reaction)
    }

    @Test
    fun aReactionWithNothingToReactToStaysAVisibleMessage() {
        // First message in the thread is a reaction: dropping it would hide a real SMS for good.
        val reaction = message(7L, "Reacted 👍 to \"a message that was deleted\"")

        val folded = foldSmsReactions(listOf(reaction))

        assertEquals(1, folded.size)
        assertEquals(reaction.body, folded[0].body)
    }

    @Test
    fun anExistingLocalReactionIsNotOverwritten() {
        val original = message(1L, "hi").copy(reaction = "❤️")
        val reaction = message(2L, "Reacted 👍 to \"hi\"")

        val folded = foldSmsReactions(listOf(original, reaction))

        // The extra bubble is gone, but the emoji the user picked stays.
        assertEquals(1, folded.size)
        assertEquals("❤️", folded[0].reaction)
    }

    @Test
    fun quotedTextWithExtraWhitespaceStillMatches() {
        val original = message(1L, "see   you  at 8")
        val reaction = message(2L, "Reacted 👍 to \"see you at 8\"")

        val folded = foldSmsReactions(listOf(original, reaction))

        assertEquals(1, folded.size)
        assertEquals("👍", folded[0].reaction)
    }

    @Test
    fun aQuoteThatMatchesNothingStillLandsOnTheNewestEarlierMessage() {
        val other = message(1L, "completely different text")
        val reaction = message(2L, "Reacted 👍 to \"a quote that was edited\"")

        val folded = foldSmsReactions(listOf(other, reaction))

        // Every SMS app with reactions keeps the emoji under the message it answers; showing a
        // separate "Reacted ..." bubble is the bug this guards against.
        assertEquals(1, folded.size)
        assertEquals("👍", folded[0].reaction)
    }
}
