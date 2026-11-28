package com.rookhq.monarch.components.intelligence.aisharedsession

/** Whether a participant of a `MonarchAiSharedSession` is a person or an AI agent. */
public enum class MonarchAiSharedSessionKind {
    /** A person. */
    Person,

    /** An AI agent: the square agent picture, its own group in the list. */
    Agent,
}
