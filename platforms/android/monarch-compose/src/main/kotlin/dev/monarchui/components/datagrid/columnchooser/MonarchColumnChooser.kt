package dev.monarchui.components.datagrid.columnchooser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableList
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListLabels
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A panel to show, hide, find and reorder the columns of a table, for a
 * sheet, a settings screen or a saved-view editor. It takes the same hidden
 * set and order as `MonarchDataTable`.
 *
 * Each column is a `MonarchSwitchRow`, on while the column is shown. Columns
 * that cannot be hidden and the last [minimumVisible] shown columns are
 * disabled switches. With [onOrderChange] the rows are a
 * `MonarchReorderableList`: a long press drags a row, and each row's handle
 * offers Move up and Move down to TalkBack, Switch Access and the arrow keys,
 * every move announced. From [searchThreshold] columns a search field filters
 * the rows; while it filters, reordering is off.
 *
 * The rows are lazy: inside a vertically scrolling parent give the chooser a
 * bounded height, such as `Modifier.heightIn(max = …)`.
 *
 * @param columns the columns in their default order.
 * @param hiddenColumns the ids of the hidden columns.
 * @param onHiddenColumnsChange called with the whole new hidden set when a switch or Show all is used.
 * @param modifier the modifier of the panel.
 * @param order column ids in display order; ids missing from it follow in their default order.
 * @param onOrderChange called with the whole new order after each move. Without it the rows do not move.
 * @param searchThreshold offers the search field from this many columns. `Int.MAX_VALUE` never offers it.
 * @param minimumVisible the fewest columns that must stay shown; the last ones cannot be switched off.
 * @param onReset adds a Reset button that runs this, to restore the app's default columns.
 * @param searchIcon an icon at the start of the search field, for example a magnifier. Monarch bundles no icons.
 * @param clearIcon the icon of the search field's clear button; the button is shown only with it.
 * @param chevrons lists the columns with a check box and move up and move down buttons, as on the web, instead of
 * switches with a drag handle. Needs [onOrderChange].
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("CyclomaticComplexMethod")
@Composable
public fun MonarchColumnChooser(
    columns: List<MonarchColumnChooserColumn>,
    hiddenColumns: Set<String>,
    onHiddenColumnsChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    order: List<String> = emptyList(),
    onOrderChange: ((List<String>) -> Unit)? = null,
    searchThreshold: Int = DefaultSearchThreshold,
    minimumVisible: Int = 1,
    onReset: (() -> Unit)? = null,
    searchIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Search,
    clearIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchColumnChooserLabels = MonarchColumnChooserLabels(),
    chevrons: Boolean = false,
) {
    val tokens = MonarchColumnChooserTokens
    val ordered = tokens.ordered(columns, order)
    val shownCount = columns.count { tokens.isShown(it, hiddenColumns) }
    var query by rememberSaveable { mutableStateOf("") }
    val searchable = columns.size >= searchThreshold
    val filtering = searchable && query.isNotBlank()
    val listed = if (filtering) tokens.matching(ordered, query) else ordered
    val required = monarchString(labels.required, R.string.monarch_columnchooser_required)
    val row: @Composable (MonarchColumnChooserColumn, Modifier) -> Unit = { column, rowModifier ->
        if (chevrons) {
            MonarchCheckboxRow(
                checked = tokens.isShown(column, hiddenColumns),
                onCheckedChange = { on -> onHiddenColumnsChange(tokens.toggled(hiddenColumns, column, on)) },
                label = column.label,
                modifier = rowModifier,
                enabled = tokens.canToggle(column, hiddenColumns, shownCount, minimumVisible),
                description = if (column.hideable) null else required,
            )
        } else {
            MonarchSwitchRow(
                checked = tokens.isShown(column, hiddenColumns),
                onCheckedChange = { on -> onHiddenColumnsChange(tokens.toggled(hiddenColumns, column, on)) },
                label = column.label,
                modifier = rowModifier,
                enabled = tokens.canToggle(column, hiddenColumns, shownCount, minimumVisible),
                description = if (column.hideable) null else required,
            )
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
        Header(shownCount, columns.size, labels)
        if (searchable) {
            val search = monarchString(labels.search, R.string.monarch_columnchooser_search)
            MonarchSearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = search,
                leadingIcon = searchIcon,
                clearIcon = clearIcon,
                clearLabel = monarchString(labels.clearSearch, R.string.monarch_columnchooser_clear_search),
            )
        }
        val listModifier = Modifier.fillMaxWidth().weight(1f, fill = false)
        when {
            listed.isEmpty() -> MonarchText(
                text = monarchString(labels.noMatches, R.string.monarch_columnchooser_no_matches),
                color = MonarchTheme.colors.textMuted,
            )
            chevrons && onOrderChange != null && !filtering -> Column(modifier = listModifier) {
                val ids = ordered.map { it.id }
                ordered.forEachIndexed { index, column ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        row(column, Modifier.weight(1f))
                        MonarchIconButton(
                            label = stringResource(R.string.monarch_columnchooser_move_up, column.label),
                            onClick = { onOrderChange(tokens.moved(ids, index, index - 1)) },
                            enabled = index > 0,
                            size = MonarchButtonSize.Sm,
                        ) { MonarchDefaultIcons.ChevronUp() }
                        MonarchIconButton(
                            label = stringResource(R.string.monarch_columnchooser_move_down, column.label),
                            onClick = { onOrderChange(tokens.moved(ids, index, index + 1)) },
                            enabled = index < ordered.lastIndex,
                            size = MonarchButtonSize.Sm,
                        ) { MonarchDefaultIcons.ChevronDown() }
                    }
                    MonarchDivider(variant = MonarchDividerVariant.Subtle)
                }
            }
            onOrderChange != null && !filtering -> MonarchReorderableList(
                items = ordered,
                key = { it.id },
                itemLabel = { it.label },
                onReorder = { from, to ->
                    onOrderChange(tokens.moved(ordered.map { it.id }, from, to))
                },
                modifier = listModifier,
                labels = MonarchReorderableListLabels(handle = labels.handle, moved = labels.moved),
            ) { column, _ -> row(column, Modifier.weight(1f)) }
            else -> LazyColumn(modifier = listModifier, verticalArrangement = Arrangement.spacedBy(tokens.RowGap)) {
                items(listed, key = { it.id }) { column -> row(column, Modifier.fillMaxWidth()) }
            }
        }
        if (shownCount < columns.size || onReset != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.FooterGap)) {
                if (shownCount < columns.size) {
                    MonarchButton(
                        text = monarchString(labels.showAll, R.string.monarch_columnchooser_show_all),
                        onClick = { onHiddenColumnsChange(emptySet()) },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
                if (onReset != null) {
                    MonarchButton(
                        text = monarchString(labels.reset, R.string.monarch_columnchooser_reset),
                        onClick = onReset,
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
    }
}

/** From this many columns the chooser offers its search field. */
private const val DefaultSearchThreshold = 8

/** The title and the count, read together as one heading. */
@Composable
private fun Header(shown: Int, total: Int, labels: MonarchColumnChooserLabels) {
    val count = labels.shownCount?.invoke(shown, total)
        ?: pluralStringResource(R.plurals.monarch_columnchooser_shown_count, shown, shown, total)
    Column(modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
        MonarchText(
            text = monarchString(labels.title, R.string.monarch_columnchooser_title),
            style = MonarchTextStyle.Subheading,
            heading = false,
        )
        MonarchText(
            text = count,
            style = MonarchTextStyle.Caption,
            color = MonarchColumnChooserTokens.countColor(MonarchTheme.colors),
        )
    }
}
