@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchLineItemsEditorSection: CataloguePage {
    static let title = "LineItemsEditor"

    @State private var items = [
        MonarchLineItem(id: "r1", description: "Discovery workshop", quantity: 1, unitPrice: 800),
        MonarchLineItem(id: "r2", description: "Design hours", quantity: 12, unitPrice: 95),
        MonarchLineItem(id: "r3", description: "Hosting (monthly)", quantity: 3, unitPrice: Decimal(string: "24.50")),
    ]
    @State private var taxRate: Decimal = 18
    @State private var discount = MonarchLineItemsDiscount(.percent, value: 5)
    @State private var isReadOnly = false
    @State private var emptyItems: [MonarchLineItem] = []

    var body: some View {
        CatalogueCaption("An invoice in a Form: swipe a row to remove it, or use its button")
        Toggle("Sent (read-only)", isOn: $isReadOnly)
        Form {
            MonarchLineItemsEditor(
                items: $items,
                currencyCode: "USD",
                taxRate: $taxRate,
                taxRateOptions: [
                    MonarchLineItemsTaxRateOption(0, label: "No tax"),
                    MonarchLineItemsTaxRateOption(5, label: "5%"),
                    MonarchLineItemsTaxRateOption(12, label: "12%"),
                    MonarchLineItemsTaxRateOption(18, label: "18%"),
                ],
                discount: $discount,
                isRequired: true,
                isReadOnly: isReadOnly,
                isTaxRateEditable: true,
                isDiscountEditable: true
            )
        }
        .frame(minHeight: 900)

        CatalogueCaption("Empty, required, with an error until a row is added")
        List {
            MonarchLineItemsEditor(
                items: $emptyItems,
                currencyCode: "EUR",
                error: emptyItems.isEmpty ? "Add at least one item." : nil,
                isRequired: true,
                showsTotals: !emptyItems.isEmpty
            )
        }
        .frame(minHeight: 420)
    }
}
