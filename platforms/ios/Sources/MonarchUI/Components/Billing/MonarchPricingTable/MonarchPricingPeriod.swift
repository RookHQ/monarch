/// A billing period of a ``MonarchPricingTable``, such as monthly or yearly.
public struct MonarchPricingPeriod: Sendable, Hashable, Identifiable {
    /// The key used in each plan's prices.
    public let id: String
    /// The picker text, such as "Monthly", already localised.
    public let label: String
    /// A short factual note shown under the picker while selected, such as
    /// "2 months included".
    public let suffix: String?
    /// Text after every price in this period, such as "/ month".
    public let priceSuffix: String?
    /// A line under every price in this period, such as "Billed yearly".
    public let priceNote: String?

    /// A billing period.
    ///
    /// - Parameters:
    ///   - id: The key used in each plan's prices.
    ///   - label: The picker text, already localised.
    ///   - suffix: A short factual note shown under the picker while selected.
    ///   - priceSuffix: Text after every price in this period.
    ///   - priceNote: A line under every price in this period.
    public init(
        _ id: String,
        label: String,
        suffix: String? = nil,
        priceSuffix: String? = nil,
        priceNote: String? = nil
    ) {
        self.id = id
        self.label = label
        self.suffix = suffix
        self.priceSuffix = priceSuffix
        self.priceNote = priceNote
    }
}
