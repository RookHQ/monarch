package dev.monarchui.components.intelligence.aiscore

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.scorering.MonarchScoreRingSize
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** A band with the highest score it covers. */
internal class AiScoreBandRange(val band: MonarchAiScoreBand, val max: Double)

/** Component tokens and rules of MonarchAiScore, built from primitives and roles only. */
@Suppress("TooManyFunctions")
internal object MonarchAiScoreTokens {
    const val MAX: Double = 100.0
    private const val IMPACT_FRACTION_DIGITS = 1

    val SectionGap: Dp = MonarchTokens.Space.space4.dp
    val ItemGap: Dp = MonarchTokens.Space.space2.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val DisplayGap: Dp = MonarchTokens.Space.space3.dp
    /** The width of the impact column: the bar on its track and the signed amount. */
    val ImpactWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space12).dp

    val ImpactBarHeight: Dp = MonarchTokens.Space.space1.dp
    val SwatchSize: Dp = MonarchTokens.Space.space3.dp

    /** The score limited to 0 through 100, or `null` when there is none or it is not a number. */
    fun clamp(value: Double?): Double? = value?.takeIf { it.isFinite() }?.coerceIn(0.0, MAX)

    /** The bands sorted by minimum, each with its highest score: the next minimum minus one, or 100. */
    fun ranges(bands: List<MonarchAiScoreBand>): List<AiScoreBandRange> {
        val sorted = bands.sortedBy { it.minimum }
        return sorted.mapIndexed { index, band ->
            val next = sorted.getOrNull(index + 1)
            AiScoreBandRange(band, if (next != null) maxOf(band.minimum, next.minimum - 1) else MAX)
        }
    }

    /** The band [value] falls in: the one with the highest minimum at or below it, else the lowest band. */
    fun band(value: Double, bands: List<MonarchAiScoreBand>): AiScoreBandRange? {
        val ranges = ranges(bands)
        return ranges.lastOrNull { value >= it.band.minimum } ?: ranges.firstOrNull()
    }

    /** The ring size for a score size. */
    fun ringSize(size: MonarchAiScoreSize): MonarchScoreRingSize = when (size) {
        MonarchAiScoreSize.Sm -> MonarchScoreRingSize.Sm
        MonarchAiScoreSize.Md -> MonarchScoreRingSize.Md
    }

    /** The largest absolute impact, at least 1, so impact bars are sized against it. */
    fun maxImpact(factors: List<MonarchAiScoreFactor>): Double =
        maxOf(1.0, factors.maxOfOrNull { abs(it.impact) } ?: 0.0)

    /** The share of the bar a factor fills, from 0 to 1. */
    fun impactFraction(impact: Double, maxImpact: Double): Float =
        if (impact.isFinite()) (abs(impact) / maxImpact).coerceIn(0.0, 1.0).toFloat() else 0f

    /** The colour of an impact bar and its number: success when it raised the score, danger when it lowered it. */
    fun impactColor(impact: Double, colors: MonarchColors): Color = when {
        impact > 0 -> colors.successStrong
        impact < 0 -> colors.dangerStrong
        else -> colors.textMuted
    }

    /** The tone of the bar: the band's tone, as on the web, or neutral without a band. */
    fun barTone(bandTone: MonarchTone?): MonarchTone = bandTone ?: MonarchTone.Neutral

    /** The swatch of a band in the legend: the solid colour of its tone. */
    fun swatchColor(tone: MonarchTone, colors: MonarchColors): Color = MonarchTonePalette.of(tone, colors).solid

    /** A whole score in [locale]: "72". */
    fun formatWhole(value: Double, locale: Locale): String =
        java.text.NumberFormat.getIntegerInstance(locale).format(value.roundToLong())

    /** The size of an impact without its sign, with at most one decimal, in [locale]: "18", "2.5". */
    fun formatImpact(impact: Double, locale: Locale): String =
        java.text.NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = IMPACT_FRACTION_DIGITS
        }.format(abs(impact))
}
