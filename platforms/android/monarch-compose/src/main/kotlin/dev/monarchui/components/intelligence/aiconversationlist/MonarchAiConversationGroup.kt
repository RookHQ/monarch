package dev.monarchui.components.intelligence.aiconversationlist

/** The sections of a `MonarchAiConversationList`, in the order they are shown. Same names as iOS. */
public enum class MonarchAiConversationGroup {
    /** Pinned conversations, whatever their date. */
    Pinned,

    /** Updated today, or dated in the future. */
    Today,

    /** Updated in the seven days before today. */
    Previous7Days,

    /** Updated earlier. */
    Older,
}
