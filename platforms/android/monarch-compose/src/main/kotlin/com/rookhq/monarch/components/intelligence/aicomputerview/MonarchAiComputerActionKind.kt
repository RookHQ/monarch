package com.rookhq.monarch.components.intelligence.aicomputerview

/** What kind of thing the agent did, read before each action in the log. */
public enum class MonarchAiComputerActionKind {
    /** A click or a tap. */
    Click,

    /** Typed text. Its detail is the text, shown only with `showTypedText`. */
    Type,

    /** A scroll. */
    Scroll,

    /** Opened an address. */
    Navigate,

    /** A key press. */
    Key,

    /** Waited for something. */
    Wait,

    /** Read the screen. */
    Read,

    /** Anything else. */
    Other,
}
