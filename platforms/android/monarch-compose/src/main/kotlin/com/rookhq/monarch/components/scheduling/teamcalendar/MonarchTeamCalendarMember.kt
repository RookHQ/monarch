package com.rookhq.monarch.components.scheduling.teamcalendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One row of a `MonarchTeamCalendar`: a person.
 *
 * @property id a stable id; entries refer to it with `memberId`.
 * @property name the person's name, shown in the people column and read with every day of the row.
 * @property image the person's photo, loaded by the app. `null` shows the avatar's initials.
 * @property description a second line, such as a role or a team.
 */
@Immutable
public class MonarchTeamCalendarMember(
    public val id: String,
    public val name: String,
    public val image: Painter? = null,
    public val description: String? = null,
)
