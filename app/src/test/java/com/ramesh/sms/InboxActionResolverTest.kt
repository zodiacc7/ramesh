package com.ramesh.sms

import com.ramesh.sms.data.model.FilterAction
import com.ramesh.sms.data.util.resolveInboxAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests for the inbox verdict of a message.
 *
 * The bug these cover: blocking a sender used to leave the cached SPAM/SILENT verdict in
 * place, so a conversation never moved to the spam tab until the app was restarted.
 */
class InboxActionResolverTest {

    @Test
    fun blockingASenderOverridesACachedNormalVerdict() {
        assertEquals(
            FilterAction.SPAM,
            resolveInboxAction(
                storedAction = FilterAction.NORMAL,
                isSenderBlocked = true,
                isContact = false,
                noSpamForContacts = false
            )
        )
    }

    @Test
    fun blockingASenderOverridesACachedSilentVerdict() {
        assertEquals(
            FilterAction.SPAM,
            resolveInboxAction(
                storedAction = FilterAction.SILENT,
                isSenderBlocked = true,
                isContact = true,
                noSpamForContacts = false
            )
        )
    }

    @Test
    fun blockingWinsEvenForContactsWhenNoSpamForContactsIsOn() {
        // Blocking is an explicit user decision, stronger than the "never spam contacts" rule.
        assertEquals(
            FilterAction.SPAM,
            resolveInboxAction(
                storedAction = FilterAction.NORMAL,
                isSenderBlocked = true,
                isContact = true,
                noSpamForContacts = true
            )
        )
    }

    @Test
    fun cachedVerdictIsUsedWhenThereIsNoBlock() {
        assertEquals(
            FilterAction.SILENT,
            resolveInboxAction(
                storedAction = FilterAction.SILENT,
                isSenderBlocked = false,
                isContact = false,
                noSpamForContacts = false
            )
        )
    }

    @Test
    fun noSpamForContactsSoftensACachedSpamVerdict() {
        assertEquals(
            FilterAction.NORMAL,
            resolveInboxAction(
                storedAction = FilterAction.SPAM,
                isSenderBlocked = false,
                isContact = true,
                noSpamForContacts = true
            )
        )
    }

    @Test
    fun noSpamForContactsStillLetsSpamThroughForStrangers() {
        assertEquals(
            FilterAction.SPAM,
            resolveInboxAction(
                storedAction = FilterAction.SPAM,
                isSenderBlocked = false,
                isContact = false,
                noSpamForContacts = true
            )
        )
    }

    @Test
    fun unknownMessageIsHandedToTheFilterEngine() {
        assertNull(
            resolveInboxAction(
                storedAction = null,
                isSenderBlocked = false,
                isContact = false,
                noSpamForContacts = false
            )
        )
    }
}
