package dev.monarchui.components.intelligence.aiconversation

/** Why the model stopped writing. In the web's JSON the names are kebab-case, such as `content-filter`. */
public enum class MonarchAiFinishReason {
    /** It finished. */
    Stop,

    /** It hit the output limit, so the answer is cut short. */
    Length,

    /** A safety filter stopped it. */
    ContentFilter,

    /** The model declined. */
    Refusal,

    /** It is waiting for tool results. */
    ToolUse,

    /** Another reason. */
    Other,
}
