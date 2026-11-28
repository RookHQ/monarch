package dev.monarchui.components.datagrid.datagrid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * The list layout for phones and large text: a toolbar with select-all and Sort, then one record per row read
 * as one element. Tapping a record with editable cells opens its editor in a `MonarchBottomSheet`;
 * otherwise it calls `onRowOpen`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T, K : Any> MonarchDataGridRecords(
    setup: MonarchDataGridSetup<T, K>,
    state: LazyListState,
    loading: Boolean,
) {
    val editing = remember { mutableStateOf<K?>(null) }
    val selector = setup.selector
    val sortable = setup.arrangement.all.filter { it.sortable }
    val sortMenu = setup.sorting.onSortChange != null && sortable.isNotEmpty()
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = state) {
        if (selector != null || sortMenu) {
            item(key = "monarch-datagrid-toolbar") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MonarchDataGridTokens.RecordPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selector != null) MonarchDataGridSelectAll(selector, setup.texts)
                    if (sortMenu) MonarchDataGridSortMenu(sortable, setup.sorting, setup.texts)
                }
            }
        }
        if (loading) {
            item(key = "monarch-datagrid-loading") {
                MonarchProgress(modifier = Modifier.fillMaxWidth(), label = setup.texts.loading)
            }
        }
        items(setup.rows, key = { setup.rowKey(it) }) { row -> DataGridRecord(setup, row, editing) }
    }
    val key = editing.value
    val row = key?.let { open -> setup.rows.firstOrNull { setup.rowKey(it) == open } }
    if (row != null) DataGridEditor(setup, row) { editing.value = null }
}

/** One record: its checkbox, its name and a "Field value" line per other column, and its actions button. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T, K : Any> DataGridRecord(setup: MonarchDataGridSetup<T, K>, row: T, editing: MutableState<K?>) {
    val texts = setup.texts
    val colors = MonarchTheme.colors
    val name = setup.name(row)
    val selected = setup.selected(row)
    val actions = setup.behavior.rowActions?.invoke(row).orEmpty()
    val open = setup.behavior.onRowOpen
    val editable = setup.editable(row)
    val fields = if (setup.behavior.rowLabel != null) setup.arrangement.all else setup.arrangement.all.drop(1)
    val tap: (() -> Unit)? = when {
        editable -> { { editing.value = setup.rowKey(row) } }
        open != null -> { { open(row) } }
        else -> null
    }
    val extra = buildList {
        if (editable && open != null) {
            add(
                CustomAccessibilityAction(texts.open) {
                    open(row)
                    true
                },
            )
        }
        addAll(monarchDataGridCustomActions(actions))
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonarchDataGridTokens.rowColor(selected == true, colors))
                .padding(horizontal = MonarchDataGridTokens.RecordPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected != null) {
                MonarchCheckbox(
                    checked = selected,
                    onCheckedChange = { setup.select(row, it) },
                    modifier = Modifier.semantics { contentDescription = texts.selectRow(name) },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (tap != null) {
                            Modifier.monarchFocusRing(MonarchShapeDefaults.Row).clickable(
                                interactionSource = null,
                                indication = ripple(),
                                onClickLabel = if (editable) texts.editRecord else texts.open,
                                role = Role.Button,
                                onClick = tap,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .semantics(mergeDescendants = true) { if (extra.isNotEmpty()) customActions = extra }
                    .padding(vertical = MonarchDataGridTokens.RecordPadding),
                verticalArrangement = Arrangement.spacedBy(MonarchDataGridTokens.LineGap),
            ) {
                MonarchText(
                    text = name,
                    style = MonarchTextStyle.BodyStrong,
                    color = if (tap != null) MonarchDataGridTokens.openableText(colors) else colors.textDefault,
                )
                fields.forEach { column ->
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchDataGridTokens.LineGap)) {
                        MonarchText(
                            text = column.title,
                            style = MonarchTextStyle.Caption,
                            color = MonarchDataGridTokens.headerText(colors),
                        )
                        RecordValue(column.value(row), texts)
                    }
                }
            }
            if (actions.isNotEmpty()) MonarchDataGridRowActions(name, actions, setup.behavior.actionsIcon, texts)
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

@Composable
private fun RecordValue(text: String, texts: MonarchDataGridTexts) {
    if (text.isBlank()) {
        MonarchText(
            text = texts.emptyCell,
            modifier = Modifier.semantics { contentDescription = texts.noValue },
            color = MonarchDataGridTokens.headerText(MonarchTheme.colors),
        )
    } else {
        MonarchText(text = text)
    }
}

/**
 * The editor of one record: a `MonarchBottomSheet` with the record's name as its title, an editable
 * cell per editable column and the value of each other column, then Open details and Done.
 */
@Composable
private fun <T, K : Any> DataGridEditor(setup: MonarchDataGridSetup<T, K>, row: T, onClose: () -> Unit) {
    val texts = setup.texts
    val colors = MonarchTheme.colors
    val name = setup.name(row)
    val open = setup.behavior.onRowOpen
    MonarchBottomSheet(onDismissRequest = onClose, title = name) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MonarchDataGridTokens.SheetGap),
        ) {
            setup.arrangement.all.forEach { column ->
                Column(verticalArrangement = Arrangement.spacedBy(MonarchDataGridTokens.LineGap)) {
                    MonarchText(
                        text = column.title,
                        style = MonarchTextStyle.Caption,
                        color = MonarchDataGridTokens.headerText(colors),
                    )
                    val edit = column.edit
                    if (edit != null && edit.editable(row)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            edit.Cell(
                                row = row,
                                label = texts.editLabel(column.title, name),
                                display = column.value(row),
                                modifier = Modifier.fillMaxWidth(),
                                startEditing = false,
                                labels = texts.editableCell,
                            )
                        }
                    } else {
                        RecordValue(column.value(row), texts)
                    }
                }
            }
            if (open != null) {
                MonarchButton(
                    text = texts.open,
                    onClick = {
                        onClose()
                        open(row)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MonarchButton(
                text = texts.done,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                variant = MonarchButtonVariant.Primary,
            )
        }
    }
}
