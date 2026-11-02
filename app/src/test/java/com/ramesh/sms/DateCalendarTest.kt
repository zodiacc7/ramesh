package com.ramesh.sms

import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.data.util.JalaliScope
import com.ramesh.sms.data.util.showsJalaliDates
import com.ramesh.sms.ui.util.SmsDateFormats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The calendar and language decisions behind every date on screen.
 *
 * The inbox used to decide its calendar with `jalaliScope == EVERYWHERE` while the chat screen used
 * `jalaliScope == EVERYWHERE || showShamsiDate`, so the same app could print Jalali dates in one
 * place and Gregorian ones in another. Both now ask [showsJalaliDates].
 */
class DateCalendarTest {

    @Test
    fun `everywhere turns the calendar on`() {
        assertTrue(showsJalaliDates(JalaliScope.EVERYWHERE, showShamsiDates = false))
        assertTrue(showsJalaliDates(JalaliScope.EVERYWHERE, showShamsiDates = true))
    }

    @Test
    fun `chat only keeps lists gregorian until a date is tapped`() {
        assertFalse(showsJalaliDates(JalaliScope.CHAT_ONLY, showShamsiDates = false))
        // Tapping a date inside a chat stores the flag; every list then follows it.
        assertTrue(showsJalaliDates(JalaliScope.CHAT_ONLY, showShamsiDates = true))
    }

    @Test
    fun `system language resolves against the device locale`() {
        assertTrue(AppLanguage.SYSTEM.isPersian(Locale("fa", "IR")))
        assertFalse(AppLanguage.SYSTEM.isPersian(Locale.US))
        assertTrue(AppLanguage.PERSIAN.isPersian(Locale.US))
        assertFalse(AppLanguage.ENGLISH.isPersian(Locale("fa", "IR")))
    }

    @Test
    fun `an explicit language wins over the device locale`() {
        val timestamp = 1_700_000_000_000L
        val english = SmsDateFormats.formatDate(timestamp, true, AppLanguage.ENGLISH, now = timestamp)

        assertTrue("English UI must not contain Persian digits", english.none { it in '۰'..'۹' })
    }

    @Test
    fun `the same timestamp formats the same way twice`() {
        val timestamp = 1_700_000_000_000L
        val first = SmsDateFormats.formatDate(timestamp, true, AppLanguage.PERSIAN, now = timestamp)
        val second = SmsDateFormats.formatDate(timestamp, true, AppLanguage.PERSIAN, now = timestamp)

        assertEquals(first, second)
        assertTrue("Persian UI should use Persian digits", first.any { it in '۰'..'۹' })
    }
}
