import MonarchUI
import SwiftUI

struct MonarchNumberInputSection: CataloguePage {
    static let title = "Number input"

    @State private var quantity: Double? = 3
    @State private var discount: Double? = 0.15
    @State private var price: Double? = 0.3
    @State private var weight: Double?
    @State private var seats: Double? = 2
    @State private var saved: String = "Nothing saved yet."

    var body: some View {
        MonarchNumberInput(
            "Quantity",
            value: $quantity,
            minimum: 0,
            maximum: 99,
            helper: "Type 120 to see the range message."
        )

        MonarchNumberInput("Discount", value: $discount, format: .percent, minimum: 0, maximum: 1, step: 0.05)

        MonarchNumberInput("Price", value: $price, step: 0.1, helper: "Steps of 0.1 never drift.")

        MonarchNumberInput("Weight", value: $weight, minimum: 0, prompt: "0", showsStepper: false)

        MonarchNumberInput("Disabled", value: .constant(7))
            .disabled(true)

        CatalogueCaption("Required; Save reads the value while the field still has focus")
        MonarchNumberInput("Seats", value: $seats, minimum: 1, maximum: 50, isRequired: true)
        MonarchButton("Save", variant: .primary) {
            saved = "Saved \(seats.map { $0.formatted() } ?? "nothing")."
        }
        CatalogueCaption(saved)
    }
}
