/// The groups of ``MonarchAiConversationList``, in display order.
public enum MonarchAiConversationGroup: String, Sendable, Hashable, CaseIterable {
    /// Pinned conversations, whatever their age.
    case pinned
    /// Changed today.
    case today
    /// Changed in the 7 days before today.
    case previous7Days
    /// Changed earlier.
    case older
}
