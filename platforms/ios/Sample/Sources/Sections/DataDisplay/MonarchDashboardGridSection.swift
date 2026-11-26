import MonarchUI
import SwiftUI

struct MonarchDashboardGridSection: CataloguePage {
    static let title = "Dashboard grid"

    private struct Tile: Identifiable {
        let id: String
        let value: Double
        var span: MonarchDashboardGridSpan = .one
    }

    private let tiles = [
        Tile(id: "Team load", value: 128, span: .two),
        Tile(id: "Response time", value: 4.2),
        Tile(id: "Backlog", value: 37),
        Tile(id: "Satisfaction", value: 0.92, span: .full),
    ]

    @State private var layout: [String] = []
    @State private var isEditing = false

    var body: some View {
        HStack {
            Toggle("Arrange", isOn: $isEditing).monarchTextStyle(.body)
            Button("Reset") { layout = [] }
        }
        CatalogueCaption("One column on a phone; two and more on an iPad")
        MonarchDashboardGrid(
            tiles,
            layout: $layout,
            itemLabel: \.id,
            span: { $0.span },
            isEditing: isEditing,
            content: { tile in
                MonarchWidget(tile.id, period: "Last 30 days") {
                    MonarchMetric(tile.id, value: tile.value, format: tile.id == "Satisfaction" ? .percent : .number)
                }
            }
        )
        Text("Stored layout: \(layout.isEmpty ? "default" : layout.joined(separator: ", "))")
            .monarchTextStyle(.caption)
    }
}
