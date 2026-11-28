import Foundation

/// One step of an agent run. Times are seconds from the start of the run.
@_spi(Experimental) public struct MonarchAiTraceSpan: Identifiable, Sendable, Hashable {
    /// A stable id, unique in the trace.
    public var id: String
    /// The span this one ran inside. `nil`, or an unknown id, is top level.
    public var parentId: String?
    /// What the span did.
    public var kind: MonarchAiTraceSpanKind
    /// What ran, such as a tool name or a step name.
    public var name: String
    /// How it ended, or `running`.
    public var status: MonarchAiTraceSpanStatus
    /// When it started, in seconds from the start of the run.
    public var start: TimeInterval
    /// When it ended, in seconds from the start of the run. `nil` while it runs.
    public var end: TimeInterval?
    /// Tokens this span itself read. Do not add children's tokens to parents: totals add every span.
    public var inputTokens: Int?
    /// Tokens this span itself wrote.
    public var outputTokens: Int?
    /// This span's own cost in millionths of `currency` (1 000 000 is 1.00).
    public var costMicros: Int?
    /// The ISO 4217 currency of `costMicros`. `nil` uses the view's currency.
    public var currency: String?
    /// The model that served a model call.
    public var model: String?
    /// Why it failed, as plain text.
    public var error: String?
    /// Extra facts, shown in the details sorted by name.
    public var attributes: [String: String]
    /// What went in, as plain text.
    public var input: String?
    /// What came out, as plain text.
    public var output: String?
    /// The conversation part this span produced or served, such as a tool call's id.
    public var partId: String?
    /// The conversation message this span belongs to.
    public var messageId: String?

    /// A span.
    ///
    /// - Parameters:
    ///   - id: A stable id, unique in the trace.
    ///   - parentId: The span this one ran inside.
    ///   - kind: What the span did.
    ///   - name: What ran.
    ///   - status: How it ended, or `running`.
    ///   - start: When it started, in seconds from the start of the run.
    ///   - end: When it ended; `nil` while it runs.
    ///   - inputTokens: Tokens this span itself read.
    ///   - outputTokens: Tokens this span itself wrote.
    ///   - costMicros: This span's own cost in millionths of the currency.
    ///   - currency: The ISO 4217 currency of the cost.
    ///   - model: The model that served a model call.
    ///   - error: Why it failed.
    ///   - attributes: Extra facts.
    ///   - input: What went in, as plain text.
    ///   - output: What came out, as plain text.
    ///   - partId: The conversation part this span produced or served.
    ///   - messageId: The conversation message this span belongs to.
    public init(
        id: String,
        parentId: String? = nil,
        kind: MonarchAiTraceSpanKind,
        name: String,
        status: MonarchAiTraceSpanStatus,
        start: TimeInterval,
        end: TimeInterval? = nil,
        inputTokens: Int? = nil,
        outputTokens: Int? = nil,
        costMicros: Int? = nil,
        currency: String? = nil,
        model: String? = nil,
        error: String? = nil,
        attributes: [String: String] = [:],
        input: String? = nil,
        output: String? = nil,
        partId: String? = nil,
        messageId: String? = nil
    ) {
        self.id = id
        self.parentId = parentId
        self.kind = kind
        self.name = name
        self.status = status
        self.start = start
        self.end = end
        self.inputTokens = inputTokens
        self.outputTokens = outputTokens
        self.costMicros = costMicros
        self.currency = currency
        self.model = model
        self.error = error
        self.attributes = attributes
        self.input = input
        self.output = output
        self.partId = partId
        self.messageId = messageId
    }
}
