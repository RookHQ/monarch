import MonarchUI
import SwiftUI

struct MonarchPayslipViewSection: CataloguePage {
    static let title = "PayslipView"

    @State private var hidesEmployer = false
    @State private var isDownloading = false

    var body: some View {
        CatalogueCaption("Grouped lines, gross-to-net summary and year to date")
        Toggle("Hide employer contributions", isOn: $hidesEmployer)
        MonarchPayslipView(
            "Payslip",
            issuer: "Northwind Studio Pvt Ltd",
            subtitle: "March 2026",
            status: MonarchStatusDefinition("Paid", tone: .success, systemImage: "checkmark.circle.fill"),
            details: [
                MonarchPayslipDetail("Employee", value: "Asha Rao"),
                MonarchPayslipDetail("Employee ID", value: "EMP-0042"),
                MonarchPayslipDetail("Paid days", value: "31"),
            ],
            lines: [
                MonarchPayslipLine("basic", name: "Basic", kind: .earning, amount: 50_000),
                MonarchPayslipLine("hra", name: "House rent allowance", kind: .earning, amount: 20_000),
                MonarchPayslipLine("pf", name: "Provident fund", kind: .deduction, amount: 6_000, note: "12% of basic"),
                MonarchPayslipLine("tds", name: "Income tax", kind: .deduction, amount: 4_500),
                MonarchPayslipLine("travel", name: "Travel", kind: .reimbursement, amount: 1_200),
                MonarchPayslipLine("pf-employer", name: "Provident fund (employer)", kind: .employer, amount: 6_000),
            ],
            totals: MonarchPayslipTotals(gross: 70_000, deductions: 10_500, reimbursements: 1_200, net: 60_700),
            yearToDate: [
                MonarchPayslipYearToDateItem("Gross pay", amount: 840_000),
                MonarchPayslipYearToDateItem("Income tax", amount: 54_000),
            ],
            currencyCode: "INR",
            hidesEmployerContributions: hidesEmployer,
            isDownloading: isDownloading,
            onDownload: { prepareDownload() }
        )

        CatalogueCaption("No lines")
        MonarchPayslipView(
            "Payslip",
            lines: [],
            totals: MonarchPayslipTotals(gross: 0, deductions: 0, net: 0),
            currencyCode: "USD"
        )
    }

    /// Stands in for the app making the PDF: the button is busy for a moment.
    private func prepareDownload() {
        isDownloading = true
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(2))
            isDownloading = false
        }
    }
}
