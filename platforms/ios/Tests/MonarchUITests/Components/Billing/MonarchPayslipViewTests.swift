import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let payLines = [
    MonarchPayslipLine("basic", name: "Basic", kind: .earning, amount: 50_000, note: "Fixed monthly"),
    MonarchPayslipLine("hra", name: "House rent allowance", kind: .earning, amount: 20_000),
    MonarchPayslipLine("pf", name: "Provident fund", kind: .deduction, amount: 6_000, note: "12% of basic"),
    MonarchPayslipLine("tds", name: "Income tax", kind: .deduction, amount: 4_500),
    MonarchPayslipLine("travel", name: "Travel", kind: .reimbursement, amount: 1_200),
    MonarchPayslipLine("pf-employer", name: "Provident fund (employer)", kind: .employer, amount: 6_000),
]
private let payTotals = MonarchPayslipTotals(gross: 70_000, deductions: 10_500, reimbursements: 1_200, net: 60_700)
private let payDetails = [
    MonarchPayslipDetail("Employee", value: "Asha Rao"),
    MonarchPayslipDetail("Employee ID", value: "EMP-0042"),
    MonarchPayslipDetail("Pay period", value: "March 2026"),
    MonarchPayslipDetail("Paid days", value: "31"),
]

enum MonarchPayslipViewFixtures: ComponentFixtures {
    static let id = "payslip-view"
    static let fixtures = [
        Fixture("every group, details, year to date, download") {
            MonarchPayslipView(
                "Payslip",
                issuer: "Northwind Studio Pvt Ltd",
                subtitle: "March 2026 · Payslip 2026-03-042",
                status: MonarchStatusDefinition("Paid", tone: .success),
                details: payDetails,
                lines: payLines,
                totals: payTotals,
                yearToDate: [
                    MonarchPayslipYearToDateItem("Gross pay", amount: 840_000),
                    MonarchPayslipYearToDateItem("Income tax", amount: 54_000),
                    MonarchPayslipYearToDateItem("Net pay", amount: 728_400),
                ],
                currencyCode: "INR",
                onDownload: {}
            )
        },
        Fixture("regular width, employer hidden, downloading, extra action") {
            MonarchPayslipView(
                "Final settlement",
                details: payDetails,
                lines: payLines,
                totals: MonarchPayslipTotals(gross: 70_000, deductions: 10_500, reimbursements: 0, net: 59_500),
                yearToDate: [MonarchPayslipYearToDateItem("Gross pay", amount: 840_000)],
                yearToDateTitle: "Year to date (from April)",
                currencyCode: "INR",
                hidesEmployerContributions: true,
                isDownloading: true,
                onDownload: {}
            ) {
                MonarchButton("Share", systemImage: "square.and.arrow.up", variant: .ghost, size: .sm) {}
            }
            .environment(\.horizontalSizeClass, .regular)
        },
        Fixture("no lines and custom labels") {
            MonarchPayslipView(
                "Gehaltsabrechnung",
                lines: [],
                totals: MonarchPayslipTotals(gross: 0, deductions: 0, net: 0),
                currencyCode: "EUR",
                labels: MonarchPayslipViewLabels(netPay: "Nettolohn", noLines: "Keine Positionen.")
            )
        },
        Fixture("long names and large amounts") {
            MonarchPayslipView(
                "Payslip for the period ending on the last working day of the financial year",
                lines: [
                    MonarchPayslipLine(
                        "1",
                        name: "Special allowance for relocation, remote work equipment and professional development",
                        kind: .earning,
                        amount: 123_456_789.12,
                        note: "Paid once, as agreed in the contract amendment of 14 February"
                    )
                ],
                totals: MonarchPayslipTotals(gross: 123_456_789.12, deductions: 0, net: 123_456_789.12),
                currencyCode: "USD"
            )
        },
    ]
}

@Suite("MonarchPayslipView")
struct MonarchPayslipViewTests {
    private let texts = MonarchPayslipViewTokens.texts(MonarchPayslipViewLabels())

    @Test("Lines are grouped in a fixed order, keeping their own order inside a group")
    func groups() {
        let shuffled = [payLines[5], payLines[2], payLines[0], payLines[4], payLines[1], payLines[3]]
        let groups = MonarchPayslipViewTokens.groups(shuffled, hidesEmployerContributions: false, texts: texts)
        #expect(groups.map(\.kind) == [.earning, .deduction, .reimbursement, .employer])
        #expect(groups.map(\.title) == ["Earnings", "Deductions", "Reimbursements", "Employer contributions"])
        #expect(groups[0].lines.map(\.id) == ["basic", "hra"])
        #expect(groups[1].lines.map(\.id) == ["pf", "tds"])
    }

    @Test("Empty groups and hidden employer contributions are left out")
    func hiddenGroups() {
        let groups = MonarchPayslipViewTokens.groups(payLines, hidesEmployerContributions: true, texts: texts)
        #expect(groups.map(\.kind) == [.earning, .deduction, .reimbursement])
        let earningsOnly = MonarchPayslipViewTokens.groups(
            [payLines[0]],
            hidesEmployerContributions: false,
            texts: texts
        )
        #expect(earningsOnly.map(\.kind) == [.earning])
        #expect(MonarchPayslipViewTokens.groups([], hidesEmployerContributions: false, texts: texts).isEmpty)
    }

    @Test("The summary runs gross to net; reimbursements only when not zero")
    func summary() {
        let rows = MonarchPayslipViewTokens.summary(payTotals, texts: texts)
        #expect(rows.map(\.label) == ["Gross earnings", "Total deductions", "Reimbursements", "Net pay"])
        #expect(rows.map(\.amount) == [70_000, 10_500, 1_200, 60_700])
        #expect(rows.map(\.isNet) == [false, false, false, true])
        let zero = MonarchPayslipTotals(gross: 10, deductions: 1, reimbursements: 0, net: 9)
        #expect(MonarchPayslipViewTokens.summary(zero, texts: texts).count == 3)
        let none = MonarchPayslipTotals(gross: 10, deductions: 1, net: 9)
        #expect(MonarchPayslipViewTokens.summary(none, texts: texts).count == 3)
    }

    @Test("Net pay stands out by type role and colour role")
    func netAppearance() {
        #expect(MonarchPayslipViewTokens.textStyle(isNet: true) == (.bodyStrong, .subheading))
        #expect(MonarchPayslipViewTokens.textStyle(isNet: false) == (.body, .body))
        #expect(MonarchPayslipViewTokens.labelRole(isNet: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchPayslipViewTokens.labelRole(isNet: false) == \MonarchColorTokens.textMuted)
        #expect(MonarchPayslipViewTokens.tileFill == \MonarchColorTokens.bgSubtle)
    }

    @Test("Details are two columns at compact width, one at accessibility sizes, three otherwise")
    func detailColumns() {
        #expect(MonarchPayslipViewTokens.detailColumns(isRegularWidth: false, isAccessibilitySize: false) == 2)
        #expect(MonarchPayslipViewTokens.detailColumns(isRegularWidth: true, isAccessibilitySize: true) == 1)
        #expect(MonarchPayslipViewTokens.detailColumns(isRegularWidth: true, isAccessibilitySize: false) == 3)
        #expect(MonarchPayslipViewTokens.rows([1, 2, 3, 4], columns: 3) == [[1, 2, 3], [4]])
        #expect(MonarchPayslipViewTokens.rows([1, 2], columns: 1) == [[1], [2]])
        #expect(MonarchPayslipViewTokens.rows([Int](), columns: 3).isEmpty)
    }

    @Test("Replacements win over the built-in strings")
    func labels() {
        let custom = MonarchPayslipViewTokens.texts(MonarchPayslipViewLabels(earnings: "Bezüge", netPay: "Nettolohn"))
        #expect(custom.earnings == "Bezüge")
        #expect(custom.netPay == "Nettolohn")
        #expect(custom.deductions == "Deductions")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchPayslipViewStrings.earnings == "Earnings")
        #expect(MonarchPayslipViewStrings.deductions == "Deductions")
        #expect(MonarchPayslipViewStrings.reimbursements == "Reimbursements")
        #expect(MonarchPayslipViewStrings.employerContributions == "Employer contributions")
        #expect(MonarchPayslipViewStrings.grossEarnings == "Gross earnings")
        #expect(MonarchPayslipViewStrings.totalDeductions == "Total deductions")
        #expect(MonarchPayslipViewStrings.netPay == "Net pay")
        #expect(MonarchPayslipViewStrings.yearToDate == "Year to date")
        #expect(MonarchPayslipViewStrings.noLines == "No pay lines.")
        #expect(MonarchPayslipViewStrings.download == "Download PDF")
        #expect(MonarchPayslipViewStrings.downloading == "Preparing the PDF")
    }

    @Test("Hiding employer contributions makes the payslip shorter")
    @MainActor
    func hidingShortens() {
        let all = Host.layout(MonarchPayslipView("Payslip", lines: payLines, totals: payTotals, currencyCode: "INR"))
        let hidden = Host.layout(
            MonarchPayslipView(
                "Payslip",
                lines: payLines,
                totals: payTotals,
                currencyCode: "INR",
                hidesEmployerContributions: true
            )
        )
        #expect(hidden.height < all.height)
    }
}
