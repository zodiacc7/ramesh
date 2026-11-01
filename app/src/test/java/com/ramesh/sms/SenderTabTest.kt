package com.ramesh.sms

import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.model.FilterAction
import com.ramesh.sms.data.model.SenderTab
import com.ramesh.sms.data.util.PhoneNumberKeys
import com.ramesh.sms.data.util.badgeCountForTab
import com.ramesh.sms.data.util.countableThreadsForTab
import com.ramesh.sms.data.util.visibleThreadsForTab
import com.ramesh.sms.ui.components.isThreadInTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderTabTest {

    private fun dummyThread(
        threadId: Long,
        address: String,
        contactName: String? = null,
        snippet: String = "Hello"
    ) = ConversationThread(
        threadId = threadId,
        address = address,
        contactName = contactName,
        snippet = snippet,
        date = 1000L,
        messageCount = 1,
        unreadCount = 0,
        lastMessageAction = FilterAction.NORMAL
    )

    @Test
    fun isThreadInTabMatchesExactAndCanonicalAddresses() {
        val tab = SenderTab(
            id = 1L,
            name = "Family",
            senderAddresses = setOf(PhoneNumberKeys.canonical("09121234567"))
        )

        val threadPlus = dummyThread(1L, "+989121234567", "Dad")
        val threadZero = dummyThread(2L, "09121234567", "Dad")
        val threadNational = dummyThread(3L, "9121234567", "Dad")
        val threadOther = dummyThread(4L, "09351234567", "Friend")

        assertTrue(isThreadInTab(threadPlus, tab))
        assertTrue(isThreadInTab(threadZero, tab))
        assertTrue(isThreadInTab(threadNational, tab))
        assertFalse(isThreadInTab(threadOther, tab))
    }

    @Test
    fun isThreadInTabMatchesAlphanumericSenders() {
        val tab = SenderTab(
            id = 2L,
            name = "Services",
            senderAddresses = setOf("Digikala", "Snapp")
        )

        val threadDigikala = dummyThread(1L, "Digikala")
        val threadSnapp = dummyThread(2L, "Snapp")
        val threadBank = dummyThread(3L, "BankMellat")

        assertTrue(isThreadInTab(threadDigikala, tab))
        assertTrue(isThreadInTab(threadSnapp, tab))
        assertFalse(isThreadInTab(threadBank, tab))
    }

    @Test
    fun emptyTabMatchesNothing() {
        val emptyTab = SenderTab(id = 3L, name = "Empty", senderAddresses = emptySet())
        val thread = dummyThread(1L, "09121234567", "Mom")
        assertFalse(isThreadInTab(thread, emptyTab))
    }

    @Test
    fun tabFilteringSeparatesDifferentGroups() {
        val familyTab = SenderTab(
            id = 1L,
            name = "Family",
            senderAddresses = setOf(PhoneNumberKeys.canonical("09121111111"), PhoneNumberKeys.canonical("09122222222"))
        )
        val friendsTab = SenderTab(
            id = 2L,
            name = "Friends",
            senderAddresses = setOf(PhoneNumberKeys.canonical("09353333333"))
        )

        val dad = dummyThread(1L, "09121111111", "Dad")
        val mom = dummyThread(2L, "+989122222222", "Mom")
        val friend = dummyThread(3L, "09353333333", "Ali")
        val coworker = dummyThread(4L, "09194444444", "Reza")

        val allThreads = listOf(dad, mom, friend, coworker)

        val familyThreads = allThreads.filter { isThreadInTab(it, familyTab) }
        val friendsThreads = allThreads.filter { isThreadInTab(it, friendsTab) }

        assertEquals(listOf(dad, mom), familyThreads)
        assertEquals(listOf(friend), friendsThreads)
    }

    @Test
    fun tabsSortBySortOrderAscendingThenIdAscending() {
        val tab1 = SenderTab(id = 10L, name = "Tab 10", sortOrder = 2)
        val tab2 = SenderTab(id = 5L, name = "Tab 5", sortOrder = 0)
        val tab3 = SenderTab(id = 7L, name = "Tab 7", sortOrder = 1)
        val tab4 = SenderTab(id = 2L, name = "Tab 2", sortOrder = 1)

        val unsorted = listOf(tab1, tab2, tab3, tab4)
        val sorted = unsorted.sortedWith(compareBy<SenderTab> { it.sortOrder }.thenBy { it.id })

        assertEquals(listOf(tab2, tab4, tab3, tab1), sorted)
    }

    @Test
    fun reorderingTabsUpdatesSortOrderIndices() {
        val tabA = SenderTab(id = 1L, name = "A", sortOrder = 0)
        val tabB = SenderTab(id = 2L, name = "B", sortOrder = 1)
        val tabC = SenderTab(id = 3L, name = "C", sortOrder = 2)

        val initialList = listOf(tabA, tabB, tabC)
        // User moves C to the first position
        val reordered = listOf(tabC, tabA, tabB)
        val updatedWithOrder = reordered.mapIndexed { index, tab -> tab.copy(sortOrder = index) }

        assertEquals(0, updatedWithOrder[0].sortOrder)
        assertEquals("C", updatedWithOrder[0].name)
        assertEquals(1, updatedWithOrder[1].sortOrder)
        assertEquals("A", updatedWithOrder[1].name)
        assertEquals(2, updatedWithOrder[2].sortOrder)
        assertEquals("B", updatedWithOrder[2].name)
    }

    // --- Silent spam badge counting -------------------------------------------------------------
    // Spam is stored read and never notifies, so `unreadCount` alone would leave the tab badge at
    // zero. These tests pin the rule that the badge = unread + new spam, including spam-only
    // conversations that the inbox itself hides.

    private fun unreadThread(
        threadId: Long,
        address: String,
        unreadCount: Int = 0,
        newSpamCount: Int = 0,
        spamOnly: Boolean = false
    ) = dummyThread(threadId, address).copy(
        unreadCount = unreadCount,
        newSpamCount = newSpamCount,
        spamOnly = spamOnly,
        hasSpam = spamOnly || newSpamCount > 0
    )

    @Test
    fun badgeAddsUnreadMessagesAndUnseenSpam() {
        val threads = listOf(
            unreadThread(1L, "09121234567", unreadCount = 3),
            unreadThread(2L, "09127654321", newSpamCount = 2)
        )

        assertEquals(5, badgeCountForTab(threads, null))
    }

    @Test
    fun badgeCountsASpamOnlyConversationTheInboxHides() {
        val spamOnly = unreadThread(1L, "09120000000", newSpamCount = 1, spamOnly = true)

        // The inbox hides spam-only threads, but the arrival still has to be visible somewhere.
        assertTrue(visibleThreadsForTab(listOf(spamOnly), null).isEmpty())
        assertEquals(1, badgeCountForTab(listOf(spamOnly), null))
        assertEquals(listOf(spamOnly), countableThreadsForTab(listOf(spamOnly), null))
    }

    @Test
    fun badgeIgnoresEmptyThreadsAndOtherTabs() {
        val tab = SenderTab(
            id = 1L,
            name = "Work",
            senderAddresses = setOf(PhoneNumberKeys.canonical("09121112222"))
        )
        val inTab = unreadThread(1L, "09121112222", unreadCount = 1)
        val otherTab = unreadThread(2L, "09129998888", newSpamCount = 4)
        val empty = unreadThread(3L, "09121112222").copy(messageCount = 0)

        val threads = listOf(inTab, otherTab, empty)

        assertEquals(1, badgeCountForTab(threads, tab))
        assertEquals(5, badgeCountForTab(threads, null))
    }
}
