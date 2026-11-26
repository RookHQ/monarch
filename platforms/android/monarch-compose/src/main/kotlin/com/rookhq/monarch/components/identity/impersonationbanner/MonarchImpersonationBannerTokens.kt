package com.rookhq.monarch.components.identity.impersonationbanner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** The colours of the banner: inverse surface and text, with a warning edge. */
internal data class MonarchImpersonationBannerColors(
    val container: Color,
    val text: Color,
    val muted: Color,
    val edge: Color,
    val error: Color,
)

/** Component tokens and the pure logic of MonarchImpersonationBanner. */
internal object MonarchImpersonationBannerTokens {
    /** From this font scale the stop button moves under the text and fills the width. */
    const val STACKED_FONT_SCALE: Float = 1.5f

    /** The width of the warning edge at the start. */
    val EdgeWidth: Dp = MonarchTokens.Space.space1.dp

    /** The padding around the content. */
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp

    /** The padding above and below the content. */
    val VerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the icon, the text and the button. */
    val ContentGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of text. */
    val LineGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The colours, the same in every theme by role: the inverse surface with a solid warning edge. */
    fun colors(colors: MonarchColors): MonarchImpersonationBannerColors = MonarchImpersonationBannerColors(
        container = colors.bgInverse,
        text = colors.textInverse,
        muted = colors.textInverseMuted,
        edge = colors.warningSolid,
        error = colors.dangerStrongInverse,
    )

    /** Whether the button sits under the text, at large font sizes. */
    fun stacked(fontScale: Float): Boolean = fontScale >= STACKED_FONT_SCALE

    /** The start and reason line: "Started 5 minutes ago · Reason: Ticket 4821", or `null` when neither is set. */
    fun metaLine(started: String?, reason: String?): String? =
        listOfNotNull(started, reason).takeIf { it.isNotEmpty() }?.joinToString(" · ")

    /** [startedAt] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(startedAt: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(startedAt, now, locale)

    /** The busy state of the stop button: the app's when it tracks it, otherwise the component's. */
    fun busy(stopping: Boolean?, running: Boolean): Boolean = stopping ?: running
}
