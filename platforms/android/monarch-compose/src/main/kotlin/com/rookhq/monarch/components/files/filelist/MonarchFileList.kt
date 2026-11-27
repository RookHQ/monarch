package com.rookhq.monarch.components.files.filelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkAction
import com.rookhq.monarch.components.datagrid.bulkactionbar.MonarchBulkActionBar
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.files.filecard.MonarchFileCard
import com.rookhq.monarch.components.files.filecard.MonarchFileCardLayout
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.internal.LabelledControlRow
import com.rookhq.monarch.internal.monarchString

/**
 * A list or a grid of files with optional selection, a place for each file's
 * actions, a bar of actions for the selected files and an empty state.
 *
 * Built by Monarch on a `LazyVerticalGrid` of `MonarchFileCard`s: one column
 * of rows for [MonarchFileListLayout.List], adaptive columns of cards for
 * [MonarchFileListLayout.Grid], whose narrowest column widens with the font
 * scale. With [onSelectedChange], each file gets a checkbox, "Select all
 * files" (a three-state `MonarchCheckbox` row) heads the list, and
 * `MonarchBulkActionBar` shows the count, [bulkActions] and Clear selection
 * while any file is selected. Escape on a hardware keyboard clears the
 * selection. Without files, `MonarchEmptyState` (or [empty]) is shown. Give
 * the list a bounded height, as for any lazy list.
 *
 * @param items the files, in display order.
 * @param modifier the modifier of the list.
 * @param layout rows or a grid of cards.
 * @param selected the ids of the selected files. Shown only with [onSelectedChange].
 * @param onSelectedChange makes the files selectable; receives the new set of ids.
 * @param onOpen opens a file, for example in a preview. Makes each file a button.
 * @param label the TalkBack name of the files, such as "Project files".
 * @param bulkActions actions for the selection (Download, Move, Delete), shown while any file is selected.
 * @param state the scroll state of the files.
 * @param labels replacements for the built-in strings, including those of the cards and the bar.
 * @param empty replaces the built-in empty state when there are no files.
 * @param actions each file's actions, such as an `IconButton` that opens a `DropdownMenu`. Name an icon-only
 * trigger after the file.
 */
@Composable
public fun MonarchFileList(
    items: List<MonarchFileListItem>,
    modifier: Modifier = Modifier,
    layout: MonarchFileListLayout = MonarchFileListLayout.List,
    selected: Set<String> = emptySet(),
    onSelectedChange: ((Set<String>) -> Unit)? = null,
    onOpen: ((MonarchFileListItem) -> Unit)? = null,
    label: String? = null,
    bulkActions: List<MonarchBulkAction> = emptyList(),
    state: LazyGridState = rememberLazyGridState(),
    labels: MonarchFileListLabels = MonarchFileListLabels(),
    empty: (@Composable () -> Unit)? = null,
    actions: (@Composable (MonarchFileListItem) -> Unit)? = null,
) {
    val tokens = MonarchFileListTokens
    if (items.isEmpty()) {
        Column(modifier = modifier) {
            if (empty != null) {
                empty()
            } else {
                MonarchEmptyState(
                    title = monarchString(labels.empty, R.string.monarch_filelist_empty),
                    modifier = Modifier.fillMaxWidth(),
                    size = MonarchEmptyStateSize.Sm,
                )
            }
        }
        return
    }
    val count = tokens.count(items, selected)
    val clear: () -> Unit = { onSelectedChange?.invoke(emptySet()) }
    Column(
        modifier = modifier.onPreviewKeyEvent { event ->
            val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
            if (escape && onSelectedChange != null && count > 0) {
                clear()
                true
            } else {
                false
            }
        },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        if (onSelectedChange != null) {
            FileListSelectAll(items, selected, onSelectedChange, labels)
            MonarchBulkActionBar(count = count, onClear = clear, actions = bulkActions, labels = labels.bulkActionBar)
        }
        val grid = layout == MonarchFileListLayout.Grid
        val fontScale = LocalDensity.current.fontScale
        LazyVerticalGrid(
            columns = if (grid) GridCells.Adaptive(tokens.gridMinCell(fontScale)) else GridCells.Fixed(1),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
            state = state,
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            items(items, key = { it.id }) { item ->
                MonarchFileCard(
                    name = item.name,
                    sizeBytes = item.sizeBytes,
                    contentType = item.contentType,
                    meta = item.meta,
                    image = item.image,
                    badges = item.badges,
                    layout = if (grid) MonarchFileCardLayout.Card else MonarchFileCardLayout.Row,
                    selected = item.id in selected,
                    onSelectedChange = onSelectedChange?.let { change ->
                        { on: Boolean -> change(tokens.toggle(selected, item.id, on)) }
                    },
                    enabled = item.enabled,
                    onOpen = onOpen?.let { open -> { open(item) } },
                    labels = labels.card,
                    actions = actions?.let { slot -> { slot(item) } },
                )
            }
        }
    }
}

/** "Select all files": one row and one TalkBack element with a checked, mixed or unchecked state. */
@Composable
private fun FileListSelectAll(
    items: List<MonarchFileListItem>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    labels: MonarchFileListLabels,
) {
    val tokens = MonarchFileListTokens
    val state = tokens.selectAllState(items, selected)
    val enabled = tokens.selectable(items).isNotEmpty()
    LabelledControlRow(
        label = monarchString(labels.selectAll, R.string.monarch_filelist_select_all),
        description = null,
        enabled = enabled,
        controlAtEnd = false,
        modifier = Modifier
            .fillMaxWidth()
            .triStateToggleable(
                state = state,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = { onSelectedChange(tokens.toggleAll(items, selected)) },
            ),
    ) {
        MonarchCheckbox(state = state, onClick = null, enabled = enabled)
    }
}
