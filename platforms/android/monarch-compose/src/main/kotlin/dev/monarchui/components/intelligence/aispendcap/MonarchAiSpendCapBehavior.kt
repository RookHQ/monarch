package dev.monarchui.components.intelligence.aispendcap

/** What happens when AI spending reaches the limit. */
public enum class MonarchAiSpendCapBehavior {
    /** AI stops and asks; nothing more is spent. */
    Pause,

    /** AI keeps going and the person is told; spending can go over the limit. */
    Notify,
}
