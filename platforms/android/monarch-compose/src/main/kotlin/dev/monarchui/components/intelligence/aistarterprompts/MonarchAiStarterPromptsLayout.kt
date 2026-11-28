package dev.monarchui.components.intelligence.aistarterprompts

/** How `MonarchAiStarterPrompts` arranges its starters. The same names exist on every platform. */
public enum class MonarchAiStarterPromptsLayout {
    /** Cards in as many columns as fit, at least 208dp wide each. One column at large font sizes. The default. */
    Grid,

    /** Full-width rows, one under the other. */
    List,
}
