package com.rookhq.monarch.components.charts.uptimebar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of one state.
 *
 * @property fill the segment and the legend marker.
 * @property outline drawn inside the segment for a state without data, otherwise `null`.
 */
@Immutable
internal data class MonarchUptimeBarStatusColors(val fill: Color, val outline: Color?)

/** Component tokens and the pure colour rules of MonarchUptimeBar. */
internal object MonarchUptimeBarTokens {
    /** The space between the header, the bar, the axis line and the legend. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The space between two segments. */
    val SegmentGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The space between two segments when they would otherwise be narrower than [ThinSegment]. */
    val ThinGap: Dp = MonarchTokens.Space.px.dp

    /** Below this width a segment's gap shrinks to [ThinGap]. */
    val ThinSegment: Dp = MonarchTokens.Space.space1.dp

    /** The radius of a segment. */
    val SegmentRadius: Dp = MonarchTokens.Space.space0_5.dp

    /** The outline of a segment without data and the gap of the inspected ring. */
    val Hairline: Dp = MonarchTokens.Space.px.dp

    /** The ring around the inspected segment. */
    val Ring: Dp = MonarchTokens.Space.space0_5.dp

    /** How far the operational green moves towards the surface. */
    const val OPERATIONAL_SOFTEN: Float = 0.3f

    /** How far a partial outage moves from the warning solid towards the danger solid. */
    const val PARTIAL_STEP: Float = 0.5f

    /** The legend marker. */
    val Swatch: Dp = MonarchTokens.Space.space2_5.dp

    /** The space between a marker and its name, and between legend entries. */
    val LegendGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The height of the segments. */
    fun height(size: MonarchUptimeBarSize): Dp = when (size) {
        MonarchUptimeBarSize.Sm -> MonarchTokens.Space.space5.dp
        MonarchUptimeBarSize.Md -> MonarchTokens.Space.space8.dp
    }

    /**
     * The colours of [status]: soft success (a dense run of healthy days stays quiet, [OPERATIONAL_SOFTEN]), warning,
     * a warning moved halfway to danger for a partial outage (so it differs from degraded in every theme), danger
     * and info solids; no data is the muted background with a default border.
     */
    fun statusColors(status: MonarchUptimeBarStatus, colors: MonarchColors): MonarchUptimeBarStatusColors =
        when (status) {
            MonarchUptimeBarStatus.Operational -> MonarchUptimeBarStatusColors(
                lerp(colors.successSolid, colors.bgSurface, OPERATIONAL_SOFTEN),
                null,
            )
            MonarchUptimeBarStatus.Degraded -> MonarchUptimeBarStatusColors(colors.warningSolid, null)
            MonarchUptimeBarStatus.Partial -> MonarchUptimeBarStatusColors(
                lerp(colors.warningSolid, colors.dangerSolid, PARTIAL_STEP),
                null,
            )
            MonarchUptimeBarStatus.Down -> MonarchUptimeBarStatusColors(colors.dangerSolid, null)
            MonarchUptimeBarStatus.Maintenance -> MonarchUptimeBarStatusColors(colors.infoSolid, null)
            MonarchUptimeBarStatus.NoData -> MonarchUptimeBarStatusColors(colors.bgMuted, colors.borderDefault)
        }

    /** The ring around the inspected segment: default text, set off from the bar by the surface colour. */
    fun ring(colors: MonarchColors): Pair<Color, Color> = colors.textDefault to colors.bgSurface

    /** The label, the start and end labels and the overall line: muted text. */
    fun mutedText(colors: MonarchColors): Color = colors.textMuted
}
