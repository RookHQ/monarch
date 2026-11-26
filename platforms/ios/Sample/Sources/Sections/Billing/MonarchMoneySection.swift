import MonarchUI
import SwiftUI

struct MonarchMoneySection: CataloguePage {
    static let title = "Money"

    @State private var isCompact = false
    @State private var tonesNegative = true

    var body: some View {
        CatalogueCaption("Amounts in their own currency")
        VStack(alignment: .trailing) {
            MonarchMoney(1_234.5, currencyCode: "USD")
            MonarchMoney(minorUnits: 129_900, currencyCode: "INR")
            MonarchMoney(minorUnits: 1_999, currencyCode: "JPY")
            MonarchMoney(minorUnits: 12_345, currencyCode: "KWD")
        }

        CatalogueCaption("Compact, read in full by VoiceOver")
        Toggle("Compact", isOn: $isCompact)
        MonarchMoney(1_250_000, currencyCode: "EUR", notation: isCompact ? .compact : .standard, textStyle: .title)

        CatalogueCaption("Gains and losses")
        Toggle("Colour negative amounts", isOn: $tonesNegative)
        VStack(alignment: .trailing) {
            MonarchMoney(42, currencyCode: "USD", signDisplay: .exceptZero)
            MonarchMoney(-1_042.75, currencyCode: "USD", tonesNegative: tonesNegative)
            MonarchMoney(-42, currencyCode: "USD", usesAccountingSign: true, tonesNegative: tonesNegative)
        }

        CatalogueCaption("Currency displays")
        MonarchMoney(42, currencyCode: "USD", currencyDisplay: .code)
        MonarchMoney(42, currencyCode: "USD", currencyDisplay: .name)

        CatalogueCaption("No amount")
        MonarchMoney(nil, currencyCode: "USD")
    }
}
