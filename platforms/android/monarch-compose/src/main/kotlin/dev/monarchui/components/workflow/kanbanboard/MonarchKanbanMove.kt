package dev.monarchui.components.workflow.kanbanboard

import androidx.compose.runtime.Immutable

/**
 * A finished move in a `MonarchKanbanBoard`.
 *
 * @property item the card that moved.
 * @property itemKey the card's key.
 * @property fromColumnId the column it left.
 * @property fromIndex its index in that column before the move.
 * @property toColumnId the column it went to, which may be the same one.
 * @property toIndex its index in that column after the move.
 */
@Immutable
public data class MonarchKanbanMove<T>(
    public val item: T,
    public val itemKey: String,
    public val fromColumnId: String,
    public val fromIndex: Int,
    public val toColumnId: String,
    public val toIndex: Int,
)
