package com.rookhq.monarch.components.intelligence.aisharedsession

/** What a person in a `MonarchAiSharedSession` may do. */
public enum class MonarchAiSharedSessionRole {
    /** Sends requests and manages the queue. */
    Owner,

    /** Sends requests. */
    Editor,

    /** Only watches. */
    Viewer,
}
