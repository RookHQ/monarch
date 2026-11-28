/// How a registered component renders while its props are still streaming.
public enum MonarchAiUiStreaming: Sendable, Hashable, CaseIterable {
    /// Render what has arrived as soon as it decodes into the props type. The default.
    /// Make the props that arrive late optional so the component can show early.
    case progressive
    /// Show a progress indicator until the part is complete.
    case onComplete
}
