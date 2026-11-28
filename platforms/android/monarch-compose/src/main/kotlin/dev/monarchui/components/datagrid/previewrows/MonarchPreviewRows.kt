package dev.monarchui.components.datagrid.previewrows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.navigation.tabs.MonarchTabs
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The rows of an import file as the import will treat them: each row's
 * number, outcome (ready, warning, error, duplicate), values and problems,
 * with filter tabs and counts.
 *
 * Built by Monarch from a `LazyColumn` of rows, each headed by a
 * `MonarchStatusBadge` with the outcome, and `MonarchTabs` of filters
 * with a count. The tabs appear when some row is not ready. Each row is read by TalkBack as
 * one element: its number, outcome, values and problems. The outcome is
 * always written, never shown by colour alone.
 *
 * The list scrolls itself: give it a bounded height and do not put it in a
 * vertically scrolling parent.
 *
 * @param rows the rows of the file.
 * @param columns the columns to show for each row, in order.
 * @param label the TalkBack name of the preview, for example "Preview of contacts.csv".
 * @param filter the outcome shown, or `null` for every row.
 * @param onFilterChange called with the outcome chosen in the tabs, or `null` for All.
 * @param modifier the modifier of the preview.
 * @param filterable shows the filter tabs when some row is not ready.
 * @param state the scroll state of the list.
 * @param statusIcon an icon for each outcome inside its badge. Monarch bundles no icons.
 * @param labels replacements for the built-in strings.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchPreviewRows(
    rows: List<MonarchPreviewRow>,
    columns: List<MonarchPreviewRowsColumn>,
    label: String,
    filter: MonarchPreviewRowStatus?,
    onFilterChange: (MonarchPreviewRowStatus?) -> Unit,
    modifier: Modifier = Modifier,
    filterable: Boolean = true,
    state: LazyListState = rememberLazyListState(),
    statusIcon: (@Composable (MonarchPreviewRowStatus) -> Unit)? = null,
    labels: MonarchPreviewRowsLabels = MonarchPreviewRowsLabels(),
) {
    val tokens = MonarchPreviewRowsTokens
    val counts = tokens.counts(rows)
    val shown = tokens.shown(rows, filter)
    val strings = previewRowsStrings(labels)
    LazyColumn(modifier = modifier.semantics { contentDescription = label }, state = state) {
        if (tokens.showsFilter(filterable, counts, filter)) {
            item(key = PreviewRowsKey.Tabs) {
                PreviewRowsTabs(
                    rowCount = rows.size,
                    counts = counts,
                    filter = filter,
                    onFilterChange = onFilterChange,
                    strings = strings,
                )
            }
        }
        if (shown.isEmpty()) {
            item(key = PreviewRowsKey.Empty) {
                if (rows.isEmpty()) {
                    MonarchEmptyState(
                        title = monarchString(labels.emptyTitle, R.string.monarch_previewrows_empty_title),
                        size = MonarchEmptyStateSize.Sm,
                        message = monarchString(labels.emptyMessage, R.string.monarch_previewrows_empty_message),
                    )
                } else {
                    MonarchEmptyState(
                        title = monarchString(labels.filteredTitle, R.string.monarch_previewrows_filtered_title),
                        kind = MonarchEmptyStateKind.Filtered,
                        size = MonarchEmptyStateSize.Sm,
                        onClearFilters = { onFilterChange(null) },
                    )
                }
            }
        }
        items(shown, key = { it.row }) { row ->
            PreviewRowCard(row, columns, strings, statusIcon)
            MonarchDivider()
        }
    }
}

/** Keys of the items that are not rows; row numbers are `Int`, so they cannot clash. */
private enum class PreviewRowsKey { Tabs, Empty }

/** The resolved strings of one composition. */
private class PreviewRowsStrings(
    val filter: String,
    val all: String,
    val status: Map<MonarchPreviewRowStatus, String>,
    val row: (Int) -> String,
    val noValue: String,
    val result: String,
    val issues: String,
)

@Composable
private fun previewRowsStrings(labels: MonarchPreviewRowsLabels): PreviewRowsStrings {
    val rowFormat = stringResource(R.string.monarch_previewrows_row)
    return PreviewRowsStrings(
        filter = monarchString(labels.filter, R.string.monarch_previewrows_filter),
        all = monarchString(labels.all, R.string.monarch_previewrows_all),
        status = mapOf(
            MonarchPreviewRowStatus.Ready to monarchString(labels.ready, R.string.monarch_previewrows_ready),
            MonarchPreviewRowStatus.Warning to monarchString(labels.warning, R.string.monarch_previewrows_warning),
            MonarchPreviewRowStatus.Error to monarchString(labels.error, R.string.monarch_previewrows_error),
            MonarchPreviewRowStatus.Duplicate to
                monarchString(labels.duplicate, R.string.monarch_previewrows_duplicate),
        ),
        row = labels.row ?: { rowFormat.format(it) },
        noValue = monarchString(labels.noValue, R.string.monarch_previewrows_no_value),
        result = monarchString(labels.result, R.string.monarch_previewrows_result),
        issues = monarchString(labels.issues, R.string.monarch_previewrows_issues),
    )
}

@Composable
private fun PreviewRowsTabs(
    rowCount: Int,
    counts: Map<MonarchPreviewRowStatus, Int>,
    filter: MonarchPreviewRowStatus?,
    onFilterChange: (MonarchPreviewRowStatus?) -> Unit,
    strings: PreviewRowsStrings,
) {
    val tabs = listOf<MonarchPreviewRowStatus?>(null) + MonarchPreviewRowsTokens.tabs(counts, filter)
    MonarchTabs(
        selectedIndex = tabs.indexOf(filter).coerceAtLeast(0),
        onSelectedIndexChange = { onFilterChange(tabs[it]) },
        modifier = Modifier.semantics { contentDescription = strings.filter },
    ) {
        tabs.forEach { status ->
            tab(
                text = if (status == null) strings.all else strings.status.getValue(status),
                count = (if (status == null) rowCount else counts.getValue(status)).toString(),
                countTone = MonarchPreviewRowsTokens.countTone(status),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewRowCard(
    row: MonarchPreviewRow,
    columns: List<MonarchPreviewRowsColumn>,
    strings: PreviewRowsStrings,
    statusIcon: (@Composable (MonarchPreviewRowStatus) -> Unit)?,
) {
    val tokens = MonarchPreviewRowsTokens
    val colors = MonarchTheme.colors
    val outcome = row.outcome
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .padding(vertical = tokens.RowPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.RowGap),
    ) {
        val heading = strings.row(row.row)
        val status = strings.status.getValue(outcome)
        val spoken = stringResource(R.string.monarch_previewrows_row_spoken, heading, status)
        // The number alone, as the web shows it; the heading's spoken name keeps the word "Row" and the outcome.
        Text(
            text = row.row.toString(),
            modifier = Modifier.clearAndSetSemantics {
                text = AnnotatedString(spoken)
                heading()
            },
            style = MonarchTheme.typography.label,
            color = tokens.valueColor(colors),
        )
        // Three columns of a label over its value, as the web lays a row out.
        BoxWithConstraints {
            // Three columns as on the web; two when a third of the width would break an address in the middle.
            val three = (maxWidth - tokens.ValueGap * (tokens.COLUMNS - 1)) / tokens.COLUMNS
            val perRow = if (three < tokens.MinCellWidth) tokens.COLUMNS - 1 else tokens.COLUMNS
            val cellWidth = (maxWidth - tokens.ValueGap * (perRow - 1)) / perRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.ValueGap),
                verticalArrangement = Arrangement.spacedBy(tokens.RowGap),
                maxItemsInEachRow = perRow,
            ) {
                PreviewRowCell(strings.result, cellWidth) {
                    Box(Modifier.clearAndSetSemantics { }) { PreviewRowBadge(outcome, status, statusIcon) }
                }
                columns.forEach { column ->
                    PreviewRowCell(column.label, cellWidth) { PreviewRowValue(column, row.values[column.id], strings) }
                }
                PreviewRowCell(strings.issues, cellWidth) {
                    row.errors.forEach {
                        PreviewRowIssueLine(it, strings.status.getValue(MonarchPreviewRowStatus.Error), true)
                    }
                    row.warnings.forEach {
                        PreviewRowIssueLine(it, strings.status.getValue(MonarchPreviewRowStatus.Warning), false)
                    }
                    if (row.note != null) {
                        // As on the web: the note about a duplicate is underlined in the error colour.
                        Text(
                            text = row.note,
                            style = MonarchTheme.typography.caption,
                            color = tokens.issueColor(true, colors),
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }
            }
        }
    }
}

/** One cell of a row: its name over its value. */
@Composable
private fun PreviewRowCell(name: String, width: Dp, value: @Composable ColumnScope.() -> Unit) {
    val tokens = MonarchPreviewRowsTokens
    Column(modifier = Modifier.width(width), verticalArrangement = Arrangement.spacedBy(tokens.RowGap)) {
        Text(text = name, style = MonarchTheme.typography.caption, color = tokens.mutedColor(MonarchTheme.colors))
        value()
    }
}

@Composable
private fun PreviewRowValue(column: MonarchPreviewRowsColumn, value: String?, strings: PreviewRowsStrings) {
    val tokens = MonarchPreviewRowsTokens
    val colors = MonarchTheme.colors
    if (value.isNullOrBlank()) {
        Text(
            text = stringResource(R.string.monarch_previewrows_dash),
            // Read as words, not as a dash. Set as text, so the merged row reads only texts.
            modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(strings.noValue) },
            style = MonarchTheme.typography.body,
            color = tokens.mutedColor(colors),
        )
    } else {
        val body = MonarchTheme.typography.body
        Text(
            text = value,
            style = if (column.numeric) MonarchTabularNumberStyle.applyTo(body) else body,
            color = tokens.valueColor(colors),
        )
    }
}

@Composable
private fun PreviewRowIssueLine(issue: MonarchPreviewRowIssue, kind: String, error: Boolean) {
    val colors = MonarchTheme.colors
    val text = issue.field?.let { stringResource(R.string.monarch_previewrows_issue_field, it, issue.message) }
        ?: issue.message
    val spoken = stringResource(R.string.monarch_previewrows_issue_spoken, kind, text)
    // The field name is bold, the message regular, as on the web.
    val shown = buildAnnotatedString {
        append(text)
        issue.field?.let { field ->
            val end = text.indexOf(issue.message).takeIf { it > 0 } ?: (field.length + 1)
            addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), 0, end.coerceAtMost(text.length))
        }
    }
    Text(
        text = shown,
        // The kind is read first; set as text, so the merged row reads only texts.
        modifier = Modifier.clearAndSetSemantics { this.text = AnnotatedString(spoken) },
        style = MonarchTheme.typography.caption,
        color = MonarchPreviewRowsTokens.issueColor(error, colors),
    )
}

@Composable
private fun PreviewRowBadge(
    outcome: MonarchPreviewRowStatus,
    status: String,
    statusIcon: (@Composable (MonarchPreviewRowStatus) -> Unit)?,
) {
    MonarchStatusBadge(
        definition = MonarchStatusDefinition(
            label = status,
            tone = MonarchPreviewRowsTokens.tone(outcome),
            icon = statusIcon?.let { icon -> { icon(outcome) } },
        ),
        size = MonarchStatusBadgeSize.Sm,
    )
}
