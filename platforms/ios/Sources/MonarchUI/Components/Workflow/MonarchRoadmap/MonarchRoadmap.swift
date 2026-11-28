import SwiftUI

/// A roadmap: projects over months and quarters, grouped by initiative, with
/// progress, the health last reported, milestones and dependencies between
/// projects. A planning view preset on ``MonarchGanttTimeline``: it opens at
/// the quarter zoom, writes each project's progress text and health in words
/// in its row heading, counts the projects of each group, tints at-risk and
/// off-track bars, and keeps projects without dates as rows that say
/// "No dates" so none disappears.
///
/// ```swift
/// MonarchRoadmap(
///     projects,
///     groups: [MonarchRoadmapGroup(id: "growth", label: "Grow repeat customers", health: .onTrack)],
///     onOpen: { project in selection = project.id }
/// )
/// ```
///
/// Built from ``MonarchGanttTimeline`` (and through it the system menus,
/// segmented picker and date pickers), because SwiftUI has no timeline
/// chart. The chart, the list view, the VoiceOver actions and the keyboard
/// moves are the timeline's. Read-only unless `onChange` is given.
///
/// The roadmap does not scroll vertically: place it in a scrolling screen.
/// Dates are read as calendar days in the environment's calendar and time
/// zone, and written in the environment's locale.
///
/// Accessibility: the chart and the list are containers named "Roadmap" and
/// the row header says "Project". Health is written in words in each row
/// heading, never shown by the bar colour alone; a group heading reads its
/// name, its project count and its health.
@_spi(Experimental) public struct MonarchRoadmap<ToolbarActions: View>: View {
    private let items: [MonarchRoadmapItem]
    private let externalState: Binding<MonarchGanttTimelineState>?
    private let groups: [MonarchRoadmapGroup]
    private let dependencies: [MonarchGanttTimelineDependency]
    private let milestones: [MonarchGanttTimelineMilestone]
    private let zoomLevels: [MonarchGanttTimelineZoom]
    private let today: Date?
    private let showsToolbar: Bool
    private let labels: MonarchRoadmapLabels
    private let timelineLabels: MonarchGanttTimelineLabels
    private let onChange: (@MainActor (MonarchRoadmapItem, MonarchGanttTimelineDates) -> Void)?
    private let onOpen: (@MainActor (MonarchRoadmapItem) -> Void)?
    private let toolbarActions: ToolbarActions

    @State private var ownState = MonarchGanttTimelineState(zoom: MonarchRoadmapTokens.defaultZoom)

    /// A roadmap with extra controls at the end of the toolbar.
    ///
    /// - Parameters:
    ///   - items: The projects, in order within their groups.
    ///   - state: The first visible day, the zoom, chart or list. `nil` keeps the state inside
    ///     the roadmap, which opens at the quarter zoom.
    ///   - groups: Initiatives, themes or teams as heading rows, in order. Projects without a
    ///     known group follow them.
    ///   - dependencies: Links between projects, drawn as arrows and written in the list.
    ///   - milestones: Dated checkpoints: on a project's row with `itemID`, otherwise in a lane
    ///     under the header.
    ///   - zoomLevels: The zoom levels offered in the toolbar.
    ///   - today: Any time on the day of the today line, or `nil` for no line and no Today button.
    ///     Pass a fixed date in tests and previews.
    ///   - showsToolbar: Shows the toolbar.
    ///   - labels: Replacements for the roadmap's own strings.
    ///   - timelineLabels: Replacements for the timeline's strings. The chart is named "Roadmap"
    ///     and the row header says "Project" unless replaced here.
    ///   - onChange: Makes dates editable and is called with the project and its new dates.
    ///     Without it the roadmap is read-only.
    ///   - onOpen: Makes each bar and list row a button that opens the project.
    ///   - toolbarActions: Extra controls at the end of the toolbar, such as a team filter.
    public init(
        _ items: [MonarchRoadmapItem],
        state: Binding<MonarchGanttTimelineState>? = nil,
        groups: [MonarchRoadmapGroup] = [],
        dependencies: [MonarchGanttTimelineDependency] = [],
        milestones: [MonarchGanttTimelineMilestone] = [],
        zoomLevels: [MonarchGanttTimelineZoom] = [.month, .quarter, .year],
        today: Date? = Date(),
        showsToolbar: Bool = true,
        labels: MonarchRoadmapLabels = MonarchRoadmapLabels(),
        timelineLabels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
        onChange: (@MainActor (MonarchRoadmapItem, MonarchGanttTimelineDates) -> Void)? = nil,
        onOpen: (@MainActor (MonarchRoadmapItem) -> Void)? = nil,
        @ViewBuilder toolbarActions: () -> ToolbarActions
    ) {
        self.items = items
        self.externalState = state
        self.groups = groups
        self.dependencies = dependencies
        self.milestones = milestones
        self.zoomLevels = zoomLevels
        self.today = today
        self.showsToolbar = showsToolbar
        self.labels = labels
        self.timelineLabels = timelineLabels
        self.onChange = onChange
        self.onOpen = onOpen
        self.toolbarActions = toolbarActions()
    }

    /// The content of the view.
    public var body: some View {
        let text = MonarchRoadmapText(labels: labels)
        let rows = MonarchRoadmapRules.rows(items, groups: groups, text: text)
        let byID = rows.byID
        let groupHealth = Dictionary(groups.map { ($0.id, $0.health) }, uniquingKeysWith: { first, _ in first })
        // As on the web the progress text and the health badge (a tone with a dot) sit beside the name and a
        // group shows only its badge, so the words are drawn here instead of as a plain meta text.
        MonarchGanttTimeline(
            rows.items.map { row in
                var shown = row
                shown.meta = nil
                return shown
            },
            state: externalState ?? $ownState,
            groups: rows.groups.map { group in
                var shown = group
                shown.meta = nil
                return shown
            },
            dependencies: dependencies,
            milestones: milestones,
            zoomLevels: zoomLevels,
            undated: .row,
            today: today,
            showsToolbar: showsToolbar,
            labels: MonarchRoadmapRules.timelineLabels(timelineLabels),
            onChange: MonarchRoadmapRules.change(onChange, rows: rows),
            onOpen: MonarchRoadmapRules.open(onOpen, rows: rows),
            itemMeta: { row in
                AnyView(
                    MonarchRoadmapMeta(
                        progress: byID[row.id]?.progressText,
                        health: byID[row.id]?.health,
                        text: text
                    )
                )
            },
            groupMeta: { group in
                AnyView(MonarchRoadmapMeta(progress: nil, health: groupHealth[group.id] ?? nil, text: text))
            },
            toolbarActions: { toolbarActions }
        )
    }
}

extension MonarchRoadmap where ToolbarActions == EmptyView {
    /// A roadmap.
    ///
    /// - Parameters:
    ///   - items: The projects, in order within their groups.
    ///   - state: The first visible day, the zoom, chart or list. `nil` keeps the state inside
    ///     the roadmap, which opens at the quarter zoom.
    ///   - groups: Initiatives, themes or teams as heading rows, in order.
    ///   - dependencies: Links between projects, drawn as arrows and written in the list.
    ///   - milestones: Dated checkpoints, drawn as diamonds.
    ///   - zoomLevels: The zoom levels offered in the toolbar.
    ///   - today: Any time on the day of the today line, or `nil` for no line and no Today button.
    ///   - showsToolbar: Shows the toolbar.
    ///   - labels: Replacements for the roadmap's own strings.
    ///   - timelineLabels: Replacements for the timeline's strings.
    ///   - onChange: Makes dates editable. Without it the roadmap is read-only.
    ///   - onOpen: Makes each bar and list row a button that opens the project.
    public init(
        _ items: [MonarchRoadmapItem],
        state: Binding<MonarchGanttTimelineState>? = nil,
        groups: [MonarchRoadmapGroup] = [],
        dependencies: [MonarchGanttTimelineDependency] = [],
        milestones: [MonarchGanttTimelineMilestone] = [],
        zoomLevels: [MonarchGanttTimelineZoom] = [.month, .quarter, .year],
        today: Date? = Date(),
        showsToolbar: Bool = true,
        labels: MonarchRoadmapLabels = MonarchRoadmapLabels(),
        timelineLabels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
        onChange: (@MainActor (MonarchRoadmapItem, MonarchGanttTimelineDates) -> Void)? = nil,
        onOpen: (@MainActor (MonarchRoadmapItem) -> Void)? = nil
    ) {
        self.init(
            items,
            state: state,
            groups: groups,
            dependencies: dependencies,
            milestones: milestones,
            zoomLevels: zoomLevels,
            today: today,
            showsToolbar: showsToolbar,
            labels: labels,
            timelineLabels: timelineLabels,
            onChange: onChange,
            onOpen: onOpen,
            toolbarActions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchRoadmap") {
        let calendar = Calendar(identifier: .gregorian)
        let start = calendar.date(from: DateComponents(year: 2026, month: 1, day: 1)) ?? Date()
        let day = { (offset: Int) in calendar.date(byAdding: .day, value: offset, to: start) }
        let items = [
            MonarchRoadmapItem(
                id: "p1",
                label: "Loyalty emails",
                start: day(4),
                end: day(78),
                groupID: "growth",
                progress: 0.4,
                progressText: "8/20",
                health: .onTrack
            ),
            MonarchRoadmapItem(
                id: "p3",
                label: "Help centre search",
                start: day(32),
                end: day(119),
                groupID: "quality",
                progress: 0.25,
                health: .atRisk
            ),
            MonarchRoadmapItem(
                id: "p4",
                label: "Crash fixes",
                start: day(11),
                end: day(57),
                groupID: "quality",
                progress: 0.7,
                health: .offTrack
            ),
            MonarchRoadmapItem(id: "p5", label: "Billing revamp", progressText: "0/6", health: .notReported),
        ]
        let groups = [
            MonarchRoadmapGroup(id: "growth", label: "Grow repeat customers", health: .onTrack),
            MonarchRoadmapGroup(id: "quality", label: "Fewer support tickets", health: .atRisk),
        ]
        MonarchPreviewMatrix {
            MonarchRoadmap(
                items,
                state: .constant(MonarchGanttTimelineState(start: start, zoom: .quarter)),
                groups: groups,
                dependencies: [MonarchGanttTimelineDependency(id: "d1", from: "p4", to: "p3")],
                today: day(13),
                onOpen: { _ in }
            )
            MonarchRoadmap(
                items,
                state: .constant(MonarchGanttTimelineState(start: start, view: .list)),
                groups: groups,
                today: nil,
                showsToolbar: false
            )
        }
    }
#endif
