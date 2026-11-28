package dev.monarchui.components.charts.funnelchart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.charts.chartframe.MonarchChartFrame
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartframe.MonarchChartTable
import dev.monarchui.components.charts.chartframe.MonarchChartTableSeries
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchChartPercentFormat
import dev.monarchui.internal.rememberMonarchChartValueFormat
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * A conversion funnel: how many reach each step, the share that continues to
 * the next step and how many drop off.
 *
 * Built by Monarch inside `MonarchChartFrame`: Material 3 has no chart. Each
 * stage is a row with the step line, the label, the value, its share of the
 * first stage and a bar; the bars are decoration and every number they show
 * is also text. The stages are a group named with a generated summary (the
 * number of stages, the overall conversion and the largest drop), and each
 * stage is one TalkBack element read as its label, value and shares. "View as
 * table" lists every count and share. The chart's kept height follows its
 * stages, so text at large sizes makes it taller instead of cutting it.
 *
 * @param title the chart's name, shown as the frame's heading.
 * @param stages the steps, first (widest) to last.
 * @param modifier the modifier of the frame.
 * @param description one line under the title, such as the period.
 * @param status what the frame shows. A ready chart without stages shows the empty state.
 * @param alignment centred bars read as a funnel; start-aligned bars are easier to compare.
 * @param showSteps shows the line between stages with the share that continued and the number dropped.
 * @param summary replaces the generated summary that TalkBack reads for the stages.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart, such as the source.
 * @param formatValue writes a count. `null` writes a number in the current locale.
 * @param formatRate writes a share from 0 to 1. `null` writes a percentage with at most one decimal.
 * @param onRetry shows Try again in the error status.
 * @param onSelect makes every stage a button called with its stage, for example to list the people who dropped.
 * @param labels replacements for the built-in strings, including the frame's.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchFunnelChart(
    title: String,
    stages: List<MonarchFunnelChartStage>,
    modifier: Modifier = Modifier,
    description: String? = null,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    alignment: MonarchFunnelChartAlignment = MonarchFunnelChartAlignment.Center,
    showSteps: Boolean = true,
    summary: String? = null,
    emptyMessage: String? = null,
    footer: String? = null,
    formatValue: ((Double) -> String)? = null,
    formatRate: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((MonarchFunnelChartStage) -> Unit)? = null,
    labels: MonarchFunnelChartLabels = MonarchFunnelChartLabels(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val writeValue = rememberMonarchChartValueFormat(formatValue)
    val writeRate = formatRate ?: rememberMonarchChartPercentFormat(1)
    val words = funnelChartWords(labels, locale, writeValue, writeRate)
    val model = remember(stages) { FunnelChartModel(stages) }
    val table = MonarchChartTable(
        categoryTitle = monarchString(labels.stage, R.string.monarch_funnelchart_stage),
        categories = stages.map { it.label },
        series = listOf(
            MonarchChartTableSeries(
                name = monarchString(labels.value, R.string.monarch_funnelchart_value),
                values = model.values,
                formattedValues = model.values.map(writeValue),
            ),
            MonarchChartTableSeries(
                name = monarchString(labels.fromPrevious, R.string.monarch_funnelchart_from_previous),
                values = model.fromPrevious,
                formattedValues = model.fromPrevious.map { rate -> rate?.let(writeRate).orEmpty() },
            ),
            MonarchChartTableSeries(
                name = monarchString(labels.ofFirst, R.string.monarch_funnelchart_of_first),
                values = model.ofFirst,
                formattedValues = model.ofFirst.map(writeRate),
            ),
        ),
    )
    val density = LocalDensity.current
    var measured by remember { mutableStateOf<Dp?>(null) }
    val height = maxOf(
        MonarchFunnelChartTokens.MinHeight,
        measured ?: MonarchFunnelChartTokens.estimatedHeight(stages.size, showSteps),
    )
    MonarchChartFrame(
        title = title,
        modifier = modifier,
        description = description,
        status = if (stages.isEmpty() && status == MonarchChartFrameStatus.Ready) {
            MonarchChartFrameStatus.Empty
        } else {
            status
        },
        table = table,
        emptyMessage = emptyMessage,
        footer = footer,
        height = height,
        onRetry = onRetry,
        labels = labels.frame,
    ) {
        val spoken = summary ?: MonarchFunnelChartTokens.summary(stages, model, words)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .onSizeChanged { measured = with(density) { it.height.toDp() } }
                .semantics {
                    isTraversalGroup = true
                    contentDescription = spoken
                    collectionInfo = CollectionInfo(rowCount = stages.size, columnCount = 1)
                },
            verticalArrangement = Arrangement.spacedBy(MonarchFunnelChartTokens.StageGap),
        ) {
            stages.forEachIndexed { index, stage ->
                FunnelStage(
                    stage = stage,
                    index = index,
                    model = model,
                    step = if (showSteps) {
                        MonarchFunnelChartTokens.stepText(model, index, words)
                    } else {
                        null
                    },
                    spoken = MonarchFunnelChartTokens.describe(stages, model, index, words),
                    alignment = alignment,
                    writeValue = writeValue,
                    writeRate = writeRate,
                    onSelect = onSelect,
                )
            }
        }
    }
}

/** The built-in strings, or their replacements. */
@Composable
private fun funnelChartWords(
    labels: MonarchFunnelChartLabels,
    locale: Locale,
    formatValue: (Double) -> String,
    formatRate: (Double) -> String,
): FunnelChartWords {
    val resources = LocalResources.current
    return FunnelChartWords(
        locale = locale,
        formatValue = formatValue,
        formatRate = formatRate,
        step = monarchString(labels.step, R.string.monarch_funnelchart_step),
        stages = labels.stages ?: { count, first, last ->
            resources.getQuantityString(R.plurals.monarch_funnelchart_stages, count, count, first, last)
        },
        overall = monarchString(labels.overall, R.string.monarch_funnelchart_overall),
        largestDrop = monarchString(labels.largestDrop, R.string.monarch_funnelchart_largest_drop),
        describeFirst = monarchString(labels.describeFirst, R.string.monarch_funnelchart_describe_first),
        describe = monarchString(labels.describe, R.string.monarch_funnelchart_describe),
        noValue = monarchString(labels.frame.noValue, R.string.monarch_chartframe_no_value),
    )
}

@Composable
@Suppress("LongParameterList")
private fun FunnelStage(
    stage: MonarchFunnelChartStage,
    index: Int,
    model: FunnelChartModel,
    step: String?,
    spoken: String,
    alignment: MonarchFunnelChartAlignment,
    writeValue: (Double) -> String,
    writeRate: (Double) -> String,
    onSelect: ((MonarchFunnelChartStage) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val shape = MonarchShapeDefaults.Row
    val interaction = if (onSelect != null) {
        Modifier
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(shape)
            .clickable(role = Role.Button) { onSelect(stage) }
    } else {
        Modifier
    }
    // One element per stage: the spoken description replaces the texts, which stay for layout and tests.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(interaction)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
            }
            .padding(
                horizontal = MonarchFunnelChartTokens.StageHorizontalPadding,
                vertical = MonarchFunnelChartTokens.StageVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(MonarchFunnelChartTokens.PartGap),
    ) {
        if (step != null) {
            Text(text = step, style = typography.caption, color = MonarchFunnelChartTokens.mutedColor(colors))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MonarchFunnelChartTokens.HeadGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stage.label,
                modifier = Modifier.weight(1f),
                style = MonarchFunnelChartTokens.bodyStyle(typography),
                color = MonarchFunnelChartTokens.textColor(colors),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchFunnelChartTokens.ValueGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = writeValue(model.values[index]),
                    style = MonarchTabularNumberStyle.applyTo(MonarchFunnelChartTokens.bodyStyle(typography)),
                    fontWeight = FontWeight.SemiBold,
                    color = MonarchFunnelChartTokens.textColor(colors),
                )
                if (index > 0) {
                    Text(
                        text = writeRate(model.ofFirst[index]),
                        style = MonarchTabularNumberStyle.applyTo(typography.caption),
                        color = MonarchFunnelChartTokens.mutedColor(colors),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MonarchFunnelChartTokens.BarHeight)
                .background(MonarchFunnelChartTokens.trackColor(), MonarchShapeDefaults.Mark),
            contentAlignment = when (alignment) {
                MonarchFunnelChartAlignment.Center -> Alignment.Center
                MonarchFunnelChartAlignment.Start -> Alignment.CenterStart
            },
        ) {
            val fraction = MonarchFunnelChartTokens.barFraction(model.values[index], model.widest)
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(MonarchFunnelChartTokens.barColor(stage.color, colors), MonarchShapeDefaults.Mark),
                )
            }
        }
    }
}
