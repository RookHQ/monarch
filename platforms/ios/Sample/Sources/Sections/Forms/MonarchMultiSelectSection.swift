import MonarchUI
import SwiftUI

struct MonarchMultiSelectSection: CataloguePage {
    static let title = "MultiSelect"

    private static let labelOptions = [
        MonarchComboboxOption(id: "bug", label: "Bug", description: "Something is broken", group: "Type"),
        MonarchComboboxOption(id: "feature", label: "Feature", group: "Type"),
        MonarchComboboxOption(id: "question", label: "Question", group: "Type"),
        MonarchComboboxOption(id: "docs", label: "Documentation", group: "Area"),
        MonarchComboboxOption(id: "ui", label: "Interface", group: "Area"),
        MonarchComboboxOption(id: "api", label: "API", group: "Area"),
        MonarchComboboxOption(id: "old", label: "Archived", group: "Area", isDisabled: true),
    ]

    @State private var chosen: [String] = ["bug"]
    @State private var limited: [String] = []
    @State private var tags: [String] = []
    @State private var tagOptions = [
        MonarchComboboxOption(id: "design", label: "Design"),
        MonarchComboboxOption(id: "research", label: "Research"),
    ]

    var body: some View {
        CatalogueCaption("Grouped and sorted by match")
        MonarchMultiSelect(
            "Labels",
            selection: $chosen,
            options: Self.labelOptions,
            prompt: "Choose labels",
            helper: "Used to sort the inbox.",
            sortsByMatch: true
        )

        CatalogueCaption("At most three, required")
        MonarchMultiSelect(
            "Areas",
            selection: $limited,
            options: Self.labelOptions,
            error: limited.isEmpty ? "Choose at least one area." : nil,
            maxSelections: 3,
            isRequired: true
        )

        CatalogueCaption("Create a new value")
        MonarchMultiSelect(
            "Topics",
            selection: $tags,
            options: tagOptions,
            prompt: "Choose or create topics",
            onCreate: { text in
                let option = MonarchComboboxOption(id: text.lowercased(), label: text)
                tagOptions.append(option)
                return option
            }
        )

        CatalogueCaption("Disabled")
        MonarchMultiSelect("Labels", selection: .constant(["docs", "ui"]), options: Self.labelOptions)
            .disabled(true)
    }
}
