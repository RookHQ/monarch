package com.rookhq.monarch.components.identity.connectedappslist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure logic of MonarchConnectedAppsList. */
internal object MonarchConnectedAppsListTokens {
    /** The separator between the parts of a line. */
    const val SEPARATOR: String = " · "

    /** The gap between the header and the list, and between rows. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the logo and the text, and between badges. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The colour of an app's name: muted while suspended. */
    fun nameColor(suspended: Boolean, colors: MonarchColors): Color =
        if (suspended) colors.textMuted else colors.textDefault

    /** The colour of a failed removal. */
    fun failureColor(colors: MonarchColors): Color = colors.dangerStrong

    /** [date] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(date: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(date, now, locale)

    /** "Name value", or `null` without a value. */
    fun labelled(label: String, value: String?): String? = value?.let { "$label $it" }

    /** The times line, "Connected 3 days ago · Last used 2 hours ago", or `null` when there is neither. */
    fun timesLine(connected: String?, lastUsed: String?): String? =
        listOfNotNull(connected, lastUsed).takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)

    /** What TalkBack reads after an app's name: its badges, domain, times and workspace. */
    fun spokenDetails(badges: List<String>, domain: String?, times: String?, workspace: String?): String =
        (badges + listOfNotNull(domain, times, workspace)).joinToString(", ")
}
