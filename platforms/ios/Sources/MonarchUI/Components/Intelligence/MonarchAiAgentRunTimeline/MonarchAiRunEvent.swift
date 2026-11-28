import Foundation

/// One event in the trace of an agent run: a model call, a tool call, a
/// hand-off or an error.
public struct MonarchAiRunEvent: Identifiable, Sendable, Hashable, Codable {
    /// A stable id.
    public var id: String
    /// What the event was.
    public var kind: MonarchAiRunEventKind
    /// What ran, such as a model or a tool name.
    public var name: String
    /// When it started.
    public var startedAt: Date
    /// When it ended. `nil` while it is still running.
    public var endedAt: Date?
    /// The input tokens it used.
    public var inputTokens: Int?
    /// The output tokens it produced.
    public var outputTokens: Int?
    /// What it cost, in millionths of the currency unit (1 000 000 is 1.00).
    public var costMicros: Int?
    /// Shown when the event is open: arguments, a result or an error message, already localised.
    public var detail: String?
    /// The conversation part the event produced or served, such as a tool call's id.
    public var partId: String?
    /// The conversation message the event belongs to.
    public var messageId: String?

    /// An event.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - kind: What the event was.
    ///   - name: What ran.
    ///   - startedAt: When it started.
    ///   - endedAt: When it ended; `nil` while running.
    ///   - inputTokens: The input tokens it used.
    ///   - outputTokens: The output tokens it produced.
    ///   - costMicros: What it cost, in millionths of the currency unit.
    ///   - detail: Shown when the event is open.
    ///   - partId: The conversation part the event produced or served.
    ///   - messageId: The conversation message the event belongs to.
    public init(
        id: String,
        kind: MonarchAiRunEventKind,
        name: String,
        startedAt: Date,
        endedAt: Date? = nil,
        inputTokens: Int? = nil,
        outputTokens: Int? = nil,
        costMicros: Int? = nil,
        detail: String? = nil,
        partId: String? = nil,
        messageId: String? = nil
    ) {
        self.id = id
        self.kind = kind
        self.name = name
        self.startedAt = startedAt
        self.endedAt = endedAt
        self.inputTokens = inputTokens
        self.outputTokens = outputTokens
        self.costMicros = costMicros
        self.detail = detail
        self.partId = partId
        self.messageId = messageId
    }
}
