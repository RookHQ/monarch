import MonarchUI
import SwiftUI

struct MonarchPaymentMethodCardSection: CataloguePage {
    static let title = "PaymentMethodCard"

    private struct Card: Identifiable {
        let id: String
        let brand: String
        let month: Int
        let year: Int
    }

    private struct Declined: LocalizedError {
        var errorDescription: String? { "The payment provider declined the change." }
    }

    @State private var cards = [
        Card(id: "4242", brand: "visa", month: 8, year: 2_030),
        Card(id: "4444", brand: "mastercard", month: 12, year: 2_026),
        Card(id: "0005", brand: "amex", month: 1, year: 2_025),
    ]
    @State private var defaultID = "4242"
    @State private var failsNext = false

    var body: some View {
        Toggle("Make the next action fail", isOn: $failsNext)

        CatalogueCaption("Saved cards")
        ForEach(cards) { card in
            MonarchPaymentMethodCard(
                brand: card.brand,
                last4: card.id,
                expiryMonth: card.month,
                expiryYear: card.year,
                holderName: "Ana Silva",
                isDefault: card.id == defaultID,
                isRemoveDisabled: cards.count == 1,
                onMakeDefault: {
                    try await Task.sleep(for: .seconds(1))
                    if failsNext { throw Declined() }
                    defaultID = card.id
                },
                onRemove: {
                    try await Task.sleep(for: .seconds(1))
                    if failsNext { throw Declined() }
                    cards.removeAll { $0.id == card.id }
                }
            )
        }

        CatalogueCaption("Without actions")
        MonarchPaymentMethodCard(brand: "rupay", last4: "1234", expiryMonth: 3, expiryYear: 2_031)
    }
}
