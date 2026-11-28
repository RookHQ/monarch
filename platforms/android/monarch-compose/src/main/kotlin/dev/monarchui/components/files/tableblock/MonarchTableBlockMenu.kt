package dev.monarchui.components.files.tableblock

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuDivider
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString

/**
 * The "Table" button above an editable table and its `MonarchMenu`. Every
 * item acts on [target]; a change is passed to [onChange] with the cell that
 * should take focus afterwards.
 */
@Composable
internal fun MonarchTableBlockMenu(
    data: MonarchTableBlockData,
    target: MonarchTableBlockCell,
    enabled: Boolean,
    labels: MonarchTableBlockLabels,
    onChange: (MonarchTableBlockData, MonarchTableBlockCell?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val act = { next: MonarchTableBlockData, focus: MonarchTableBlockCell? ->
        expanded = false
        onChange(next, focus)
    }
    Box {
        MonarchButton(
            text = monarchString(labels.controls, R.string.monarch_tableblock_controls),
            onClick = { expanded = true },
            variant = MonarchButtonVariant.Secondary,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            leadingIcon = MonarchDefaultIcons.Table,
        )
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            MonarchMenuItem(
                text = monarchString(labels.addRow, R.string.monarch_tableblock_add_row),
                onClick = {
                    act(data.addRow(after = target.row), MonarchTableBlockCell(target.row + 1, target.column))
                },
                enabled = data.canAddRow,
            )
            MonarchMenuItem(
                text = monarchString(labels.addColumn, R.string.monarch_tableblock_add_column),
                onClick = {
                    act(data.addColumn(after = target.column), MonarchTableBlockCell(target.row, target.column + 1))
                },
                enabled = data.canAddColumn,
            )
            MonarchMenuItem(
                text = monarchString(labels.deleteRow, R.string.monarch_tableblock_delete_row),
                onClick = {
                    val next = data.deleteRow(at = target.row)
                    act(next, MonarchTableBlockTokens.target(target, next))
                },
                enabled = data.canDeleteRow,
            )
            MonarchMenuItem(
                text = monarchString(labels.deleteColumn, R.string.monarch_tableblock_delete_column),
                onClick = {
                    val next = data.deleteColumn(at = target.column)
                    act(next, MonarchTableBlockTokens.target(target, next))
                },
                enabled = data.canDeleteColumn,
            )
            MonarchMenuItem(
                onClick = { act(data.withHeaderRow(!data.hasHeaderRow), null) },
                modifier = Modifier.semantics {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(data.hasHeaderRow)
                },
                // The item carries the state; the check box only shows it.
                trailingContent = { MonarchCheckbox(checked = data.hasHeaderRow, onCheckedChange = null) },
            ) { Text(monarchString(labels.headerRow, R.string.monarch_tableblock_header_row)) }
            if (onDelete != null) {
                MonarchMenuDivider()
                MonarchMenuItem(
                    text = monarchString(labels.deleteTable, R.string.monarch_tableblock_delete_table),
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                    destructive = true,
                )
            }
        }
    }
}
