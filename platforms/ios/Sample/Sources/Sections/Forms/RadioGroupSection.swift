import MonarchUI
import SwiftUI

/// The Monarch radio style, and the system inline picker.
struct RadioGroupSection: CataloguePage {
    static let title = "Radio group"

    @State private var plan = "monthly"

    var body: some View {
        CatalogueCaption("Monarch radio")
        VStack(alignment: .leading, spacing: 0) {
            ForEach(["monthly", "yearly"], id: \.self) { value in
                Toggle(
                    value.capitalized,
                    isOn: Binding(get: { plan == value }, set: { if $0 { plan = value } })
                )
                .toggleStyle(.monarchRadio)
            }
            Toggle("Disabled", isOn: .constant(true))
                .toggleStyle(.monarchRadio)
                .disabled(true)
        }
        CatalogueCaption("The system inline picker")
        Picker("Billing period", selection: $plan) {
            Text("Monthly").tag("monthly")
            Text("Yearly").tag("yearly")
        }
        .pickerStyle(.inline)
    }
}
