/// Which spans a ``MonarchAiTraceView`` shows, and in what order.
@_spi(Experimental) public enum MonarchAiTraceFilter: String, Sendable, Hashable, CaseIterable {
    /// Every span, siblings in start order.
    case all
    /// Failed spans and the spans they ran inside.
    case errors
    /// Every span, siblings slowest first.
    case slowest
}
