package com.rookhq.monarch.components.intelligence.aistarterprompts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One suggested way to start a conversation, shown by `MonarchAiStarterPrompts`.
 *
 * @property id a stable id, unique among the starters.
 * @property title the short visible label, for example "Summarise a document".
 * @property prompt the prompt the app sends or puts in the input when the starter is chosen. Shown under the
 * title.
 * @property icon a decorative icon before the title, for example a `MonarchIcon` with a `null` content
 * description. `null` for none.
 */
@Immutable
public class MonarchAiStarterPrompt(
    public val id: String,
    public val title: String,
    public val prompt: String,
    public val icon: (@Composable () -> Unit)? = null,
) {
    override fun equals(other: Any?): Boolean = other is MonarchAiStarterPrompt &&
        other.id == id && other.title == title && other.prompt == prompt && other.icon === icon

    override fun hashCode(): Int = (id.hashCode() * HASH + title.hashCode()) * HASH + prompt.hashCode()

    override fun toString(): String = "MonarchAiStarterPrompt(id=$id, title=$title)"

    private companion object {
        const val HASH = 31
    }
}
