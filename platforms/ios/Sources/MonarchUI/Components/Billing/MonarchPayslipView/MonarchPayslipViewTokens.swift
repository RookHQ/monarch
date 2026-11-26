import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchPayslipView``.
enum MonarchPayslipViewTokens {
    /// The gap between the header, the details, the groups and the summary.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between a group heading and its lines, and between lines.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between a name and its note, or a label and its value.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the title, the status and the actions.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between detail columns and year-to-date tiles.
    static let gridSpacing: CGFloat = MonarchTokens.Space.space4
    /// The smallest gap between a name and its amount.
    static let amountGap: CGFloat = MonarchTokens.Space.space4
    /// The padding inside a year-to-date tile.
    static let tilePadding: CGFloat = MonarchTokens.Space.space3
    /// The narrowest year-to-date tile at the default text size (9rem on
    /// the web); it scales with the text.
    static let tileMinimumWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space12
    /// The fill of a year-to-date tile.
    static var tileFill: MonarchColorRole { \.bgSubtle }
    /// The SF Symbol of the download button.
    static let downloadSymbol = "arrow.down.to.line"
    /// The number of detail columns at regular width.
    static let regularDetailColumns = 3

    /// Every built-in string, with the app's replacements applied.
    struct Texts: Equatable {
        var earnings: String
        var deductions: String
        var reimbursements: String
        var employerContributions: String
        var grossEarnings: String
        var totalDeductions: String
        var netPay: String
        var yearToDate: String
        var noLines: String
        var download: String
        var downloading: String
    }

    /// The strings to show: each replacement, else the built-in string.
    static func texts(_ labels: MonarchPayslipViewLabels) -> Texts {
        typealias Built = MonarchPayslipViewStrings
        return Texts(
            earnings: labels.earnings ?? Built.earnings,
            deductions: labels.deductions ?? Built.deductions,
            reimbursements: labels.reimbursements ?? Built.reimbursements,
            employerContributions: labels.employerContributions ?? Built.employerContributions,
            grossEarnings: labels.grossEarnings ?? Built.grossEarnings,
            totalDeductions: labels.totalDeductions ?? Built.totalDeductions,
            netPay: labels.netPay ?? Built.netPay,
            yearToDate: labels.yearToDate ?? Built.yearToDate,
            noLines: labels.noLines ?? Built.noLines,
            download: labels.download ?? Built.download,
            downloading: labels.downloading ?? Built.downloading
        )
    }

    /// One group of lines with its heading.
    struct Group: Equatable {
        let kind: MonarchPayslipLineKind
        let title: String
        let lines: [MonarchPayslipLine]
    }

    /// The heading of a group.
    static func title(_ kind: MonarchPayslipLineKind, texts: Texts) -> String {
        switch kind {
        case .earning: texts.earnings
        case .deduction: texts.deductions
        case .reimbursement: texts.reimbursements
        case .employer: texts.employerContributions
        }
    }

    /// The lines grouped in the order earnings, deductions, reimbursements,
    /// employer contributions, keeping the given order inside a group.
    /// Empty groups are left out, and employer contributions when hidden.
    static func groups(_ lines: [MonarchPayslipLine], hidesEmployerContributions: Bool, texts: Texts) -> [Group] {
        MonarchPayslipLineKind.allCases
            .filter { !(hidesEmployerContributions && $0 == .employer) }
            .map { kind in Group(kind: kind, title: title(kind, texts: texts), lines: lines.filter { $0.kind == kind })
            }
            .filter { !$0.lines.isEmpty }
    }

    /// A row of the gross-to-net summary.
    struct SummaryRow: Equatable {
        let label: String
        let amount: Decimal
        let isNet: Bool
    }

    /// The gross-to-net summary: gross, deductions, reimbursements when not
    /// zero, then net pay.
    static func summary(_ totals: MonarchPayslipTotals, texts: Texts) -> [SummaryRow] {
        var rows = [
            SummaryRow(label: texts.grossEarnings, amount: totals.gross, isNet: false),
            SummaryRow(label: texts.totalDeductions, amount: totals.deductions, isNet: false),
        ]
        if let reimbursements = totals.reimbursements, reimbursements != 0 {
            rows.append(SummaryRow(label: texts.reimbursements, amount: reimbursements, isNet: false))
        }
        rows.append(SummaryRow(label: texts.netPay, amount: totals.net, isNet: true))
        return rows
    }

    /// The type roles of a summary row's label and amount: net pay is the
    /// largest, so it stands out without colour.
    static func textStyle(isNet: Bool) -> (label: MonarchTextStyle, amount: MonarchTextStyle) {
        isNet ? (.bodyStrong, .subheading) : (.body, .body)
    }

    /// The colour of a summary row's label: net pay in the default text
    /// colour, the others muted.
    static func labelRole(isNet: Bool) -> MonarchColorRole {
        isNet ? \.textDefault : \.textMuted
    }

    /// The number of detail columns: one at the accessibility sizes, two at compact width, three otherwise, with
    /// the label above its value as on the web.
    static func detailColumns(isRegularWidth: Bool, isAccessibilitySize: Bool) -> Int {
        if isAccessibilitySize { return 1 }
        return isRegularWidth ? regularDetailColumns : 2
    }

    /// Splits the details into grid rows of `columns`.
    static func rows<Item>(_ items: [Item], columns: Int) -> [[Item]] {
        guard columns > 0 else { return [items] }
        return stride(from: 0, to: items.count, by: columns).map { Array(items[$0..<min($0 + columns, items.count)]) }
    }
}
