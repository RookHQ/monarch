@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchGanttTimelineSection: CataloguePage {
    static let title = "Gantt timeline"

    private static let calendar = Calendar.current
    private static let start = calendar.startOfDay(for: Date())

    private static func day(_ offset: Int) -> Date {
        calendar.date(byAdding: .day, value: offset, to: start) ?? start
    }

    @State private var items = [
        MonarchGanttTimelineItem(id: "design", label: "Design", start: day(-3), end: day(8), progress: 0.6),
        MonarchGanttTimelineItem(
            id: "build",
            label: "Build",
            start: day(6),
            end: day(29),
            conflict: "Starts before Design ends"
        ),
        MonarchGanttTimelineItem(id: "api", label: "API changes", start: day(11), end: day(20), parentID: "build"),
        MonarchGanttTimelineItem(id: "qa", label: "Testing", start: day(24), end: day(34), tone: .info),
        MonarchGanttTimelineItem(id: "launch", label: "Launch", end: day(36), tone: .success, isEditable: false),
        MonarchGanttTimelineItem(id: "docs", label: "Docs"),
    ]
    @State private var state = MonarchGanttTimelineState()
    @State private var opened: String?

    var body: some View {
        CatalogueCaption("Editable: touch and hold a bar to move it, or an end to change that date")
        MonarchGanttTimeline(
            items,
            state: $state,
            dependencies: [
                MonarchGanttTimelineDependency(id: "d1", from: "design", to: "build", isConflict: true),
                MonarchGanttTimelineDependency(id: "d2", from: "build", to: "qa", kind: .finishToFinish),
            ],
            milestones: [
                MonarchGanttTimelineMilestone(id: "m1", label: "Design review", date: Self.day(8), isDone: true),
                MonarchGanttTimelineMilestone(id: "m2", label: "API frozen", date: Self.day(20), itemID: "api"),
            ],
            zoomLevels: MonarchGanttTimelineZoom.allCases,
            onChange: { item, dates in
                guard let index = items.firstIndex(where: { $0.id == item.id }) else { return }
                items[index].start = dates.start
                items[index].end = dates.end
            },
            onOpen: { item in opened = item.label }
        )
        Text("Opened: \(opened ?? "nothing")")
            .monarchTextStyle(.caption)

        CatalogueCaption("Read only, with groups and no toolbar")
        MonarchGanttTimeline(
            [
                MonarchGanttTimelineItem(
                    id: "a",
                    label: "Research",
                    start: Self.day(0),
                    end: Self.day(6),
                    groupID: "g1"
                ),
                MonarchGanttTimelineItem(
                    id: "b",
                    label: "Prototype",
                    start: Self.day(5),
                    end: Self.day(20),
                    groupID: "g1"
                ),
                MonarchGanttTimelineItem(id: "c", label: "Pilot", start: Self.day(22), groupID: "g2"),
            ],
            state: .constant(MonarchGanttTimelineState(zoom: .week)),
            groups: [
                MonarchGanttTimelineGroup(id: "g1", label: "Phase one", start: Self.day(0), end: Self.day(20)),
                MonarchGanttTimelineGroup(id: "g2", label: "Phase two", meta: "Planned"),
            ],
            showsToolbar: false
        )
    }
}
