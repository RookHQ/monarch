package dev.monarchui.components.datadisplay.metric

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.charts.deltaindicator.MonarchDeltaIndicatorSize
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and logic of MonarchMetric, built from primitives and roles only. */
internal object MonarchMetricTokens {
    val StackGap: Dp = MonarchTokens.Space.space1.dp
    val UnitGap: Dp = MonarchTokens.Space.space1.dp
    val InlineGap: Dp = MonarchTokens.Space.space2.dp

    /** The value's type role, with tabular figures. */
    fun valueStyle(size: MonarchMetricSize, typography: MonarchTypography): TextStyle =
        MonarchTabularNumberStyle.applyTo(
            when (size) {
                MonarchMetricSize.Sm -> typography.bodyStrong
                MonarchMetricSize.Md -> typography.heading
                MonarchMetricSize.Lg -> typography.display
            },
        )

    fun unitStyle(size: MonarchMetricSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchMetricSize.Sm -> typography.caption
        MonarchMetricSize.Md, MonarchMetricSize.Lg -> typography.body
    }

    fun labelStyle(size: MonarchMetricSize, typography: MonarchTypography): TextStyle = when (size) {
        MonarchMetricSize.Sm -> typography.caption
        MonarchMetricSize.Md, MonarchMetricSize.Lg -> typography.label
    }

    fun deltaSize(size: MonarchMetricSize): MonarchDeltaIndicatorSize =
        if (size == MonarchMetricSize.Lg) MonarchDeltaIndicatorSize.Md else MonarchDeltaIndicatorSize.Sm

    fun labelColor(colors: MonarchColors): Color = colors.textMuted

    fun unitColor(colors: MonarchColors): Color = colors.textMuted

    fun valueColor(empty: Boolean, colors: MonarchColors): Color = if (empty) colors.textSubtle else colors.textDefault

    /** The text shown for [value], or `null` when it is missing or not finite. */
    fun shown(value: Double?, format: MonarchMetricFormat, compact: Boolean, locale: Locale): String? =
        value?.takeIf { it.isFinite() }?.let { format.format(it, compact, locale) }

    /** What TalkBack reads instead of the shown value, or `null` when it reads the value itself. */
    fun spoken(value: Double?, format: MonarchMetricFormat, compact: Boolean, locale: Locale): String? {
        val shown = shown(value, format, compact, locale) ?: return null
        val full = shown(value, format, false, locale)
        return full?.takeIf { compact && it != shown }
    }
}
