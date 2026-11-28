package dev.monarchui.components.scheduling.teamcalendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * One absence or other entry on a person's row of a `MonarchTeamCalendar`. Days are epoch milliseconds read in
 * UTC, as in the Material 3 `DatePickerState`; any time on a day stands for that day.
 *
 * @property id a stable id, passed back when the entry is tapped.
 * @property memberId the `id` of the member whose row it is on.
 * @property start the first day.
 * @property end the last day, inclusive. `null`, or a day before [start], is [start].
 * @property label what it is, such as "Annual leave", or "Away" for a private type. Shown in the bar when the
 * bar spans two days or more, in a tooltip on a long press and read by TalkBack.
 * @property color the colour of the bar. `Color.Unspecified` is the accent. The colour is mixed into the surface
 * colour for the fill, so the label keeps the default text colour.
 * @property portion the part of every day the entry covers: half days are half bars.
 * @property portions per-day portions that replace [portion] on those days, keyed by any time on the day.
 * @property pending waiting for approval: an outlined bar without fill, and "pending" in what TalkBack reads.
 */
@Immutable
public class MonarchTeamCalendarEntry(
    public val id: String,
    public val memberId: String,
    public val start: Long,
    public val end: Long? = null,
    public val label: String,
    public val color: Color = Color.Unspecified,
    public val portion: MonarchTeamCalendarPortion = MonarchTeamCalendarPortion.Full,
    public val portions: Map<Long, MonarchTeamCalendarPortion> = emptyMap(),
    public val pending: Boolean = false,
)
