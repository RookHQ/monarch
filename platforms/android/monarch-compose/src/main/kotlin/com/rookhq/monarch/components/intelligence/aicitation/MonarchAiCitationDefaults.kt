package com.rookhq.monarch.components.intelligence.aicitation

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourcePart

/** Helpers for numbering the citations of a message. */
public object MonarchAiCitationDefaults {
    /**
     * The number of each source of a message, by source id, counting from 1 in the order the sources first
     * appear. A source repeated in the message keeps its first number.
     *
     * @param parts the parts of the message.
     */
    public fun sourceNumbers(parts: List<MonarchAiPart>): Map<String, Int> {
        val numbers = LinkedHashMap<String, Int>()
        parts.filterIsInstance<MonarchAiSourcePart>().forEach { source ->
            if (source.id !in numbers) numbers[source.id] = numbers.size + 1
        }
        return numbers
    }
}
