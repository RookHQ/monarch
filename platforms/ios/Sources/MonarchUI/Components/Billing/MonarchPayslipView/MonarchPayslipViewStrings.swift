/// Built-in strings of the payslip view, from the `MonarchPayslipView` strings table.
enum MonarchPayslipViewStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.payslipView.\(key)", table: "MonarchPayslipView")
    }

    /// The earnings group.
    static var earnings: String { string("earnings") }
    /// The deductions group.
    static var deductions: String { string("deductions") }
    /// The reimbursements group and summary row.
    static var reimbursements: String { string("reimbursements") }
    /// The employer contributions group.
    static var employerContributions: String { string("employerContributions") }
    /// The gross earnings summary row.
    static var grossEarnings: String { string("grossEarnings") }
    /// The total deductions summary row.
    static var totalDeductions: String { string("totalDeductions") }
    /// The net pay summary row.
    static var netPay: String { string("netPay") }
    /// The year-to-date heading.
    static var yearToDate: String { string("yearToDate") }
    /// Shown when there are no lines.
    static var noLines: String { string("noLines") }
    /// The download button.
    static var download: String { string("download") }
    /// Spoken while the download is prepared.
    static var downloading: String { string("downloading") }
}
