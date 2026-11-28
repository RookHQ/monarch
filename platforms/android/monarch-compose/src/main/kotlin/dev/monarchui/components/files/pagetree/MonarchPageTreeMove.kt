package dev.monarchui.components.files.pagetree

import androidx.compose.runtime.Immutable

/**
 * What changed in a reorder of a `MonarchPageTree`.
 *
 * @property kind whether a page or a group moved.
 * @property id the id of the page or the group.
 * @property fromGroupId for a page: the group it left. `null` for a group.
 * @property toGroupId for a page: the group it joined (the same group for a move within it). `null` for a group.
 * @property toIndex the new index, from 0: within its group for a page, among the groups for a group.
 */
@Immutable
public class MonarchPageTreeMove(
    public val kind: MonarchPageTreeMoveKind,
    public val id: String,
    public val fromGroupId: String?,
    public val toGroupId: String?,
    public val toIndex: Int,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchPageTreeMove &&
            other.kind == kind &&
            other.id == id &&
            other.fromGroupId == fromGroupId &&
            other.toGroupId == toGroupId &&
            other.toIndex == toIndex

    override fun hashCode(): Int =
        (((kind.hashCode() * HASH + id.hashCode()) * HASH + fromGroupId.hashCode()) * HASH + toGroupId.hashCode()) *
            HASH + toIndex

    override fun toString(): String =
        "MonarchPageTreeMove($kind, id=$id, from=$fromGroupId, to=$toGroupId, toIndex=$toIndex)"

    private companion object {
        const val HASH = 31
    }
}
