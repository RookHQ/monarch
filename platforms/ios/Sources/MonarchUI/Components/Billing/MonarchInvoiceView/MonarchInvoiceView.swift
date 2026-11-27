import Foundation
import SwiftUI

/// A read-only invoice: the issuer and the billed party, number, dates and
/// status, the billed lines, totals, notes and small print, with an optional
/// Print button.
///
/// ```swift
/// ScrollView {
///     MonarchInvoiceView(
///         number: "INV-1042",
///         status: MonarchStatusDefinition("Paid", tone: .success),
///         issueDate: invoice.issuedAt,
///         dueDate: invoice.dueAt,
///         from: MonarchInvoiceParty("Northwind Studio", details: ["12 Harbour Street", "Leeds"]),
///         to: MonarchInvoiceParty("Contoso Ltd"),
///         items: invoice.lines.map {
///             MonarchInvoiceLineItem($0.id, description: $0.name, quantity: $0.quantity, unitPrice: $0.price)
///         },
///         currencyCode: "GBP",
///         taxRate: 20,
///         isPrintButtonShown: true
///     )
///     .padding()
/// }
/// ```
///
/// Totals the billing system has calculated (`totals`) are shown exactly.
/// Anything left out is worked out from the lines: each line rounded to the
/// currency, the discount on the subtotal and capped at it, tax on what is
/// left. At compact width and at the accessibility text sizes each line is
/// a block with its quantity and unit price under the description; at
/// regular width the lines are a table. The view does not scroll; place it
/// in a `ScrollView`.
///
/// Print renders the invoice to PDF pages with SwiftUI's `ImageRenderer`, in
/// the light theme at the default text size and without the actions, and
/// opens the system print panel, where it can also be saved as a PDF. Pass
/// `onPrint` to print something else, such as the billing system's own PDF.
///
/// Accessibility: the invoice is a container named by its heading
/// ("Invoice INV-1042"), which is a header, like "From", "Bill to", "Line
/// items" and "Notes". Each line is one element read as its description and
/// detail, then "Qty", "Unit price" and "Amount" with their values, in both
/// layouts. Each total is read as its label and amount. The status badge has
/// a label and an icon, so it never depends on colour.
public struct MonarchInvoiceView<Actions: View>: View {
    private let content: MonarchInvoiceContent
    private let isPrintButtonShown: Bool
    private let labels: MonarchInvoiceViewLabels
    private let onPrint: (@MainActor () -> Void)?
    private let actions: Actions

    @Environment(\.locale) private var environmentLocale
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.timeZone) private var timeZone

    /// An invoice with extra header actions, such as Download PDF or Send.
    ///
    /// - Parameters:
    ///   - title: Replaces the built-in title "Invoice".
    ///   - number: The invoice number, shown after the title and part of the invoice's name.
    ///   - status: The invoice status beside the title, such as "Paid" or "Overdue".
    ///   - issueDate: The issue date, written as a long date.
    ///   - dueDate: The due date, written as a long date.
    ///   - details: Further facts after the dates: terms, purchase order, project.
    ///   - brand: A logo shown above the title. Decorative.
    ///   - from: Who issues the invoice.
    ///   - to: Who is billed.
    ///   - items: The billed lines.
    ///   - currencyCode: The ISO 4217 code of every amount.
    ///   - locale: The locale of amounts and quantities; dates follow the environment. Default: the environment's.
    ///   - taxRate: The tax rate in percent, used when `totals.tax` is not given.
    ///   - discount: The discount, used when `totals.discount` is not given.
    ///   - totals: Totals from the billing system; they win over anything worked out.
    ///   - notes: Payment instructions, terms or a thank-you note, under the totals.
    ///   - footer: Small print at the bottom, such as the registered office.
    ///   - isPrintButtonShown: Whether a Print button is shown in the header.
    ///   - labels: Replacements for the built-in strings.
    ///   - onPrint: Replaces the built-in printing for the Print button.
    ///   - actions: Extra header actions, left out of the printed copy.
    public init(
        _ title: String? = nil,
        number: String? = nil,
        status: MonarchStatusDefinition? = nil,
        issueDate: Date? = nil,
        dueDate: Date? = nil,
        details: [MonarchInvoiceDetail] = [],
        brand: Image? = nil,
        from: MonarchInvoiceParty? = nil,
        to: MonarchInvoiceParty? = nil,
        items: [MonarchInvoiceLineItem],
        currencyCode: String,
        locale: Locale? = nil,
        taxRate: Decimal? = nil,
        discount: MonarchInvoiceDiscount? = nil,
        totals: MonarchInvoiceTotals = MonarchInvoiceTotals(),
        notes: String? = nil,
        footer: String? = nil,
        isPrintButtonShown: Bool = false,
        labels: MonarchInvoiceViewLabels = MonarchInvoiceViewLabels(),
        onPrint: (@MainActor () -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.content = MonarchInvoiceContent(
            title: title,
            number: number,
            status: status,
            issueDate: issueDate,
            dueDate: dueDate,
            details: details,
            brand: brand,
            from: from,
            to: to,
            items: items,
            currencyCode: currencyCode,
            locale: locale,
            taxRate: taxRate,
            discount: discount,
            totals: totals,
            notes: notes,
            footer: footer
        )
        self.isPrintButtonShown = isPrintButtonShown
        self.labels = labels
        self.onPrint = onPrint
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        MonarchInvoiceDocument(content: content, labels: labels, isStacked: nil, actions: header)
    }

    private var showsPrint: Bool {
        MonarchInvoiceViewTokens.showsPrintButton(
            isShown: isPrintButtonShown,
            hasHandler: onPrint != nil,
            canPrint: MonarchInvoicePrinter.canPrint
        )
    }

    @ViewBuilder private var header: some View {
        if showsPrint || Actions.self != EmptyView.self {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchInvoiceViewTokens.headerSpacing) { headerButtons }
                VStack(alignment: .leading, spacing: MonarchInvoiceViewTokens.headerSpacing) { headerButtons }
            }
        }
    }

    @ViewBuilder private var headerButtons: some View {
        if showsPrint {
            MonarchButton(
                labels.print ?? MonarchInvoiceViewStrings.print,
                variant: .secondary,
                size: .sm,
                action: printInvoice
            )
        }
        actions
    }

    private func printInvoice() {
        if let onPrint {
            onPrint()
            return
        }
        #if os(iOS)
            let texts = MonarchInvoiceViewTokens.texts(labels)
            let printed = MonarchInvoiceDocument(
                content: content,
                labels: labels,
                isStacked: false,
                actions: EmptyView()
            )
            .environment(\.monarchTheme, .light)
            .environment(\.colorScheme, .light)
            .environment(\.dynamicTypeSize, .large)
            .environment(\.locale, content.locale ?? environmentLocale)
            .environment(\.timeZone, timeZone)
            .environment(\.layoutDirection, layoutDirection)
            if let pdf = MonarchInvoicePrinter.pdf(printed) {
                MonarchInvoicePrinter.present(
                    pdf,
                    jobName: MonarchInvoiceViewTokens.heading(
                        title: content.title,
                        number: content.number,
                        texts: texts
                    )
                )
            }
        #endif
    }
}

extension MonarchInvoiceView where Actions == EmptyView {
    /// An invoice.
    ///
    /// - Parameters:
    ///   - title: Replaces the built-in title "Invoice".
    ///   - number: The invoice number, shown after the title and part of the invoice's name.
    ///   - status: The invoice status beside the title, such as "Paid" or "Overdue".
    ///   - issueDate: The issue date, written as a long date.
    ///   - dueDate: The due date, written as a long date.
    ///   - details: Further facts after the dates: terms, purchase order, project.
    ///   - brand: A logo shown above the title. Decorative.
    ///   - from: Who issues the invoice.
    ///   - to: Who is billed.
    ///   - items: The billed lines.
    ///   - currencyCode: The ISO 4217 code of every amount.
    ///   - locale: The locale of amounts and quantities; dates follow the environment. Default: the environment's.
    ///   - taxRate: The tax rate in percent, used when `totals.tax` is not given.
    ///   - discount: The discount, used when `totals.discount` is not given.
    ///   - totals: Totals from the billing system; they win over anything worked out.
    ///   - notes: Payment instructions, terms or a thank-you note, under the totals.
    ///   - footer: Small print at the bottom, such as the registered office.
    ///   - isPrintButtonShown: Whether a Print button is shown in the header.
    ///   - labels: Replacements for the built-in strings.
    ///   - onPrint: Replaces the built-in printing for the Print button.
    public init(
        _ title: String? = nil,
        number: String? = nil,
        status: MonarchStatusDefinition? = nil,
        issueDate: Date? = nil,
        dueDate: Date? = nil,
        details: [MonarchInvoiceDetail] = [],
        brand: Image? = nil,
        from: MonarchInvoiceParty? = nil,
        to: MonarchInvoiceParty? = nil,
        items: [MonarchInvoiceLineItem],
        currencyCode: String,
        locale: Locale? = nil,
        taxRate: Decimal? = nil,
        discount: MonarchInvoiceDiscount? = nil,
        totals: MonarchInvoiceTotals = MonarchInvoiceTotals(),
        notes: String? = nil,
        footer: String? = nil,
        isPrintButtonShown: Bool = false,
        labels: MonarchInvoiceViewLabels = MonarchInvoiceViewLabels(),
        onPrint: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            title,
            number: number,
            status: status,
            issueDate: issueDate,
            dueDate: dueDate,
            details: details,
            brand: brand,
            from: from,
            to: to,
            items: items,
            currencyCode: currencyCode,
            locale: locale,
            taxRate: taxRate,
            discount: discount,
            totals: totals,
            notes: notes,
            footer: footer,
            isPrintButtonShown: isPrintButtonShown,
            labels: labels,
            onPrint: onPrint
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchInvoiceView") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchInvoiceView(
                    number: "INV-1042",
                    status: MonarchStatusDefinition("Paid", tone: .success),
                    issueDate: Date(timeIntervalSince1970: 1_767_225_600),
                    from: MonarchInvoiceParty("Northwind Studio", details: ["12 Harbour Street", "Leeds"]),
                    to: MonarchInvoiceParty("Contoso Ltd"),
                    items: [
                        MonarchInvoiceLineItem(
                            "1",
                            description: "Discovery workshop",
                            detail: "Two sessions",
                            quantity: 2,
                            unitPrice: 400
                        ),
                        MonarchInvoiceLineItem("2", description: "Support hours", quantity: 6.5, unitPrice: 80),
                    ],
                    currencyCode: "GBP",
                    taxRate: 20,
                    discount: .percent(10),
                    totals: MonarchInvoiceTotals(amountPaid: 0),
                    notes: "Pay within 30 days.",
                    isPrintButtonShown: true
                )
                MonarchInvoiceView(items: [], currencyCode: "USD")
            }
        }
    }
#endif
