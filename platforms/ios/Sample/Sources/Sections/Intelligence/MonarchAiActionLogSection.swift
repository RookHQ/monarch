import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct UndoError: Error {}

struct MonarchAiActionLogSection: CataloguePage {
    static let title = "AI action log"

    private static let research = MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources")
    private static let scheduler = MonarchAiAgentRef(id: "scheduler", name: "Scheduler", role: "Plans meetings")

    private let entries = [
        MonarchAiActionLogEntry(
            id: "summary",
            date: .now.addingTimeInterval(-240),
            agent: Self.research,
            action: "added a summary to",
            target: "Quarterly report",
            url: URL(string: "https://example.com/report"),
            isUndoable: true
        ),
        MonarchAiActionLogEntry(
            id: "meeting",
            date: .now.addingTimeInterval(-3_600),
            agent: Self.scheduler,
            action: "moved the planning meeting to",
            target: "Thursday",
            isUndoable: true
        ),
        MonarchAiActionLogEntry(
            id: "rename",
            date: .now.addingTimeInterval(-86_400),
            agent: Self.research,
            action: "renamed",
            target: "Roadmap",
            isUndoable: true,
            isUndone: true
        ),
        MonarchAiActionLogEntry(
            id: "tag",
            date: .now.addingTimeInterval(-86_400 * 3),
            agent: Self.research,
            action: "tagged 12 documents as",
            target: "Finance"
        ),
    ]
    @State private var undoFails = true

    var body: some View {
        CatalogueCaption("The first undo fails, the next one succeeds; two entries per page")
        MonarchAiActionLog(
            entries,
            pageSize: 2,
            onUndo: { _ in
                try await Task.sleep(for: .milliseconds(600))
                if undoFails {
                    undoFails = false
                    throw UndoError()
                }
            }
        )

        CatalogueCaption("Empty")
        MonarchAiActionLog([])
    }
}
