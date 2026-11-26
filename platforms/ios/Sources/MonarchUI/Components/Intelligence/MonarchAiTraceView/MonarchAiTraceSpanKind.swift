/// What a span of a trace did.
@_spi(Experimental) public enum MonarchAiTraceSpanKind: String, Sendable, Hashable, Codable, CaseIterable {
    /// A call to a model.
    case model
    /// A call to a tool.
    case tool
    /// A search of a knowledge source.
    case retrieval
    /// A sub-agent.
    case agent
    /// A guardrail check.
    case guardrail
    /// Anything else.
    case other
}
