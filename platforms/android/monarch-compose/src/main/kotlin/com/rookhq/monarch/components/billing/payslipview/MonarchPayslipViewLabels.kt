package com.rookhq.monarch.components.billing.payslipview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPayslipView`, for
 * example with the labels of the payslip's own language. Every `null` uses
 * the built-in string named after it in `strings_payslipview.xml`.
 *
 * @property earnings the heading of the earning lines: "Earnings".
 * @property deductions the heading of the deduction lines: "Deductions".
 * @property reimbursements the heading of the reimbursement lines and the summary row: "Reimbursements".
 * @property employerContributions the heading of the employer lines: "Employer contributions".
 * @property grossEarnings the summary row of gross pay: "Gross earnings".
 * @property totalDeductions the summary row of deductions: "Total deductions".
 * @property netPay the summary row of net pay: "Net pay".
 * @property yearToDate the heading of the year-to-date figures: "Year to date".
 * @property noLines shown when there are no lines: "No pay lines.".
 * @property download the download button: "Download PDF".
 * @property downloading the state TalkBack reads on the button while the file is prepared: "Preparing PDF".
 */
@Immutable
public class MonarchPayslipViewLabels(
    public val earnings: String? = null,
    public val deductions: String? = null,
    public val reimbursements: String? = null,
    public val employerContributions: String? = null,
    public val grossEarnings: String? = null,
    public val totalDeductions: String? = null,
    public val netPay: String? = null,
    public val yearToDate: String? = null,
    public val noLines: String? = null,
    public val download: String? = null,
    public val downloading: String? = null,
)
