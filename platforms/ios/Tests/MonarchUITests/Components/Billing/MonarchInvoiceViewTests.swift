import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let issued = Date(timeIntervalSince1970: 1_767_225_600)
private let lines = [
    MonarchInvoiceLineItem("1", description: "Discovery workshop", detail: "Two sessions", quantity: 2, unitPrice: 400),
    MonarchInvoiceLineItem("2", description: "Design system setup", quantity: 1, unitPrice: 1_200),
    MonarchInvoiceLineItem("3", description: "Support hours", detail: "Billed hourly", quantity: 6.5, unitPrice: 80),
]
private let from = MonarchInvoiceParty(
    "Northwind Studio",
    details: ["12 Harbour Street", "Leeds LS1 4AB", "VAT GB 123 4567 89"]
)
private let to = MonarchInvoiceParty("Contoso Ltd", details: ["accounts@contoso.example"])
private let english = Locale(identifier: "en_US")

enum MonarchInvoiceViewFixtures: ComponentFixtures {
    static let id = "invoice-view"
    static let fixtures = [
        Fixture("full invoice, worked-out totals, stacked lines") {
            MonarchInvoiceView(
                number: "INV-1042",
                status: MonarchStatusDefinition("Overdue", tone: .danger),
                issueDate: issued,
                dueDate: issued.addingTimeInterval(30 * 86_400),
                details: [MonarchInvoiceDetail("Terms", value: "Net 30")],
                from: from,
                to: to,
                items: lines,
                currencyCode: "GBP",
                taxRate: 20,
                discount: .percent(10),
                notes: "Pay by bank transfer to the account on file.",
                footer: "Northwind Studio Ltd, registered in England and Wales, no. 01234567.",
                isPrintButtonShown: true
            )
        },
        Fixture("table at regular width, given totals, paid and due") {
            MonarchInvoiceView(
                "Tax invoice",
                number: "2026/0042",
                status: MonarchStatusDefinition("Partly paid", tone: .warning),
                from: from,
                to: to,
                items: lines,
                currencyCode: "INR",
                totals: MonarchInvoiceTotals(
                    subtotal: 2_520,
                    discount: 0,
                    tax: 453.6,
                    total: 2_973.6,
                    amountPaid: 1_000,
                    amountDue: 1_973.6
                )
            ) {
                MonarchButton("Send", variant: .primary, size: .sm) {}
            }
            .environment(\.horizontalSizeClass, .regular)
        },
        Fixture("no lines, no parties, zero-decimal currency") {
            MonarchInvoiceView(items: [], currencyCode: "JPY")
        },
        Fixture("long text and custom labels") {
            MonarchInvoiceView(
                number: "INV-2026-000000000123",
                to: MonarchInvoiceParty(
                    "The International Society of Very Long Organisation Names and Associated Bodies"
                ),
                items: [
                    MonarchInvoiceLineItem(
                        "1",
                        description:
                            "Quarterly maintenance of the regional distribution network and all its subsystems",
                        quantity: 1_000_000,
                        unitPrice: 1_234.5678,
                        amount: 1_234_567_800
                    )
                ],
                currencyCode: "EUR",
                discount: .amount(50),
                labels: MonarchInvoiceViewLabels(title: "Rechnung", to: "Rechnungsempfänger", total: "Gesamt")
            )
        },
    ]
}

@Suite("MonarchInvoiceView")
struct MonarchInvoiceViewTests {
    private let texts = MonarchInvoiceViewTokens.texts(MonarchInvoiceViewLabels())

    @Test("Rounding is half away from zero at the currency's digits")
    func rounding() {
        #expect(
            MonarchInvoiceViewTokens.rounded(Decimal(string: "1.005")!, fractionDigits: 2) == Decimal(string: "1.01")!
        )
        #expect(
            MonarchInvoiceViewTokens.rounded(Decimal(string: "-1.005")!, fractionDigits: 2) == Decimal(string: "-1.01")!
        )
        #expect(MonarchInvoiceViewTokens.rounded(Decimal(string: "2.5")!, fractionDigits: 0) == 3)
        #expect(MonarchInvoiceViewTokens.rounded(Decimal(string: "1.004")!, fractionDigits: 2) == 1)
    }

    @Test("Lines, discount before tax, and tax are worked out like the web editor")
    func figures() {
        let figures = MonarchInvoiceViewTokens.figures(
            items: lines,
            currencyCode: "GBP",
            taxRate: 20,
            discount: .percent(10)
        )
        #expect(figures.lines == [800, 1_200, 520])
        #expect(figures.subtotal == 2_520)
        #expect(figures.discount == 252)
        #expect(figures.tax == Decimal(string: "453.6")!)
        #expect(figures.total == Decimal(string: "2721.6")!)
    }

    @Test("Each line is rounded to the currency; a zero-decimal currency rounds to whole units")
    func lineRounding() {
        let items = [
            MonarchInvoiceLineItem("1", description: "Widget", quantity: 3, unitPrice: Decimal(string: "0.335")!)
        ]
        #expect(
            MonarchInvoiceViewTokens.figures(items: items, currencyCode: "USD", taxRate: nil, discount: nil).lines == [
                Decimal(string: "1.01")!
            ]
        )
        let yen = [MonarchInvoiceLineItem("1", description: "Widget", quantity: 3, unitPrice: Decimal(string: "33.5")!)]
        #expect(
            MonarchInvoiceViewTokens.figures(items: yen, currencyCode: "JPY", taxRate: nil, discount: nil).lines == [
                101
            ]
        )
    }

    @Test("A discount is capped at the subtotal and a percentage at 100")
    func discountCaps() {
        let items = [MonarchInvoiceLineItem("1", description: "Widget", quantity: 1, unitPrice: 100)]
        let amount = MonarchInvoiceViewTokens.figures(
            items: items,
            currencyCode: "USD",
            taxRate: 10,
            discount: .amount(500)
        )
        #expect(amount.discount == 100)
        #expect(amount.tax == 0)
        #expect(amount.total == 0)
        let percent = MonarchInvoiceViewTokens.figures(
            items: items,
            currencyCode: "USD",
            taxRate: nil,
            discount: .percent(150)
        )
        #expect(percent.discount == 100)
        let negative = MonarchInvoiceViewTokens.figures(
            items: items,
            currencyCode: "USD",
            taxRate: nil,
            discount: .amount(-5)
        )
        #expect(negative.discount == 0)
    }

    @Test("A line shows the billing system's amount when given")
    func lineAmount() {
        let given = MonarchInvoiceLineItem("1", description: "Widget", quantity: 2, unitPrice: 10, amount: 19)
        let figures = MonarchInvoiceViewTokens.figures(items: [given], currencyCode: "USD", taxRate: nil, discount: nil)
        #expect(MonarchInvoiceViewTokens.lineAmount(given, index: 0, figures: figures) == 19)
        let worked = MonarchInvoiceLineItem("2", description: "Widget", quantity: 2, unitPrice: 10)
        #expect(MonarchInvoiceViewTokens.lineAmount(worked, index: 0, figures: figures) == 20)
    }

    @Test("Totals rows: given values win; optional rows appear only when they apply")
    func totalRows() {
        let figures = MonarchInvoiceViewTokens.figures(
            items: lines,
            currencyCode: "GBP",
            taxRate: 20,
            discount: .percent(10)
        )
        let worked = MonarchInvoiceViewTokens.totalRows(
            totals: MonarchInvoiceTotals(),
            figures: figures,
            taxRate: 20,
            texts: texts,
            locale: english
        )
        #expect(worked.map(\.kind) == [.subtotal, .discount, .tax, .total])
        #expect(worked[1].amount == -252)
        #expect(worked[2].label == "Tax (20%)")

        let given = MonarchInvoiceViewTokens.totalRows(
            totals: MonarchInvoiceTotals(discount: 0, tax: 10, total: 999, amountPaid: 500, amountDue: 499),
            figures: figures,
            taxRate: 20,
            texts: texts,
            locale: english
        )
        #expect(given.map(\.kind) == [.subtotal, .tax, .total, .amountPaid, .amountDue])
        #expect(given[1].label == "Tax")
        #expect(given[2].amount == 999)

        let plain = MonarchInvoiceViewTokens.totalRows(
            totals: MonarchInvoiceTotals(),
            figures: MonarchInvoiceViewTokens.figures(items: lines, currencyCode: "GBP", taxRate: nil, discount: nil),
            taxRate: nil,
            texts: texts,
            locale: english
        )
        #expect(plain.map(\.kind) == [.subtotal, .total])
    }

    @Test("The tax rate keeps up to three decimals")
    func rateText() {
        #expect(MonarchInvoiceViewTokens.rateText(Decimal(string: "7.25")!, locale: english) == "7.25%")
        #expect(MonarchInvoiceViewTokens.rateText(18, locale: english) == "18%")
        #expect(MonarchInvoiceViewTokens.rateText(Decimal(string: "8.8756")!, locale: english) == "8.876%")
    }

    @Test("The total stands out by type and colour role, not colour alone")
    func totalAppearance() {
        #expect(MonarchInvoiceViewTokens.textStyle(.total) == (.bodyStrong, .subheading))
        #expect(MonarchInvoiceViewTokens.textStyle(.amountDue) == (.bodyStrong, .bodyStrong))
        #expect(MonarchInvoiceViewTokens.textStyle(.subtotal) == (.body, .body))
        #expect(MonarchInvoiceViewTokens.labelRole(.total) == \MonarchColorTokens.textDefault)
        #expect(MonarchInvoiceViewTokens.labelRole(.tax) == \MonarchColorTokens.textMuted)
        #expect(MonarchInvoiceViewTokens.hasDivider(.total))
        #expect(!MonarchInvoiceViewTokens.hasDivider(.subtotal))
    }

    @Test("The heading is the title and the number")
    func heading() {
        #expect(MonarchInvoiceViewTokens.heading(title: nil, number: "INV-1042", texts: texts) == "Invoice INV-1042")
        #expect(MonarchInvoiceViewTokens.heading(title: "Credit note", number: nil, texts: texts) == "Credit note")
        #expect(MonarchInvoiceViewTokens.heading(title: nil, number: "", texts: texts) == "Invoice")
    }

    @Test("Facts are the dates, then the app's details")
    func facts() {
        let facts = MonarchInvoiceViewTokens.facts(
            issueDate: issued,
            dueDate: nil,
            details: [MonarchInvoiceDetail("Terms", value: "Net 30")],
            texts: texts,
            locale: english,
            timeZone: TimeZone(identifier: "UTC")!
        )
        #expect(facts.map(\.label) == ["Issue date", "Terms"])
        #expect(facts[0].value == "January 1, 2026")
    }

    @Test("A line is read as its description, detail and every column with its name")
    func lineLabel() {
        let label = MonarchInvoiceViewTokens.lineLabel(
            lines[0],
            quantity: "2",
            unitPrice: "£400.00",
            amount: "£800.00",
            texts: texts
        )
        #expect(label == "Discovery workshop, Two sessions, Qty 2, Unit price £400.00, Amount £800.00")
        #expect(
            MonarchInvoiceViewTokens.lineLabel(
                lines[1],
                quantity: "1",
                unitPrice: "£1,200.00",
                amount: "£1,200.00",
                texts: texts
            )
                == "Design system setup, Qty 1, Unit price £1,200.00, Amount £1,200.00"
        )
        #expect(
            MonarchInvoiceViewTokens.stackedMeta(quantity: "6.5", unitPrice: "£80.00", texts: texts)
                == "Qty 6.5 · Unit price £80.00"
        )
        #expect(MonarchInvoiceViewTokens.quantityText(Decimal(string: "6.12346")!, locale: english) == "6.1235")
    }

    @Test("Lines stack at compact width and at accessibility sizes")
    func layout() {
        #expect(MonarchInvoiceViewTokens.usesStackedRows(isRegularWidth: false, isAccessibilitySize: false))
        #expect(MonarchInvoiceViewTokens.usesStackedRows(isRegularWidth: true, isAccessibilitySize: true))
        #expect(!MonarchInvoiceViewTokens.usesStackedRows(isRegularWidth: true, isAccessibilitySize: false))
    }

    @Test("Print shows only when asked for and when something can print")
    func printButton() {
        #expect(MonarchInvoiceViewTokens.showsPrintButton(isShown: true, hasHandler: false, canPrint: true))
        #expect(MonarchInvoiceViewTokens.showsPrintButton(isShown: true, hasHandler: true, canPrint: false))
        #expect(!MonarchInvoiceViewTokens.showsPrintButton(isShown: true, hasHandler: false, canPrint: false))
        #expect(!MonarchInvoiceViewTokens.showsPrintButton(isShown: false, hasHandler: true, canPrint: true))
    }

    @Test("Pages are counted by rounding up")
    func pageCount() {
        #expect(MonarchInvoiceViewTokens.pageCount(contentHeight: 100, pageHeight: 700) == 1)
        #expect(MonarchInvoiceViewTokens.pageCount(contentHeight: 1_401, pageHeight: 700) == 3)
        #expect(MonarchInvoiceViewTokens.pageCount(contentHeight: 0, pageHeight: 700) == 1)
    }

    @Test("The printed copy is a PDF with a page per page height")
    @MainActor
    func printedPDF() throws {
        let short = MonarchInvoicePrinter.pdf(MonarchInvoiceView(items: lines, currencyCode: "GBP"))
        let data = try #require(short)
        #expect(data.starts(with: Data("%PDF".utf8)))
        let document = try #require(CGPDFDocument(CGDataProvider(data: data as CFData)!))
        #expect(document.numberOfPages == 1)

        let many = (0..<80).map {
            MonarchInvoiceLineItem("\($0)", description: "Line \($0)", quantity: 1, unitPrice: 10)
        }
        let long = try #require(
            MonarchInvoicePrinter.pdf(
                MonarchInvoiceDocument(
                    content: MonarchInvoiceContent(
                        details: [],
                        items: many,
                        currencyCode: "USD",
                        totals: MonarchInvoiceTotals()
                    ),
                    labels: MonarchInvoiceViewLabels(),
                    isStacked: false,
                    actions: EmptyView()
                )
            )
        )
        let longDocument = try #require(CGPDFDocument(CGDataProvider(data: long as CFData)!))
        #expect(longDocument.numberOfPages > 1)
    }

    @Test("Replacements win over the built-in strings")
    func labels() {
        let custom = MonarchInvoiceViewTokens.texts(
            MonarchInvoiceViewLabels(title: "Rechnung", taxWithRate: { "MwSt. \($0)" })
        )
        #expect(custom.title == "Rechnung")
        #expect(custom.taxWithRate("19%") == "MwSt. 19%")
        #expect(custom.total == "Total")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchInvoiceViewStrings.title == "Invoice")
        #expect(MonarchInvoiceViewStrings.from == "From")
        #expect(MonarchInvoiceViewStrings.to == "Bill to")
        #expect(MonarchInvoiceViewStrings.issueDate == "Issue date")
        #expect(MonarchInvoiceViewStrings.dueDate == "Due date")
        #expect(MonarchInvoiceViewStrings.lineItems == "Line items")
        #expect(MonarchInvoiceViewStrings.description == "Description")
        #expect(MonarchInvoiceViewStrings.quantity == "Qty")
        #expect(MonarchInvoiceViewStrings.unitPrice == "Unit price")
        #expect(MonarchInvoiceViewStrings.amount == "Amount")
        #expect(MonarchInvoiceViewStrings.subtotal == "Subtotal")
        #expect(MonarchInvoiceViewStrings.discount == "Discount")
        #expect(MonarchInvoiceViewStrings.tax == "Tax")
        #expect(MonarchInvoiceViewStrings.taxWithRate("18%") == "Tax (18%)")
        #expect(MonarchInvoiceViewStrings.total == "Total")
        #expect(MonarchInvoiceViewStrings.amountPaid == "Amount paid")
        #expect(MonarchInvoiceViewStrings.amountDue == "Amount due")
        #expect(MonarchInvoiceViewStrings.noItems == "No line items.")
        #expect(MonarchInvoiceViewStrings.notes == "Notes")
        #expect(MonarchInvoiceViewStrings.print == "Print")
    }

    // The size class can be set only on iOS; the host ignores it.
    #if os(iOS)
        @Test("The table at regular width is shorter than stacked lines")
        @MainActor
        func tableIsShorter() {
            let stacked = Host.layout(MonarchInvoiceView(items: lines, currencyCode: "GBP"))
            let table = Host.layout(
                MonarchInvoiceView(items: lines, currencyCode: "GBP").environment(\.horizontalSizeClass, .regular)
            )
            #expect(table.height < stacked.height)
        }
    #endif
}
