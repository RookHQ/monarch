package dev.monarchui.components.files.pagetree

import androidx.compose.runtime.Immutable

/**
 * One page of a `MonarchPageTreeGroup`.
 *
 * @property id a stable, unique id: the value of the tree's selection.
 * @property title the title. A blank title shows "Untitled page".
 */
@Immutable
public class MonarchPageTreePage(public val id: String, public val title: String) {
    override fun equals(other: Any?): Boolean = other is MonarchPageTreePage && other.id == id && other.title == title

    override fun hashCode(): Int = id.hashCode() * HASH + title.hashCode()

    override fun toString(): String = "MonarchPageTreePage(id=$id, title=$title)"

    private companion object {
        const val HASH = 31
    }
}
