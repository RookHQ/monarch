/// A call the model made to a tool.
public struct MonarchAiToolCallPart: Identifiable, Sendable, Hashable, Codable {
    /// The id of the call.
    public var id: String
    /// The tool name as the model called it, for example `search_documents`.
    public var name: String
    /// The arguments.
    public var input: MonarchAiJSON
    /// Where the call is in its lifecycle.
    public var state: MonarchAiToolCallState
    /// What the tool returned.
    public var output: MonarchAiJSON?
    /// Why the call failed or was denied.
    public var error: String?
    /// The ISO 8601 time the tool started.
    public var startedAt: String?
    /// The ISO 8601 time the tool ended.
    public var endedAt: String?
    /// The arguments as raw JSON text while they stream, before `input` is parsed.
    public var inputText: String?
    /// How much harm running it can do, when the service says.
    public var risk: MonarchAiToolCallRisk?

    /// A tool call.
    ///
    /// - Parameters:
    ///   - id: The id of the call.
    ///   - name: The tool name as the model called it.
    ///   - input: The arguments.
    ///   - state: Where the call is in its lifecycle.
    ///   - output: What the tool returned.
    ///   - error: Why the call failed or was denied.
    ///   - startedAt: The ISO 8601 time the tool started.
    ///   - endedAt: The ISO 8601 time the tool ended.
    ///   - inputText: The arguments as raw JSON text while they stream.
    ///   - risk: How much harm running it can do.
    public init(
        id: String,
        name: String,
        input: MonarchAiJSON = .null,
        state: MonarchAiToolCallState,
        output: MonarchAiJSON? = nil,
        error: String? = nil,
        startedAt: String? = nil,
        endedAt: String? = nil,
        inputText: String? = nil,
        risk: MonarchAiToolCallRisk? = nil
    ) {
        self.id = id
        self.name = name
        self.input = input
        self.state = state
        self.output = output
        self.error = error
        self.startedAt = startedAt
        self.endedAt = endedAt
        self.inputText = inputText
        self.risk = risk
    }
}
