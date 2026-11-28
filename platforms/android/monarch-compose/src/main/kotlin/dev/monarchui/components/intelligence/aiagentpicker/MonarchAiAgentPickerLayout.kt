package dev.monarchui.components.intelligence.aiagentpicker

/** How a `MonarchAiAgentPicker` lays out its agents. */
public enum class MonarchAiAgentPickerLayout {
    /** One agent per row. */
    List,

    /** Two columns on a window at least 640dp wide; one column on narrower windows and at large font scales. */
    Grid,
}
