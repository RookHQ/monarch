/// The change shown under a ``MonarchMetric``, drawn by
/// ``MonarchDeltaIndicator``.
///
/// ```swift
/// MonarchMetricChange(0.08, comparison: "vs last week")
/// MonarchMetricChange(current: 120, previous: 140, goodDirection: .down)
/// ```
public struct MonarchMetricChange: Sendable {
    /// The change. A ratio unless `formatValue` writes it another way.
    /// `nil` means there is nothing to compare with.
    public var value: Double?
    /// The comparison period, such as "vs last week".
    public var comparison: String?
    /// Which direction is an improvement.
    public var goodDirection: MonarchDeltaIndicatorGoodDirection
    /// Plain text or a tinted pill.
    public var variant: MonarchDeltaIndicatorVariant
    /// Changes whose size is at or below this count as no change.
    public var flatThreshold: Double
    /// Writes the size of the change, without a sign. By default a percentage.
    public var formatValue: (@Sendable (Double) -> String)?

    /// A change that is already known.
    ///
    /// - Parameters:
    ///   - value: The change, a ratio by default. `nil` when there is nothing to compare with.
    ///   - comparison: The comparison period, such as "vs last week".
    ///   - goodDirection: Which direction is an improvement.
    ///   - variant: Plain text or a tinted pill.
    ///   - flatThreshold: Changes whose size is at or below this count as no change.
    ///   - formatValue: Writes the size of the change, without a sign.
    public init(
        _ value: Double?,
        comparison: String? = nil,
        goodDirection: MonarchDeltaIndicatorGoodDirection = .up,
        variant: MonarchDeltaIndicatorVariant = .plain,
        flatThreshold: Double = 0,
        formatValue: (@Sendable (Double) -> String)? = nil
    ) {
        self.value = value
        self.comparison = comparison
        self.goodDirection = goodDirection
        self.variant = variant
        self.flatThreshold = flatThreshold
        self.formatValue = formatValue
    }

    /// A change worked out from two periods, as a ratio of the previous one.
    ///
    /// - Parameters:
    ///   - current: This period's value.
    ///   - previous: The previous period's value. Zero or `nil` means there is nothing to compare with.
    ///   - comparison: The comparison period, such as "vs last week".
    ///   - goodDirection: Which direction is an improvement.
    ///   - variant: Plain text or a tinted pill.
    ///   - flatThreshold: Changes whose size is at or below this count as no change.
    ///   - formatValue: Writes the size of the change, without a sign.
    public init(
        current: Double?,
        previous: Double?,
        comparison: String? = nil,
        goodDirection: MonarchDeltaIndicatorGoodDirection = .up,
        variant: MonarchDeltaIndicatorVariant = .plain,
        flatThreshold: Double = 0,
        formatValue: (@Sendable (Double) -> String)? = nil
    ) {
        self.init(
            MonarchDeltaIndicatorTokens.ratio(current: current, previous: previous),
            comparison: comparison,
            goodDirection: goodDirection,
            variant: variant,
            flatThreshold: flatThreshold,
            formatValue: formatValue
        )
    }
}
