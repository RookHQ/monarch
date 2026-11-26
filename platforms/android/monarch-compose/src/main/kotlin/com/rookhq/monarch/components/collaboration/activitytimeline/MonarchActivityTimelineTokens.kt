package com.rookhq.monarch.components.collaboration.activitytimeline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of an event's marker.
 *
 * @property dot the fill of the dot.
 * @property icon the tint of an icon marker.
 * @property ring the ring around the dot and the fill behind an icon, so the line stops at the marker.
 * @property outline the hairline around the dot and the icon.
 */
@Immutable
internal data class MonarchActivityTimelineMarkerColors(
    val dot: Color,
    val icon: Color,
    val ring: Color,
    val outline: Color,
)

/** Component tokens and the pure colour rules of MonarchActivityTimeline. */
internal object MonarchActivityTimelineTokens {
    /** The marker column: the size of a small avatar, 24dp. */
    val Marker: Dp = MonarchTokens.Space.space6.dp

    /** The dot, without its ring. */
    val Dot: Dp = MonarchTokens.Space.space2.dp

    /** The ring around the dot. */
    val DotRing: Dp = MonarchTokens.Space.space0_5.dp

    /** The line between markers and the outlines. */
    val Line: Dp = MonarchTokens.Space.px.dp

    /** The space between the line and a marker. */
    val LineInset: Dp = MonarchTokens.Space.space1.dp

    /** The space between the marker and the text. */
    val MarkerGap: Dp = MonarchTokens.Space.space3.dp

    /** The space between the parts of one event. */
    val PartGap: Dp = MonarchTokens.Space.space1.dp

    /** The border of a comment's body, drawn as the web's boxed comment. */
    val BodyBorder: Dp = MonarchTokens.Space.px.dp

    /** The padding inside a comment's body box. */
    val BodyPadding: Dp = MonarchTokens.Space.space2.dp

    /** The padding above and below a day heading. */
    val HeadingPadding: Dp = MonarchTokens.Space.space2.dp

    /** The number of placeholder rows while loading. */
    const val LOADING_ROWS: Int = 3

    /** The width of the second placeholder line, as a share of the row. */
    const val LOADING_LINE: Float = 0.6f

    /** The space under each event. */
    fun eventGap(density: MonarchActivityTimelineDensity): Dp = when (density) {
        MonarchActivityTimelineDensity.Regular -> MonarchTokens.Space.space5.dp
        MonarchActivityTimelineDensity.Compact -> MonarchTokens.Space.space3.dp
    }

    /** The space under an event: [eventGap] between events, a small gap under the last one of a day. */
    fun bottomGap(density: MonarchActivityTimelineDensity, last: Boolean): Dp = if (last) PartGap else eventGap(density)

    /**
     * The marker colours of [tone], as on the web: a neutral dot in the strong border colour, a toned dot in the
     * tone's solid colour; icons in the tone's strong colour (muted text when neutral); a surface ring with a subtle
     * outline.
     */
    fun markerColors(tone: MonarchTone, colors: MonarchColors): MonarchActivityTimelineMarkerColors {
        val palette = MonarchTonePalette.of(tone, colors)
        val neutral = tone == MonarchTone.Neutral
        return MonarchActivityTimelineMarkerColors(
            dot = if (neutral) colors.borderStrong else palette.solid,
            icon = if (neutral) colors.textMuted else palette.strong,
            ring = colors.bgSurface,
            outline = colors.borderSubtle,
        )
    }

    /** The line that joins the markers of one day: the subtle border colour. */
    fun line(colors: MonarchColors): Color = colors.borderSubtle

    /** The time: muted text. */
    fun time(colors: MonarchColors): Color = colors.textMuted

    /** The background of a day heading, so it covers the events that scroll under it: the canvas colour. */
    fun headingBackground(colors: MonarchColors): Color = colors.bgCanvas

    /** The error text without Try again: the strong danger colour. */
    fun error(colors: MonarchColors): Color = colors.dangerStrong
}
