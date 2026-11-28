/// The group a ``MonarchPayslipLine`` belongs to. Groups are shown in the
/// order of the cases.
public enum MonarchPayslipLineKind: String, Sendable, Hashable, CaseIterable {
    /// Pay: basic salary, allowances, overtime, bonus.
    case earning
    /// Amounts withheld: income tax, social security, pension.
    case deduction
    /// Expenses paid back, outside gross pay.
    case reimbursement
    /// Contributions the employer pays on top, which are not part of net pay.
    case employer
}
