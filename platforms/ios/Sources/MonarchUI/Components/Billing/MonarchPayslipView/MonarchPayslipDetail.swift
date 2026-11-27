/// A label and value among the employee and period facts of a
/// ``MonarchPayslipView``, such as "Employee ID" or "Paid days".
public struct MonarchPayslipDetail: Sendable, Hashable {
    /// The label, already localised.
    public let label: String
    /// The value, already formatted.
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
