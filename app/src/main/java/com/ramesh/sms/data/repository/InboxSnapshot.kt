package com.ramesh.sms.data.repository

import com.ramesh.sms.data.model.FilterAction
import com.ramesh.sms.data.util.SmsReactionParser

/**
 * One row of the telephony inbox, as read by [SmsRepository].
 *
 * Keeping the provider rows in a plain data class is what makes the inbox assembly testable: the
 * counting, the spam-only detection and the "newest message" lookup are pure functions over this
 * list, so they run in a JVM unit test without an emulator or a content provider.
 */
data class InboxRow(
    val messageId: Long,
    val threadId: Long,
    val body: String
)

/** Unread counters for the whole inbox, built from a single provider query. */
data class UnreadSnapshot(
    val countByThread: Map<Long, Int>,
    val latestUnreadByThread: Map<Long, InboxRow>
) {
    fun countFor(threadId: Long): Int = countByThread[threadId] ?: 0
    fun latestFor(threadId: Long): InboxRow? = latestUnreadByThread[threadId]
}

/**
 * Counts unread messages per thread.
 *
 * [rows] must be ordered newest first, because the first row seen for a thread is its newest unread
 * message. A reaction SMS is *not* counted: it is folded onto the message it quotes and produces no
 * bubble of its own, so counting it would leave the badge pointing at a message the user cannot see.
 */
fun buildUnreadSnapshot(
    rows: List<InboxRow>,
    isReaction: (String) -> Boolean = { SmsReactionParser.parse(it) != null }
): UnreadSnapshot {
    if (rows.isEmpty()) return UnreadSnapshot(emptyMap(), emptyMap())
    val counts = HashMap<Long, Int>()
    val latest = HashMap<Long, InboxRow>()
    for (row in rows) {
        if (!isReaction(row.body)) {
            counts[row.threadId] = (counts[row.threadId] ?: 0) + 1
        }
        if (!latest.containsKey(row.threadId)) {
            latest[row.threadId] = row
        }
    }
    return UnreadSnapshot(counts, latest)
}

/**
 * Newest inbox row per thread. [rows] must be ordered newest first.
 *
 * The repository keeps the raw body of the newest inbox message so the inbox snippet can be
 * rewritten when that message is a reaction.
 */
fun latestInboxByThread(rows: List<InboxRow>): Map<Long, InboxRow> {
    if (rows.isEmpty()) return emptyMap()
    val latest = LinkedHashMap<Long, InboxRow>()
    for (row in rows) {
        if (!latest.containsKey(row.threadId)) latest[row.threadId] = row
    }
    return latest
}

/**
 * Threads whose every received message resolves to spam.
 *
 * Such a conversation has nothing left to show, so the inbox hides it entirely; it must not surface
 * as an empty row or appear in a "not spam" tab.
 *
 * This used to be one provider query per conversation (`THREAD_ID = ?` inside a loop), which is an
 * N+1: with fifty spam conversations reaching the inbox meant fifty extra queries on every refresh.
 * The caller now reads the messages of every candidate thread in a single batched query and hands
 * the rows here.
 *
 * A thread with no rows is *not* spam-only: guessing "yes" would hide a real conversation.
 */
fun spamOnlyThreads(
    rows: List<InboxRow>,
    actionOf: (InboxRow) -> FilterAction
): Set<Long> {
    if (rows.isEmpty()) return emptySet()
    val spamOnly = LinkedHashSet<Long>()
    val seen = LinkedHashSet<Long>()
    val failed = HashSet<Long>()
    for (row in rows) {
        seen.add(row.threadId)
        if (row.threadId in failed) continue
        if (actionOf(row) != FilterAction.SPAM) {
            failed.add(row.threadId)
        }
    }
    for (threadId in seen) {
        if (threadId !in failed) spamOnly.add(threadId)
    }
    return spamOnly
}
