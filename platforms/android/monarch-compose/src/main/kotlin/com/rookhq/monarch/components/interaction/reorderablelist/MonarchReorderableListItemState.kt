package com.rookhq.monarch.components.interaction.reorderablelist

import androidx.compose.runtime.Immutable

/**
 * What the content of one row of a `MonarchReorderableList` knows about it.
 *
 * @property index the row's position as shown, from zero, including a drag in progress.
 * @property dragged this row is being dragged.
 * @property enabled this row can be moved.
 */
@Immutable
public class MonarchReorderableListItemState(
    public val index: Int,
    public val dragged: Boolean,
    public val enabled: Boolean,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchReorderableListItemState &&
            other.index == index &&
            other.dragged == dragged &&
            other.enabled == enabled

    override fun hashCode(): Int = (index * HASH + dragged.hashCode()) * HASH + enabled.hashCode()

    override fun toString(): String =
        "MonarchReorderableListItemState(index=$index, dragged=$dragged, enabled=$enabled)"

    private companion object {
        const val HASH = 31
    }
}
