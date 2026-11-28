/// A label and value among the facts of a ``MonarchInvoiceView``, such as
/// payment terms, a purchase order number or a project.
public struct MonarchInvoiceDetail: Sendable, Hashable {
    /// The label, already localised, such as "Terms".
    public let label: String
    /// The value, already formatted, such as "Net 30".
    public let value: String

    /// A fact.
    ///
    /// - Parameters:
    ///   - label: The label, already localised.
    ///   - value: The value, already formatted.
    public init(_ label: String, value: String) {
        self.label = label
        self.value = value
    }
}
