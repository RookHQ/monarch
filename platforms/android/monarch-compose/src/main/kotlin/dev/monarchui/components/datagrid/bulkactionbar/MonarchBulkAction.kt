package dev.monarchui.components.datagrid.bulkactionbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One action of a `MonarchBulkActionBar`, applied to every selected item.
 *
 * @property id identifies the action, for example for `busyActionId`.
 * @property title the visible label of the button or menu item.
 * @property destructive draws the action with the danger variant and the danger menu colour. Confirm it in
 * [onClick], for example with an `AlertDialog`.
 * @property enabled whether the action can be used now.
 * @property icon a decorative icon before the title, for example a `MonarchIcon` with a `null` content
 * description. The title is always shown.
 * @property onClick runs the action on the selection.
 */
@Immutable
public class MonarchBulkAction(
    public val id: String,
    public val title: String,
    public val destructive: Boolean = false,
    public val enabled: Boolean = true,
    public val icon: (@Composable () -> Unit)? = null,
    public val onClick: () -> Unit,
)
