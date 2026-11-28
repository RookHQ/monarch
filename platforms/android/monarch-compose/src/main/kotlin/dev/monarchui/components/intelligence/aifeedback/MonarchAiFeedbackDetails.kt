package dev.monarchui.components.intelligence.aifeedback

/** Which ratings of a `MonarchAiFeedback` open the sheet with reasons and a comment. */
public enum class MonarchAiFeedbackDetails {
    /** Only a thumbs down asks for details; a thumbs up is sent at once. */
    Down,

    /** Both ratings ask for details. */
    Both,

    /** No rating asks for details; every rating is sent at once. */
    None,
}
