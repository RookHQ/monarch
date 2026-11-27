/// The window a spending limit applies to; spending starts again from zero each period.
@_spi(Experimental) public enum MonarchAiSpendCapPeriod: String, Sendable, Hashable, Codable, CaseIterable {
    /// One day.
    case day
    /// One week.
    case week
    /// One month.
    case month
}
