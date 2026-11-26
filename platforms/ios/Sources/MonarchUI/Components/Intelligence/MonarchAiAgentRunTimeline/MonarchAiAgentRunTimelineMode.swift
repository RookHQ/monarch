/// How ``MonarchAiAgentRunTimeline`` shows a run.
public enum MonarchAiAgentRunTimelineMode: String, Sendable, Hashable, CaseIterable {
    /// Bars on a shared time axis, with details that open per event.
    case timeline
    /// The same data in rows and columns.
    case table
}
