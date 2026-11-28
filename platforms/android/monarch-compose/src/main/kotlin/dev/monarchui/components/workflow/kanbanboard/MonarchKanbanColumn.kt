package dev.monarchui.components.workflow.kanbanboard

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One column of a `MonarchKanbanBoard`.
 *
 * @property id the stable, unique id, passed back in moves.
 * @property title the plain-text name, shown in the tab and header and used in announcements.
 * @property items the cards, in display order.
 * @property wipLimit the work-in-progress limit. The header shows "count / limit" and marks the column at and over
 * it; with `enforceWipLimits` a full column refuses cards.
 * @property emptyText shown when the column has no cards. `null` uses the built-in "No cards".
 * @property icon a decorative icon or colour mark before the title.
 * @property actions buttons at the end of the header, such as Add card.
 * @property footer content under the cards, such as an Add card button.
 */
@Immutable
public class MonarchKanbanColumn<T>(
    public val id: String,
    public val title: String,
    public val items: List<T>,
    public val wipLimit: Int? = null,
    public val emptyText: String? = null,
    public val icon: (@Composable () -> Unit)? = null,
    public val actions: (@Composable RowScope.() -> Unit)? = null,
    public val footer: (@Composable () -> Unit)? = null,
) {
    /** A copy of this column with [items] in place of its cards. */
    public fun withItems(items: List<T>): MonarchKanbanColumn<T> =
        MonarchKanbanColumn(id, title, items, wipLimit, emptyText, icon, actions, footer)
}
