/// Where spending stands against the limit.
@_spi(Experimental) public enum MonarchAiSpendCapState: String, Sendable, Hashable, Codable, CaseIterable {
    /// Below the warning share.
    case ok
    /// At or above the warning share, below the limit.
    case warning
    /// At or over the limit. A limit of zero is always reached.
    case reached
    /// No limit is set.
    case noLimit = "no-limit"
}
