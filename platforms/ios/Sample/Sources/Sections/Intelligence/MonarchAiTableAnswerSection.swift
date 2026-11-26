import MonarchUI
import SwiftUI

struct MonarchAiTableAnswerSection: CataloguePage {
    static let title = "AI table answer"

    @State private var rows: [[String: MonarchAiJSON]] = []
    @State private var isStreaming = false
    @State private var lastAction = "No action yet."

    private let all: [[String: MonarchAiJSON]] = [
        ["region": "North", "revenue": 1200, "growth": "12%", "updated": "2024-10-14"],
        ["region": "South", "revenue": 980.5, "growth": "-3%", "updated": "2024-09-30"],
        ["region": "East", "revenue": nil, "growth": "4%", "updated": "2024-10-01"],
        ["region": "West", "revenue": 2045, "growth": "21%", "updated": "2024-10-02"],
    ]

    private let columns = [
        MonarchAiTableColumn("region", label: "Region"),
        MonarchAiTableColumn("revenue", label: "Revenue"),
        MonarchAiTableColumn("growth", label: "Growth"),
        MonarchAiTableColumn("updated", label: "Updated"),
    ]

    var body: some View {
        Text(lastAction).monarchTextStyle(.body)

        CatalogueCaption("Sortable, copyable, with Open as artifact")
        MonarchAiTableAnswer(all, title: "Revenue by region", columns: columns) {
            lastAction = "Open as artifact"
        }

        CatalogueCaption("Streaming rows in")
        MonarchButton("Stream the table", variant: .primary) { stream() }
            .disabled(isStreaming)
        MonarchAiTableAnswer(rows, title: "Revenue by region", columns: columns, isStreaming: isStreaming)

        CatalogueCaption("Columns inferred, two rows before Show all")
        MonarchAiTableAnswer(all, visibleRows: 2)
    }

    private func stream() {
        rows = []
        isStreaming = true
        Task {
            for row in all {
                try? await Task.sleep(for: .milliseconds(600))
                rows.append(row)
            }
            isStreaming = false
        }
    }
}
