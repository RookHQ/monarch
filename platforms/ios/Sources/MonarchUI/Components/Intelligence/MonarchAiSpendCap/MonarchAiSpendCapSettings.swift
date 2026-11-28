/// The settings of a spending limit that a person can change; ``MonarchAiSpendCap`` hands them to `onSave`.
@_spi(Experimental) public struct MonarchAiSpendCapSettings: Sendable, Hashable {
    /// The limit as an integer in the cap's unit, or `nil` for no limit.
    public var limit: Int?
    /// The share of the limit, between 0 and 1, at which to warn.
    public var warnAt: Double
    /// What happens when the limit is reached.
    public var behavior: MonarchAiSpendCapBehavior

    /// Settings.
    ///
    /// - Parameters:
    ///   - limit: The limit as an integer in the cap's unit, or `nil` for no limit.
    ///   - warnAt: The share of the limit at which to warn.
    ///   - behavior: What happens when the limit is reached.
    public init(limit: Int?, warnAt: Double, behavior: MonarchAiSpendCapBehavior) {
        self.limit = limit
        self.warnAt = warnAt
        self.behavior = behavior
    }
}
