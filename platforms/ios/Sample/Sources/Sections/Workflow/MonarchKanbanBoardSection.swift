@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchKanbanBoardSection: CataloguePage {
    static let title = "Kanban board"

    private struct Issue: Identifiable {
        let id: String
        let title: String
        let priority: MonarchIssueCardPriority
    }

    private struct SaveError: Error {}

    @State private var columns = [
        MonarchKanbanColumn(
            id: "todo",
            title: "To do",
            items: [
                Issue(id: "ENG-101", title: "Sign in with passkeys", priority: .high),
                Issue(id: "ENG-102", title: "Fix the CSV export", priority: .medium),
                Issue(id: "ENG-103", title: "Audit log export", priority: .low),
            ]
        ),
        MonarchKanbanColumn(
            id: "doing",
            title: "Doing",
            items: [Issue(id: "ENG-104", title: "Rate limit the public API", priority: .urgent)],
            wipLimit: 2
        ),
        MonarchKanbanColumn(id: "review", title: "In review", items: [Issue]()),
        MonarchKanbanColumn(
            id: "done",
            title: "Done",
            items: [Issue(id: "ENG-100", title: "Dark mode for emails", priority: .none)]
        ),
    ]
    @State private var isWipLimitEnforced = true
    @State private var failsSaves = false
    @State private var lastMove = "No moves yet"

    var body: some View {
        Toggle("Enforce WIP limits", isOn: $isWipLimitEnforced)
        Toggle("Fail every save", isOn: $failsSaves)
        Text(lastMove).monarchTextStyle(.caption)
        MonarchKanbanBoard(
            columns: $columns,
            itemLabel: { $0.id },
            canMove: { _, to, from in !(from == "done" && to == "todo") },
            isWipLimitEnforced: isWipLimitEnforced,
            isItemEnabled: { $0.id != "ENG-100" },
            onMove: { move, _ in
                lastMove = "\(move.itemID): \(move.fromColumnID) → \(move.toColumnID)"
                if failsSaves {
                    try await Task.sleep(for: .seconds(1))
                    throw SaveError()
                }
            },
            card: { issue, state in
                MonarchIssueCard(issue.title, key: issue.id, priority: issue.priority, isDragging: state.isPending)
            }
        )
        .frame(height: 560)
        Text("ENG-100 is locked. Done cards cannot go back to To do.").monarchTextStyle(.caption)
    }
}
