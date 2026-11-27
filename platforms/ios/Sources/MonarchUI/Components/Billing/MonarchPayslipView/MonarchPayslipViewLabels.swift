/// Replacements for the built-in strings of ``MonarchPayslipView``, for
/// example the labels of the payslip's own language.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchPayslipViewLabels: Sendable, Hashable {
    /// The earnings group. Built in: "Earnings".
    public var earnings: String?
    /// The deductions group. Built in: "Deductions".
    public var deductions: String?
    /// The reimbursements group and summary row. Built in: "Reimbursements".
    public var reimbursements: String?
    /// The employer contributions group. Built in: "Employer contributions".
    public var employerContributions: String?
    /// Built in: "Gross earnings".
    public var grossEarnings: String?
    /// Built in: "Total deductions".
    public var totalDeductions: String?
    /// Built in: "Net pay".
    public var netPay: String?
    /// The year-to-date heading. Built in: "Year to date".
    public var yearToDate: String?
    /// Shown when there are no lines. Built in: "No pay lines."
    public var noLines: String?
    /// The download button. Built in: "Download PDF".
    public var download: String?
    /// Spoken while the download is prepared. Built in: "Preparing the PDF".
    public var downloading: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        earnings: String? = nil,
        deductions: String? = nil,
        reimbursements: String? = nil,
        employerContributions: String? = nil,
        grossEarnings: String? = nil,
        totalDeductions: String? = nil,
        netPay: String? = nil,
        yearToDate: String? = nil,
        noLines: String? = nil,
        download: String? = nil,
        downloading: String? = nil
    ) {
        self.earnings = earnings
        self.deductions = deductions
        self.reimbursements = reimbursements
        self.employerContributions = employerContributions
        self.grossEarnings = grossEarnings
        self.totalDeductions = totalDeductions
        self.netPay = netPay
        self.yearToDate = yearToDate
        self.noLines = noLines
        self.download = download
        self.downloading = downloading
    }
}
