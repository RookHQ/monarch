package com.rookhq.monarch.components.collaboration.messagecomposer

/**
 * Which hardware key sends from a `MonarchMessageComposer`. The on-screen
 * keyboard's Enter always adds a line; the send button sends.
 */
public enum class MonarchMessageComposerSendShortcut {
    /** Enter sends and Shift+Enter adds a line. */
    Enter,

    /** Ctrl+Enter (or Meta+Enter) sends and Enter adds a line. */
    CtrlEnter,
}
