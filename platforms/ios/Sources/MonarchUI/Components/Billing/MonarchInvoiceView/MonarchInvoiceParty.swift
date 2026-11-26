/// One side of a ``MonarchInvoiceView``: who bills or who is billed.
public struct MonarchInvoiceParty: Sendable, Hashable {
    /// The organisation or person name, already localised.
    public let name: String
    /// Address, tax registration, email and similar lines, one per entry.
    public let details: [String]

    /// A party.
    ///
    /// - Parameters:
    ///   - name: The organisation or person name.
    ///   - details: Address, tax registration, email and similar lines, one per entry.
    public init(_ name: String, details: [String] = []) {
        self.name = name
        self.details = details
    }
}
