import MonarchUI
import SwiftUI

struct MonarchRatingPickerSection: CataloguePage {
    static let title = "Rating picker"

    @State private var overall: Int? = 4
    @State private var performance: Int?
    @State private var stars: Int? = 3
    @State private var effort: Int?
    @State private var required: Int?

    private let levels = [
        MonarchRatingOption(1, label: "Below"),
        MonarchRatingOption(2, label: "Meets"),
        MonarchRatingOption(3, label: "Exceeds"),
    ]

    var body: some View {
        MonarchRatingPicker("Overall, 1 to 5", value: $overall)

        CatalogueCaption("A label per value; choose the chosen value again to clear")
        MonarchRatingPicker("Performance", value: $performance, options: levels, isClearable: true)

        CatalogueCaption("Stars")
        MonarchRatingPicker("Stars", value: $stars, variant: .stars, isClearable: true)
        MonarchRatingPicker("Performance", value: $performance, options: levels, variant: .stars, size: .sm)

        CatalogueCaption("Small, 0 to 10")
        MonarchRatingPicker("Effort", value: $effort, in: 0...10, size: .sm)

        CatalogueCaption("Read-only and disabled")
        MonarchRatingPicker("Last review", value: .constant(4), variant: .stars, isReadOnly: true)
        MonarchRatingPicker("Disabled", value: .constant(2))
            .disabled(true)

        CatalogueCaption("Required, with an error until rated")
        MonarchRatingPicker(
            "Overall",
            value: $required,
            variant: .stars,
            isRequired: true,
            error: required == nil ? "Choose a rating." : nil
        )
    }
}
