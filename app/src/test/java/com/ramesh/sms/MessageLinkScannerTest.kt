package com.ramesh.sms

import com.ramesh.sms.data.util.MessageLinkScanner
import com.ramesh.sms.data.util.MessageSpan
import com.ramesh.sms.data.util.findOtpHighlight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The spans the bubble menu turns into tappable links, and the code range it highlights. */
class MessageLinkScannerTest {

    private fun spans(body: String) = MessageLinkScanner.scan(body)

    @Test
    fun `a url is tappable and keeps its own text`() {
        val body = "مشاهدهٔ قبض: https://bill.example.ir/pay?id=42"
        val span = spans(body).single { it.kind == MessageSpan.Kind.URL }

        assertEquals("https://bill.example.ir/pay?id=42", span.value)
        assertEquals("https://bill.example.ir/pay?id=42", body.substring(span.start, span.end))
    }

    @Test
    fun `trailing sentence punctuation is not part of the link`() {
        val body = "برای جزئیات به https://adliran-sana.xyz/app مراجعه کنید."
        val span = spans(body).single { it.kind == MessageSpan.Kind.URL }

        assertEquals("https://adliran-sana.xyz/app", span.value)
    }

    @Test
    fun `a bare www host is promoted to https`() {
        val span = spans("سایت ما www.ramesh.ir").single { it.kind == MessageSpan.Kind.URL }

        assertEquals("https://www.ramesh.ir", span.value)
    }

    @Test
    fun `a full mobile number becomes a dialable span`() {
        val body = "تماس با پشتیبانی: ۰۹۱۲ ۱۲۳ ۴۵۶۷"
        val span = spans(body).single { it.kind == MessageSpan.Kind.PHONE }

        assertEquals("09121234567", span.value)
    }

    @Test
    fun `an international number keeps its plus`() {
        val span = spans("Call +98 912 123 4567 now").single { it.kind == MessageSpan.Kind.PHONE }

        assertEquals("+989121234567", span.value)
    }

    @Test
    fun `short codes and amounts are not phone numbers`() {
        assertTrue(spans("موجودی: 500000 تومان").none { it.kind == MessageSpan.Kind.PHONE })
        assertTrue(spans("از شمارهٔ 1000").none { it.kind == MessageSpan.Kind.PHONE })
    }

    @Test
    fun `a card number is not dialed`() {
        assertTrue(spans("کارت 6037 9977 1234 5678").none { it.kind == MessageSpan.Kind.PHONE })
    }

    @Test
    fun `a jalali date is recognised`() {
        val span = spans("جلسه 1404/03/23 ساعت ۱۰").single { it.kind == MessageSpan.Kind.DATE }

        assertEquals("1404/03/23", span.value)
    }

    @Test
    fun `a date inside a url does not become a date span`() {
        val found = spans("https://example.com/1404/03/23/report")

        assertEquals(1, found.size)
        assertEquals(MessageSpan.Kind.URL, found.single().kind)
    }

    @Test
    fun `a phone-looking tail of a url does not become a phone span`() {
        val found = spans("https://t.me/joinchat/9891212345")

        assertEquals(1, found.size)
        assertEquals(MessageSpan.Kind.URL, found.single().kind)
    }

    @Test
    fun `spans are sorted and do not overlap`() {
        val found = spans("کد ۱۲۳۴۵۶ را به 09121234567 بدهید یا به https://x.ir/1404/03/23 بروید")

        assertEquals(found.sortedBy { it.start }, found)
        found.zipWithNext().forEach { (a, b) ->
            assertTrue("span ${a.start}..${a.end} overlaps ${b.start}..${b.end}", a.end <= b.start)
        }
    }

    @Test
    fun `a blank body has no spans`() {
        assertTrue(spans("   ").isEmpty())
    }

    @Test
    fun `the otp highlight points at the code inside the body`() {
        val body = "رمز پویا: ۴۸۲۹۱۳\nمبلغ: ۵۰۰۰۰ تومان"
        val highlight = findOtpHighlight(body) { "482913" }

        assertEquals("482913", highlight?.code)
        assertEquals("۴۸۲۹۱۳", body.substring(highlight!!.start, highlight.end))
    }

    @Test
    fun `a latin code is highlighted in place`() {
        val body = "Your verification code is 482913"
        val highlight = findOtpHighlight(body) { "482913" }

        assertEquals("482913", body.substring(highlight!!.start, highlight.end))
    }

    @Test
    fun `a body without a code has no highlight`() {
        assertNull(findOtpHighlight("سلام علی جان") { null })
    }
}
