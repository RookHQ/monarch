package dev.monarchui.components.technical.logviewer

/** The severity of a line in a `MonarchLogViewer`, lowest first. */
public enum class MonarchLogLevel {
    /** Detail for debugging. */
    Debug,

    /** Normal operation. The level of a line that gives none. */
    Info,

    /** Something unexpected that did not stop the work. */
    Warn,

    /** A failure. */
    Error,
}
