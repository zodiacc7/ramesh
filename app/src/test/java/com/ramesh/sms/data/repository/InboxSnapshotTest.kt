package com.ramesh.sms.data.repository

import com.ramesh.sms.data.model.FilterAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The inbox maths that used to live inside `SmsRepository.loadThreads`.
 *
 * Every case here corresponds to a rule the 300-line function encoded implicitly: unread counts are
 * per thread, a reaction SMS is not a bubble, the newest row wins, and a conversation is spam-only
 * only when all of its messages are spam.
 */
class InboxSnapshotTest {

    private fun row(id: Long, threadId: Long, body: String) = InboxRow(id, threadId, body)

    @Test
    fun `unread counts are per thread`() {
        val snapshot = buildUnreadSnapshot(
            listOf(
                row(5, 1, "سلام"),
                row(4, 1, "چطوری"),
                row(3, 2, "کجایی")
            )
        )

        assertEquals(2, snapshot.countFor(1))
        assertEquals(1, snapshot.countFor(2))
        assertEquals(0, snapshot.countFor(99))
    }

    @Test
    fun `a reaction sms is not counted but still provides the newest unread message`() {
        // A reaction is folded onto the bubble it quotes, so it must not raise the badge; it is
        // still the newest row of the thread, which is what the spam check looks at.
        val reaction = "👍 به پیام «سلام» واکنش داد"
        val snapshot = buildUnreadSnapshot(
            listOf(
                row(9, 1, reaction),
                row(8, 1, "سلام")
            ),
            isReaction = { it.contains("واکنش") }
        )

        assertEquals(1, snapshot.countFor(1))
        assertEquals(9L, snapshot.latestFor(1)?.messageId)
    }

    @Test
    fun `empty snapshot counts nothing`() {
        val snapshot = buildUnreadSnapshot(emptyList())

        assertEquals(0, snapshot.countFor(1))
        assertNull(snapshot.latestFor(1))
    }

    @Test
    fun `newest row wins for the latest inbox message`() {
        val latest = latestInboxByThread(
            listOf(
                row(30, 2, "تازه"),
                row(20, 1, "قدیمی‌تر"),
                row(10, 2, "قدیمی")
            )
        )

        assertEquals(20L, latest.getValue(1).messageId)
        assertEquals(30L, latest.getValue(2).messageId)
        assertEquals(2, latest.size)
    }

    @Test
    fun `a thread is spam only when every message is spam`() {
        val rows = listOf(
            row(1, 10, "تخفیف"),
            row(2, 10, "وام"),
            row(3, 11, "تخفیف"),
            row(4, 11, "سلام")
        )

        val spamOnly = spamOnlyThreads(rows) { inboxRow ->
            if (inboxRow.body == "سلام") FilterAction.NORMAL else FilterAction.SPAM
        }

        assertEquals(setOf(10L), spamOnly)
    }

    @Test
    fun `a thread whose messages were never read is not spam only`() {
        // Guessing "spam only" for a thread without rows would hide a real conversation.
        assertTrue(spamOnlyThreads(emptyList()) { FilterAction.SPAM }.isEmpty())

        val spamOnly = spamOnlyThreads(listOf(row(1, 10, "تخفیف"))) { FilterAction.SPAM }
        assertEquals(setOf(10L), spamOnly)
    }

    @Test
    fun `one normal message is enough to keep the thread visible`() {
        val spamOnly = spamOnlyThreads(
            listOf(
                row(1, 10, "تخفیف"),
                row(2, 10, "سلام"),
                row(3, 10, "تخفیف")
            )
        ) { if (it.body == "سلام") FilterAction.NORMAL else FilterAction.SPAM }

        assertFalse(10L in spamOnly)
    }
}
