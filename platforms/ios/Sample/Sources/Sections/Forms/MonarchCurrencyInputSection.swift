import MonarchUI
import SwiftUI

struct MonarchCurrencyInputSection: CataloguePage {
    static let title = "Currency input"

    @State private var amount: Double? = 19.99
    @State private var cents: Int? = 1999
    @State private var yen: Double? = 1500
    @State private var refund: Double? = -12.5
    @State private var tip: Double?

    var body: some View {
        CatalogueCaption("Major units")
        MonarchCurrencyInput("Amount", value: $amount, currencyCode: "USD", helper: "Charged today.")
        Text("Value: \(amount.map { String($0) } ?? "empty")").monarchTextStyle(.caption)

        CatalogueCaption("Minor units, stored as an integer")
        MonarchCurrencyInput("Price", minorUnits: $cents, currencyCode: "EUR", maximum: 100_000)
        Text("Cents: \(cents.map { String($0) } ?? "empty")").monarchTextStyle(.caption)

        CatalogueCaption("No decimals, code, with a stepper")
        MonarchCurrencyInput(
            "Deposit",
            value: $yen,
            currencyCode: "JPY",
            currencyDisplay: .code,
            step: 500,
            showsStepper: true
        )

        CatalogueCaption("Negative amounts allowed")
        MonarchCurrencyInput("Adjustment", value: $refund, currencyCode: "GBP", allowsNegative: true)

        CatalogueCaption("Range with a custom message")
        MonarchCurrencyInput(
            "Tip",
            value: $tip,
            currencyCode: "USD",
            maximum: 100,
            prompt: "$0.00",
            rangeMessage: "Tips are up to $100."
        )

        CatalogueCaption("Disabled")
        MonarchCurrencyInput("Amount", value: $amount, currencyCode: "USD").disabled(true)
    }
}
