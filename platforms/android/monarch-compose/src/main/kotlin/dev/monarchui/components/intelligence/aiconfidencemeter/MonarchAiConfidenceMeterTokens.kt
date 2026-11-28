package dev.monarchui.components.intelligence.aiconfidencemeter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchNumberText
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure logic of MonarchAiConfidenceMeter. */
internal object MonarchAiConfidenceMeterTokens {
    val ContentGap: Dp = MonarchTokens.Space.space1.dp
    val BarGap: Dp = MonarchTokens.Space.space1_5.dp
    val IconSize: Dp = MonarchTokens.Space.space4.dp
    const val FONT_WEIGHT: Int = MonarchTokens.FontWeight.medium

    fun minHeight(size: MonarchAiConfidenceMeterSize): Dp = when (size) {
        MonarchAiConfidenceMeterSize.Sm -> MonarchTokens.Space.space5.dp
        MonarchAiConfidenceMeterSize.Md -> MonarchTokens.Space.space6.dp
    }

    fun horizontalPadding(size: MonarchAiConfidenceMeterSize): Dp = when (size) {
        MonarchAiConfidenceMeterSize.Sm -> MonarchTokens.Space.space1_5.dp
        MonarchAiConfidenceMeterSize.Md -> MonarchTokens.Space.space2.dp
    }

    fun trackHeight(size: MonarchAiConfidenceMeterSize): Dp = when (size) {
        MonarchAiConfidenceMeterSize.Sm -> MonarchTokens.Space.space1.dp
        MonarchAiConfidenceMeterSize.Md -> MonarchTokens.Space.space1_5.dp
    }

    /** The colour of the icon, the label and the bar fill: the confidence role of the band. */
    fun foreground(level: MonarchAiConfidenceLevel, colors: MonarchColors): Color = when (level) {
        MonarchAiConfidenceLevel.Low -> colors.confidenceLow
        MonarchAiConfidenceLevel.Medium -> colors.confidenceMedium
        MonarchAiConfidenceLevel.High -> colors.confidenceHigh
    }

    /** The badge: the subtle fill and border of the status that matches the band, and the confidence role. */
    fun badgeColors(level: MonarchAiConfidenceLevel, colors: MonarchColors): MonarchSurfaceColors = when (level) {
        MonarchAiConfidenceLevel.Low ->
            MonarchSurfaceColors(colors.dangerSubtle, colors.confidenceLow, colors.dangerBorder)
        MonarchAiConfidenceLevel.Medium ->
            MonarchSurfaceColors(colors.warningSubtle, colors.confidenceMedium, colors.warningBorder)
        MonarchAiConfidenceLevel.High ->
            MonarchSurfaceColors(colors.successSubtle, colors.confidenceHigh, colors.successBorder)
    }

    /** The track of the bar. */
    fun track(colors: MonarchColors): Color = colors.bgMuted

    /** Whether the percentage is shown: by default only by the bar. */
    fun showsValue(showValue: Boolean?, variant: MonarchAiConfidenceMeterVariant): Boolean =
        showValue ?: (variant == MonarchAiConfidenceMeterVariant.Bar)

    /** The percentage in the locale, without decimals: "42%". */
    fun percent(value: Float, locale: Locale): String =
        MonarchNumberText.percent(locale, maxFractionDigits = 0)(value.toDouble())
}
