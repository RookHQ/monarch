@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchIssueCardSection: CataloguePage {
    static let title = "Issue card"

    private static let statuses = [
        MonarchIssueCardStatus(id: "backlog", name: "Backlog", category: .backlog),
        MonarchIssueCardStatus(id: "todo", name: "Todo", category: .unstarted),
        MonarchIssueCardStatus(id: "doing", name: "In progress", category: .started),
        MonarchIssueCardStatus(id: "done", name: "Done", category: .completed),
        MonarchIssueCardStatus(id: "wontfix", name: "Won't fix", category: .cancelled),
    ]

    @State private var statusID = "doing"
    @State private var priority = MonarchIssueCardPriority.high
    @State private var isSelected = false
    @State private var opened = 0

    var body: some View {
        CatalogueCaption("Change the status and priority from the card; tap the title to open")
        MonarchIssueCard(
            "Sign-in fails on slow networks",
            key: "STR-142",
            status: Self.statuses.first { $0.id == statusID },
            statuses: Self.statuses,
            priority: priority,
            assignee: .person("Ana Ruiz"),
            tags: ["Bug", "Auth"],
            dueDate: Date().addingTimeInterval(-86_400 * 2),
            isSelected: isSelected,
            onStatusChange: { statusID = $0 },
            onPriorityChange: { priority = $0 },
            onOpen: { opened += 1 }
        )
        Toggle("Selected", isOn: $isSelected)
        Text("Opened \(opened) times")
        CatalogueCaption("Read-only, unassigned, dragging")
        MonarchIssueCard(
            "Write the release notes",
            key: "STR-150",
            status: Self.statuses[1],
            priority: MonarchIssueCardPriority.none,
            assignee: .unassigned,
            dueDate: Date().addingTimeInterval(86_400 * 5),
            isDragging: true
        )
    }
}
