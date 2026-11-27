/// What a context divider marks in a thread.
public enum MonarchAiContextDividerKind: Sendable, Hashable, CaseIterable {
    /// A calendar day: "Today", "Yesterday" or a date.
    case day
    /// The point where the assistant's memory of the conversation starts again.
    case contextReset
    /// Earlier messages that were condensed to save context.
    case summarized
}
