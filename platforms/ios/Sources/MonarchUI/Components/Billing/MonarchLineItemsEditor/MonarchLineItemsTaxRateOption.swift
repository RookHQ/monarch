import Foundation

/// A preset tax rate offered by a ``MonarchLineItemsEditor`` in a picker.
@_spi(Experimental) public struct MonarchLineItemsTaxRateOption: Sendable, Hashable, Identifiable {
    /// The rate as a percentage: 18 for 18%.
    public let value: Decimal
    /// The visible name, already localised, such as "GST 18%" or "No tax".
    public let label: String

    /// The rate.
    public var id: Decimal { value }

    /// A preset rate.
    ///
    /// - Parameters:
    ///   - value: The rate as a percentage: 18 for 18%.
    ///   - label: The visible name, already localised.
    public init(_ value: Decimal, label: String) {
        self.value = value
        self.label = label
    }
}
