package com.rookhq.monarch.components.scheduling.timezoneselect

import androidx.compose.runtime.Immutable

/**
 * What `MonarchTimezoneSelect` knows about one IANA time zone at one instant, for building option labels.
 *
 * @property id the current IANA id, such as `Asia/Kolkata`.
 * @property city the last part of the id, readable: `Kolkata`, `New York`; `UTC` for UTC.
 * @property region the first part of the id: `Asia`, `America`. Empty for `UTC`.
 * @property offsetSeconds the offset from UTC at the instant, daylight saving included, such as 19800.
 * @property offsetLabel the offset as text: `UTC+05:30`, `UTC-08:00`, `UTC+00:00`.
 * @property longName the zone's long name in the configuration's locale, such as "India Standard Time", or an
 * empty string when the system has none.
 */
@Immutable
public class MonarchTimezoneInfo(
    public val id: String,
    public val city: String,
    public val region: String,
    public val offsetSeconds: Int,
    public val offsetLabel: String,
    public val longName: String,
)
