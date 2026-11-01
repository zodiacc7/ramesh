package com.ramesh.sms.data.repository

import com.ramesh.sms.data.db.SpamMessageMeta
import com.ramesh.sms.data.model.FilterAction
import com.ramesh.sms.data.model.FilterRule
import com.ramesh.sms.data.model.SenderPreference
import com.ramesh.sms.engine.SmsFilterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-message verdicts `SmsRepository.loadThreads` reaches while assembling the inbox.
 *
 * These rules used to be a closure buried inside the loader; they are now a class with plain inputs
 * so they can be pinned here without a content provider.
 */
class InboxActionEvaluatorTest {

    private val spamRule = FilterRule(
        id = 1,
        name = "Discount",
        pattern = "تخفیف",
        action = FilterAction.SPAM
    )

    private val prepared = SmsFilterEngine.prepareRules(listOf(spamRule))

    private fun evaluator(
        stored: Map<Long, SpamMessageMeta> = emptyMap(),
        preferences: Map<String, SenderPreference> = emptyMap(),
        noSpamForContacts: Boolean = false
    ) = InboxActionEvaluator(
        storedMeta = stored,
        senderPreferences = preferences,
        preparedRules = prepared,
        noSpamForContacts = noSpamForContacts,
        normalizeAddress = { it.trim().removePrefix("+") }
    )

    @Test
    fun `a stored verdict is trusted without reevaluating the message`() {
        val subject = evaluator(
            stored = mapOf(7L to SpamMessageMeta(true, "Discount", false, FilterAction.SPAM))
        )

        assertEquals(FilterAction.SPAM, subject.actionFor(7L, "0912", "سلام", false))
        assertTrue("nothing had to be decided", subject.pendingMarks.isEmpty())
    }

    @Test
    fun `an unknown message is evaluated once and remembered for persistence`() {
        val subject = evaluator()

        val first = subject.actionFor(11L, "0912", "۵۰ درصد تخفیف ویژه", false)
        val second = subject.actionFor(11L, "0912", "۵۰ درصد تخفیف ویژه", false)

        assertEquals(FilterAction.SPAM, first)
        assertEquals(FilterAction.SPAM, second)
        assertEquals(1, subject.pendingMarks.size)
        assertEquals(11L, subject.pendingMarks.single().messageId)
        assertEquals("Discount", subject.pendingMarks.single().matchedRuleName)
    }

    @Test
    fun `a normal message leaves no pending mark`() {
        val subject = evaluator()

        assertEquals(FilterAction.NORMAL, subject.actionFor(12L, "0912", "سلام علی جان", false))
        assertTrue(subject.pendingMarks.isEmpty())
    }

    @Test
    fun `a sender blocked a second ago wins over a stored normal verdict`() {
        // The stored verdict is a cache: blocking the sender must show immediately.
        val subject = evaluator(
            stored = mapOf(13L to SpamMessageMeta(false, null, false, FilterAction.NORMAL)),
            preferences = mapOf("0912" to SenderPreference(address = "0912", isBlocked = true))
        )

        assertEquals(FilterAction.SPAM, subject.actionFor(13L, "0912", "سلام", false))
        assertTrue("the verdict came from the preference, not the engine", subject.pendingMarks.isEmpty())
    }

    @Test
    fun `sender preferences are looked up through the normalizer`() {
        val subject = evaluator(
            preferences = mapOf("989121234567" to SenderPreference(address = "989121234567", isBlocked = true))
        )

        assertEquals(
            FilterAction.SPAM,
            subject.actionFor(14L, "+989121234567", "سلام", false)
        )
    }

    @Test
    fun `the contact exemption turns a stored spam verdict into a normal one`() {
        val subject = evaluator(
            stored = mapOf(15L to SpamMessageMeta(true, "Discount", false, FilterAction.SPAM)),
            noSpamForContacts = true
        )

        assertEquals(FilterAction.NORMAL, subject.actionFor(15L, "0912", "تخفیف", true))
    }

    @Test
    fun `a silent message is stored with its own action`() {
        val silentRules = SmsFilterEngine.prepareRules(
            listOf(FilterRule(id = 2, name = "Bank", pattern = "موجودی", action = FilterAction.SILENT))
        )
        val subject = InboxActionEvaluator(
            storedMeta = emptyMap(),
            senderPreferences = emptyMap(),
            preparedRules = silentRules,
            noSpamForContacts = false,
            normalizeAddress = { it }
        )

        assertEquals(
            FilterAction.SILENT,
            subject.actionFor(16L, "1000", "موجودی حساب شما ۵۰۰ هزار تومان است", false)
        )
        assertEquals(FilterAction.SILENT, subject.pendingMarks.single().action)
    }
}
