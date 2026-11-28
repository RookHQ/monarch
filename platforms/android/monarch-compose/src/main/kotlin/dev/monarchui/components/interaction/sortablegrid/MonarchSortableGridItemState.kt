package dev.monarchui.components.interaction.sortablegrid

import androidx.compose.runtime.Immutable

/**
 * What the content of one tile of a `MonarchSortableGrid` knows about it.
 *
 * @property index the tile's position as shown, from zero, including a drag in progress.
 * @property dragged this tile is being dragged.
 * @property enabled this tile can be moved.
 */
@Immutable
public class MonarchSortableGridItemState(
    public val index: Int,
    public val dragged: Boolean,
    public val enabled: Boolean,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchSortableGridItemState &&
            other.index == index &&
            other.dragged == dragged &&
            other.enabled == enabled

    override fun hashCode(): Int = (index * HASH + dragged.hashCode()) * HASH + enabled.hashCode()

    override fun toString(): String =
        "MonarchSortableGridItemState(index=$index, dragged=$dragged, enabled=$enabled)"

    private companion object {
        const val HASH = 31
    }
}
