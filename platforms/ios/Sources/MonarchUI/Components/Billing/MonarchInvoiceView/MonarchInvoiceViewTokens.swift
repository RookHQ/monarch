import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchInvoiceView``.
enum MonarchInvoiceViewTokens {
    /// The gap between the header, the parties, the lines, the totals and the notes.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between blocks inside a section, such as the two parties.
    static let blockSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between rows of lines and totals.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between table columns.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between a label and its value, or a name and its details.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the title, the status and the actions.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The smallest gap between a total's label and its amount.
    static let amountGap: CGFloat = MonarchTokens.Space.space4
    /// The height of the brand image at the default text size; it scales.
    static let brandHeight: CGFloat = MonarchTokens.Space.space8
    /// The SF Symbol of the print button.
    static let printSymbol = "printer"
    /// The printed page: A4 in points. The print system scales it to the paper.
    static let printPageSize = CGSize(width: 595, height: 842)
    /// The margin around the printed content.
    static let printMargin: CGFloat = MonarchTokens.Space.space10

    /// Every built-in string, with the app's replacements applied.
    struct Texts {
        var title: String
        var from: String
        var to: String
        var issueDate: String
        var dueDate: String
        var lineItems: String
        var description: String
        var quantity: String
        var unitPrice: String
        var amount: String
        var subtotal: String
        var discount: String
        var tax: String
        var taxWithRate: (String) -> String
        var total: String
        var amountPaid: String
        var amountDue: String
        var noItems: String
        var notes: String
        var print: String
    }

    /// The strings to show: each replacement, else the built-in string.
    static func texts(_ labels: MonarchInvoiceViewLabels) -> Texts {
        typealias Built = MonarchInvoiceViewStrings
        return Texts(
            title: labels.title ?? Built.title,
            from: labels.from ?? Built.from,
            to: labels.to ?? Built.to,
            issueDate: labels.issueDate ?? Built.issueDate,
            dueDate: labels.dueDate ?? Built.dueDate,
            lineItems: labels.lineItems ?? Built.lineItems,
            description: labels.description ?? Built.description,
            quantity: labels.quantity ?? Built.quantity,
            unitPrice: labels.unitPrice ?? Built.unitPrice,
            amount: labels.amount ?? Built.amount,
            subtotal: labels.subtotal ?? Built.subtotal,
            discount: labels.discount ?? Built.discount,
            tax: labels.tax ?? Built.tax,
            taxWithRate: labels.taxWithRate ?? Built.taxWithRate,
            total: labels.total ?? Built.total,
            amountPaid: labels.amountPaid ?? Built.amountPaid,
            amountDue: labels.amountDue ?? Built.amountDue,
            noItems: labels.noItems ?? Built.noItems,
            notes: labels.notes ?? Built.notes,
            print: labels.print ?? Built.print
        )
    }

    // MARK: Figures

    /// The worked-out figures, each rounded to the currency's minor unit.
    struct Figures: Equatable {
        /// quantity × unit price per line, rounded.
        var lines: [Decimal]
        var subtotal: Decimal
        var discount: Decimal
        var tax: Decimal
        var total: Decimal
    }

    /// Rounds to `fractionDigits` decimal places, half away from zero.
    static func rounded(_ value: Decimal, fractionDigits: Int) -> Decimal {
        var magnitude = value < 0 ? -value : value
        var result = Decimal()
        NSDecimalRound(&result, &magnitude, fractionDigits, .plain)
        return value < 0 ? -result : result
    }

    /// Works out the line amounts and totals the same way as the web
    /// LineItemsEditor: each line is rounded to the currency, the discount
    /// applies to the subtotal and is capped at it, and tax applies to what
    /// is left.
    static func figures(
        items: [MonarchInvoiceLineItem],
        currencyCode: String,
        taxRate: Decimal?,
        discount: MonarchInvoiceDiscount?
    ) -> Figures {
        let digits = MonarchMoneyTokens.fractionDigits(currencyCode: currencyCode)
        let lines = items.map { rounded($0.quantity * $0.unitPrice, fractionDigits: digits) }
        let subtotal = lines.reduce(0, +)
        var off: Decimal = 0
        switch discount {
        case .percent(let percent)? where percent > 0:
            off = rounded(subtotal * min(percent, 100) / 100, fractionDigits: digits)
        case .amount(let amount)? where amount > 0:
            off = rounded(amount, fractionDigits: digits)
        default:
            break
        }
        off = max(0, min(off, max(subtotal, 0)))
        let taxable = subtotal - off
        let tax = taxRate.map { rounded(taxable * $0 / 100, fractionDigits: digits) } ?? 0
        return Figures(lines: lines, subtotal: subtotal, discount: off, tax: tax, total: taxable + tax)
    }

    /// The amount a line shows: the billing system's, else the worked-out one.
    static func lineAmount(_ item: MonarchInvoiceLineItem, index: Int, figures: Figures) -> Decimal {
        item.amount ?? (figures.lines.indices.contains(index) ? figures.lines[index] : 0)
    }

    /// A row of the totals block.
    enum TotalKind: Equatable {
        case subtotal, discount, tax, total, amountPaid, amountDue
    }

    /// One row of the totals block, with the amount to show.
    struct TotalRow: Equatable {
        let kind: TotalKind
        let label: String
        let amount: Decimal
    }

    /// The tax rate as shown in the tax label, such as "18%" or "7.25%".
    static func rateText(_ rate: Decimal, locale: Locale) -> String {
        let number = (rate as NSDecimalNumber).doubleValue / 100
        return number.formatted(.percent.precision(.fractionLength(0...3)).locale(locale))
    }

    /// The rows of the totals block. Given totals win; the rest is worked
    /// out. The discount is shown only when it is not zero, the tax when it
    /// is given or there is a rate, and the amount paid and due only when
    /// given. The discount is shown as a negative amount. The tax label has
    /// the rate only when the tax is worked out from it.
    static func totalRows(
        totals: MonarchInvoiceTotals,
        figures: Figures,
        taxRate: Decimal?,
        texts: Texts,
        locale: Locale
    ) -> [TotalRow] {
        var rows = [TotalRow(kind: .subtotal, label: texts.subtotal, amount: totals.subtotal ?? figures.subtotal)]
        let discount = totals.discount ?? figures.discount
        if discount != 0 {
            rows.append(TotalRow(kind: .discount, label: texts.discount, amount: -discount))
        }
        if totals.tax != nil || taxRate != nil {
            let label = totals.tax == nil ? taxRate.map { texts.taxWithRate(rateText($0, locale: locale)) } : nil
            rows.append(TotalRow(kind: .tax, label: label ?? texts.tax, amount: totals.tax ?? figures.tax))
        }
        rows.append(TotalRow(kind: .total, label: texts.total, amount: totals.total ?? figures.total))
        if let paid = totals.amountPaid {
            rows.append(TotalRow(kind: .amountPaid, label: texts.amountPaid, amount: paid))
        }
        if let due = totals.amountDue {
            rows.append(TotalRow(kind: .amountDue, label: texts.amountDue, amount: due))
        }
        return rows
    }

    /// The type role of a total row's label and amount: the total is the
    /// largest, the amount due is strong, the rest are body text.
    static func textStyle(_ kind: TotalKind) -> (label: MonarchTextStyle, amount: MonarchTextStyle) {
        switch kind {
        case .total: (.bodyStrong, .subheading)
        case .amountDue: (.bodyStrong, .bodyStrong)
        default: (.body, .body)
        }
    }

    /// The colour of a total row's label: the total and the amount due in
    /// the default text colour, the others muted.
    static func labelRole(_ kind: TotalKind) -> MonarchColorRole {
        switch kind {
        case .total, .amountDue: \.textDefault
        default: \.textMuted
        }
    }

    /// Whether a divider is drawn above the row: only above the total.
    static func hasDivider(_ kind: TotalKind) -> Bool {
        kind == .total
    }

    // MARK: Text

    /// The heading: the title, then the invoice number.
    static func heading(title: String?, number: String?, texts: Texts) -> String {
        let title = title ?? texts.title
        guard let number, !number.isEmpty else { return title }
        return "\(title) \(number)"
    }

    /// A quantity as text, with up to four decimals.
    static func quantityText(_ quantity: Decimal, locale: Locale) -> String {
        quantity.formatted(.number.precision(.fractionLength(0...4)).locale(locale))
    }

    /// A date as the facts show it, such as "12 March 2026".
    static func dateText(_ date: Date, locale: Locale, timeZone: TimeZone) -> String {
        var style = Date.FormatStyle(date: .long, time: .omitted).locale(locale)
        style.timeZone = timeZone
        return date.formatted(style)
    }

    /// The facts: issue date, due date, then the app's details.
    static func facts(
        issueDate: Date?,
        dueDate: Date?,
        details: [MonarchInvoiceDetail],
        texts: Texts,
        locale: Locale,
        timeZone: TimeZone
    ) -> [MonarchInvoiceDetail] {
        var facts: [MonarchInvoiceDetail] = []
        if let issueDate {
            facts.append(
                MonarchInvoiceDetail(texts.issueDate, value: dateText(issueDate, locale: locale, timeZone: timeZone))
            )
        }
        if let dueDate {
            facts.append(
                MonarchInvoiceDetail(texts.dueDate, value: dateText(dueDate, locale: locale, timeZone: timeZone))
            )
        }
        return facts + details
    }

    /// The line under a stacked line's description: "Qty 2 · Unit price $400.00".
    static func stackedMeta(quantity: String, unitPrice: String, texts: Texts) -> String {
        "\(texts.quantity) \(quantity) · \(texts.unitPrice) \(unitPrice)"
    }

    /// What VoiceOver reads for a line, in both layouts: the description,
    /// the detail, then each column with its name.
    static func lineLabel(
        _ item: MonarchInvoiceLineItem,
        quantity: String,
        unitPrice: String,
        amount: String,
        texts: Texts
    ) -> String {
        [
            item.description,
            item.detail,
            "\(texts.quantity) \(quantity)",
            "\(texts.unitPrice) \(unitPrice)",
            "\(texts.amount) \(amount)",
        ]
        .compactMap { $0 }
        .filter { !$0.isEmpty }
        .joined(separator: ", ")
    }

    // MARK: Layout and printing

    /// Whether lines are stacked blocks instead of table rows: at compact
    /// width and at the accessibility text sizes. Printing always uses the table.
    static func usesStackedRows(isRegularWidth: Bool, isAccessibilitySize: Bool) -> Bool {
        !isRegularWidth || isAccessibilitySize
    }

    /// Whether the print button is shown: when asked for, and only where
    /// it can do something (the app's handler, or the system print panel on iOS).
    static func showsPrintButton(isShown: Bool, hasHandler: Bool, canPrint: Bool) -> Bool {
        isShown && (hasHandler || canPrint)
    }

    /// The number of pages a content height needs at a page content height.
    static func pageCount(contentHeight: CGFloat, pageHeight: CGFloat) -> Int {
        guard contentHeight > 0, pageHeight > 0 else { return 1 }
        return max(1, Int((contentHeight / pageHeight).rounded(.up)))
    }
}
