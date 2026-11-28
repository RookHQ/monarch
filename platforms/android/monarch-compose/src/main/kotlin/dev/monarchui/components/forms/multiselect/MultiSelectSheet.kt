package dev.monarchui.components.forms.multiselect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.combobox.ComboboxEntries
import dev.monarchui.components.forms.combobox.ComboboxEntry
import dev.monarchui.components.forms.combobox.ComboboxMessages
import dev.monarchui.components.forms.combobox.ComboboxResults
import dev.monarchui.components.forms.combobox.MonarchComboboxLabels
import dev.monarchui.internal.MonarchSearchListSheet
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/** The rows of the list for [results]: options under headings, Create, and a message. No clear row. */
@Composable
internal fun multiSelectEntries(
    results: ComboboxResults,
    query: String,
    loading: Boolean,
    labels: MonarchComboboxLabels,
): List<ComboboxEntry> {
    val locale = LocalConfiguration.current.locales[0]
    val create = stringResource(R.string.monarch_combobox_create)
    val more = stringResource(R.string.monarch_combobox_more)
    val messages = ComboboxMessages(
        loading = monarchString(labels.loading, R.string.monarch_combobox_loading),
        empty = monarchString(labels.empty, R.string.monarch_combobox_empty),
        create = labels.create ?: { String.format(locale, create, it) },
        more = labels.more ?: { shown, total -> String.format(locale, more, shown, total) },
    )
    return ComboboxEntries.build(results, query, clearOptionLabel = null, loading = loading, messages = messages)
}

/** The bottom sheet of a multi-select: title, count, search, the checklist and Done. */
@Composable
@Suppress("LongParameterList")
internal fun MultiSelectSheet(
    title: String,
    query: String,
    loading: Boolean,
    entries: List<ComboboxEntry>,
    selected: List<String>,
    atLimit: Boolean,
    count: String?,
    texts: MultiSelectSheetTexts,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDone: () -> Unit,
) {
    val tokens = MonarchMultiSelectTokens
    val colors = MonarchTheme.colors
    MonarchSearchListSheet(
        title = title,
        query = query,
        onQueryChange = onQueryChange,
        onDismiss = onDone,
        searchPlaceholder = texts.search,
        searching = loading,
        containerColor = tokens.sheetColor(colors),
        gap = tokens.Gap,
        status = count?.let {
            {
                Text(
                    text = it,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MonarchTheme.typography.caption,
                    color = tokens.secondaryColor(true, colors),
                )
            }
        },
        buttons = {
            MonarchButton(
                text = texts.done,
                onClick = onDone,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = tokens.SheetPadding),
                variant = MonarchButtonVariant.Primary,
            )
        },
    ) {
        items(entries) { entry ->
            MultiSelectRow(entry, selected, atLimit, onToggle, onCreate)
        }
    }
}

/** One row of the checklist: an option with a check mark, Create, a heading or a message. */
@Composable
private fun MultiSelectRow(
    entry: ComboboxEntry,
    selected: List<String>,
    atLimit: Boolean,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
    val tokens = MonarchMultiSelectTokens
    val colors = MonarchTheme.colors
    when (entry) {
        is ComboboxEntry.Header -> Text(
            text = entry.text,
            modifier = Modifier
                .padding(vertical = tokens.RowVerticalPadding)
                .semantics { heading() },
            style = MonarchTheme.typography.caption,
            color = tokens.secondaryColor(true, colors),
        )
        is ComboboxEntry.Message -> Text(
            text = entry.text,
            modifier = Modifier
                .padding(vertical = tokens.RowVerticalPadding)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.caption,
            color = tokens.secondaryColor(true, colors),
        )
        is ComboboxEntry.Create -> Text(
            text = entry.label,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = tokens.RowMinHeight)
                .clickable(enabled = !atLimit, role = Role.Button) { onCreate(entry.text) }
                .padding(vertical = tokens.RowVerticalPadding),
            style = MonarchTheme.typography.body,
            color = tokens.labelColor(!atLimit, colors),
        )
        is ComboboxEntry.Option -> {
            val option = entry.option
            val chosen = option.id in selected
            val enabled = tokens.rowEnabled(option, chosen, atLimit, enabled = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.RowMinHeight)
                    .toggleable(value = chosen, enabled = enabled, role = Role.Checkbox) { onToggle(option.id) }
                    .padding(vertical = tokens.RowVerticalPadding),
                horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchCheckbox(
                    checked = chosen,
                    onCheckedChange = null,
                    modifier = Modifier.clearAndSetSemantics { },
                    enabled = enabled,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.label, style = MonarchTheme.typography.body, color = tokens.labelColor(enabled, colors))
                    option.description?.let {
                        Text(
                            it,
                            style = MonarchTheme.typography.caption,
                            color = tokens.secondaryColor(enabled, colors),
                        )
                    }
                }
                option.hint?.let {
                    Text(it, style = MonarchTheme.typography.caption, color = tokens.secondaryColor(enabled, colors))
                }
            }
        }
        is ComboboxEntry.Clear -> Unit
    }
}
