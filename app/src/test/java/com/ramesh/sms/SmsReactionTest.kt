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
    fun ordinaryMessagesAreNotReactions() {
        assertNull(SmsReactionParser.parse("Reacted to the plan yesterday"))
        assertNull(SmsReactionParser.parse("I reacted badly, sorry"))
        assertNull(SmsReactionParser.parse("سلام، خوبی؟"))
        assertNull(SmsReactionParser.parse(""))
        // Quotes without a reaction word must stay a normal message.
        assertNull(SmsReactionParser.parse("she said \"hello\""))
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
    fun unmatchedReactionStaysVisibleInsteadOfBeingSwallowed() {
        val reaction = message(7L, "Reacted 👍 to \"a message that was deleted\"")

        val folded = foldSmsReactions(listOf(message(1L, "something else"), reaction))

        assertEquals(2, folded.size)
        assertEquals(reaction.body, folded[1].body)
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
    fun shortQuotesDoNotMatchUnrelatedMessages() {
        val other = message(1L, "ok then")
        val reaction = message(2L, "Reacted 👍 to \"ok\"")

        val folded = foldSmsReactions(listOf(other, reaction))

        // Two characters is too little to guess; the message is kept.
        assertEquals(2, folded.size)
    }
}
