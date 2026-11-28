package com.rookhq.monarch.components.forms.datefield

import java.text.DateFormat
import java.util.Locale
import java.util.TimeZone

/** Component tokens and logic of MonarchDateField. */
internal object MonarchDateFieldTokens {
    /**
     * The date as the field shows it: the locale's medium date ("Nov 17, 2026", "17 nov. 2026"), read at UTC,
     * because the platform picker gives midnight UTC. Formatting in the device zone would show the day before
     * west of UTC.
     */
    fun format(utcMillis: Long, locale: Locale): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(utcMillis)
}
