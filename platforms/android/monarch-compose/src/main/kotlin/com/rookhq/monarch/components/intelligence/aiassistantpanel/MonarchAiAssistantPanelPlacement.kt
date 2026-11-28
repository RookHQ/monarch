package com.rookhq.monarch.components.intelligence.aiassistantpanel

/** Where a `MonarchAiAssistantPanel` is shown. */
public enum class MonarchAiAssistantPanelPlacement {
    /** A full-height bottom sheet on windows narrower than 640dp, a pane in the layout on wider ones. */
    Auto,

    /** Always a pane where the app places it, such as a supporting pane. Give it a bounded height. */
    Inline,

    /** Always a full-height Material 3 modal bottom sheet. */
    Sheet,
}
