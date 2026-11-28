package dev.monarchui.components.workflow.statuspicker

import androidx.compose.runtime.Immutable

/**
 * One status a team uses, such as "In review" in the [MonarchStatusCategory.Started]
 * category. Two options are equal when their [id], [name] and [category] are.
 *
 * @property id the stable id the app stores.
 * @property name the visible name, already localised.
 * @property category the stage it belongs to, which sets its glyph, colour and menu group.
 */
@Immutable
public class MonarchStatusOption(
    public val id: String,
    public val name: String,
    public val category: MonarchStatusCategory,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchStatusOption && other.id == id && other.name == name && other.category == category

    override fun hashCode(): Int = (id.hashCode() * HASH + name.hashCode()) * HASH + category.hashCode()

    override fun toString(): String = "MonarchStatusOption(id=$id, name=$name, category=$category)"

    private companion object {
        const val HASH = 31
    }
}
