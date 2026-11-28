package dev.monarchui.components.scheduling.slotpicker

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import dev.monarchui.components.scheduling.timezoneselect.TimezoneSelectZones
import java.util.Date
import java.util.Locale

/** The day, time and zone texts of MonarchSlotPicker in one locale and zone. */
internal class SlotPickerFormats(private val locale: Locale, is24Hour: Boolean, private val zone: String) {
    private val zoneRules = TimeZone.getTimeZone(zone)
    private val day = DateFormat.getInstanceForSkeleton(DAY, locale).apply { timeZone = TimeZone.GMT_ZONE }
    private val time = DateFormat.getInstanceForSkeleton(if (is24Hour) TIME_24 else TIME_12, locale)
        .apply { timeZone = zoneRules }

    /** "Wednesday, November 4" for a day as its UTC midnight. */
    fun day(day: Long): String = this.day.format(Date(day))

    /** "9:30 AM" or "09:30" for an instant, in the zone. */
    fun time(instant: Long): String = time.format(Date(instant))

    /** The date [instant] falls on in the zone, as its UTC midnight. */
    fun dayOf(instant: Long): Long = MonarchSlotPickerTokens.dayOf(instant, zoneRules.getOffset(instant))

    /**
     * The readable name of [zone] at [at], with the zone id as the web writes it: "Greenwich Mean Time
     * (Europe/London)", or "(UTC+01:00) Berlin" when the zone has no long name.
     */
    fun zoneName(zone: String = this.zone, at: Long): String {
        val info = TimezoneSelectZones.info(zone, at, locale)
        return if (info.longName.isEmpty()) TimezoneSelectZones.defaultLabel(info) else "${info.longName} ($zone)"
    }

    private companion object {
        const val DAY = "EEEEMMMMd"
        const val TIME_12 = "hmm"
        const val TIME_24 = "Hmm"
    }
}
