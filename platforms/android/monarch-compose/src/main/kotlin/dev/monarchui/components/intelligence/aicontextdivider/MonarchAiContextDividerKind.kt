package dev.monarchui.components.intelligence.aicontextdivider

/** What a `MonarchAiContextDivider` marks in a thread. The same three names exist on every platform. */
public enum class MonarchAiContextDividerKind {
    /** A calendar day: "Today", "Yesterday", "Monday, 3 March". Solid subtle lines. */
    Day,

    /** Where the assistant's memory of the conversation starts again. Dashed lines and a description. */
    ContextReset,

    /** Earlier messages were condensed to save context. Lines and label in the AI colours. */
    Summarized,
}
