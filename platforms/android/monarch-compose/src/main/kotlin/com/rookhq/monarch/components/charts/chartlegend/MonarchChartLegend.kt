package com.rookhq.monarch.components.charts.chartlegend

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The names and colours of a chart's series, as a plain list.
 *
 * Built by Monarch: Material 3 has no chart or legend. Each entry is a marker
 * in the series' palette slot, the label and an optional value, read by
 * TalkBack as one element ("Revenue, $48,200"); the marker is decoration, so
 * a series is never identified by colour alone. For a legend that shows and
 * hides series, use the overload with `hiddenSeries`.
 *
 * @param items the entries, in series order.
 * @param modifier the modifier of the legend.
 * @param marker the shape of the markers. Use the shape the chart draws.
 * @param orientation `Horizontal` wraps the entries onto new rows; `Vertical` stacks them with the values
 * at the end.
 * @param enabled `false` draws markers and text in the disabled colour.
 * @param legendLabel replaces the built-in spoken name of the legend, "Legend" (`monarch_chartlegend_legend`).
 */
@Composable
public fun MonarchChartLegend(
    items: List<MonarchChartLegendItem>,
    modifier: Modifier = Modifier,
    marker: MonarchChartLegendMarker = MonarchChartLegendMarker.Line,
    orientation: Orientation = Orientation.Horizontal,
    enabled: Boolean = true,
    legendLabel: String? = null,
) {
    val name = monarchString(legendLabel, R.string.monarch_chartlegend_legend)
    LegendLayout(orientation, modifier, name) {
        items.forEachIndexed { index, item ->
            val slot = MonarchChartLegendTokens.slot(item.color, index)
            Row(
                modifier = Modifier
                    .then(if (orientation == Orientation.Vertical) Modifier.fillMaxWidth() else Modifier)
                    .semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(MonarchChartLegendTokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartLegendMarkerShape(
                    marker = marker,
                    color = MonarchChartLegendTokens.markerColor(slot, shown = true, enabled, MonarchTheme.colors),
                    dashed = item.dashed,
                )
                EntryText(item, shown = true, enabled, orientation)
            }
        }
    }
}

/**
 * A chart legend whose entries show and hide their series.
 *
 * Each entry is a borderless toggle (a selectable row with the checkbox role)
 * that is selected while its series is shown, with the state "Shown" or "Hidden" for TalkBack. A hidden series
 * has a struck-through label and a grey marker, so the state is not colour
 * alone. With [keepOneVisible] the last shown series ignores the tap, so the
 * chart is never blank. While one of the legend's series is hidden, a "Show
 * all" button follows the entries.
 *
 * @param items the entries, in series order.
 * @param hiddenSeries the keys of the hidden series. Keys the legend does not list are kept, so several
 * legends can share one set.
 * @param onHiddenSeriesChange called with the new set after a toggle or Show all.
 * @param modifier the modifier of the legend.
 * @param marker the shape of the markers.
 * @param orientation `Horizontal` wraps the entries onto new rows; `Vertical` stacks them.
 * @param enabled whether the user can toggle the entries.
 * @param keepOneVisible stops the last shown series from being hidden.
 * @param showAllButton shows the "Show all" button while one of the legend's series is hidden.
 * @param legendLabel replaces the built-in spoken name "Legend" (`monarch_chartlegend_legend`).
 * @param showAllLabel replaces the built-in button text "Show all" (`monarch_chartlegend_show_all`).
 * @param shownLabel replaces the built-in state "Shown" (`monarch_chartlegend_shown`).
 * @param hiddenLabel replaces the built-in state "Hidden" (`monarch_chartlegend_hidden`).
 */
@Composable
public fun MonarchChartLegend(
    items: List<MonarchChartLegendItem>,
    hiddenSeries: Set<String>,
    onHiddenSeriesChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    marker: MonarchChartLegendMarker = MonarchChartLegendMarker.Line,
    orientation: Orientation = Orientation.Horizontal,
    enabled: Boolean = true,
    keepOneVisible: Boolean = true,
    showAllButton: Boolean = true,
    legendLabel: String? = null,
    showAllLabel: String? = null,
    shownLabel: String? = null,
    hiddenLabel: String? = null,
) {
    val name = monarchString(legendLabel, R.string.monarch_chartlegend_legend)
    val shown = monarchString(shownLabel, R.string.monarch_chartlegend_shown)
    val hidden = monarchString(hiddenLabel, R.string.monarch_chartlegend_hidden)
    val showAll = monarchString(showAllLabel, R.string.monarch_chartlegend_show_all)
    val keys = items.map { it.key }
    val colors = MonarchTheme.colors
    LegendLayout(orientation, modifier, name) {
        items.forEachIndexed { index, item ->
            val visible = item.key !in hiddenSeries
            val slot = MonarchChartLegendTokens.slot(item.color, index)
            LegendEntry(
                selected = visible,
                onClick = {
                    val next = MonarchChartLegendTokens.toggle(hiddenSeries, item.key, keys, keepOneVisible)
                    if (next != hiddenSeries) onHiddenSeriesChange(next)
                },
                modifier = Modifier
                    .then(if (orientation == Orientation.Vertical) Modifier.fillMaxWidth() else Modifier)
                    .semantics { stateDescription = if (visible) shown else hidden },
                enabled = enabled,
            ) {
                ChartLegendMarkerShape(
                    marker = marker,
                    color = MonarchChartLegendTokens.markerColor(slot, visible, enabled, colors),
                    dashed = item.dashed,
                )
                EntryText(item, visible, enabled, orientation)
            }
        }
        if (MonarchChartLegendTokens.showsShowAll(hiddenSeries, keys, showAllButton)) {
            MonarchButton(
                text = showAll,
                onClick = { onHiddenSeriesChange(MonarchChartLegendTokens.showAll(hiddenSeries, keys)) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = enabled,
            )
        }
    }
}

/** The row or the column of entries, named for TalkBack and read as one group. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegendLayout(
    orientation: Orientation,
    modifier: Modifier,
    name: String,
    content: @Composable () -> Unit,
) {
    val group = modifier.semantics {
        isTraversalGroup = true
        contentDescription = name
    }
    if (orientation == Orientation.Horizontal) {
        FlowRow(
            modifier = group,
            horizontalArrangement = Arrangement.spacedBy(MonarchChartLegendTokens.ItemSpacing),
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) { content() }
    } else {
        Column(modifier = group) { content() }
    }
}

/**
 * One entry: a borderless row with the checkbox role that is selected while its series is shown, 32dp high in a
 * 48dp touch target. Hover, press and keyboard focus take one surface step; there is no ripple.
 */
@Composable
private fun LegendEntry(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val active = pressed || hovered || focused
    val highlight = if (enabled && active) MonarchTheme.colors.bgSubtle else Color.Transparent
    Row(
        modifier = modifier
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactions,
                indication = null,
                onClick = onClick,
            )
            .monarchMinimumTouchTarget()
            .heightIn(min = MonarchChartLegendTokens.EntryHeight)
            .clip(MonarchShapeDefaults.Row)
            .background(highlight)
            .padding(horizontal = MonarchChartLegendTokens.EntryPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchChartLegendTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** The label, struck through while hidden, and the value. */
@Composable
private fun EntryText(item: MonarchChartLegendItem, shown: Boolean, enabled: Boolean, orientation: Orientation) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    Row(
        modifier = if (orientation == Orientation.Vertical) Modifier.fillMaxWidth() else Modifier,
        horizontalArrangement = Arrangement.spacedBy(MonarchChartLegendTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.label,
            modifier = if (orientation == Orientation.Vertical) Modifier.weight(1f) else Modifier.weight(1f, false),
            style = typography.caption,
            color = MonarchChartLegendTokens.labelColor(shown, enabled, colors),
            textDecoration = if (shown) null else TextDecoration.LineThrough,
        )
        if (item.value != null) {
            Text(
                text = item.value,
                style = typography.caption,
                fontWeight = FontWeight.Medium,
                color = if (enabled) colors.textDefault else colors.textDisabled,
            )
        }
    }
}
