import MonarchUI
import SwiftUI

struct MonarchInvoiceViewSection: CataloguePage {
    static let title = "InvoiceView"

    @State private var isPaid = false
    @State private var hasDiscount = true

    private let lines = [
        MonarchInvoiceLineItem(
            "1",
            description: "Discovery workshop",
            detail: "Two sessions",
            quantity: 2,
            unitPrice: 400
        ),
        MonarchInvoiceLineItem("2", description: "Design system setup", quantity: 1, unitPrice: 1_200),
        MonarchInvoiceLineItem(
            "3",
            description: "Support hours",
            detail: "Billed hourly",
            quantity: 6.5,
            unitPrice: 80
        ),
    ]

    var body: some View {
        CatalogueCaption("Totals worked out from the lines; Print opens the system print panel")
        Toggle("Paid", isOn: $isPaid)
        Toggle("10% discount", isOn: $hasDiscount)
        MonarchInvoiceView(
            number: "INV-1042",
            status: isPaid
                ? MonarchStatusDefinition("Paid", tone: .success, systemImage: "checkmark.circle.fill")
                : MonarchStatusDefinition("Due", tone: .warning, systemImage: "clock.fill"),
            issueDate: Date(timeIntervalSince1970: 1_767_225_600),
            dueDate: Date(timeIntervalSince1970: 1_769_817_600),
            details: [MonarchInvoiceDetail("Terms", value: "Net 30")],
            from: MonarchInvoiceParty("Northwind Studio", details: ["12 Harbour Street", "Leeds LS1 4AB"]),
            to: MonarchInvoiceParty("Contoso Ltd", details: ["accounts@contoso.example"]),
            items: lines,
            currencyCode: "GBP",
            taxRate: 20,
            discount: hasDiscount ? .percent(10) : nil,
            notes: "Pay by bank transfer within 30 days.",
            footer: "Northwind Studio Ltd, registered in England and Wales.",
            isPrintButtonShown: true
        ) {
            MonarchButton("Send", systemImage: "paperplane", variant: .ghost, size: .sm) {}
        }

        CatalogueCaption("Totals from the billing system, with amount due")
        MonarchInvoiceView(
            "Tax invoice",
            number: "2026/0042",
            to: MonarchInvoiceParty("Asha Traders"),
            items: [MonarchInvoiceLineItem("1", description: "Annual licence", quantity: 1, unitPrice: 50_000)],
            currencyCode: "INR",
            totals: MonarchInvoiceTotals(tax: 9_000, total: 59_000, amountPaid: 20_000, amountDue: 39_000)
        )

        CatalogueCaption("No lines")
        MonarchInvoiceView(items: [], currencyCode: "USD")
    }
}
