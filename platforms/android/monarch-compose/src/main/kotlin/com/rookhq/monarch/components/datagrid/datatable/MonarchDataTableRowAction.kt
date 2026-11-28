package com.rookhq.monarch.components.datagrid.datatable

import androidx.compose.runtime.Immutable

/**
 * An action on one row of a `MonarchDataTable`, offered in the row's actions
 * menu and as a TalkBack action of the row.
 *
 * @property title the action's name, already localised: "Archive".
 * @property id a stable id. The title by default.
 * @property danger draws the action in the danger text colour. Confirm destructive actions in [onClick].
 * @property enabled whether the action can be used now.
 * @property onClick runs the action.
 */
@Immutable
public class MonarchDataTableRowAction(
    public val title: String,
    public val id: String = title,
    public val danger: Boolean = false,
    public val enabled: Boolean = true,
    public val onClick: () -> Unit,
) {
    override fun toString(): String = "MonarchDataTableRowAction(id=$id, title=$title)"
}
