package com.rookhq.monarch.components.billing.payslipview

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * The totals of a `MonarchPayslipView` as payroll calculated them. The view
 * never recalculates them.
 *
 * @property gross the gross earnings.
 * @property deductions the total deductions, as a positive number.
 * @property reimbursements the total reimbursements. Shown only when given and not zero.
 * @property net the net pay.
 */
@Immutable
public class MonarchPayslipTotals(
    public val gross: BigDecimal,
    public val deductions: BigDecimal,
    public val net: BigDecimal,
    public val reimbursements: BigDecimal? = null,
)
