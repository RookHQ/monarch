package com.rookhq.monarch.components.charts.chartframe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegend
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendItem
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendMarker
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The card a chart sits in: title, description, header controls, legend,
 * loading, empty and error states, and the chart's data as a table.
 *
 * Built by Monarch from `MonarchCard` and slots: Material 3 has no chart. The
 * body keeps [height] in every status, so the screen does not jump when the
 * data arrives. With a [table], "View as table" replaces the chart with its
 * data, the text alternative TalkBack users and anyone who needs exact values
 * can read; each table row is read as the category and every series with its
 * value. Draw the chart itself in [content], for example with a `Canvas` or a
 * `MonarchSparkline`.
 *
 * @param title the chart's name, shown as a heading.
 * @param modifier the modifier of the frame.
 * @param description one line under the title: the period, the unit or what the chart answers.
 * @param status what the body shows: the chart, a skeleton, the empty state or the error.
 * @param table the chart's data. Enables "View as table".
 * @param legend the legend entries, shown under the header while the chart is visible.
 * @param hiddenSeries the keys of the hidden series, used when [onHiddenSeriesChange] is set.
 * @param onHiddenSeriesChange makes the legend entries toggles; called with the new hidden set.
 * @param legendMarker the shape of the legend markers.
 * @param emptyMessage the message of the empty state, such as what will appear and when.
 * @param footer notes under the chart: the source, the last update.
 * @param height the height kept for the body in every status. The empty and error states grow when their
 * text needs more room, for example at large font sizes.
 * @param tableToggle offers "View as table" when there is a [table].
 * @param onRetry shows Try again in the error status.
 * @param labels replacements for the built-in strings.
 * @param actions controls in the header, such as a period picker.
 * @param content the chart, in a box of the frame's width and [height].
 */
@Composable
public fun MonarchChartFrame(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    table: MonarchChartTable? = null,
    legend: List<MonarchChartLegendItem> = emptyList(),
    hiddenSeries: Set<String> = emptySet(),
    onHiddenSeriesChange: ((Set<String>) -> Unit)? = null,
    legendMarker: MonarchChartLegendMarker = MonarchChartLegendMarker.Line,
    emptyMessage: String? = null,
    footer: String? = null,
    height: Dp = MonarchChartFrameTokens.DefaultHeight,
    tableToggle: Boolean = true,
    onRetry: (() -> Unit)? = null,
    labels: MonarchChartFrameLabels = MonarchChartFrameLabels(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var showTable by rememberSaveable { mutableStateOf(false) }
    val tableShown = showTable && table != null && status == MonarchChartFrameStatus.Ready
    MonarchCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchChartFrameTokens.SectionGap)) {
            val toggle = MonarchChartFrameTokens.showsTableToggle(status, table, tableToggle)
            Header(title, description, actions, toggle, tableShown, { showTable = !showTable }, labels)
            if (MonarchChartFrameTokens.showsLegend(status, tableShown, legend.size)) {
                if (onHiddenSeriesChange != null) {
                    MonarchChartLegend(
                        items = legend,
                        hiddenSeries = hiddenSeries,
                        onHiddenSeriesChange = onHiddenSeriesChange,
                        marker = legendMarker,
                        legendLabel = labels.legend,
                        showAllLabel = labels.showAll,
                    )
                } else {
                    MonarchChartLegend(items = legend, marker = legendMarker, legendLabel = labels.legend)
                }
            }
            Body(status, table, tableShown, height, emptyMessage, onRetry, labels, content)
            Footer(footer)
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun Header(
    title: String,
    description: String?,
    actions: (@Composable RowScope.() -> Unit)?,
    toggle: Boolean,
    showTable: Boolean,
    onToggle: () -> Unit,
    labels: MonarchChartFrameLabels,
) {
    val toggleButton: @Composable () -> Unit = {
        if (toggle) {
            MonarchButton(
                text = if (showTable) {
                    monarchString(labels.viewChart, R.string.monarch_chartframe_view_chart)
                } else {
                    monarchString(labels.viewTable, R.string.monarch_chartframe_view_table)
                },
                onClick = onToggle,
                modifier = Modifier.offset(x = MonarchChartFrameTokens.ghostInset()),
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MonarchChartFrameTokens.ActionsGap),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MonarchChartFrameTokens.TitleGap),
            ) {
                MonarchText(text = title, style = MonarchTextStyle.BodyStrong, heading = true)
                if (description != null) {
                    MonarchText(
                        text = description,
                        style = MonarchTextStyle.Caption,
                        color = MonarchTheme.colors.textMuted,
                    )
                }
            }
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MonarchChartFrameTokens.ActionsGap),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
        // As on the web, "View as table" has its own row under the title and the actions, at the end.
        if (toggle) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { toggleButton() }
    }
}

@Composable
@Suppress("LongParameterList")
private fun Body(
    status: MonarchChartFrameStatus,
    table: MonarchChartTable?,
    tableShown: Boolean,
    height: Dp,
    emptyMessage: String?,
    onRetry: (() -> Unit)?,
    labels: MonarchChartFrameLabels,
    content: @Composable BoxScope.() -> Unit,
) {
    when (status) {
        MonarchChartFrameStatus.Ready -> if (tableShown && table != null) {
            ChartDataTable(
                table = table,
                noValue = monarchString(labels.noValue, R.string.monarch_chartframe_no_value),
                modifier = Modifier.heightIn(min = height),
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(height), content = content)
        }
        MonarchChartFrameStatus.Loading -> {
            val loading = monarchString(labels.loading, R.string.monarch_chartframe_loading)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clearAndSetSemantics {
                        contentDescription = loading
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    },
            ) { MonarchSkeleton(modifier = Modifier.fillMaxSize()) }
        }
        MonarchChartFrameStatus.Empty -> Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = height),
            contentAlignment = Alignment.Center,
        ) {
            MonarchEmptyState(
                title = monarchString(labels.emptyTitle, R.string.monarch_chartframe_empty_title),
                message = emptyMessage,
                size = MonarchEmptyStateSize.Sm,
            )
        }
        MonarchChartFrameStatus.Error -> {
            val error = monarchString(labels.error, R.string.monarch_chartframe_error)
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = height),
                contentAlignment = Alignment.Center,
            ) {
                if (onRetry != null) {
                    MonarchInlineErrorRetry(message = error, onRetry = onRetry)
                } else {
                    MonarchText(
                        text = error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = MonarchTheme.colors.dangerStrong,
                    )
                }
            }
        }
    }
}

@Composable
private fun Footer(footer: String?) {
    if (footer == null) return
    MonarchText(text = footer, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
}
