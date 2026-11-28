import MonarchUI
import SwiftUI

struct MonarchToggleGroupSection: CataloguePage {
    static let title = "ToggleGroup"

    @State private var styles: Set<String> = ["bold"]
    @State private var view: String? = "list"
    @State private var filter: String?

    var body: some View {
        CatalogueCaption("Multiple, icon only")
        MonarchToggleGroup(
            "Formatting",
            selection: $styles,
            options: [
                MonarchToggleGroupOption("bold", label: "Bold", systemImage: "bold", isIconOnly: true),
                MonarchToggleGroupOption("italic", label: "Italic", systemImage: "italic", isIconOnly: true),
                MonarchToggleGroupOption("underline", label: "Underline", systemImage: "underline", isIconOnly: true),
            ]
        )
        CatalogueCaption("Single, one always on")
        MonarchToggleGroup(
            "View",
            selection: $view,
            options: [
                MonarchToggleGroupOption("list", label: "List", systemImage: "list.bullet"),
                MonarchToggleGroupOption("board", label: "Board", systemImage: "square.grid.2x2"),
                MonarchToggleGroupOption("calendar", label: "Calendar", systemImage: "calendar"),
            ],
            variant: .outline,
            isRequired: true
        )
        CatalogueCaption("Single, with counts, can be cleared")
        MonarchToggleGroup(
            "Status",
            selection: $filter,
            options: [
                MonarchToggleGroupOption("open", label: "Open", count: "12"),
                MonarchToggleGroupOption("closed", label: "Closed", count: "3"),
                MonarchToggleGroupOption("spam", label: "Spam", isDisabled: true),
            ],
            size: .sm
        )
        Text("Filter: \(filter ?? "none")").monarchTextStyle(.caption)
    }
}
