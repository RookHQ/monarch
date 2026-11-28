import SwiftUI

/// A Gantt timeline: items as bars on a calendar from their first to their
/// last day, with dependency arrows, milestones, a today line, zoom from
/// weeks to years, and a list view that writes every date in words and
/// changes dates with the system date pickers. Data in, changes out: the app
/// saves each change and passes the new items.
///
/// ```swift
/// MonarchGanttTimeline(
///     items,
///     state: $timeline,
///     dependencies: [MonarchGanttTimelineDependency(id: "d1", from: "design", to: "build")],
///     onChange: { item, dates in store.reschedule(item.id, to: dates) },
///     onOpen: { item in selection = item.id }
/// )
/// ```
///
/// Built by Monarch from Monarch and system parts, because SwiftUI has no
/// timeline chart. The toolbar is a ``MonarchCalendarToolbar`` with a zoom
/// menu and a segmented chart and list switch. The chart keeps the row
/// labels at the leading edge and scrolls the calendar sideways; its time
/// axis runs left to right in every layout direction, as on the web. When
/// the timeline is editable a bar is moved by touching and holding it and
/// then dragging, its ends by holding near an end, with a haptic tick per
/// day; a hardware keyboard moves a focused bar with the arrow keys (Shift
/// for the end, Option for the start). The list is the alternative for
/// phones and VoiceOver: nothing in the chart is only available by dragging.
///
/// The timeline does not scroll vertically: place it in a scrolling screen.
/// Dates are read as calendar days in the environment's calendar and time
/// zone, and written in the environment's locale.
///
/// Accessibility: the chart and the list are containers named "Timeline".
/// Each bar is one element read as "Design, Jan 12, 2026 to Jan 24, 2026,
/// 60% complete", with the conflict after it; with `onOpen` it is a button.
/// Editable bars have six actions (move, start and end a day earlier or
/// later) and every change is announced. Parents read Expanded or
/// Collapsed. Header labels, gridlines, the today line and arrows are hidden.
@_spi(Experimental) public struct MonarchGanttTimeline<ToolbarActions: View>: View {
    private let items: [MonarchGanttTimelineItem]
    private let externalState: Binding<MonarchGanttTimelineState>?
    private let groups: [MonarchGanttTimelineGroup]
    private let dependencies: [MonarchGanttTimelineDependency]
    private let milestones: [MonarchGanttTimelineMilestone]
    private let zoomLevels: [MonarchGanttTimelineZoom]
    private let undated: MonarchGanttTimelineUndated
    private let today: Date?
    private let showsToolbar: Bool
    private let labels: MonarchGanttTimelineLabels
    private let onChange: (@MainActor (MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Void)?
    private let onOpen: (@MainActor (MonarchGanttTimelineItem) -> Void)?
    private let itemMeta: (@MainActor (MonarchGanttTimelineItem) -> AnyView)?
    private let groupMeta: (@MainActor (MonarchGanttTimelineGroup) -> AnyView)?
    private let toolbarActions: ToolbarActions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.locale) private var locale
    @State private var ownState = MonarchGanttTimelineState()

    /// A Gantt timeline with extra controls at the end of the toolbar.
    ///
    /// - Parameters:
    ///   - items: The rows. Nesting comes from `parentID`, grouping from `groupID`.
    ///   - state: The first visible day, the zoom, chart or list and the collapsed parents.
    ///     `nil` keeps the state inside the timeline.
    ///   - groups: Heading rows that group items.
    ///   - dependencies: Links between items, drawn as arrows.
    ///   - milestones: Dated checkpoints, drawn as diamonds.
    ///   - zoomLevels: The zoom levels offered in the toolbar. With one level or none there is no zoom menu.
    ///   - undated: Where top-level items without dates go.
    ///   - today: Any time on the day of the today line, or `nil` for no line and no Today button.
    ///     Pass a fixed date in tests and previews.
    ///   - showsToolbar: Shows the toolbar.
    ///   - labels: Replacements for the built-in strings.
    ///   - onChange: Called with an item and its new dates after a drag, a key press, a VoiceOver
    ///     action or the date sheet. Without it the timeline is read-only.
    ///   - onOpen: Makes each bar and list row a button that opens the item.
    ///   - itemMeta: Extra content under an item's name, after its `meta` text, in the chart's row labels and
    ///     the list: for example a status badge or an assignee avatar. VoiceOver reads it where it is drawn.
    ///   - groupMeta: Extra content under a group's heading, after its `meta` text.
    ///   - toolbarActions: Extra controls at the end of the toolbar, such as a filter.
    public init(
        _ items: [MonarchGanttTimelineItem],
        state: Binding<MonarchGanttTimelineState>? = nil,
        groups: [MonarchGanttTimelineGroup] = [],
        dependencies: [MonarchGanttTimelineDependency] = [],
        milestones: [MonarchGanttTimelineMilestone] = [],
        zoomLevels: [MonarchGanttTimelineZoom] = [.month, .quarter, .year],
        undated: MonarchGanttTimelineUndated = .list,
        today: Date? = Date(),
        showsToolbar: Bool = true,
        labels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
        onChange: (@MainActor (MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Void)? = nil,
        onOpen: (@MainActor (MonarchGanttTimelineItem) -> Void)? = nil,
        itemMeta: (@MainActor (MonarchGanttTimelineItem) -> AnyView)? = nil,
        groupMeta: (@MainActor (MonarchGanttTimelineGroup) -> AnyView)? = nil,
        @ViewBuilder toolbarActions: () -> ToolbarActions
    ) {
        self.items = items
        self.externalState = state
        self.groups = groups
        self.dependencies = dependencies
        self.milestones = milestones
        self.zoomLevels = zoomLevels
        self.undated = undated
        self.today = today
        self.showsToolbar = showsToolbar
        self.labels = labels
        self.onChange = onChange
        self.onOpen = onOpen
        self.itemMeta = itemMeta
        self.groupMeta = groupMeta
        self.toolbarActions = toolbarActions()
    }

    /// The content of the view.
    public var body: some View {
        let model = self.model
        VStack(alignment: .leading, spacing: MonarchGanttTimelineTokens.spacing) {
            if showsToolbar {
                MonarchGanttTimelineToolbar(model: model, zoomLevels: zoomLevels, actions: toolbarActions)
            }
            switch model.state.view {
            case .chart:
                if model.isEditable {
                    Text(model.text.hint)
                        .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                        .foregroundStyle(theme.colors[role: MonarchGanttTimelineTokens.mutedColor])
                        .fixedSize(horizontal: false, vertical: true)
                }
                MonarchGanttTimelineChart(model: model)
                if !model.lines.unscheduled.isEmpty {
                    DisclosureGroup(model.text.unscheduled(model.lines.unscheduled.count)) {
                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(Array(model.lines.unscheduled.enumerated()), id: \.element.id) { index, item in
                                if index > 0 { MonarchDivider() }
                                MonarchGanttTimelineItemRow(model: model, item: item, depth: 0, children: 0)
                            }
                        }
                    }
                    .disclosureGroupStyle(.monarch)
                }
            case .list:
                MonarchGanttTimelineList(model: model)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var state: MonarchGanttTimelineState {
        externalState?.wrappedValue ?? ownState
    }

    private var model: MonarchGanttTimelineModel {
        var zoned = calendar
        zoned.timeZone = timeZone
        let externalState = externalState
        let ownState = $ownState
        return MonarchGanttTimelineModel.make(
            items: items,
            groups: groups,
            dependencies: dependencies,
            milestones: milestones,
            state: state,
            undated: undated,
            today: today,
            calendar: zoned,
            locale: locale,
            labels: labels,
            onChange: onChange,
            onOpen: onOpen,
            setState: { next in
                if let externalState {
                    externalState.wrappedValue = next
                } else {
                    ownState.wrappedValue = next
                }
            },
            itemMeta: itemMeta,
            groupMeta: groupMeta
        )
    }
}

extension MonarchGanttTimeline where ToolbarActions == EmptyView {
    /// A Gantt timeline.
    ///
    /// - Parameters:
    ///   - items: The rows. Nesting comes from `parentID`, grouping from `groupID`.
    ///   - state: The first visible day, the zoom, chart or list and the collapsed parents.
    ///     `nil` keeps the state inside the timeline.
    ///   - groups: Heading rows that group items.
    ///   - dependencies: Links between items, drawn as arrows.
    ///   - milestones: Dated checkpoints, drawn as diamonds.
    ///   - zoomLevels: The zoom levels offered in the toolbar.
    ///   - undated: Where top-level items without dates go.
    ///   - today: Any time on the day of the today line, or `nil` for no line and no Today button.
    ///   - showsToolbar: Shows the toolbar.
    ///   - labels: Replacements for the built-in strings.
    ///   - onChange: Called with an item and its new dates. Without it the timeline is read-only.
    ///   - onOpen: Makes each bar and list row a button that opens the item.
    ///   - itemMeta: Extra content under an item's name, such as a status badge. VoiceOver reads it where it is drawn.
    ///   - groupMeta: Extra content under a group's heading, after its `meta` text.
    public init(
        _ items: [MonarchGanttTimelineItem],
        state: Binding<MonarchGanttTimelineState>? = nil,
        groups: [MonarchGanttTimelineGroup] = [],
        dependencies: [MonarchGanttTimelineDependency] = [],
        milestones: [MonarchGanttTimelineMilestone] = [],
        zoomLevels: [MonarchGanttTimelineZoom] = [.month, .quarter, .year],
        undated: MonarchGanttTimelineUndated = .list,
        today: Date? = Date(),
        showsToolbar: Bool = true,
        labels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
        onChange: (@MainActor (MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Void)? = nil,
        onOpen: (@MainActor (MonarchGanttTimelineItem) -> Void)? = nil,
        itemMeta: (@MainActor (MonarchGanttTimelineItem) -> AnyView)? = nil,
        groupMeta: (@MainActor (MonarchGanttTimelineGroup) -> AnyView)? = nil
    ) {
        self.init(
            items,
            state: state,
            groups: groups,
            dependencies: dependencies,
            milestones: milestones,
            zoomLevels: zoomLevels,
            undated: undated,
            today: today,
            showsToolbar: showsToolbar,
            labels: labels,
            onChange: onChange,
            onOpen: onOpen,
            itemMeta: itemMeta,
            groupMeta: groupMeta,
            toolbarActions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchGanttTimeline") {
        let calendar = Calendar(identifier: .gregorian)
        let start = calendar.date(from: DateComponents(year: 2026, month: 1, day: 5)) ?? Date()
        let day = { (offset: Int) in calendar.date(byAdding: .day, value: offset, to: start) }
        let items = [
            MonarchGanttTimelineItem(id: "design", label: "Design", start: day(7), end: day(18), progress: 0.6),
            MonarchGanttTimelineItem(
                id: "build",
                label: "Build",
                start: day(16),
                end: day(39),
                conflict: "Starts before Design ends"
            ),
            MonarchGanttTimelineItem(id: "api", label: "API changes", start: day(21), end: day(30), parentID: "build"),
            MonarchGanttTimelineItem(id: "launch", label: "Launch", end: day(46), tone: .success),
            MonarchGanttTimelineItem(id: "docs", label: "Docs"),
        ]
        MonarchPreviewMatrix {
            MonarchGanttTimeline(
                items,
                state: .constant(MonarchGanttTimelineState(start: start)),
                dependencies: [MonarchGanttTimelineDependency(id: "d1", from: "design", to: "build", isConflict: true)],
                milestones: [MonarchGanttTimelineMilestone(id: "m1", label: "Review", date: day(18) ?? start)],
                today: day(9),
                onChange: { _, _ in },
                onOpen: { _ in }
            )
            MonarchGanttTimeline(
                items,
                state: .constant(MonarchGanttTimelineState(start: start, view: .list)),
                today: nil,
                showsToolbar: false
            )
        }
    }
#endif
