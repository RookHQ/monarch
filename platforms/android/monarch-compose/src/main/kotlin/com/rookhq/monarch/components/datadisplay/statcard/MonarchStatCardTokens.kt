package com.rookhq.monarch.components.datadisplay.statcard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorSize
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetricFormat
import com.rookhq.monarch.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** The colours of the secondary text of a stat card. */
internal data class MonarchStatCardTextColors(val muted: Color, val error: Color)

/** Component tokens and logic of MonarchStatCard, built from primitives and roles only. */
internal object MonarchStatCardTokens {
    val Gap: Dp = MonarchTokens.Space.space1.dp
    val IconGap: Dp = MonarchTokens.Space.space2.dp
    val VisualGap: Dp = MonarchTokens.Space.space3.dp
    val SparklineGap: Dp = MonarchTokens.Space.space2.dp
    val SkeletonWidth: Dp = MonarchTokens.Space.space20.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The text of the help button when the app gives no icon. */
    const val HELP_GLYPH: String = "?"

    /** The padding of the card; none for the plain variant. */
    fun padding(size: MonarchStatCardSize, variant: MonarchStatCardVariant): PaddingValues = when {
        variant == MonarchStatCardVariant.Plain -> PaddingValues(MonarchTokens.Space.space0.dp)
        size == MonarchStatCardSize.Sm -> PaddingValues(MonarchTokens.Space.space3.dp)
        size == MonarchStatCardSize.Md -> PaddingValues(MonarchTokens.Space.space4.dp)
        else -> PaddingValues(MonarchTokens.Space.space5.dp)
    }

    fun labelStyle(size: MonarchStatCardSize, typography: MonarchTypography): TextStyle =
        if (size == MonarchStatCardSize.Sm) typography.caption else typography.label

    /** The value's type role, with tabular figures. */
    fun valueStyle(size: MonarchStatCardSize, typography: MonarchTypography): TextStyle =
        MonarchTabularNumberStyle.applyTo(
            when (size) {
                MonarchStatCardSize.Sm -> typography.bodyStrong
                MonarchStatCardSize.Md -> typography.heading
                MonarchStatCardSize.Lg -> typography.display
            },
        )

    fun unitStyle(size: MonarchStatCardSize, typography: MonarchTypography): TextStyle =
        if (size == MonarchStatCardSize.Sm) typography.caption else typography.body

    fun deltaSize(size: MonarchStatCardSize): MonarchDeltaIndicatorSize =
        if (size == MonarchStatCardSize.Lg) MonarchDeltaIndicatorSize.Md else MonarchDeltaIndicatorSize.Sm

    /**
     * The fill and border: the surface fill and default border for a card,
     * the accent subtle fill and accent border when highlighted, nothing for
     * the plain variant.
     */
    fun container(variant: MonarchStatCardVariant, highlighted: Boolean, colors: MonarchColors): MonarchSurfaceColors =
        when {
            variant == MonarchStatCardVariant.Plain ->
                MonarchSurfaceColors(Color.Transparent, colors.textDefault, null)
            highlighted -> MonarchSurfaceColors(colors.accentSubtle, colors.textDefault, colors.accentBorder)
            else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderDefault)
        }

    /** The value colour: default text for neutral, the strong colour of the tone otherwise, subtle when empty. */
    fun valueColor(tone: MonarchTone, empty: Boolean, colors: MonarchColors): Color = when {
        empty -> colors.textSubtle
        tone == MonarchTone.Info -> colors.infoStrong
        tone == MonarchTone.Success -> colors.successStrong
        tone == MonarchTone.Warning -> colors.warningStrong
        tone == MonarchTone.Danger -> colors.dangerStrong
        else -> colors.textDefault
    }

    /** The label, unit, caption and hint are muted; an error is in the danger strong colour. */
    fun textColors(colors: MonarchColors): MonarchStatCardTextColors =
        MonarchStatCardTextColors(muted = colors.textMuted, error = colors.dangerStrong)

    /** The text shown for [value], or `null` when it is missing or not finite. */
    fun shown(value: Double?, format: MonarchMetricFormat, compact: Boolean, locale: Locale): String? =
        value?.takeIf { it.isFinite() }?.let { format.format(it, compact, locale) }

    /** What TalkBack reads instead of a compact value: the full value, or `null` when they are the same. */
    fun spoken(value: Double?, format: MonarchMetricFormat, compact: Boolean, locale: Locale): String? {
        val shown = shown(value, format, compact, locale) ?: return null
        return shown(value, format, false, locale)?.takeIf { compact && it != shown }
    }
}
