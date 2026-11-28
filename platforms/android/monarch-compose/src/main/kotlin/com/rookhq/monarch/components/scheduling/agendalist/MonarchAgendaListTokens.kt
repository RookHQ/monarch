package com.rookhq.monarch.components.scheduling.agendalist

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a day heading.
 *
 * @property container the fill behind the pinned heading, so events scroll under it.
 * @property date the date.
 * @property count the number of events.
 */
@Immutable
internal data class AgendaListHeadingColors(
    val container: Color,
    val date: Color,
    val count: Color,
)

/** Component tokens of MonarchAgendaList, built from primitives and roles only. */
internal object MonarchAgendaListTokens {
    val HeadingHorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val HeadingVerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val HeadingGap: Dp = MonarchTokens.Space.space2.dp
    val ItemHorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val ItemVerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val DayBottomPadding: Dp = MonarchTokens.Space.space3.dp
    val HatchSpacing: Dp = MonarchTokens.Space.space2.dp
    val HatchWidth: Dp = MonarchTokens.Space.space0_5.dp
    val NoteVerticalPadding: Dp = MonarchTokens.Space.space1.dp

    /**
     * The heading of a day: the surface colour behind it, the default text
     * colour for the date (today is marked by its badge, not by an accent), the
     * muted colour for the count and a subtle divider.
     */
    fun headingColors(colors: MonarchColors): AgendaListHeadingColors = AgendaListHeadingColors(
        container = colors.bgSurface,
        date = colors.textDefault,
        count = colors.textMuted,
    )

    /** The colour of a day note: muted, it is information, not an event. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** What TalkBack reads for a day heading: "Wednesday 4 November, Today, 3 events". */
    fun headingDescription(date: String, today: String?, count: String): String =
        listOfNotNull(date, today, count).joinToString(", ")
}
