package dev.monarchui.components.files.pagetree

import androidx.compose.runtime.Immutable

/**
 * A named group of pages in a `MonarchPageTree`, such as a section or a chapter.
 *
 * @property id a stable, unique id.
 * @property label the heading of the group.
 * @property pages the pages, in order.
 */
@Immutable
public class MonarchPageTreeGroup(
    public val id: String,
    public val label: String,
    public val pages: List<MonarchPageTreePage>,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchPageTreeGroup && other.id == id && other.label == label && other.pages == pages

    override fun hashCode(): Int = (id.hashCode() * HASH + label.hashCode()) * HASH + pages.hashCode()

    override fun toString(): String = "MonarchPageTreeGroup(id=$id, label=$label, pages=$pages)"

    private companion object {
        const val HASH = 31
    }
}
