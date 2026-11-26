import MonarchUI
import SwiftUI

struct MonarchAvailabilityToggleSection: CataloguePage {
    static let title = "Availability toggle"

    @State private var isAvailable = true
    @State private var isSaving = false
    @State private var plain = false

    var body: some View {
        CatalogueCaption("With the hint. A change is saved for a second.")
        MonarchAvailabilityToggle(isAvailable: $isAvailable, isPending: isSaving, showsHint: true)
            .onChange(of: isAvailable) {
                isSaving = true
                Task {
                    try? await Task.sleep(for: .seconds(1))
                    isSaving = false
                }
            }

        CatalogueCaption("Status word only")
        MonarchAvailabilityToggle(isAvailable: $plain)

        CatalogueCaption("Disabled")
        MonarchAvailabilityToggle(isAvailable: .constant(true), showsHint: true)
            .disabled(true)
    }
}
