@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchRoadmapSection: CataloguePage {
    static let title = "Roadmap"

    private static let calendar = Calendar.current
    private static let start = calendar.startOfDay(for: Date())

    private static func day(_ offset: Int) -> Date {
        calendar.date(byAdding: .day, value: offset, to: start) ?? start
    }

    private static let groups = [
        MonarchRoadmapGroup(
            id: "growth",
            label: "Grow repeat customers",
            start: day(-20),
            end: day(160),
            health: .onTrack
        ),
        MonarchRoadmapGroup(
            id: "quality",
            label: "Fewer support tickets",
            start: day(10),
            end: day(220),
            health: .atRisk
        ),
    ]

    @State private var items = [
        MonarchRoadmapItem(
            id: "p1",
            label: "Loyalty emails",
            start: day(-10),
            end: day(64),
            groupID: "growth",
            progress: 0.4,
            progressText: "8/20",
            health: .onTrack
        ),
        MonarchRoadmapItem(
            id: "p2",
            label: "Referral page",
            start: day(45),
            end: day(120),
            groupID: "growth",
            progress: 0.1,
            progressText: "1/10",
            health: .notReported
        ),
        MonarchRoadmapItem(
            id: "p3",
            label: "Help centre search",
            start: day(18),
            end: day(105),
            groupID: "quality",
            progress: 0.25,
            progressText: "3/12",
            health: .atRisk
        ),
        MonarchRoadmapItem(
            id: "p4",
            label: "Crash fixes",
            start: day(-3),
            end: day(43),
            groupID: "quality",
            progress: 0.7,
            progressText: "14/20",
            health: .offTrack
        ),
        MonarchRoadmapItem(id: "p5", label: "Billing revamp", progressText: "0/6"),
    ]
    @State private var state = MonarchGanttTimelineState(zoom: .quarter)
    @State private var opened: String?

    var body: some View {
        CatalogueCaption("Editable: touch and hold a bar to move it, or an end to change that date")
        MonarchRoadmap(
            items,
            state: $state,
            groups: Self.groups,
            dependencies: [MonarchGanttTimelineDependency(id: "d1", from: "p4", to: "p3")],
            milestones: [MonarchGanttTimelineMilestone(id: "m1", label: "Launch", date: Self.day(64), itemID: "p1")],
            onChange: { item, dates in
                guard let index = items.firstIndex(where: { $0.id == item.id }) else { return }
                items[index].start = dates.start
                items[index].end = dates.end
            },
            onOpen: { item in opened = item.label }
        )
        Text("Opened: \(opened ?? "nothing")")
            .monarchTextStyle(.caption)

        CatalogueCaption("Read only, as a list, without the toolbar")
        MonarchRoadmap(
            items,
            state: .constant(MonarchGanttTimelineState(view: .list)),
            groups: Self.groups,
            dependencies: [MonarchGanttTimelineDependency(id: "d1", from: "p4", to: "p3")],
            showsToolbar: false
        )
    }
}
