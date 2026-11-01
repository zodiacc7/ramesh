package com.ramesh.sms.data.db

import android.database.Cursor
import androidx.test.core.app.ApplicationProvider
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.model.FilterAction
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The cached snapshot the inbox paints on the next launch.
 *
 * `replaceCachedThreads` runs on every refresh — once per incoming SMS, once per return from a
 * chat — so it is a diff now: only the conversations that actually changed are written and the ones
 * that disappeared are deleted. A SQLite trigger records every write, which is the only way to tell
 * "row already identical" from "row rewritten with the same values" (the primary key is the thread
 * id, so the rowid cannot move).
 *
 * These tests use Robolectric's real SQLite, so the SQL and the column list are exercised too.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RameshDatabaseHelperCacheTest {

    private lateinit var helper: RameshDatabaseHelper

    @Before
    fun setUp() {
        resetSingleton()
        helper = RameshDatabaseHelper.getInstance(ApplicationProvider.getApplicationContext())
        // One database file survives across the methods of this class, so start from a clean cache
        // and hook the writes into a log table this test owns.
        helper.writableDatabase.delete("cached_threads", null, null)
        helper.writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS test_write_log (op TEXT, thread_id INTEGER)")
        helper.writableDatabase.execSQL("DELETE FROM test_write_log")
        helper.writableDatabase.execSQL(
            "CREATE TRIGGER IF NOT EXISTS test_cached_threads_insert AFTER INSERT ON cached_threads " +
                "BEGIN INSERT INTO test_write_log(op, thread_id) VALUES ('insert', NEW.thread_id); END"
        )
        helper.writableDatabase.execSQL(
            "CREATE TRIGGER IF NOT EXISTS test_cached_threads_delete AFTER DELETE ON cached_threads " +
                "BEGIN INSERT INTO test_write_log(op, thread_id) VALUES ('delete', OLD.thread_id); END"
        )
    }

    @After
    fun tearDown() {
        helper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS test_cached_threads_insert")
        helper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS test_cached_threads_delete")
        helper.writableDatabase.execSQL("DROP TABLE IF EXISTS test_write_log")
        helper.close()
        resetSingleton()
    }

    private fun thread(
        id: Long,
        address: String = "+989121234567",
        snippet: String = "سلام",
        unread: Int = 0
    ) = ConversationThread(
        threadId = id,
        address = address,
        contactName = null,
        snippet = snippet,
        date = 1_700_000_000_000 + id,
        messageCount = 3,
        unreadCount = unread,
        newSpamCount = 0,
        hasSpam = false,
        isUnreadSpam = false,
        lastMessageAction = FilterAction.NORMAL,
        subId = 1
    )

    private fun <T> read(block: (Cursor) -> T): T =
        helper.readableDatabase.query("cached_threads", null, null, null, null, null, "thread_id ASC")
            .use(block)

    private fun count(): Int = read { it.count }

    private fun threadIds(): List<Long> = read { cursor ->
        val ids = ArrayList<Long>()
        val idx = cursor.getColumnIndexOrThrow("thread_id")
        while (cursor.moveToNext()) ids.add(cursor.getLong(idx))
        ids
    }

    private fun snippetOf(threadId: Long): String = read { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow("thread_id")
        val snippetIdx = cursor.getColumnIndexOrThrow("snippet")
        var found: String? = null
        while (cursor.moveToNext()) {
            if (cursor.getLong(idIdx) == threadId) found = cursor.getString(snippetIdx)
        }
        found ?: error("thread $threadId is not cached")
    }

    /** The `(op, thread_id)` pairs the triggers recorded since the last [clearWriteLog]. */
    private fun writeLog(): List<Pair<String, Long>> {
        val entries = ArrayList<Pair<String, Long>>()
        helper.readableDatabase.query("test_write_log", arrayOf("op", "thread_id"), null, null, null, null, null)
            .use { cursor ->
                while (cursor.moveToNext()) {
                    entries.add(cursor.getString(0) to cursor.getLong(1))
                }
            }
        return entries
    }

    private fun clearWriteLog() {
        helper.writableDatabase.execSQL("DELETE FROM test_write_log")
    }

    @Test
    fun `the snapshot is written`() = runBlocking {
        helper.replaceCachedThreads(listOf(thread(1), thread(2, snippet = "کجایی")))

        assertEquals(listOf(1L, 2L), threadIds())
        assertEquals("کجایی", snippetOf(2))
        assertEquals(
            listOf("insert" to 1L, "insert" to 2L),
            writeLog()
        )
    }

    @Test
    fun `writing an identical snapshot rewrites nothing`() = runBlocking {
        val snapshot = listOf(thread(1), thread(2))
        helper.replaceCachedThreads(snapshot)
        clearWriteLog()

        helper.replaceCachedThreads(snapshot)

        assertEquals(emptyList<Pair<String, Long>>(), writeLog())
        assertEquals(2, count())
    }

    @Test
    fun `only the conversation that changed is rewritten`() = runBlocking {
        helper.replaceCachedThreads(listOf(thread(1), thread(2)))
        clearWriteLog()

        helper.replaceCachedThreads(listOf(thread(1), thread(2, snippet = "پیام تازه")))

        assertEquals(listOf("insert" to 2L), writeLog())
        assertEquals("پیام تازه", snippetOf(2))
        assertEquals(2, count())
    }

    @Test
    fun `conversations missing from the snapshot are deleted`() = runBlocking {
        helper.replaceCachedThreads(listOf(thread(1), thread(2), thread(3)))
        clearWriteLog()

        helper.replaceCachedThreads(listOf(thread(1), thread(3)))

        assertEquals(listOf("delete" to 2L), writeLog())
        assertEquals(listOf(1L, 3L), threadIds())
    }

    @Test
    fun `an empty snapshot clears the cache`() = runBlocking {
        helper.replaceCachedThreads(listOf(thread(1)))

        helper.replaceCachedThreads(emptyList())

        assertEquals(0, count())
    }

    @Test
    fun `the cached snapshot keeps the spam flags the badges read`() = runBlocking {
        helper.replaceCachedThreads(
            listOf(
                thread(1).copy(
                    unreadCount = 2,
                    newSpamCount = 4,
                    hasSpam = true,
                    isUnreadSpam = true,
                    lastMessageAction = FilterAction.SPAM
                )
            )
        )

        val cached = helper.getCachedThreads().single()

        assertEquals(2, cached.unreadCount)
        assertEquals(4, cached.newSpamCount)
        assertTrue(cached.hasSpam)
        assertTrue(cached.isUnreadSpam)
        assertEquals(FilterAction.SPAM, cached.lastMessageAction)
    }

    @Test
    fun `the columns added by later migrations exist`() {
        // onOpen adds these defensively; a database that skipped a step must still answer the
        // SELECTs the inbox issues.
        val columns = helper.readableDatabase.query("PRAGMA table_info(cached_threads)").use { cursor ->
            val names = HashSet<String>()
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) names.add(cursor.getString(nameIdx))
            names
        }

        assertTrue(columns.containsAll(setOf("thread_id", "new_spam_count", "sub_id", "last_message_action")))
    }

    private fun resetSingleton() {
        // The helper is a singleton over a fixed database name; a fresh instance per test is what
        // points it at the database file of the current Robolectric sandbox.
        val outer = RameshDatabaseHelper::class.java
        val holders = listOfNotNull(
            outer to null,
            runCatching {
                val companion = outer.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
                companion.javaClass to companion
            }.getOrNull()
        )
        for ((holder, instance) in holders) {
            val field = holder.declaredFields.firstOrNull { it.name == "INSTANCE" } ?: continue
            field.isAccessible = true
            field.set(instance, null)
        }
    }
}
