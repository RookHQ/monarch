package com.rookhq.monarch.components.datagrid.tabletoolbar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkAction
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkActionBar
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.text.NumberFormat

/**
 * The bar above a table or a list: a search field, filters, the result count
 * and the app's controls (a view switcher, a column menu, actions). While rows
 * are selected, the search and the filters give way to `MonarchBulkActionBar`
 * with the count, [bulkActions] and Clear selection.
 *
 * Built by Monarch from Compose layouts: Material 3 has no table toolbar. The
 * search field, the filters and the controls are slots, so the app passes
 * `MonarchSearchBar`, filter chips, a `SingleChoiceSegmentedButtonRow` and a
 * Columns `DropdownMenu` (see the view-switcher and column-menu recipes). From
 * the `sm` breakpoint (640dp), when the count and the controls take at most
 * half the width, the start part and the end part share one line; otherwise
 * the controls take a second line, aligned to the end. The filters scroll
 * sideways when they do not fit; the controls wrap. The result count is a
 * polite live region, so TalkBack reads it when it changes.
 *
 * @param modifier the modifier of the toolbar.
 * @param resultCount rows that match the search and filters, shown as "N results". `null` hides the count.
 * @param selectedCount selected rows. While it is above 0 and [onClearSelection] is set, the bulk bar replaces
 * the search and the filters.
 * @param totalCount rows that could be selected, for "3 of 120 selected".
 * @param bulkActions actions on the selected rows, shown in the bulk bar.
 * @param onClearSelection empties the selection. Without it there is no bulk bar.
 * @param labels replacements for the built-in strings.
 * @param search a search field, such as `MonarchSearchBar`, first in the start part.
 * @param filters filter controls after the search, in a row that scrolls sideways.
 * @param controls the trailing controls: a view switcher, a column menu and actions. They wrap when they do
 * not fit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchTableToolbar(
    modifier: Modifier = Modifier,
    resultCount: Int? = null,
    selectedCount: Int = 0,
    totalCount: Int? = null,
    bulkActions: List<MonarchBulkAction> = emptyList(),
    onClearSelection: (() -> Unit)? = null,
    labels: MonarchTableToolbarLabels = MonarchTableToolbarLabels(),
    search: (@Composable () -> Unit)? = null,
    filters: (@Composable () -> Unit)? = null,
    controls: @Composable () -> Unit = {},
) {
    val tokens = MonarchTableToolbarTokens
    val selecting = tokens.selecting(selectedCount, onClearSelection != null)
    val toolbarName = monarchString(labels.toolbar, R.string.monarch_tabletoolbar_toolbar)
    val countText = resultCount?.let { tableToolbarResults(it, labels) }
    TableToolbarLayout(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = toolbarName
        },
        start = {
            Column {
                if (!selecting) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (search != null) {
                            Box(modifier = Modifier.weight(1f)) { search() }
                        }
                        if (filters != null) {
                            Row(
                                modifier = if (search != null) {
                                    Modifier
                                } else {
                                    Modifier.horizontalScroll(rememberScrollState())
                                },
                                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                                verticalAlignment = Alignment.CenterVertically,
                            ) { filters() }
                        }
                    }
                }
                if (onClearSelection != null) {
                    // Composed at 0 as well, so the bar announces "Selection cleared" when the selection empties.
                    MonarchBulkActionBar(
                        count = selectedCount,
                        onClear = onClearSelection,
                        totalCount = totalCount,
                        actions = bulkActions,
                        labels = labels.bulkActionBar,
                    )
                }
            }
        },
        end = {
            // The controls wrap onto a second line when they do not fit, so none is cut off at the edge.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (countText != null) {
                    Text(
                        text = countText,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MonarchTheme.typography.caption,
                        color = tokens.countText(MonarchTheme.colors),
                    )
                }
                controls()
            }
        },
    )
}

/** The result count in the locale's digits. */
@Composable
private fun tableToolbarResults(count: Int, labels: MonarchTableToolbarLabels): String {
    labels.results?.let { return it(count) }
    val locale = LocalConfiguration.current.locales[0]
    val formatted = NumberFormat.getIntegerInstance(locale).format(count)
    return pluralStringResource(R.plurals.monarch_tabletoolbar_results, count, formatted)
}

/**
 * Places [start] and [end] on one line when [MonarchTableToolbarTokens.wide] allows it, with [end] at the end;
 * otherwise [end] goes under [start], aligned to the end. An empty part takes no room and no gap.
 */
@Composable
private fun TableToolbarLayout(
    modifier: Modifier,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
) {
    val gapDp = MonarchTableToolbarTokens.Gap
    Layout(contents = listOf(start, end), modifier = modifier) { (startMeasurables, endMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val endPlaceables = endMeasurables.map { it.measure(loose) }
        val endSize = tableToolbarSize(endPlaceables)
        val bounded = constraints.hasBoundedWidth
        val wide = bounded && MonarchTableToolbarTokens.wide(constraints.maxWidth.toDp(), endSize.width.toDp())
        val gap = gapDp.roundToPx()
        val startMax = when {
            !bounded -> Constraints.Infinity
            wide && endSize.width > 0 -> (constraints.maxWidth - endSize.width - gap).coerceAtLeast(0)
            else -> constraints.maxWidth
        }
        val startConstraints = Constraints(maxWidth = startMax, maxHeight = loose.maxHeight)
        val startPlaceables = startMeasurables.map { it.measure(startConstraints) }
        val startSize = tableToolbarSize(startPlaceables)
        val width = if (bounded) constraints.maxWidth else maxOf(startSize.width, endSize.width, constraints.minWidth)
        val rows = TableToolbarRows(wide, startSize.height, endSize.height, gap)
        layout(width, rows.height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            startPlaceables.forEach { it.placeRelative(0, rows.startY) }
            endPlaceables.forEach { it.placeRelative(width - it.width, rows.endY) }
        }
    }
}

/** The widest and the tallest of [placeables]; zero when there are none. */
private fun tableToolbarSize(placeables: List<Placeable>): IntSize =
    IntSize(placeables.maxOfOrNull { it.width } ?: 0, placeables.maxOfOrNull { it.height } ?: 0)

/** Where the two parts go: side by side and centred when [wide], else stacked with [gap] when both have height. */
private class TableToolbarRows(wide: Boolean, startHeight: Int, endHeight: Int, gap: Int) {
    private val stacked = !wide && startHeight > 0 && endHeight > 0
    val height: Int = when {
        wide -> maxOf(startHeight, endHeight)
        stacked -> startHeight + endHeight + gap
        else -> startHeight + endHeight
    }
    val startY: Int = if (wide) (height - startHeight) / 2 else 0
    val endY: Int = when {
        wide -> (height - endHeight) / 2
        stacked -> startHeight + gap
        else -> 0
    }
}
