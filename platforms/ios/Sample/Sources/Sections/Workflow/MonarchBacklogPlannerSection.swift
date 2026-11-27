@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchBacklogPlannerSection: CataloguePage {
    static let title = "Backlog planner"

    private struct Issue: Identifiable {
        let id: String
        let title: String
        let points: Int?
    }

    @State private var buckets = [
        MonarchBacklogBucket(
            id: "s14",
            title: "Sprint 14",
            subtitle: "Mar 3 – Mar 14",
            badge: "Active",
            capacity: 10,
            items: [
                Issue(id: "ENG-101", title: "Sign in with passkeys", points: 5),
                Issue(id: "ENG-102", title: "Fix the CSV export", points: 2),
            ]
        ),
        MonarchBacklogBucket(
            id: "s15",
            title: "Sprint 15",
            subtitle: "Mar 17 – Mar 28",
            capacity: 10,
            items: [Issue]()
        ),
        MonarchBacklogBucket(
            id: "backlog",
            title: "Backlog",
            items: [
                Issue(id: "ENG-103", title: "Rate limit the public API", points: 5),
                Issue(id: "ENG-104", title: "Audit log export", points: nil),
                Issue(id: "ENG-105", title: "Dark mode for emails", points: 1),
            ]
        ),
    ]
    @State private var lastMove = "No moves yet"

    var body: some View {
        Text(lastMove).monarchTextStyle(.caption)
        MonarchBacklogPlanner(
            buckets: $buckets,
            itemLabel: { $0.id },
            points: { $0.points },
            onMove: { move in lastMove = "\(move.itemID): \(move.fromBucketID) → \(move.toBucketID)" },
            row: { issue in
                VStack(alignment: .leading) {
                    Text(issue.id).monarchTextStyle(.caption)
                    Text(issue.title).monarchTextStyle(.body)
                }
            }
        )
        .frame(height: 640)
    }
}
