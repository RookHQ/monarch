import Foundation

/// One calculated line of a ``MonarchPayslipView``.
public struct MonarchPayslipLine: Sendable, Hashable, Identifiable {
    /// A stable id.
    public let id: String
    /// The component name, such as "Basic" or "Income tax".
    public let name: String
    /// The group the line belongs to.
    public let kind: MonarchPayslipLineKind
    /// The amount in major units, as a positive number. Deductions are not negated.
    public let amount: Decimal
    /// How the amount was worked out, shown under the name.
    public let note: String?

    /// A pay line.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The component name.
    ///   - kind: The group the line belongs to.
    ///   - amount: The amount in major units, positive.
    ///   - note: How the amount was worked out.
    public init(_ id: String, name: String, kind: MonarchPayslipLineKind, amount: Decimal, note: String? = nil) {
        self.id = id
        self.name = name
        self.kind = kind
        self.amount = amount
        self.note = note
    }
}
