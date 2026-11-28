package dev.monarchui.components.workflow.kanbanboard

import androidx.compose.runtime.Immutable

/**
 * What the content of a `MonarchKanbanBoard` card knows about it.
 *
 * @property columnId the column the card is in.
 * @property index the card's position in that column, from zero.
 * @property dragging whether the person is dragging this card.
 * @property overlay whether this is the copy that follows the finger during a drag.
 * @property pending whether this card's move is waiting for `onMove` to return.
 * @property enabled whether the card can be moved.
 */
@Immutable
public class MonarchKanbanCardState(
    public val columnId: String,
    public val index: Int,
    public val dragging: Boolean,
    public val overlay: Boolean,
    public val pending: Boolean,
    public val enabled: Boolean,
)
