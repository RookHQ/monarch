import Foundation

/// Replacements for the built-in strings of ``MonarchAiAgentRunTimeline``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiAgentRunTimelineLabels: Sendable {
    /// The heading and spoken name of the trace. Built in: "Agent run".
    public var label: String?
    /// The spoken name of the view switch. Built in: "View".
    public var modeLabel: String?
    /// The name of a view. Built in: "Timeline", "Table".
    public var mode: (@Sendable (MonarchAiAgentRunTimelineMode) -> String)?
    /// The name of a kind. Built in: "Model", "Tool", "Hand-off", "Error".
    public var kind: (@Sendable (MonarchAiRunEventKind) -> String)?
    /// The spoken name of the event list. Built in: "Run events".
    public var events: String?
    /// The total time. Built in: "Total time".
    public var totalDuration: String?
    /// Input tokens. Built in: "Input tokens".
    public var inputTokens: String?
    /// Output tokens. Built in: "Output tokens".
    public var outputTokens: String?
    /// Cost. Built in: "Cost".
    public var cost: String?
    /// The offset from the start of the run. Built in: "Start".
    public var start: String?
    /// An event's duration. Built in: "Duration".
    public var duration: String?
    /// The table column of the event name. Built in: "Event".
    public var eventColumn: String?
    /// The table column of the kind. Built in: "Kind".
    public var kindColumn: String?
    /// Shown instead of a duration while an event runs. Built in: "Running".
    public var running: String?
    /// Shown when there are no events. Built in: "No events yet".
    public var empty: String?
    /// Shown for a value that is not known. Built in: "–".
    public var notAvailable: String?
    /// The button in an open event that shows its conversation part. Built in: "Show in conversation".
    public var openPart: String?
    /// Formats a duration in seconds. Built in: "850 ms", "4.2 s", "2 min 5 s".
    public var formatDuration: (@Sendable (TimeInterval) -> String)?
    /// Formats an offset from the start of the run in seconds. Built in: "+1.2 s".
    public var formatOffset: (@Sendable (TimeInterval) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: The heading and spoken name of the trace.
    ///   - modeLabel: The spoken name of the view switch.
    ///   - mode: The name of a view.
    ///   - kind: The name of a kind.
    ///   - events: The spoken name of the event list.
    ///   - totalDuration: The total time.
    ///   - inputTokens: Input tokens.
    ///   - outputTokens: Output tokens.
    ///   - cost: Cost.
    ///   - start: The offset from the start of the run.
    ///   - duration: An event's duration.
    ///   - eventColumn: The table column of the event name.
    ///   - kindColumn: The table column of the kind.
    ///   - running: Shown instead of a duration while an event runs.
    ///   - empty: Shown when there are no events.
    ///   - notAvailable: Shown for a value that is not known.
    ///   - openPart: The button that shows an event's conversation part.
    ///   - formatDuration: Formats a duration in seconds.
    ///   - formatOffset: Formats an offset from the start of the run in seconds.
    public init(
        label: String? = nil,
        modeLabel: String? = nil,
        mode: (@Sendable (MonarchAiAgentRunTimelineMode) -> String)? = nil,
        kind: (@Sendable (MonarchAiRunEventKind) -> String)? = nil,
        events: String? = nil,
        totalDuration: String? = nil,
        inputTokens: String? = nil,
        outputTokens: String? = nil,
        cost: String? = nil,
        start: String? = nil,
        duration: String? = nil,
        eventColumn: String? = nil,
        kindColumn: String? = nil,
        running: String? = nil,
        empty: String? = nil,
        notAvailable: String? = nil,
        openPart: String? = nil,
        formatDuration: (@Sendable (TimeInterval) -> String)? = nil,
        formatOffset: (@Sendable (TimeInterval) -> String)? = nil
    ) {
        self.label = label
        self.modeLabel = modeLabel
        self.mode = mode
        self.kind = kind
        self.events = events
        self.totalDuration = totalDuration
        self.inputTokens = inputTokens
        self.outputTokens = outputTokens
        self.cost = cost
        self.start = start
        self.duration = duration
        self.eventColumn = eventColumn
        self.kindColumn = kindColumn
        self.running = running
        self.empty = empty
        self.notAvailable = notAvailable
        self.openPart = openPart
        self.formatDuration = formatDuration
        self.formatOffset = formatOffset
    }
}
