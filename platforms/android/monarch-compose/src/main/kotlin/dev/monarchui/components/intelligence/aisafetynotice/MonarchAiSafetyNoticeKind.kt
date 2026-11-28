package dev.monarchui.components.intelligence.aisafetynotice

/** What a safety system did to a request or an answer. Each kind has its own tone, title and explanation. */
public enum class MonarchAiSafetyNoticeKind {
    /** The request was not answered. Warning tone: "This request wasn’t answered". */
    Blocked,

    /** The answer was changed. Info tone: "This response was adjusted". */
    Modified,

    /** Parts of the answer were removed. Info tone: "Some content was hidden". */
    Filtered,
}
