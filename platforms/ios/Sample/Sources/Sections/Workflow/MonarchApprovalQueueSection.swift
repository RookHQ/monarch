@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchApprovalQueueSection: CataloguePage {
    static let title = "Approval queue"

    private static let initial: [MonarchApprovalQueueItem] = {
        let now = Date()
        return [
            MonarchApprovalQueueItem(
                id: "1",
                requester: MonarchApprovalPerson("Alex Morgan"),
                requesterDetail: "Design",
                title: "Expense claim",
                summary: "3 receipts, travel",
                submittedAt: now.addingTimeInterval(-86_400 * 2),
                isOverdue: true
            ),
            MonarchApprovalQueueItem(
                id: "2",
                requester: MonarchApprovalPerson("Sam Lee"),
                requesterDetail: "Engineering",
                title: "Leave, 2 days",
                summary: "14 to 15 April",
                submittedAt: now.addingTimeInterval(-3_600)
            ),
            MonarchApprovalQueueItem(
                id: "3",
                requester: MonarchApprovalPerson("Jo Park"),
                requesterDetail: "Operations",
                title: "Timesheet",
                summary: "Week of 2 March, 41 h",
                submittedAt: now.addingTimeInterval(-600),
                badges: [MonarchStatusDefinition("Resubmitted", tone: .warning)]
            ),
        ]
    }()

    @State private var items = Self.initial
    @State private var failsNext = false
    @State private var reviewed: String?

    var body: some View {
        CatalogueCaption("Swipe a request, or use its buttons; select several to approve them together")
        Toggle("Make the next approval fail", isOn: $failsNext)
        Button("Reset") { items = Self.initial }
            .buttonStyle(.monarch(.ghost, size: .sm))
        if let reviewed {
            Text("Opened the request from \(reviewed)")
        }
        NavigationStack {
            MonarchApprovalQueue(
                items,
                isSearchable: true,
                onApprove: { item in
                    try await Task.sleep(for: .milliseconds(600))
                    if failsNext {
                        failsNext = false
                        throw CancellationError()
                    }
                    items.removeAll { $0.id == item.id }
                },
                onReject: { item, _ in items.removeAll { $0.id == item.id } },
                onReview: { item in reviewed = item.requester.name },
                onApproveMany: { chosen in
                    let ids = Set(chosen.map(\.id))
                    items.removeAll { ids.contains($0.id) }
                }
            )
            .navigationTitle("Approvals")
        }
        .frame(height: 620)
    }
}
