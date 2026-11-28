package com.rookhq.monarch.components.intelligence.aimessageactions

/** The actions of a `MonarchAiMessageActions` row, passed to its icon slot. */
public enum class MonarchAiMessageAction {
    /** Copies the message text. Active while the copied state shows. */
    Copy,

    /** Asks for a new answer. */
    Regenerate,

    /** Edits the person's own message. */
    Edit,

    /** Thumbs up. Active while it is the rating. */
    Good,

    /** Thumbs down. Active while it is the rating. */
    Bad,

    /** Shares the message. */
    Share,

    /** Reads the message aloud. Active while speaking, when the button stops reading. */
    ReadAloud,

    /** Opens the menu of further actions. */
    More,
}
