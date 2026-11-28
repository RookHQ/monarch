import Foundation

/// The totals of a ``MonarchPayslipView`` as payroll calculated them, in
/// major units. The view never recalculates them.
public struct MonarchPayslipTotals: Sendable, Hashable {
    /// Gross earnings.
    public let gross: Decimal
    /// Total deductions, as a positive number.
    public let deductions: Decimal
    /// Reimbursements. Shown only when given and not zero.
    public let reimbursements: Decimal?
    /// Net pay.
    public let net: Decimal

    /// Totals.
    ///
    /// - Parameters:
    ///   - gross: Gross earnings.
    ///   - deductions: Total deductions, as a positive number.
    ///   - reimbursements: Reimbursements, shown only when not zero.
    ///   - net: Net pay.
    public init(gross: Decimal, deductions: Decimal, reimbursements: Decimal? = nil, net: Decimal) {
        self.gross = gross
        self.deductions = deductions
        self.reimbursements = reimbursements
        self.net = net
    }
}
