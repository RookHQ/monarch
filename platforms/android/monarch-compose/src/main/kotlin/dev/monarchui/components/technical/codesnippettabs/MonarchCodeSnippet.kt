package dev.monarchui.components.technical.codesnippettabs

import androidx.compose.runtime.Immutable

/**
 * One snippet of a `MonarchCodeSnippetTabs`: the same example in one
 * language or tool.
 *
 * @property label the tab's text, such as "Kotlin" or "cURL".
 * @property code the code, as plain text.
 * @property language the language for highlighting and the code block's spoken name, such as `kotlin`.
 * @property id the value that selects this snippet and that sets with the same sync key share. [label] when
 * `null`.
 * @property title a title over the code, such as a file name.
 */
@Immutable
public class MonarchCodeSnippet(
    public val label: String,
    public val code: String,
    public val language: String? = null,
    id: String? = null,
    public val title: String? = null,
) {
    /** The value that selects this snippet: the id given, or the label. */
    public val id: String = id ?: label

    override fun equals(other: Any?): Boolean = other is MonarchCodeSnippet &&
        other.id == id && other.label == label && other.code == code && other.language == language &&
        other.title == title

    override fun hashCode(): Int = listOf(id, label, code, language, title).hashCode()

    override fun toString(): String = "MonarchCodeSnippet(id=$id, label=$label, language=$language)"
}
