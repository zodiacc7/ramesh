package com.ramesh.sms.ui.util

import com.ramesh.sms.data.util.AppLanguage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reusable, thread-safe formatters for SMS timestamps.
 * Constructing [java.text.SimpleDateFormat] per list item is expensive during scroll.
 */
object SmsDateFormats {
    private val zone: ZoneId
        get() = ZoneId.systemDefault()

    private val clock: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val weekday: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val monthDay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val monthDayYear: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val monthDayClock: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
    private val yearMonthDayClock: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy MMMM d, HH:mm", Locale.getDefault())

    /** Jalali month names are always Persian: the calendar itself is Persian by definition. */
    private val SHAMSI_MONTH_NAMES = arrayOf(
        "فروردین",
        "اردیبهشت",
        "خرداد",
        "تیر",
        "مرداد",
        "شهریور",
        "مهر",
        "آبان",
        "آذر",
        "دی",
        "بهمن",
        "اسفند"
    )

    /** Indexed by [java.time.DayOfWeek.value] - 1, so index 0 is Monday. */
    private val PERSIAN_WEEKDAYS = arrayOf(
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنجشنبه",
        "جمعه",
        "شنبه",
        "یکشنبه"
    )

    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    fun toJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDays = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1
        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) {
            gDayNo += gDays[i + 1]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo += 1
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        while (jm < 11) {
            val daysInMonth = if (jm < 6) 31 else 30
            if (jDayNo < daysInMonth) {
                break
            }
            jDayNo -= daysInMonth
            jm += 1
        }
        return JalaliDate(jy, jm + 1, jDayNo + 1)
    }

    fun toPersianDigits(number: Int): String = toPersianDigits(number.toString())

    fun toPersianDigits(input: String): String {
        val out = StringBuilder(input.length)
        for (ch in input) {
            val p = when (ch) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> ch
            }
            out.append(p)
        }
        return out.toString()
    }

    fun gregorianDate(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val messageDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val currentDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return if (messageDate.year == currentDate.year) {
            messageDate.format(monthDay)
        } else {
            messageDate.format(monthDayYear)
        }
    }

    /** English (Latin) Jalali month names for English UI. */
    private val SHAMSI_MONTH_NAMES_EN = arrayOf(
        "Farvardin",
        "Ordibehesht",
        "Khordad",
        "Tir",
        "Mordad",
        "Shahrivar",
        "Mehr",
        "Aban",
        "Azar",
        "Dey",
        "Bahman",
        "Esfand"
    )

    fun shamsiDate(
        timestamp: Long,
        appLanguage: AppLanguage = AppLanguage.SYSTEM,
        now: Long = System.currentTimeMillis()
    ): String {
        val messageDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val currentDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

        val jDate = toJalali(messageDate.year, messageDate.monthValue, messageDate.dayOfMonth)
        val nowJDate = toJalali(currentDate.year, currentDate.monthValue, currentDate.dayOfMonth)

        val isPersian = appLanguage == AppLanguage.PERSIAN
        val monthNames = if (isPersian) SHAMSI_MONTH_NAMES else SHAMSI_MONTH_NAMES_EN
        val monthName = monthNames.getOrElse(jDate.month - 1) { "" }
        val monthNum = String.format(Locale.US, "%02d", jDate.month)
        val dayNum = String.format(Locale.US, "%02d", jDate.day)
        val yearNum = jDate.year.toString()

        // Short date (same year): "23 Shahrivar" / "شهریور ۲۳"
        return if (jDate.year == nowJDate.year) {
            if (isPersian) {
                "${toPersianDigits(jDate.day)} $monthName"
            } else {
                "$monthName ${jDate.day}"
            }
        } else {
            // Long date: Persian keeps month names; English uses numeric YYYY/MM/DD
            if (isPersian) {
                "${toPersianDigits(jDate.day)} $monthName ${toPersianDigits(jDate.year)}"
            } else {
                "$yearNum/$monthNum/$dayNum"
            }
        }
    }

    fun formatDate(
        timestamp: Long,
        useShamsi: Boolean,
        appLanguage: AppLanguage = AppLanguage.SYSTEM,
        now: Long = System.currentTimeMillis()
    ): String {
        return if (useShamsi) {
            shamsiDate(timestamp, appLanguage, now)
        } else {
            gregorianDate(timestamp, now)
        }
    }

    fun formatDateTimeWithYear(
        timestamp: Long,
        useShamsi: Boolean = false,
        appLanguage: AppLanguage = AppLanguage.SYSTEM
    ): String {
        return if (useShamsi) {
            val messageDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            val jDate = toJalali(messageDate.year, messageDate.monthValue, messageDate.dayOfMonth)
            val timePart = clock(timestamp)
            val isPersian = appLanguage == AppLanguage.PERSIAN
            // Long date: Persian keeps month names; English uses numeric YYYY/MM/DD
            val datePart = if (isPersian) {
                val monthName = SHAMSI_MONTH_NAMES.getOrElse(jDate.month - 1) { "" }
                "${toPersianDigits(jDate.year)} $monthName ${toPersianDigits(jDate.day)}"
            } else {
                "${String.format(Locale.US, "%04d", jDate.year)}/${String.format(Locale.US, "%02d", jDate.month)}/${String.format(Locale.US, "%02d", jDate.day)}"
            }
            val t = if (isPersian) toPersianDigits(timePart) else timePart
            "$datePart، $t"
        } else {
            Instant.ofEpochMilli(timestamp).atZone(zone).format(yearMonthDayClock)
        }
    }

    fun clock(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime().format(clock)

    fun monthDayClock(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(monthDayClock)

    fun isSameDay(first: Long, second: Long): Boolean {
        val firstDate = Instant.ofEpochMilli(first).atZone(zone).toLocalDate()
        val secondDate = Instant.ofEpochMilli(second).atZone(zone).toLocalDate()
        return firstDate == secondDate
    }

    /**
     * Sticky-style chat divider label: "Today", "Yesterday", or a formatted date.
     */
    fun dayHeader(
        timestamp: Long,
        useShamsi: Boolean = false,
        appLanguage: AppLanguage = AppLanguage.SYSTEM,
        now: Long = System.currentTimeMillis()
    ): String {
        val messageDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val isPersian = appLanguage == AppLanguage.PERSIAN
        val todayLabel = if (useShamsi && isPersian) "امروز" else "Today"
        val yesterdayLabel = if (useShamsi && isPersian) "دیروز" else "Yesterday"
        return when {
            messageDate == today -> todayLabel
            messageDate == today.minusDays(1) -> yesterdayLabel
            else -> formatDate(timestamp, useShamsi, appLanguage, now)
        }
    }

    fun conversationList(
        timestamp: Long,
        useShamsi: Boolean = false,
        appLanguage: AppLanguage = AppLanguage.SYSTEM,
        now: Long = System.currentTimeMillis()
    ): String {
        val diff = now - timestamp
        val oneDay = 24 * 60 * 60 * 1000L
        val local = Instant.ofEpochMilli(timestamp).atZone(zone)
        val isPersian = appLanguage == AppLanguage.PERSIAN
        return when {
            diff < oneDay -> {
                val text = local.toLocalTime().format(clock)
                if (useShamsi && isPersian) toPersianDigits(text) else text
            }
            diff < 7 * oneDay -> {
                if (useShamsi && isPersian) {
                    PERSIAN_WEEKDAYS.getOrElse(local.dayOfWeek.value - 1) { "" }
                } else {
                    local.toLocalDate().format(weekday)
                }
            }
            else -> if (useShamsi) shamsiDate(timestamp, appLanguage, now)
            else local.toLocalDate().format(monthDay)
        }
    }
}
