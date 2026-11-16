package com.ramesh.sms

import androidx.compose.ui.unit.LayoutDirection
import com.ramesh.sms.ui.util.resolveTextDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The composer has to pick a side from what is being typed: Persian reads right to left, English
 * left to right, and a mixed message follows its Persian half.
 */
class TextDirectionTest {

    @Test
    fun persianIsRightToLeft() {
        assertEquals(
            LayoutDirection.Rtl,
            resolveTextDirection("سلام خوبی؟", LayoutDirection.Ltr)
        )
    }

    @Test
    fun englishIsLeftToRight() {
        assertEquals(
            LayoutDirection.Ltr,
            resolveTextDirection("Hello there", LayoutDirection.Rtl)
        )
    }

    @Test
    fun mixedMessageFollowsPersian() {
        assertEquals(
            LayoutDirection.Rtl,
            resolveTextDirection("Hello سلام", LayoutDirection.Ltr)
        )
        assertEquals(
            LayoutDirection.Rtl,
            resolveTextDirection("سلام Hello 123", LayoutDirection.Ltr)
        )
    }

    @Test
    fun persianDigitsAloneKeepTheAppDirection() {
        // Digits carry no direction of their own, so the screen's own direction wins.
        assertEquals(LayoutDirection.Ltr, resolveTextDirection("12345", LayoutDirection.Ltr))
        assertEquals(LayoutDirection.Rtl, resolveTextDirection("۱۲۳۴۵ - 67", LayoutDirection.Rtl))
    }

    @Test
    fun emptyTextKeepsTheAppDirection() {
        assertEquals(LayoutDirection.Rtl, resolveTextDirection("", LayoutDirection.Rtl))
        assertEquals(LayoutDirection.Ltr, resolveTextDirection("   ", LayoutDirection.Ltr))
    }
}
