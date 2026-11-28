import MonarchUI
import SwiftUI

struct MonarchColumnChooserSection: CataloguePage {
    static let title = "Column chooser"

    private static let columns = [
        MonarchColumnChooserColumn("Name", isHideable: false),
        MonarchColumnChooserColumn("Status"),
        MonarchColumnChooserColumn("Owner"),
        MonarchColumnChooserColumn("Priority"),
        MonarchColumnChooserColumn("Region"),
        MonarchColumnChooserColumn("Created"),
        MonarchColumnChooserColumn("Updated"),
        MonarchColumnChooserColumn("Due date"),
    ]

    @State private var hidden: Set<String> = ["Region", "Created"]
    @State private var order: [String] = []
    @State private var editMode: EditMode = .inactive

    var body: some View {
        CatalogueCaption("Switch columns on and off, drag to reorder, or search")
        Toggle(
            "Show reorder controls",
            isOn: Binding(get: { editMode == .active }, set: { editMode = $0 ? .active : .inactive })
        )
        .monarchTextStyle(.body)
        MonarchColumnChooser(
            Self.columns,
            hiddenColumns: $hidden,
            order: $order,
            onReset: {
                hidden = ["Region", "Created"]
                order = []
            }
        )
        .environment(\.editMode, $editMode)
        .frame(minHeight: 520)
        Text("Shown: \(shownSummary)").monarchTextStyle(.caption)
    }

    private var shownSummary: String {
        let ids = order.isEmpty ? Self.columns.map(\.id) : order + Self.columns.map(\.id).filter { !order.contains($0) }
        return ids.filter { $0 == "Name" || !hidden.contains($0) }.joined(separator: ", ")
    }
}
