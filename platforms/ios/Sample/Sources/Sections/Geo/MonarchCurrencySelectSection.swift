import MonarchUI
import SwiftUI

struct MonarchCurrencySelectSection: CataloguePage {
    static let title = "Currency select"

    @State private var currency: String? = "EUR"
    @State private var payout: String?
    @State private var display = MonarchCurrencySelectDisplay.name

    var body: some View {
        CatalogueCaption("Every active currency, with suggestions")
        Picker("Field shows", selection: $display) {
            Text("Name").tag(MonarchCurrencySelectDisplay.name)
            Text("Code").tag(MonarchCurrencySelectDisplay.code)
            Text("Both").tag(MonarchCurrencySelectDisplay.codeAndName)
        }
        .pickerStyle(.segmented)
        MonarchCurrencySelect(
            "Billing currency",
            selection: $currency,
            helper: "Used on every invoice.",
            priority: ["USD", "EUR", "GBP"],
            display: display
        )
        Text("Value: \(currency ?? "none")").monarchTextStyle(.caption)

        CatalogueCaption("Limited list, required, no symbols")
        MonarchCurrencySelect(
            "Payout currency",
            selection: $payout,
            prompt: "Choose a currency",
            error: payout == nil ? "Choose a payout currency." : nil,
            currencies: ["USD", "EUR", "GBP", "INR", "JPY"],
            showsSymbol: false,
            isRequired: true
        )

        CatalogueCaption("Disabled")
        MonarchCurrencySelect("Currency", selection: .constant("JPY")).disabled(true)
    }
}
