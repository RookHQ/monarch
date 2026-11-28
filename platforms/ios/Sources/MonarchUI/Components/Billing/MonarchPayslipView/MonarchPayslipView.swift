import Foundation
import SwiftUI

/// An on-screen payslip: issuer and title, employee and period details, pay
/// lines grouped into earnings, deductions, reimbursements and employer
/// contributions, the gross-to-net summary and year-to-date figures.
///
/// ```swift
/// ScrollView {
///     MonarchPayslipView(
///         "Payslip",
///         issuer: "Northwind Studio",
///         subtitle: "March 2026",
///         details: [MonarchPayslipDetail("Employee", value: "Ada Lovelace")],
///         lines: [
///             MonarchPayslipLine("basic", name: "Basic", kind: .earning, amount: 4_000),
///             MonarchPayslipLine("tax", name: "Income tax", kind: .deduction, amount: 640),
///         ],
///         totals: MonarchPayslipTotals(gross: 4_000, deductions: 640, net: 3_360),
///         currencyCode: "GBP",
///         onDownload: { downloadPayslip() }
///     )
///     .padding()
/// }
/// ```
///
/// The payslip shows numbers payroll has already calculated and never
/// recalculates them. Each group is a ``MonarchSectionHeader`` with its
/// lines; the summary ends with net pay in a larger type role. The view does
/// not scroll; place it in a `ScrollView`. Download PDF appears with
/// `onDownload`; the app makes the file.
///
/// Accessibility: the payslip is a container named by its title, which is a
/// header, like each group heading. Each line is one element read as its
/// name, note and amount. Each summary row and year-to-date figure is read
/// as its label and amount. While `isDownloading`, the button shows a
/// spinner and is read as "Preparing the PDF".
public struct MonarchPayslipView<Actions: View>: View {
    private let title: String
    private let issuer: String?
    private let subtitle: String?
    private let status: MonarchStatusDefinition?
    private let details: [MonarchPayslipDetail]
    private let lines: [MonarchPayslipLine]
    private let totals: MonarchPayslipTotals
    private let yearToDate: [MonarchPayslipYearToDateItem]
    private let yearToDateTitle: String?
    private let currencyCode: String
    private let locale: Locale?
    private let hidesEmployerContributions: Bool
    private let isDownloading: Bool
    private let labels: MonarchPayslipViewLabels
    private let onDownload: (@MainActor () -> Void)?
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var environmentLocale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @ScaledMetric(relativeTo: .body) private var tileMinimumWidth = MonarchPayslipViewTokens.tileMinimumWidth

    private typealias Tokens = MonarchPayslipViewTokens

    /// A payslip with extra header actions, such as Print or Share.
    ///
    /// - Parameters:
    ///   - title: The heading, such as "Payslip" or "Final settlement".
    ///   - issuer: The organisation or legal entity that pays, above the title.
    ///   - subtitle: A short line under the title, such as the period.
    ///   - status: A status beside the title, such as "Paid".
    ///   - details: Employee and period facts.
    ///   - lines: The calculated lines, grouped by kind.
    ///   - totals: The totals from payroll.
    ///   - yearToDate: Year-to-date figures, shown as tiles.
    ///   - yearToDateTitle: Replaces the year-to-date heading, such as "Year to date (from April)".
    ///   - currencyCode: The ISO 4217 code of every amount.
    ///   - locale: The locale of amounts. Default: the environment's.
    ///   - hidesEmployerContributions: Leaves out employer contributions, for an employee's copy where policy requires it.
    ///   - isDownloading: Whether the download is being prepared; the button shows a spinner.
    ///   - labels: Replacements for the built-in strings.
    ///   - onDownload: Shows Download PDF, which runs this. The app makes the file.
    ///   - actions: Extra header actions after Download PDF.
    public init(
        _ title: String,
        issuer: String? = nil,
        subtitle: String? = nil,
        status: MonarchStatusDefinition? = nil,
        details: [MonarchPayslipDetail] = [],
        lines: [MonarchPayslipLine],
        totals: MonarchPayslipTotals,
        yearToDate: [MonarchPayslipYearToDateItem] = [],
        yearToDateTitle: String? = nil,
        currencyCode: String,
        locale: Locale? = nil,
        hidesEmployerContributions: Bool = false,
        isDownloading: Bool = false,
        labels: MonarchPayslipViewLabels = MonarchPayslipViewLabels(),
        onDownload: (@MainActor () -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.issuer = issuer
        self.subtitle = subtitle
        self.status = status
        self.details = details
        self.lines = lines
        self.totals = totals
        self.yearToDate = yearToDate
        self.yearToDateTitle = yearToDateTitle
        self.currencyCode = currencyCode
        self.locale = locale
        self.hidesEmployerContributions = hidesEmployerContributions
        self.isDownloading = isDownloading
        self.labels = labels
        self.onDownload = onDownload
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        let texts = Tokens.texts(labels)
        let groups = Tokens.groups(lines, hidesEmployerContributions: hidesEmployerContributions, texts: texts)
        VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
            header(texts: texts)
            if !details.isEmpty {
                detailsGrid
            }
            if groups.isEmpty {
                Text(texts.noLines)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                ForEach(groups, id: \.kind) { group in
                    groupView(group)
                }
            }
            summary(texts: texts)
            if !yearToDate.isEmpty {
                yearToDateView(texts: texts)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }

    private var resolvedLocale: Locale { locale ?? environmentLocale }

    // MARK: Header

    private func header(texts: MonarchPayslipViewTokens.Texts) -> some View {
        VStack(alignment: .leading, spacing: Tokens.headerSpacing) {
            if let issuer, !issuer.isEmpty {
                Text(issuer)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textMuted)
            }
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: Tokens.headerSpacing) {
                    titleText
                    statusBadge
                }
                VStack(alignment: .leading, spacing: Tokens.headerSpacing) {
                    titleText
                    statusBadge
                }
            }
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if onDownload != nil || Actions.self != EmptyView.self {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: Tokens.headerSpacing) { headerButtons(texts: texts) }
                    VStack(alignment: .leading, spacing: Tokens.headerSpacing) { headerButtons(texts: texts) }
                }
            }
        }
    }

    private var titleText: some View {
        Text(title)
            .monarchTextStyle(.title)
            .foregroundStyle(theme.colors.textDefault)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private var statusBadge: some View {
        if let status {
            MonarchStatusBadge(status, size: .sm)
        }
    }

    @ViewBuilder private func headerButtons(texts: MonarchPayslipViewTokens.Texts) -> some View {
        if let onDownload {
            MonarchButton(
                texts.download,
                systemImage: Tokens.downloadSymbol,
                variant: .secondary,
                size: .sm,
                isLoading: isDownloading,
                loadingLabel: texts.downloading,
                action: onDownload
            )
        }
        actions
    }

    // MARK: Details

    @ViewBuilder private var detailsGrid: some View {
        let columns = Tokens.detailColumns(
            isRegularWidth: horizontalSizeClass == .regular,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
        if columns == 1 {
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                ForEach(Array(details.enumerated()), id: \.offset) { _, detail in
                    LabeledContent(detail.label, value: detail.value)
                }
            }
            .labeledContentStyle(.monarch(.inline))
        } else {
            Grid(alignment: .topLeading, horizontalSpacing: Tokens.gridSpacing, verticalSpacing: Tokens.gridSpacing) {
                ForEach(Array(Tokens.rows(details, columns: columns).enumerated()), id: \.offset) { _, row in
                    GridRow {
                        ForEach(Array(row.enumerated()), id: \.offset) { _, detail in
                            LabeledContent(detail.label, value: detail.value)
                        }
                    }
                }
            }
            .labeledContentStyle(.monarch)
        }
    }

    // MARK: Groups

    private func groupView(_ group: MonarchPayslipViewTokens.Group) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            sectionTitle(group.title)
            // The lines sit in a bordered box with a divider between them, as on the web.
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(group.lines.enumerated()), id: \.element.id) { index, line in
                    if index > 0 {
                        MonarchDivider()
                    }
                    lineRow(line)
                        .padding(.horizontal, Tokens.tilePadding)
                        .padding(.vertical, Tokens.rowSpacing)
                }
            }
            .overlay(MonarchShapes.container.strokeBorder(theme.colors.borderSubtle, lineWidth: 1))
        }
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title)
            .monarchTextStyle(.caption, weight: .semibold)
            .textCase(.uppercase)
            .tracking(0.5)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityAddTraits(.isHeader)
    }

    private func lineRow(_ line: MonarchPayslipLine) -> some View {
        let name = VStack(alignment: .leading, spacing: Tokens.textSpacing) {
            Text(line.name)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
            if let note = line.note, !note.isEmpty {
                Text(note)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        let amount = MonarchMoney(line.amount, currencyCode: currencyCode, locale: resolvedLocale)
            .foregroundStyle(theme.colors.textDefault)
        return ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.amountGap) {
                name
                Spacer(minLength: 0)
                amount.fixedSize()
            }
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                name
                amount
            }
        }
        .accessibilityElement(children: .combine)
    }

    // MARK: Summary

    private func summary(texts: MonarchPayslipViewTokens.Texts) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(Array(Tokens.summary(totals, texts: texts).enumerated()), id: \.offset) { _, row in
                if row.isNet {
                    MonarchDivider()
                }
                summaryRow(row)
            }
        }
        .padding(Tokens.tilePadding)
        .background(theme.colors.bgSubtle, in: MonarchShapes.container)
    }

    private func summaryRow(_ row: MonarchPayslipViewTokens.SummaryRow) -> some View {
        let style = Tokens.textStyle(isNet: row.isNet)
        let label = Text(row.label)
            .monarchTextStyle(style.label)
            .foregroundStyle(theme.colors[role: Tokens.labelRole(isNet: row.isNet)])
        let amount = MonarchMoney(
            row.amount,
            currencyCode: currencyCode,
            locale: resolvedLocale,
            textStyle: style.amount
        )
        .foregroundStyle(theme.colors.textDefault)
        return ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.amountGap) {
                label
                Spacer(minLength: 0)
                amount.fixedSize()
            }
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                label
                amount
            }
        }
        .accessibilityElement(children: .combine)
    }

    // MARK: Year to date

    private func yearToDateView(texts: MonarchPayslipViewTokens.Texts) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            sectionTitle(yearToDateTitle ?? texts.yearToDate)
            LazyVGrid(
                columns: [
                    GridItem(.adaptive(minimum: tileMinimumWidth), spacing: Tokens.gridSpacing, alignment: .topLeading)
                ],
                alignment: .leading,
                spacing: Tokens.gridSpacing
            ) {
                ForEach(Array(yearToDate.enumerated()), id: \.offset) { _, item in
                    LabeledContent(item.label) {
                        MonarchMoney(item.amount, currencyCode: currencyCode, locale: resolvedLocale)
                    }
                    .labeledContentStyle(.monarch)
                    .padding(Tokens.tilePadding)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .overlay(MonarchShapes.container.strokeBorder(theme.colors.borderSubtle, lineWidth: 1))
                }
            }
        }
    }
}

extension MonarchPayslipView where Actions == EmptyView {
    /// A payslip.
    ///
    /// - Parameters:
    ///   - title: The heading, such as "Payslip" or "Final settlement".
    ///   - issuer: The organisation or legal entity that pays, above the title.
    ///   - subtitle: A short line under the title, such as the period.
    ///   - status: A status beside the title, such as "Paid".
    ///   - details: Employee and period facts.
    ///   - lines: The calculated lines, grouped by kind.
    ///   - totals: The totals from payroll.
    ///   - yearToDate: Year-to-date figures, shown as tiles.
    ///   - yearToDateTitle: Replaces the year-to-date heading.
    ///   - currencyCode: The ISO 4217 code of every amount.
    ///   - locale: The locale of amounts. Default: the environment's.
    ///   - hidesEmployerContributions: Leaves out employer contributions.
    ///   - isDownloading: Whether the download is being prepared; the button shows a spinner.
    ///   - labels: Replacements for the built-in strings.
    ///   - onDownload: Shows Download PDF, which runs this. The app makes the file.
    public init(
        _ title: String,
        issuer: String? = nil,
        subtitle: String? = nil,
        status: MonarchStatusDefinition? = nil,
        details: [MonarchPayslipDetail] = [],
        lines: [MonarchPayslipLine],
        totals: MonarchPayslipTotals,
        yearToDate: [MonarchPayslipYearToDateItem] = [],
        yearToDateTitle: String? = nil,
        currencyCode: String,
        locale: Locale? = nil,
        hidesEmployerContributions: Bool = false,
        isDownloading: Bool = false,
        labels: MonarchPayslipViewLabels = MonarchPayslipViewLabels(),
        onDownload: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            title,
            issuer: issuer,
            subtitle: subtitle,
            status: status,
            details: details,
            lines: lines,
            totals: totals,
            yearToDate: yearToDate,
            yearToDateTitle: yearToDateTitle,
            currencyCode: currencyCode,
            locale: locale,
            hidesEmployerContributions: hidesEmployerContributions,
            isDownloading: isDownloading,
            labels: labels,
            onDownload: onDownload
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchPayslipView") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchPayslipView(
                    "Payslip",
                    issuer: "Northwind Studio",
                    subtitle: "March 2026",
                    status: MonarchStatusDefinition("Paid", tone: .success),
                    details: [MonarchPayslipDetail("Employee", value: "Ada Lovelace")],
                    lines: [
                        MonarchPayslipLine("basic", name: "Basic", kind: .earning, amount: 4_000),
                        MonarchPayslipLine(
                            "tax",
                            name: "Income tax",
                            kind: .deduction,
                            amount: 640,
                            note: "Tax code 1257L"
                        ),
                        MonarchPayslipLine("pension", name: "Pension", kind: .employer, amount: 120),
                    ],
                    totals: MonarchPayslipTotals(gross: 4_000, deductions: 640, net: 3_360),
                    yearToDate: [MonarchPayslipYearToDateItem("Gross pay", amount: 12_000)],
                    currencyCode: "GBP",
                    onDownload: {}
                )
                MonarchPayslipView(
                    "Payslip",
                    lines: [],
                    totals: MonarchPayslipTotals(gross: 0, deductions: 0, net: 0),
                    currencyCode: "USD",
                    isDownloading: true,
                    onDownload: {}
                )
            }
        }
    }
#endif
