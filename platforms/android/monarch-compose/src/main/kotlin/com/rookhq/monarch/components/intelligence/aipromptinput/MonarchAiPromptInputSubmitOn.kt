package com.rookhq.monarch.components.intelligence.aipromptinput

/**
 * Which hardware key sends the prompt of a `MonarchAiPromptInput`. The
 * on-screen keyboard's Enter always adds a line; its send button is the
 * component's Send button.
 */
public enum class MonarchAiPromptInputSubmitOn {
    /** Enter sends and Shift+Enter adds a line. Ctrl+Enter also sends. The default, as on the web. */
    Enter,

    /** Ctrl+Enter (or Meta+Enter) sends and Enter adds a line, for long prompts. */
    CtrlEnter,
}
