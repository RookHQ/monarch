package com.rookhq.monarch.components.datagrid.datagrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRowAction
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableSort
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/** The enabled row actions as TalkBack actions. */
internal fun monarchDataGridCustomActions(actions: List<MonarchDataTableRowAction>): List<CustomAccessibilityAction> =
    actions.filter { it.enabled }.map { action ->
        CustomAccessibilityAction(action.title) {
            action.onClick()
            true
        }
    }

/** A row's actions button and the menu it opens. */
@Composable
internal fun MonarchDataGridRowActions(
    name: String,
    actions: List<MonarchDataTableRowAction>,
    actionsIcon: (@Composable () -> Unit)?,
    texts: MonarchDataGridTexts,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        MonarchDataGridActionsButton(texts.rowActions(name), enabled = actions.isNotEmpty(), actionsIcon) {
            open = true
        }
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                MonarchMenuItem(
                    text = action.title,
                    onClick = {
                        open = false
                        action.onClick()
                    },
                    enabled = action.enabled,
                    destructive = action.danger,
                )
            }
        }
    }
}

/** The actions button: the app's icon in a `MonarchIconButton`, or a small "Actions" button. */
@Composable
internal fun MonarchDataGridActionsButton(
    name: String,
    enabled: Boolean,
    actionsIcon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    MonarchIconButton(
        label = name,
        onClick = onClick,
        enabled = enabled,
        icon = actionsIcon ?: MonarchDefaultIcons.More,
    )
}

/** The select-all checkbox of the list layout, with its label and the selected count, as one control. */
@Composable
internal fun <K> MonarchDataGridSelectAll(selector: MonarchDataGridSelector<K>, texts: MonarchDataGridTexts) {
    val count = selector.selection.size
    Row(
        modifier = Modifier
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .triStateToggleable(state = selector.allState, role = Role.Checkbox) {
                selector.onSelectionChange(selector.toggledAll())
            }
            .padding(end = MonarchDataGridTokens.CellPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(MonarchDataGridTokens.LineGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchCheckbox(state = selector.allState, onClick = null, modifier = Modifier.monarchMinimumTouchTarget())
        MonarchText(text = texts.selectAll)
        if (count > 0) {
            MonarchText(
                text = texts.selectedCount?.invoke(count)
                    ?: pluralStringResource(R.plurals.monarch_datagrid_selected_count, count, count),
                style = MonarchTextStyle.Caption,
                color = MonarchDataGridTokens.headerText(MonarchTheme.colors),
            )
        }
    }
}

/** The list layout's sort button and its menu: each sortable column, then Clear sort. */
@Composable
internal fun <T> MonarchDataGridSortMenu(
    columns: List<MonarchDataGridColumn<T>>,
    sorting: MonarchDataGridSorting,
    texts: MonarchDataGridTexts,
) {
    val onSortChange = sorting.onSortChange ?: return
    var open by remember { mutableStateOf(false) }
    val sort = sorting.sort
    val sorted = columns.firstOrNull { it.id == sort?.columnId }
    val state = sorted?.let { "${it.title}, ${texts.sortState(sort?.direction)}" } ?: texts.notSorted
    Box {
        MonarchButton(
            text = texts.sort,
            onClick = { open = true },
            modifier = Modifier.semantics { stateDescription = state },
            size = MonarchButtonSize.Sm,
        )
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            columns.forEach { column ->
                val direction = sort?.direction?.takeIf { sort.columnId == column.id }
                MonarchMenuItem(
                    onClick = {
                        open = false
                        val again = sort?.takeIf { it.columnId == column.id }?.toggled()
                        onSortChange(again ?: MonarchDataTableSort(column.id))
                    },
                    modifier = Modifier.semantics {
                        if (direction != null) stateDescription = texts.sortState(direction)
                    },
                    trailingContent = if (direction != null) {
                        { Text(texts.mark(direction), modifier = Modifier.clearAndSetSemantics { }) }
                    } else {
                        null
                    },
                ) { Text(column.title) }
            }
            if (sorted != null) {
                MonarchMenuItem(
                    text = texts.clearSort,
                    onClick = {
                        open = false
                        onSortChange(null)
                    },
                )
            }
        }
    }
}
