package com.rookhq.monarch.components.collaboration.threadpanel

/** What the body of a `MonarchThreadPanel` shows. */
public enum class MonarchThreadPanelStatus {
    /** The root message, the reply count and the replies. */
    Ready,

    /** The thread is loading: placeholder rows. */
    Loading,

    /** The thread could not be loaded: an error, with Try again when the panel has `onRetry`. */
    Error,

    /** The thread was deleted: a notice, with Close when the panel has `onClose`. */
    Deleted,
}
