/// What happens when spending reaches the limit.
@_spi(Experimental) public enum MonarchAiSpendCapBehavior: String, Sendable, Hashable, Codable, CaseIterable {
    /// AI stops and asks; nothing more is spent until the limit is raised or the period resets.
    case pause
    /// AI keeps going and the person is told; spending can go over the limit.
    case notify
}
