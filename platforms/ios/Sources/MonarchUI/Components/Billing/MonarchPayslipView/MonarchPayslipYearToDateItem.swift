import Foundation

/// One year-to-date figure of a ``MonarchPayslipView``, such as gross pay
/// or tax paid so far this year.
public struct MonarchPayslipYearToDateItem: Sendable, Hashable {
    /// The label, already localised.
    public let label: String
    /// The amount in major units.
    public let amount: Decimal

    /// A year-to-date figure.
    ///
    /// - Parameters:
    ///   - label: The label, already localised.
    ///   - amount: The amount in major units.
    public init(_ label: String, amount: Decimal) {
        self.label = label
        self.amount = amount
    }
}
