import MonarchUI
import SwiftUI

struct MonarchCheckboxToggleStyleSection: CataloguePage {
    static let title = "Checkbox"

    @State private var receipt = false
    @State private var terms = true
    @State private var items = [true, false, false]

    var body: some View {
        Group {
            Toggle("Email me a receipt", isOn: $receipt)
            Toggle("I agree to the terms", isOn: $terms)
            Toggle("Disabled", isOn: .constant(true))
                .disabled(true)

            CatalogueCaption("Mixed state from several sources")
            Toggle(sources: $items, isOn: \.self) { Text("All items") }
            ForEach(items.indices, id: \.self) { index in
                Toggle("Item \(index + 1)", isOn: $items[index])
            }
        }
        .toggleStyle(.monarchCheckbox)
    }
}
