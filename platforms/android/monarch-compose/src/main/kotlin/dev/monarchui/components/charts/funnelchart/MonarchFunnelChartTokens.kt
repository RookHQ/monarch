package dev.monarchui.components.charts.funnelchart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendTokens
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** The resolved strings and number formats of a funnel. Templates use `%1$s`-style placeholders. */
@Suppress("LongParameterList")
internal class FunnelChartWords(
    val locale: Locale,
    val formatValue: (Double) -> String,
    val formatRate: (Double) -> String,
    val step: String,
    val stages: (Int, String, String) -> String,
    val overall: String,
    val largestDrop: String,
    val describeFirst: String,
    val describe: String,
    val noValue: String,
)

/** The largest drop between two stages: the index of the stage before it and the share lost. */
internal data class FunnelChartDrop(val from: Int, val lost: Double)

/** The numbers of a funnel, computed once from its stages. */
internal class FunnelChartModel(stages: List<MonarchFunnelChartStage>) {
    /** The values, with negative and non-finite ones as zero. */
    val values: List<Double> = stages.map { if (it.value.isFinite()) maxOf(0.0, it.value) else 0.0 }

    /** The largest value, which fills the track. */
    val widest: Double = values.maxOrNull() ?: 0.0

    /** The share of the previous stage: `null` for the first, zero after a stage of zero. */
    val fromPrevious: List<Double?> = values.mapIndexed { index, value ->
        if (index == 0) null else values[index - 1].let { previous -> if (previous > 0) value / previous else 0.0 }
    }

    /** The share of the first stage, zero when the first is zero. */
    val ofFirst: List<Double> = values.map { value ->
        values.firstOrNull()?.let { first -> if (first > 0) value / first else 0.0 } ?: 0.0
    }

    /** The largest loss between two stages; a stage larger than the one before is not a drop. */
    val largestDrop: FunnelChartDrop? = run {
        var best: FunnelChartDrop? = null
        fromPrevious.forEachIndexed { index, rate ->
            if (rate == null || values[index - 1] <= 0) return@forEachIndexed
            val lost = 1 - rate
            if (lost > 0 && (best == null || lost > best.lost)) best = FunnelChartDrop(index - 1, lost)
        }
        best
    }

    /** How many dropped between the previous stage and [index], never below zero. */
    fun dropped(index: Int): Double = if (index == 0) 0.0 else maxOf(0.0, values[index - 1] - values[index])
}

/** Component tokens and the pure rules of MonarchFunnelChart. */
internal object MonarchFunnelChartTokens {
    /** The stage label and value: 14 regular (the value semibold, tabular), under the 16 title of the card. */
    fun bodyStyle(typography: MonarchTypography): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = (MonarchTokens.FontSize.sm * MonarchTokens.LineHeight.normal).sp,
    )

    /** The space between stages. */
    val StageGap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the step line, the head and the bar of a stage. */
    val PartGap: Dp = MonarchTokens.Space.space1.dp

    /**
     * The padding of a stage: 4dp above and below, none at the sides, so the labels and the bars start at the
     * same edge as the chart's title (PREMIUM rule 5).
     */
    val StageVerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val StageHorizontalPadding: Dp = MonarchTokens.Space.space0.dp

    /** The space between the label and the value, and between the value and the share. */
    val HeadGap: Dp = MonarchTokens.Space.space3.dp
    val ValueGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The height of a bar: a slab as on the web, so the funnel narrows visibly from stage to stage. */
    val BarHeight: Dp = MonarchTokens.Space.space5.dp

    /** The smallest kept height of the chart. */
    val MinHeight: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp

    /** The first guess of the height of one stage, before it is measured: 76dp with steps, 56dp without. */
    val StageEstimateWithStep: Dp = (MonarchTokens.Space.space16 + MonarchTokens.Space.space3).dp
    val StageEstimate: Dp = (MonarchTokens.Space.space12 + MonarchTokens.Space.space2).dp

    /** The smallest drawn share of a value above zero, so a tiny stage still shows a sliver. */
    private const val SLIVER = 0.005f

    /** The first guess of the chart's height, before the stages are measured. */
    fun estimatedHeight(count: Int, showSteps: Boolean): Dp =
        maxOf(MinHeight, (if (showSteps) StageEstimateWithStep else StageEstimate) * count)

    /** The share of the track a bar fills: its value over the widest, with a sliver for a tiny value above zero. */
    fun barFraction(value: Double, widest: Double): Float = when {
        widest <= 0 || value <= 0 -> 0f
        else -> maxOf((value / widest).toFloat().coerceAtMost(1f), SLIVER)
    }

    /**
     * The colour of a stage's bar: its palette slot when the stage names one, the first data colour otherwise,
     * as on the web (`--mn-color-data-1`). Not the accent: the default Rook accent is red and reads as danger.
     */
    fun barColor(color: MonarchChartColor?, colors: MonarchColors): Color =
        color?.let { MonarchChartLegendTokens.color(it, colors) } ?: colors.data1

    /** No track: the web draws only the bars, centred. */
    fun trackColor(): Color = Color.Transparent

    /** The label and value of a stage. */
    fun textColor(colors: MonarchColors): Color = colors.textDefault

    /** The share and the step line. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The step line's text, or `null` for the first stage. */
    fun stepText(model: FunnelChartModel, index: Int, words: FunnelChartWords): String? {
        val rate = model.fromPrevious.getOrNull(index) ?: return null
        return String.format(words.locale, words.step, words.formatRate(rate), words.formatValue(model.dropped(index)))
    }

    /** What TalkBack reads for one stage. */
    fun describe(
        stages: List<MonarchFunnelChartStage>,
        model: FunnelChartModel,
        index: Int,
        words: FunnelChartWords,
    ): String {
        val label = stages[index].label
        val value = words.formatValue(model.values[index])
        val rate = model.fromPrevious[index]
            ?: return String.format(words.locale, words.describeFirst, label, value)
        val ofFirst = words.formatRate(model.ofFirst[index])
        return String.format(words.locale, words.describe, label, value, words.formatRate(rate), ofFirst)
    }

    /** The generated summary: the stages, the overall conversion and the largest drop. */
    fun summary(stages: List<MonarchFunnelChartStage>, model: FunnelChartModel, words: FunnelChartWords): String {
        val formatRate = words.formatRate
        if (stages.isEmpty()) return words.noValue
        val sentences = mutableListOf(words.stages(stages.size, stages.first().label, stages.last().label))
        if (stages.size > 1) {
            sentences += String.format(words.locale, words.overall, formatRate(model.ofFirst.last()))
        }
        model.largestDrop?.let { drop ->
            sentences += String.format(
                words.locale,
                words.largestDrop,
                stages[drop.from].label,
                stages[drop.from + 1].label,
                formatRate(drop.lost),
            )
        }
        return sentences.joinToString(". ")
    }
}
