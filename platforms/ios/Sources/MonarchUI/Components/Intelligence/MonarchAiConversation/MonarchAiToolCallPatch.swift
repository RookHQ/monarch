/// The fields of a tool call that a `toolUpdate` stream event changes. A
/// field that is `nil` is left as it is.
public struct MonarchAiToolCallPatch: Sendable, Hashable {
    /// The new tool name.
    public var name: String?
    /// The new arguments.
    public var input: MonarchAiJSON?
    /// The new state.
    public var state: MonarchAiToolCallState?
    /// What the tool returned.
    public var output: MonarchAiJSON?
    /// Why the call failed or was denied.
    public var error: String?
    /// The ISO 8601 time the tool started.
    public var startedAt: String?
    /// The ISO 8601 time the tool ended.
    public var endedAt: String?
    /// The arguments as raw JSON text.
    public var inputText: String?
    /// How much harm running the tool can do.
    public var risk: MonarchAiToolCallRisk?

    /// A patch. Pass only the fields to change.
    ///
    /// - Parameters:
    ///   - name: The new tool name.
    ///   - input: The new arguments.
    ///   - state: The new state.
    ///   - output: What the tool returned.
    ///   - error: Why the call failed or was denied.
    ///   - startedAt: The ISO 8601 time the tool started.
    ///   - endedAt: The ISO 8601 time the tool ended.
    ///   - inputText: The arguments as raw JSON text.
    ///   - risk: How much harm running the tool can do.
    public init(
        name: String? = nil,
        input: MonarchAiJSON? = nil,
        state: MonarchAiToolCallState? = nil,
        output: MonarchAiJSON? = nil,
        error: String? = nil,
        startedAt: String? = nil,
        endedAt: String? = nil,
        inputText: String? = nil,
        risk: MonarchAiToolCallRisk? = nil
    ) {
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

    /// The tool call with the patch applied.
    func applied(to call: MonarchAiToolCallPart) -> MonarchAiToolCallPart {
        var call = call
        if let name { call.name = name }
        if let input { call.input = input }
        if let state { call.state = state }
        if let output { call.output = output }
        if let error { call.error = error }
        if let startedAt { call.startedAt = startedAt }
        if let endedAt { call.endedAt = endedAt }
        if let inputText { call.inputText = inputText }
        if let risk { call.risk = risk }
        return call
    }
}
