package com.rookhq.monarch.components.intelligence.aisourcelist

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourcePart

/**
 * A source as `MonarchAiSourceList` shows it: the source part of a reply,
 * whether the answer relies on it, and a short excerpt.
 *
 * @property source the source part of the reply, from the conversation model.
 * @property used whether the answer relies on the source. `false` lists it under "Considered".
 * @property snippet a short excerpt shown under the title, up to two lines.
 */
@Immutable
public data class MonarchAiSourceListItem(
    public val source: MonarchAiSourcePart,
    public val used: Boolean = true,
    public val snippet: String? = null,
) {
    /** The id of the source. */
    public val id: String get() = source.id

    /** Builds items from the conversation model. */
    public companion object {
        /**
         * The sources of a message, in the order they first appear, each once.
         * This is the numbering a reply's citations use: the first is `[1]`.
         * Every item is used and has no snippet.
         */
        public fun sources(message: MonarchAiConversationMessage): List<MonarchAiSourceListItem> =
            message.parts.filterIsInstance<MonarchAiSourcePart>()
                .distinctBy { it.id }
                .map { MonarchAiSourceListItem(it) }
    }
}
