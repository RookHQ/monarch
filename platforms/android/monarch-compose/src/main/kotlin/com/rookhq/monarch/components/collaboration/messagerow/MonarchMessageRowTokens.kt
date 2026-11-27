package com.rookhq.monarch.components.collaboration.messagerow

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarTokens
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.internal.currentIcuTimeZone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and pure logic of MonarchMessageRow, built from primitives and roles only. */
internal object MonarchMessageRowTokens {
    /** The avatar size; a grouped row keeps the same space empty so bodies line up. */
    val AvatarSize: MonarchAvatarSize = MonarchAvatarSize.Md
    val AvatarSide: Dp get() = MonarchAvatarTokens.side(AvatarSize)
    val HorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val GroupedVerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val HeaderGap: Dp = MonarchTokens.Space.space2.dp
    val StackGap: Dp = MonarchTokens.Space.space1.dp
    val InternalBarWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The row's fill: the subtle accent when highlighted, the subtle warning for an internal note, else none. */
    fun background(variant: MonarchMessageRowVariant, highlighted: Boolean, colors: MonarchColors): Color = when {
        highlighted -> colors.accentSubtle
        variant == MonarchMessageRowVariant.Internal -> colors.warningSubtle
        else -> Color.Transparent
    }

    /** The bar along the start edge of an internal note; `null` for the other variants. */
    fun bar(variant: MonarchMessageRowVariant, colors: MonarchColors): Color? =
        if (variant == MonarchMessageRowVariant.Internal) colors.warningBorder else null

    /** The body's content colour: muted while pending or after deletion, the default text colour otherwise. */
    fun bodyColor(status: MonarchMessageRowStatus, deleted: Boolean, colors: MonarchColors): Color =
        if (deleted || status == MonarchMessageRowStatus.Pending) colors.textMuted else colors.textDefault

    /** The colour of the failure text. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of the time, the edited marker and system lines. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** Whether the long-press menu is offered: a sent, not deleted, non-system message with actions. */
    fun offersActions(
        variant: MonarchMessageRowVariant,
        status: MonarchMessageRowStatus,
        deleted: Boolean,
        actionCount: Int,
    ): Boolean = variant != MonarchMessageRowVariant.System &&
        status == MonarchMessageRowStatus.Sent &&
        !deleted &&
        actionCount > 0

    /**
     * The relative time of [time] at [now] in [locale], by the shared Monarch rule (the web rule, as the web
     * message row's `RelativeTime`): "30 seconds ago", "5 minutes ago", "last week", "in 2 minutes".
     */
    fun relativeTime(time: Long, now: Long, locale: Locale): String = MonarchRelativeTime.format(time, now, locale)

    /** The time of day in [locale]: "2:05 PM". */
    fun clockTime(time: Long, locale: Locale, timeZone: TimeZone = currentIcuTimeZone()): String {
        val format = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        format.timeZone = timeZone
        return format.format(time)
    }

    /** The full date and time TalkBack reads: "Saturday, November 14, 2026 at 2:05 PM". */
    fun fullTime(time: Long, locale: Locale, timeZone: TimeZone = currentIcuTimeZone()): String {
        val format = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, locale)
        format.timeZone = timeZone
        return format.format(time)
    }
}
