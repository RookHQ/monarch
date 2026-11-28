package dev.monarchui.components.technical.incidenttimeline

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchIncidentTimeline. */
internal object MonarchIncidentTimelineTokens {
    /** The gap between the marker column and the update. */
    val ColumnGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the header and the body of an update. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the parts of the header. */
    val MetaGap: Dp = MonarchTokens.Space.space2.dp

    /** The side of the marker column at font scale 1. */
    val MarkerColumn: Dp = MonarchTokens.Space.space6.dp

    /** The side of the dot. */
    val Dot: Dp = MonarchTokens.Space.space3.dp

    /** The width of the line that joins the dots. */
    val Line: Dp = MonarchTokens.Space.space0_5.dp

    /** The marker column for a font scale: the base size, larger when the user enlarges text, never smaller. */
    fun markerColumn(fontScale: Float): Dp = MarkerColumn * fontScale.coerceAtLeast(1f)

    /** The space after each update but the last. */
    fun rowGap(density: MonarchIncidentTimelineDensity, last: Boolean): Dp = when {
        last -> MonarchTokens.Space.space0.dp
        density == MonarchIncidentTimelineDensity.Regular -> MonarchTokens.Space.space6.dp
        else -> MonarchTokens.Space.space3.dp
    }

    /** The colour of the dot of a tone: the tone's solid colour, the strong border for neutral. */
    fun marker(tone: MonarchTone, colors: MonarchColors): Color =
        if (tone == MonarchTone.Neutral) colors.borderStrong else MonarchTonePalette.of(tone, colors).solid

    /** The line that joins the dots: the default border. */
    fun line(colors: MonarchColors): Color = colors.borderDefault

    /** The time, the author and the empty text: muted text. */
    fun muted(colors: MonarchColors): Color = colors.textMuted


}
