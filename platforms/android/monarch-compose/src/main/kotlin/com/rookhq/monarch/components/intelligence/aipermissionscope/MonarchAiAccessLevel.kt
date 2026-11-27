package com.rookhq.monarch.components.intelligence.aipermissionscope

/** How far an AI agent may go in one app. The same names exist on every platform, in this order. */
public enum class MonarchAiAccessLevel {
    /** No access. */
    None,

    /** The agent may read. */
    Read,

    /** The agent may read and edit. */
    Write,
    ;

    /** Whether this level allows [needed]: `Write` includes `Read`, and every level includes `None`. */
    public fun includes(needed: MonarchAiAccessLevel): Boolean = ordinal >= needed.ordinal
}
