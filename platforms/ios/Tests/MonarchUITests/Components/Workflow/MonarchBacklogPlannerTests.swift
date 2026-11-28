import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private struct PlanItem: Identifiable, Hashable {
    let id: String
    let title: String
    let points: Int?
}

private enum PlanSamples {
    static let sprint = MonarchBacklogBucket(
        id: "s14",
        title: "Sprint 14",
        subtitle: "Mar 3 – Mar 14",
        badge: "Active",
        capacity: 8,
        items: [
            PlanItem(id: "P-1", title: "Sign in with passkeys", points: 5),
            PlanItem(id: "P-2", title: "Fix the CSV export", points: 2),
            PlanItem(id: "P-3", title: "Rate limit the API", points: 3),
        ]
    )
    static let next = MonarchBacklogBucket(id: "s15", title: "Sprint 15", capacity: 20, items: [PlanItem]())
    static let backlog = MonarchBacklogBucket(
        id: "backlog",
        title: "Backlog",
        items: [
            PlanItem(id: "P-4", title: "Audit log", points: nil),
            PlanItem(id: "P-5", title: "Dark mode for emails", points: 1),
        ]
    )
    static let all = [sprint, next, backlog]

    static func ids(_ buckets: [MonarchBacklogBucket<PlanItem>]) -> [[String]] {
        buckets.map { $0.items.map(\.id) }
    }
}

enum MonarchBacklogPlannerFixtures: ComponentFixtures {
    static let id = "backlog-planner"
    static let fixtures = [
        Fixture("over capacity, empty bucket, unestimated item") {
            MonarchBacklogPlanner(
                buckets: .constant(PlanSamples.all),
                itemLabel: { $0.id },
                points: { $0.points },
                row: { item in Text(item.title).monarchTextStyle(.body) }
            )
            .frame(height: 700)
        },
        Fixture("collapsed bucket, read only, no points") {
            MonarchBacklogPlanner(
                buckets: .constant(PlanSamples.all),
                itemLabel: { $0.id },
                collapsed: .constant(["backlog"]),
                isReadOnly: true,
                row: { item in Text(item.title).monarchTextStyle(.body) }
            )
            .frame(height: 600)
        },
        Fixture("issue cards as rows, long titles") {
            MonarchBacklogPlanner(
                buckets: .constant([
                    MonarchBacklogBucket(
                        id: "r1",
                        title: "Release 2.4 with a long name that wraps onto a second line",
                        items: [
                            PlanItem(
                                id: "ENG-1042",
                                title: "Migrate billing webhooks to the new delivery queue",
                                points: 8
                            )
                        ],
                        emptyMessage: "Drag work here."
                    )
                ]),
                itemLabel: { $0.id },
                points: { $0.points },
                row: { item in MonarchIssueCard(item.title, key: item.id) }
            )
            .frame(height: 500)
        },
    ]
}

@Suite("MonarchBacklogPlanner")
@MainActor
struct MonarchBacklogPlannerTests {
    private let text = MonarchBacklogPlannerText(labels: MonarchBacklogPlannerLabels())
    private let allowAll: (PlanItem, String, String) -> Bool = { _, _, _ in true }

    @Test("Moving takes the item out, then puts it at the clamped place")
    func moving() throws {
        let across = try #require(
            MonarchBacklogPlannerTokens.moving(PlanSamples.all, itemID: "P-2", toBucketID: "backlog", toIndex: 1)
        )
        #expect(PlanSamples.ids(across.buckets) == [["P-1", "P-3"], [], ["P-4", "P-2", "P-5"]])
        #expect(
            across.move
                == MonarchBacklogMove(
                    itemID: "P-2",
                    fromBucketID: "s14",
                    fromIndex: 1,
                    toBucketID: "backlog",
                    toIndex: 1
                )
        )
        let clamped = try #require(
            MonarchBacklogPlannerTokens.moving(PlanSamples.all, itemID: "P-1", toBucketID: "s15", toIndex: 99)
        )
        #expect(clamped.move.toIndex == 0)
        #expect(PlanSamples.ids(clamped.buckets)[1] == ["P-1"])
        let within = try #require(
            MonarchBacklogPlannerTokens.moving(PlanSamples.all, itemID: "P-1", toBucketID: "s14", toIndex: 2)
        )
        #expect(PlanSamples.ids(within.buckets)[0] == ["P-2", "P-3", "P-1"])
        #expect(
            MonarchBacklogPlannerTokens.moving(PlanSamples.all, itemID: "P-1", toBucketID: "s14", toIndex: 0) == nil
        )
        #expect(
            MonarchBacklogPlannerTokens.moving(PlanSamples.all, itemID: "nope", toBucketID: "s14", toIndex: 0) == nil
        )
        #expect(
            MonarchBacklogPlannerTokens.moving(PlanSamples.all, itemID: "P-1", toBucketID: "nope", toIndex: 0) == nil
        )
    }

    @Test("onMove destinations count places before the row is taken out")
    func reordering() throws {
        let down = try #require(
            MonarchBacklogPlannerTokens.reordering(PlanSamples.all, bucketID: "s14", from: IndexSet(integer: 0), to: 2)
        )
        #expect(PlanSamples.ids(down.buckets)[0] == ["P-2", "P-1", "P-3"])
        #expect(down.move.toIndex == 1)
        let up = try #require(
            MonarchBacklogPlannerTokens.reordering(PlanSamples.all, bucketID: "s14", from: IndexSet(integer: 2), to: 0)
        )
        #expect(PlanSamples.ids(up.buckets)[0] == ["P-3", "P-1", "P-2"])
        #expect(
            MonarchBacklogPlannerTokens.reordering(PlanSamples.all, bucketID: "s14", from: IndexSet(integer: 1), to: 2)
                == nil
        )
    }

    @Test("Move up and down cross bucket edges and skip refused buckets")
    func stepping() {
        let all = PlanSamples.all
        #expect(MonarchBacklogPlannerTokens.step(all, itemID: "P-2", up: true, allows: allowAll)! == ("s14", 0))
        #expect(MonarchBacklogPlannerTokens.step(all, itemID: "P-3", up: false, allows: allowAll)! == ("s15", 0))
        #expect(MonarchBacklogPlannerTokens.step(all, itemID: "P-4", up: true, allows: allowAll)! == ("s15", 0))
        #expect(MonarchBacklogPlannerTokens.step(all, itemID: "P-1", up: true, allows: allowAll) == nil)
        #expect(MonarchBacklogPlannerTokens.step(all, itemID: "P-5", up: false, allows: allowAll) == nil)
        let noS15: (PlanItem, String, String) -> Bool = { _, to, _ in to != "s15" }
        #expect(MonarchBacklogPlannerTokens.step(all, itemID: "P-3", up: false, allows: noS15)! == ("backlog", 0))
        #expect(MonarchBacklogPlannerTokens.step(all, itemID: "P-4", up: true, allows: noS15)! == ("s14", 3))
    }

    @Test("Totals count points and unestimated items; capacity reports the overflow")
    func totals() {
        let sprint = MonarchBacklogPlannerTokens.totals(PlanSamples.sprint.items, points: { $0.points })
        #expect(sprint.count == 3)
        #expect(sprint.points == 10)
        #expect(sprint.unestimated == 0)
        let backlog = MonarchBacklogPlannerTokens.totals(PlanSamples.backlog.items, points: { $0.points })
        #expect(backlog.points == 1)
        #expect(backlog.unestimated == 1)
        #expect(MonarchBacklogPlannerTokens.totals(PlanSamples.backlog.items, points: nil).points == nil)
        #expect(MonarchBacklogPlannerTokens.overCapacity(points: 10, capacity: 8) == 2)
        #expect(MonarchBacklogPlannerTokens.overCapacity(points: 8, capacity: 8) == nil)
        #expect(MonarchBacklogPlannerTokens.overCapacity(points: nil, capacity: 8) == nil)
        #expect(MonarchBacklogPlannerTokens.overCapacity(points: 9, capacity: nil) == nil)
        #expect(MonarchBacklogPlannerTokens.capacityFraction(points: 4, capacity: 8) == 0.5)
        #expect(MonarchBacklogPlannerTokens.capacityFraction(points: 12, capacity: 8) == 1)
        #expect(MonarchBacklogPlannerTokens.capacityFraction(points: 0, capacity: 0) == 0)
    }

    @Test("Header and row text")
    func headerText() {
        #expect(text.summary(count: 3, points: 10, unestimated: 0) == "3 items · 10 points")
        #expect(text.summary(count: 1, points: 1, unestimated: 1) == "1 item · 1 point · 1 unestimated")
        #expect(text.summary(count: 2, points: nil, unestimated: 0) == "2 items")
        #expect(text.capacity(points: 18, capacity: 20) == "18 of 20 points")
        #expect(text.overCapacity(1) == "Planned work is over capacity by 1 point.")
        #expect(text.overCapacity(3) == "Planned work is over capacity by 3 points.")
        #expect(text.rowPoints(3) == "3 pt")
        #expect(text.rowPoints(nil) == "–")
        #expect(text.spokenPoints(nil) == "Not estimated")
        #expect(text.spokenPoints(3) == "3 points")
    }

    @Test("Menus and announcements name the item, the bucket and the place")
    func spoken() {
        #expect(text.moveMenu("P-1") == "Move P-1 to…")
        #expect(text.moveToBucket("Sprint 15") == "Move to Sprint 15")
        #expect(
            text.moved("P-1", bucket: "Sprint 15", position: 2, count: 5) == "P-1 moved to Sprint 15, position 2 of 5."
        )
        #expect(text.moveUp == "Move up")
        #expect(text.moveDown == "Move down")
        #expect(text.moveTo == "Move to")
        #expect(text.expanded == "Expanded")
        #expect(text.collapsed == "Collapsed")
        #expect(text.empty == "Nothing here yet.")
        #expect(text.capacityLabel == "Capacity")
        #expect(text.toggleHint == "Shows or hides the items.")
    }

    @Test("Replacements win over the built-in strings")
    func overrides() {
        let custom = MonarchBacklogPlannerText(
            labels: MonarchBacklogPlannerLabels(
                summary: { count, _, _ in "\(count) tickets" },
                rowPoints: { "\($0)sp" },
                moveUp: "Raise",
                moved: { item, bucket, _, _ in "\(item) → \(bucket)" }
            )
        )
        #expect(custom.summary(count: 4, points: 9, unestimated: 0) == "4 tickets")
        #expect(custom.rowPoints(3) == "3sp")
        #expect(custom.moveUp == "Raise")
        #expect(custom.moved("P-1", bucket: "Backlog", position: 1, count: 1) == "P-1 → Backlog")
    }

    @Test("The drag payload is the item id as text")
    func payload() {
        typealias Planner = MonarchBacklogPlanner<PlanItem, Text>
        #expect(Planner.payload(PlanItem(id: "P-9", title: "", points: nil)) == "P-9")
    }
}
