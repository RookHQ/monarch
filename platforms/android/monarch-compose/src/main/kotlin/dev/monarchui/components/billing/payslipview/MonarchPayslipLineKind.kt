package dev.monarchui.components.billing.payslipview

/** The group a payslip line belongs to, shown in this order by `MonarchPayslipView`. */
public enum class MonarchPayslipLineKind {
    /** Pay earned: basic, allowances, overtime. Under "Earnings". */
    Earning,

    /** Taken off the pay: tax, social security, loans. Under "Deductions". */
    Deduction,

    /** Paid back to the employee: expenses. Under "Reimbursements". */
    Reimbursement,

    /** Paid by the employer on top of the pay: pension, insurance. Under "Employer contributions". */
    Employer,
}
