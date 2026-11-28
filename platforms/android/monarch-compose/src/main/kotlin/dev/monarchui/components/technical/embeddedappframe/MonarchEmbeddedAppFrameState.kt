package dev.monarchui.components.technical.embeddedappframe

/** What a [MonarchEmbeddedAppFrame] shows. */
public enum class MonarchEmbeddedAppFrameState {
    /** The embedded content is loading: a spinner and "Loading…". */
    Loading,

    /** The content is shown. */
    Ready,

    /** The content did not load: a message with "Try again" and "Open the page". */
    Error,
}
