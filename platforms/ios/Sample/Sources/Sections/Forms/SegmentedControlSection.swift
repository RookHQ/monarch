import MonarchUI
import SwiftUI

/// The Monarch segmented control, and the system one for short text.
struct SegmentedControlSection: CataloguePage {
    static let title = "Segmented control"

    @State private var layout = "list"
    @State private var filter = "open"
    @State private var view = "list"

    var body: some View {
        CatalogueCaption("With icons")
        MonarchSegmentedControl(
            "View",
            selection: $layout,
            items: [
                MonarchSegmentedItem("List", value: "list", systemImage: "list.bullet"),
                MonarchSegmentedItem("Board", value: "board", systemImage: "rectangle.split.3x1"),
                MonarchSegmentedItem("Grid", value: "grid", systemImage: "square.grid.2x2"),
            ]
        )

        CatalogueCaption("With counts, small")
        MonarchSegmentedControl(
            "Tickets",
            selection: $filter,
            items: [
                MonarchSegmentedItem("Open", value: "open", count: "12"),
                MonarchSegmentedItem("Waiting", value: "waiting", count: "3"),
                MonarchSegmentedItem("Closed", value: "closed", count: "48", isDisabled: true),
            ],
            size: .sm
        )

        CatalogueCaption("The system control, for up to four plain-text choices")
        Picker("View", selection: $view) {
            Text("List").tag("list")
            Text("Board").tag("board")
            Text("Calendar").tag("calendar")
        }
        .pickerStyle(.segmented)
    }
}
