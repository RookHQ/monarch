package com.rookhq.monarch.components.datagrid.columnmapping

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The mapping step of an import: one picker per imported column with its
 * example values, suggested fields from names and aliases, the required
 * fields still unmapped, and the fields chosen twice.
 *
 * Built by Monarch as a `LazyColumn` of list rows, each holding the column's
 * header and example values above a `MonarchSelect` (a Material 3
 * `ExposedDropdownMenuBox`), with a `MonarchBadge` for a suggestion and
 * ghost `MonarchButton`s for the bulk actions. The web's three-column grid
 * does not fit a phone, so each column is a row. The list scrolls itself:
 * give it its own screen or a bounded height.
 *
 * Start from `MonarchColumnMappingDefaults.mapping(MonarchColumnMappingDefaults.suggest(sources, fields))`
 * and check the result with `MonarchColumnMappingDefaults.issues` before going on.
 *
 * @param sources the columns of the imported data, in file order.
 * @param mapping column name to field id. A column missing from it is not imported.
 * @param fields the fields the columns can be mapped to.
 * @param onMappingChange called with the whole new mapping.
 * @param modifier the modifier of the list.
 * @param suggestions the suggestions behind the badges and Use suggestions. By default,
 * `MonarchColumnMappingDefaults.suggest(sources, fields)`.
 * @param showIssues shows the missing required fields and the duplicate errors. Pass `false` until the person
 * first tries to go on.
 * @param showBulkActions shows Use suggestions (when the mapping differs from them) and Don’t import any (when
 * something is mapped).
 * @param enabled whether the pickers and buttons can be used.
 * @param state the scroll state of the list.
 * @param contentPadding the padding around the rows, inside the scrolling area.
 * @param labels replacements for the built-in strings.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchColumnMapping(
    sources: List<MonarchColumnMappingSource>,
    mapping: Map<String, String>,
    fields: List<MonarchColumnMappingField>,
    onMappingChange: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: Map<String, MonarchColumnMappingSuggestion>? = null,
    showIssues: Boolean = true,
    showBulkActions: Boolean = true,
    enabled: Boolean = true,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    labels: MonarchColumnMappingLabels = MonarchColumnMappingLabels(),
) {
    val tokens = MonarchColumnMappingTokens
    val computed = suggestions ?: remember(sources, fields) { MonarchColumnMappingDefaults.suggest(sources, fields) }
    val rows = tokens.rows(sources, mapping, fields, computed, showIssues)
    val options = columnMappingOptions(fields, labels)
    val name = monarchString(labels.label, R.string.monarch_columnmapping_label)
    // A table as on the web; it scrolls sideways when the screen is narrower than its columns.
    LazyColumn(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .widthIn(min = tokens.TableWidth)
            .width(tokens.TableWidth)
            .semantics {
                contentDescription = name
                isTraversalGroup = true
            },
        state = state,
        contentPadding = contentPadding,
    ) {
        item(key = "summary") {
            ColumnMappingSummary(
                sources = sources,
                mapping = mapping,
                fields = fields,
                suggestions = computed,
                showIssues = showIssues,
                showBulkActions = showBulkActions,
                enabled = enabled,
                labels = labels,
                onMappingChange = onMappingChange,
            )
        }
        item(key = "headings") { ColumnMappingHeadings(labels) }
        itemsIndexed(rows, key = { index, row -> "$index:${row.source.name}" }) { _, row ->
            MonarchDivider(variant = MonarchDividerVariant.Subtle)
            ColumnMappingRowContent(
                row = row,
                options = options,
                enabled = enabled,
                labels = labels,
                onChoose = { onMappingChange(tokens.choose(mapping, row.source.name, it)) },
            )
        }
    }
}

@Composable
private fun columnMappingOptions(
    fields: List<MonarchColumnMappingField>,
    labels: MonarchColumnMappingLabels,
): List<MonarchSelectOption<String>> {
    val skip = monarchString(labels.skip, R.string.monarch_columnmapping_skip)
    val required = monarchString(labels.required, R.string.monarch_columnmapping_required)
    val requiredFormat = stringResource(R.string.monarch_columnmapping_required_option)
    return remember(fields, skip, required, requiredFormat) {
        listOf(MonarchSelectOption("", skip)) + fields.map { field ->
            val label = if (field.required) requiredFormat.format(field.label, required) else field.label
            MonarchSelectOption(field.id, label, description = field.description)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ColumnMappingSummary(
    sources: List<MonarchColumnMappingSource>,
    mapping: Map<String, String>,
    fields: List<MonarchColumnMappingField>,
    suggestions: Map<String, MonarchColumnMappingSuggestion>,
    showIssues: Boolean,
    showBulkActions: Boolean,
    enabled: Boolean,
    labels: MonarchColumnMappingLabels,
    onMappingChange: (Map<String, String>) -> Unit,
) {
    val tokens = MonarchColumnMappingTokens
    val colors = MonarchTheme.colors
    val mapped = tokens.mappedCount(sources, mapping, fields)
    val summary = labels.summary?.invoke(mapped, sources.size)
        ?: pluralStringResource(R.plurals.monarch_columnmapping_summary, sources.size, mapped, sources.size)
    val missing = if (showIssues) MonarchColumnMappingDefaults.issues(mapping, fields).missing else emptyList()
    val canSuggest = suggestions.isNotEmpty() && !tokens.sameAsSuggested(sources, mapping, suggestions)
    val anyMapped = sources.any { mapping[it.name].orEmpty().isNotEmpty() }
    Column(
        modifier = Modifier.fillMaxWidth().padding(tokens.Padding),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Text(
            text = summary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
            color = tokens.mutedColor(colors),
        )
        if (showBulkActions && (canSuggest || anyMapped)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                if (canSuggest) {
                    MonarchButton(
                        text = monarchString(labels.useSuggestions, R.string.monarch_columnmapping_use_suggestions),
                        onClick = { onMappingChange(MonarchColumnMappingDefaults.mapping(suggestions)) },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                        enabled = enabled,
                    )
                }
                if (anyMapped) {
                    MonarchButton(
                        text = monarchString(labels.skipAll, R.string.monarch_columnmapping_skip_all),
                        onClick = { onMappingChange(emptyMap()) },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                        enabled = enabled,
                    )
                }
            }
        }
        if (missing.isNotEmpty()) {
            val separator = stringResource(R.string.monarch_columnmapping_separator)
            val names = missing.joinToString(separator) { it.label }
            val message = labels.missing?.invoke(names) ?: stringResource(R.string.monarch_columnmapping_missing, names)
            val notice = tokens.notice(colors)
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .monarchSurface(notice, MonarchTheme.shapes.md)
                    .padding(tokens.NoticePadding)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        error(message)
                    },
                style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                color = notice.content,
            )
        }
    }
}

@Composable
private fun ColumnMappingRowContent(
    row: MonarchColumnMappingRow,
    options: List<MonarchSelectOption<String>>,
    enabled: Boolean,
    labels: MonarchColumnMappingLabels,
    onChoose: (String) -> Unit,
) {
    val tokens = MonarchColumnMappingTokens
    val colors = MonarchTheme.colors
    val header = row.source.name.ifBlank {
        labels.position?.invoke(row.index + 1) ?: stringResource(R.string.monarch_columnmapping_position, row.index + 1)
    }
    val separator = stringResource(R.string.monarch_columnmapping_separator)
    val samples = if (row.samples.isEmpty()) {
        monarchString(labels.noSamples, R.string.monarch_columnmapping_no_samples)
    } else {
        row.samples.joinToString(separator)
    }
    val field = row.field
    val duplicate = if (field != null && row.others.isNotEmpty()) {
        val others = row.others.joinToString(separator)
        labels.duplicate?.invoke(field.label, others)
            ?: stringResource(R.string.monarch_columnmapping_duplicate, field.label, others)
    } else {
        null
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(tokens.Padding),
        horizontalArrangement = Arrangement.spacedBy(tokens.ColumnGap),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = header,
            modifier = Modifier.width(tokens.SourceWidth).padding(top = tokens.CellTop),
            style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.headerColor(colors),
        )
        Text(
            text = samples,
            modifier = Modifier.width(tokens.SampleWidth).padding(top = tokens.CellTop),
            style = MonarchTheme.typography.caption,
            color = tokens.mutedColor(colors),
        )
        Column(modifier = Modifier.width(tokens.TargetWidth), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            MonarchSelect(
                options = options,
                selected = row.fieldId.takeIf { id -> options.any { it.value == id } } ?: "",
                onSelect = onChoose,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = labels.fieldFor?.invoke(header)
                    ?: stringResource(R.string.monarch_columnmapping_field_for, header),
                helperText = field?.description,
                errorText = duplicate,
                enabled = enabled,
            )
            row.suggestion?.let { suggestion ->
                val text = when (suggestion.confidence) {
                    MonarchColumnMappingConfidence.Exact ->
                        monarchString(labels.exactMatch, R.string.monarch_columnmapping_exact_match)
                    MonarchColumnMappingConfidence.Similar ->
                        monarchString(labels.suggested, R.string.monarch_columnmapping_suggested)
                }
                MonarchBadge(text = text, tone = tokens.badgeTone(suggestion.confidence))
            }
        }
    }
}

/** The column headings of the table; read as part of each row, so hidden from TalkBack. */
@Composable
private fun ColumnMappingHeadings(labels: MonarchColumnMappingLabels) {
    val tokens = MonarchColumnMappingTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    Row(
        modifier = Modifier.fillMaxWidth().padding(tokens.Padding).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(tokens.ColumnGap),
    ) {
        listOf(
            monarchString(labels.columnInFile, R.string.monarch_columnmapping_column_in_file) to tokens.SourceWidth,
            monarchString(labels.exampleValues, R.string.monarch_columnmapping_example_values) to tokens.SampleWidth,
            monarchString(labels.importAs, R.string.monarch_columnmapping_import_as) to tokens.TargetWidth,
        ).forEach { (heading, width) ->
            Text(
                text = heading,
                modifier = Modifier.width(width),
                style = MonarchTheme.typography.caption,
                color = muted,
            )
        }
    }
}

