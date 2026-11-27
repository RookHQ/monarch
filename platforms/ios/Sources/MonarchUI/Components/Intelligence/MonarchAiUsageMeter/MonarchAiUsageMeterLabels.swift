/// Replacements for the built-in strings of ``MonarchAiUsageMeter``. Every
/// value is optional; leave one out to keep the built-in English text.
public struct MonarchAiUsageMeterLabels: Sendable {
    /// The spoken name of the meter group when it has no visible title.
    public var label: String
    /// The unit after an amount: "token", "tokens", "credit" or "credits".
    /// It gets the unit and the number the unit refers to (the limit, or the
    /// amount used when there is no limit).
    public var unit: @Sendable (_ unit: MonarchAiUsageUnit, _ count: Double) -> String
    /// The strings of the underlying ``MonarchUsageMeter``: the amount, the
    /// percentage, near and over the limit, the announcement, loading and retry.
    public var meter: MonarchUsageMeterLabels

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "AI usage".
    ///   - unit: Replaces the unit names.
    ///   - meter: Replaces the strings of the underlying usage meter.
    public init(
        label: String? = nil,
        unit: (@Sendable (_ unit: MonarchAiUsageUnit, _ count: Double) -> String)? = nil,
        meter: MonarchUsageMeterLabels = MonarchUsageMeterLabels()
    ) {
        self.label = label ?? MonarchAiUsageMeterStrings.label
        self.unit = unit ?? { MonarchAiUsageMeterStrings.unit($0, count: $1) }
        self.meter = meter
    }
}
