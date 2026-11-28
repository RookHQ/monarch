/// A filter option of a ``MonarchRenewalsPanel``, such as a time window or a
/// group of statuses. The app filters the items.
@_spi(Experimental) public struct MonarchRenewalFilter: Identifiable, Sendable, Hashable {
    /// The value set on the selection.
    public let value: String
    /// The visible label, already localised.
    public let label: String
    /// An optional count shown with the label.
    public let count: Int?

    /// The value.
    public var id: String { value }

    /// A filter option.
    ///
    /// - Parameters:
    ///   - value: The value set on the selection.
    ///   - label: The visible label.
    ///   - count: An optional count shown with the label.
    public init(value: String, label: String, count: Int? = nil) {
        self.value = value
        self.label = label
        self.count = count
    }
}
