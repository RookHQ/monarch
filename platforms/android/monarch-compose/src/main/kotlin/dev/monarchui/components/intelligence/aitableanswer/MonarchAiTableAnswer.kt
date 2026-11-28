package dev.monarchui.components.intelligence.aitableanswer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A table the model produced, shown as a real table: column types inferred,
 * sortable headers, Copy as CSV, an optional Open as artifact, and rows that
 * can stream in.
 *
 * Built by Monarch: Compose has no data table, so the grid is rows of cells
 * in a horizontally scrolling `Row`, with each column as wide as its widest
 * value, measured with a `TextMeasurer`. Cells stay on one line. Columns are
 * numbers, ISO dates or text, inferred from the values unless [columns] says
 * otherwise; numbers sit at the end with tabular digits. A sortable header
 * is a button that sorts ascending, then descending; blanks always go last.
 * Copy is a `MonarchCopyButton` that copies every row, in the current order,
 * as CSV. The table is kept independent of the data table components.
 *
 * TalkBack reads the label, title and row count as one heading, each header
 * as a heading button with its sort as the state, and each row as one
 * element of header and value pairs. A sort and the end of streaming are
 * announced through a polite live region.
 *
 * @param rows the rows, as the conversation model's JSON values, for example a tool's output.
 * @param modifier the modifier of the table answer.
 * @param title the title under the AI table label.
 * @param columns order, headers, types and alignment. `null` makes a column of every key, in the order the
 * keys first appear, with inferred types.
 * @param streaming rows are still arriving: a placeholder row, Open as artifact disabled, and "Table
 * complete." announced when it turns `false`.
 * @param sortable whether the headers sort the table.
 * @param copyable shows Copy as CSV.
 * @param visibleRows rows drawn before "Show all rows". Sorting and the CSV use every row.
 * @param icon an AI glyph before the label, as a `MonarchIcon` with a `null` content description. Monarch
 * bundles no icons.
 * @param labels replacements for every built-in string.
 * @param onOpenAsArtifact shows Open as artifact.
 */
@Composable
public fun MonarchAiTableAnswer(
    rows: List<Map<String, MonarchAiJSON>>,
    modifier: Modifier = Modifier,
    title: String? = null,
    columns: List<MonarchAiTableColumn>? = null,
    streaming: Boolean = false,
    sortable: Boolean = true,
    copyable: Boolean = true,
    visibleRows: Int = MonarchAiTableAnswerTokens.DEFAULT_VISIBLE_ROWS,
    icon: (@Composable () -> Unit)? = null,
    labels: MonarchAiTableAnswerLabels = MonarchAiTableAnswerLabels(),
    onOpenAsArtifact: (() -> Unit)? = null,
) {
    val locale = Locale.current.platformLocale
    val resolved = remember(columns, rows) { MonarchAiTableAnswerData.columns(columns, rows) }
    var sortKey by rememberSaveable { mutableStateOf<String?>(null) }
    var ascending by rememberSaveable { mutableStateOf(true) }
    var showingAll by rememberSaveable { mutableStateOf(false) }
    var announcement by remember { mutableStateOf("") }
    val sort = sortKey?.let { MonarchAiTableAnswerData.Sort(it, ascending) }
    val order = remember(rows, sort, resolved, locale) { MonarchAiTableAnswerData.order(rows, sort, resolved, locale) }
    val shown = MonarchAiTableAnswerData.shownCount(rows.size, visibleRows, showingAll)
    val sortedLabel: (String, Boolean) -> String = sortedText(labels)
    val completeText = monarchString(labels.complete, R.string.monarch_aitableanswer_complete)
    var wasStreaming by remember { mutableStateOf(streaming) }
    LaunchedEffect(streaming) {
        if (wasStreaming && !streaming) announcement = completeText
        wasStreaming = streaming
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchAiTableAnswerTokens.SectionGap)) {
        Header(title, rows.size, icon, labels)
        Actions(rows, order, resolved, copyable, streaming, labels, onOpenAsArtifact)
        if (rows.isEmpty() && !streaming) {
            Text(
                text = monarchString(labels.noRows, R.string.monarch_aitableanswer_no_rows),
                style = MonarchTheme.typography.body,
                color = MonarchTheme.colors.textMuted,
            )
        } else {
            MonarchAiTableAnswerGrid(
                rows = order.take(shown).map { rows[it] },
                columns = resolved,
                sort = sort,
                sortable = sortable,
                streaming = streaming,
                locale = locale,
                labels = labels,
                onSort = { key ->
                    val next = MonarchAiTableAnswerData.toggled(sort, key)
                    sortKey = next.key
                    ascending = next.ascending
                    val label = resolved.first { it.key == key }.label
                    announcement = sortedLabel(label, next.ascending)
                },
            )
        }
        if (shown < rows.size) {
            MonarchButton(
                text = labels.showAllRows?.invoke(rows.size)
                    ?: pluralStringResource(R.plurals.monarch_aitableanswer_show_all_rows, rows.size, rows.size),
                onClick = { showingAll = true },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun sortedText(labels: MonarchAiTableAnswerLabels): (String, Boolean) -> String {
    val resources = LocalResources.current
    return { column, ascending ->
        val id = if (ascending) {
            R.string.monarch_aitableanswer_sorted_ascending
        } else {
            R.string.monarch_aitableanswer_sorted_descending
        }
        labels.sorted?.invoke(column, ascending) ?: resources.getString(id, column)
    }
}

@Composable
private fun Header(title: String?, count: Int, icon: (@Composable () -> Unit)?, labels: MonarchAiTableAnswerLabels) {
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
        verticalArrangement = Arrangement.spacedBy(MonarchAiTableAnswerTokens.TitleGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchAiTableAnswerTokens.EyebrowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MonarchAiTableAnswerTokens.eyebrowColor(colors),
                    content = icon,
                )
            }
            Text(
                text = monarchString(labels.aiTable, R.string.monarch_aitableanswer_ai_table),
                style = MonarchTheme.typography.caption,
                color = MonarchAiTableAnswerTokens.eyebrowColor(colors),
            )
        }
        if (!title.isNullOrBlank()) {
            Text(text = title, style = MonarchTheme.typography.subheading, color = colors.textDefault)
        }
        Text(
            text = labels.rowCount?.invoke(count)
                ?: pluralStringResource(R.plurals.monarch_aitableanswer_row_count, count, count),
            style = MonarchTheme.typography.caption,
            color = colors.textMuted,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun Actions(
    rows: List<Map<String, MonarchAiJSON>>,
    order: List<Int>,
    columns: List<MonarchAiTableAnswerData.Column>,
    copyable: Boolean,
    streaming: Boolean,
    labels: MonarchAiTableAnswerLabels,
    onOpenAsArtifact: (() -> Unit)?,
) {
    if (!copyable && onOpenAsArtifact == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiTableAnswerTokens.ActionGap),
        verticalArrangement = Arrangement.spacedBy(MonarchAiTableAnswerTokens.ActionGap),
    ) {
        if (copyable) {
            val csv = remember(rows, order, columns) { MonarchAiTableAnswerData.csv(order.map { rows[it] }, columns) }
            MonarchCopyButton(
                value = csv,
                size = MonarchButtonSize.Sm,
                enabled = rows.isNotEmpty(),
                label = monarchString(labels.copyCsv, R.string.monarch_aitableanswer_copy_csv),
                copiedLabel = monarchString(labels.copied, R.string.monarch_aitableanswer_copied),
            )
        }
        if (onOpenAsArtifact != null) {
            MonarchButton(
                text = monarchString(labels.openAsArtifact, R.string.monarch_aitableanswer_open_as_artifact),
                onClick = onOpenAsArtifact,
                size = MonarchButtonSize.Sm,
                enabled = !streaming,
            )
        }
    }
}
