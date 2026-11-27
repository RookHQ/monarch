package com.rookhq.monarch.components.intelligence.aimemorypanel

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryAction
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryPart

/**
 * One remembered fact as `MonarchAiMemoryPanel` lists it: always about an
 * entity, with the time it was saved or last changed.
 *
 * @property id the memory's stable id, the same as the conversation's `MonarchAiMemoryPart.id`.
 * @property text what is remembered, in plain text.
 * @property entity who or what it is about.
 * @property updatedAt when it was saved or last changed, in epoch milliseconds (UTC).
 */
public data class MonarchAiMemoryItem(
    public val id: String,
    public val text: String,
    public val entity: MonarchAiMemoryEntity,
    public val updatedAt: Long,
) {
    /** Keeps a memory list in step with the memory events a conversation reports. */
    public companion object {
        /**
         * Applies one memory event from a reply to [memories]: `Write` updates
         * the text and time of the memory with that id or adds it at the end,
         * `Forget` removes it, `Read` changes nothing. Returns the same list
         * when nothing changed, so it is safe as a state update.
         *
         * @param memories the current list.
         * @param event the memory event, from `MonarchAiConversation.onMemoryEvent`.
         * @param now the time a written memory gets, in epoch milliseconds.
         * @param defaultEntityName the group of a new memory without a scope.
         * @param entity who or what a new memory is about. By default an `Other` entity named by the event's scope.
         */
        public fun apply(
            memories: List<MonarchAiMemoryItem>,
            event: MonarchAiMemoryPart,
            now: Long,
            defaultEntityName: String = "General",
            entity: ((MonarchAiMemoryPart) -> MonarchAiMemoryEntity)? = null,
        ): List<MonarchAiMemoryItem> {
            val at = memories.indexOfFirst { it.id == event.id }
            return when (event.action) {
                MonarchAiMemoryAction.Read -> memories
                MonarchAiMemoryAction.Forget -> if (at < 0) memories else memories.filterIndexed { i, _ -> i != at }
                MonarchAiMemoryAction.Write -> when {
                    at >= 0 && memories[at].text == event.text -> memories
                    at >= 0 -> memories.mapIndexed { i, memory ->
                        if (i == at) memory.copy(text = event.text, updatedAt = now) else memory
                    }
                    else -> {
                        val about = entity?.invoke(event)
                            ?: MonarchAiMemoryEntity(MonarchAiMemoryEntityKind.Other, event.scope ?: defaultEntityName)
                        memories + MonarchAiMemoryItem(event.id, event.text, about, now)
                    }
                }
            }
        }
    }
}
