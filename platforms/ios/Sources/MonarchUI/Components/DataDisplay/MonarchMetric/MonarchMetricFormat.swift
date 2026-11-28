/// How a ``MonarchMetric`` writes a number.
public enum MonarchMetricFormat: Sendable, Hashable {
    /// A plain number in the current locale: "12,345". The default.
    case number
    /// A ratio written as a percentage: 0.12 is "12%".
    case percent
    /// An amount of money in an ISO 4217 currency, such as `EUR`.
    case currency(code: String)
}
