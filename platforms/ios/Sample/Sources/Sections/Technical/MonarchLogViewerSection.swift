import MonarchUI
import SwiftUI

struct MonarchLogViewerSection: CataloguePage {
    static let title = "Log viewer"

    @State private var lines: [MonarchLogLine] = (0..<30).map(Self.line)
    @State private var query = ""

    var body: some View {
        CatalogueCaption("Add lines while following, then drag back to pause")
        MonarchLogViewer(lines, query: $query) {
            Button("Add 10 lines") {
                lines += (lines.count..<lines.count + 10).map(Self.line)
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
        }
        .frame(height: 460)
        CatalogueCaption("Highlight mode, wrapped, with line numbers")
        MonarchLogViewer(
            Array(lines.prefix(12)),
            query: .constant("request"),
            isWrapped: .constant(true),
            searchMode: .highlight,
            showsLineNumbers: true
        )
        .frame(height: 360)
        CatalogueCaption("Loading")
        MonarchLogViewer([], isLoading: true).frame(height: 200)
    }

    nonisolated private static func line(_ index: Int) -> MonarchLogLine {
        let messages = [
            "GET /v1/invoices 200 in 34 ms",
            "Request to the billing service took longer than expected; retrying with a longer timeout",
            "Worker 3 picked up job \(index)",
            "Request failed: connection reset by peer",
        ]
        let level = MonarchLogLevel.allCases[(index * 5) % 4]
        return MonarchLogLine(
            id: "\(index)",
            message: messages[index % messages.count],
            level: level,
            time: Date(timeIntervalSinceNow: Double(index) - 60),
            source: index % 2 == 0 ? "api" : "worker"
        )
    }
}
