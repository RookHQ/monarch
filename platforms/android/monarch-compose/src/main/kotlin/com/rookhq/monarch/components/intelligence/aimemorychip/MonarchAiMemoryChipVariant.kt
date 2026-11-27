package com.rookhq.monarch.components.intelligence.aimemorychip

/** How a `MonarchAiMemoryChip` is drawn. */
public enum class MonarchAiMemoryChipVariant {
    /** An inline pill in the AI colours with the prefix, for use inside a thread. */
    Chip,

    /** A full-width row without the prefix, for lists of memories such as a memory settings screen. */
    Row,
}
