import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// A fixed calendar, time zone and locale, so dates and words never depend on the machine.
private enum GanttTimelineTestClock {
    static let locale = Locale(identifier: "en_US")

    static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        calendar.locale = locale
        calendar.firstWeekday = 2
        return calendar
    }()

    /// Monday 5 January 2026, at noon so any time of day is read as its date.
    static let jan5: Date = calendar.date(from: DateComponents(year: 2026, month: 1, day: 5, hour: 12)) ?? .distantPast

    static func day(_ offset: Int) -> Date {
        calendar.date(byAdding: .day, value: offset, to: jan5) ?? jan5
    }

    static let days = MonarchGanttTimelineDays(calendar: calendar)
}

enum MonarchGanttTimelineFixtures: ComponentFixtures {
    static let id = "gantt-timeline"

    private static func day(_ offset: Int) -> Date { GanttTimelineTestClock.day(offset) }

    static let items = [
        MonarchGanttTimelineItem(id: "design", label: "Design", start: day(7), end: day(18), progress: 0.6),
        MonarchGanttTimelineItem(
            id: "build",
            label: "Build",
            start: day(16),
            end: day(39),
            conflict: "Starts before Design ends",
            meta: "In progress"
        ),
        MonarchGanttTimelineItem(id: "api", label: "API changes", start: day(21), end: day(30), parentID: "build"),
        MonarchGanttTimelineItem(id: "launch", label: "Launch", end: day(46), tone: .success, isEditable: false),
        MonarchGanttTimelineItem(id: "docs", label: "Docs"),
    ]

    static let tones: [MonarchGanttTimelineItem] =
        [nil, .neutral, .info, .success, .warning, .danger].enumerated().map { index, tone in
            MonarchGanttTimelineItem(
                id: "t\(index)",
                label: "Tone \(index)",
                start: day(index * 3),
                end: day(index * 3 + 10),
                progress: Double(index) / 5,
                tone: tone
            )
        }

    static let dependencies = [
        MonarchGanttTimelineDependency(id: "d1", from: "design", to: "build", isConflict: true),
        MonarchGanttTimelineDependency(id: "d2", from: "design", to: "api", kind: .startToStart),
        MonarchGanttTimelineDependency(id: "d3", from: "api", to: "build", kind: .finishToFinish),
    ]

    static let milestones = [
        MonarchGanttTimelineMilestone(id: "m1", label: "Design review", date: day(18), isDone: true),
        MonarchGanttTimelineMilestone(id: "m2", label: "API frozen", date: day(30), itemID: "api"),
    ]

    static let groups = [
        MonarchGanttTimelineGroup(id: "g1", label: "Phase one", start: day(0), end: day(20), meta: "Q1"),
        MonarchGanttTimelineGroup(id: "g2", label: "Phase two"),
    ]

    static let grouped = [
        MonarchGanttTimelineItem(id: "a", label: "Research", start: day(0), end: day(6), groupID: "g1"),
        MonarchGanttTimelineItem(id: "b", label: "Prototype", start: day(5), end: day(20), groupID: "g1"),
        MonarchGanttTimelineItem(id: "c", label: "Pilot", start: day(21), groupID: "g2"),
        MonarchGanttTimelineItem(id: "d", label: "Old work", start: day(-60), end: day(-40)),
        MonarchGanttTimelineItem(id: "e", label: "Next year", start: day(400), end: day(420)),
    ]

    static func state(_ zoom: MonarchGanttTimelineZoom = .month, view: MonarchGanttTimelineView = .chart)
        -> Binding<MonarchGanttTimelineState>
    {
        .constant(MonarchGanttTimelineState(start: GanttTimelineTestClock.jan5, zoom: zoom, view: view))
    }

    @MainActor static func timeline(_ content: some View) -> some View {
        content
            .environment(\.calendar, GanttTimelineTestClock.calendar)
            .environment(\.timeZone, GanttTimelineTestClock.calendar.timeZone)
            .environment(\.locale, GanttTimelineTestClock.locale)
    }

    static let fixtures = [
        Fixture("editable chart, dependencies, milestones, conflict, today") {
            timeline(
                MonarchGanttTimeline(
                    items,
                    state: state(),
                    dependencies: dependencies,
                    milestones: milestones,
                    today: day(9),
                    onChange: { _, _ in },
                    onOpen: { _ in }
                )
            )
        },
        Fixture("read-only list with sub-items, unscheduled and milestones") {
            timeline(
                MonarchGanttTimeline(
                    items,
                    state: state(view: .list),
                    dependencies: dependencies,
                    milestones: milestones,
                    today: nil
                )
            )
        },
        Fixture("editable list with Change dates") {
            timeline(MonarchGanttTimeline(items, state: state(view: .list), today: day(9), onChange: { _, _ in }))
        },
        Fixture("groups, week zoom, ended and starts notes") {
            timeline(MonarchGanttTimeline(grouped, state: state(.week), groups: groups, today: day(3)))
        },
        Fixture("every tone with progress, quarter zoom") {
            timeline(MonarchGanttTimeline(tones, state: state(.quarter), today: day(2)))
        },
        Fixture("year zoom, undated as rows, collapsed parent") {
            timeline(
                MonarchGanttTimeline(
                    items,
                    state: .constant(
                        MonarchGanttTimelineState(start: GanttTimelineTestClock.jan5, zoom: .year, collapsed: ["build"])
                    ),
                    zoomLevels: [.year],
                    undated: .row,
                    today: day(9)
                )
            )
        },
        Fixture("no toolbar, long labels") {
            timeline(
                MonarchGanttTimeline(
                    [
                        MonarchGanttTimelineItem(
                            id: "long",
                            label: "A very long item name that wraps onto several lines in the label column",
                            start: day(2),
                            end: day(3),
                            meta: "A long status text under the name"
                        )
                    ],
                    state: state(),
                    today: nil,
                    showsToolbar: false
                )
            )
        },
        Fixture("empty, with toolbar actions") {
            timeline(
                MonarchGanttTimeline([], state: state(), today: day(0)) {
                    MonarchButton("New", systemImage: "plus", variant: .primary, size: .sm) {}
                }
            )
        },
        Fixture("meta slots in the chart and the list") {
            VStack {
                timeline(
                    MonarchGanttTimeline(
                        grouped,
                        state: state(),
                        groups: groups,
                        today: day(3),
                        itemMeta: { item in AnyView(MonarchBadge(item.label.count.formatted())) },
                        groupMeta: { _ in AnyView(MonarchBadge("Group")) }
                    )
                )
                timeline(
                    MonarchGanttTimeline(
                        grouped,
                        state: state(view: .list),
                        groups: groups,
                        today: day(3),
                        itemMeta: { item in AnyView(MonarchBadge(item.label.count.formatted())) },
                        groupMeta: { _ in AnyView(MonarchBadge("Group")) }
                    )
                )
            }
        },
        Fixture("own state") {
            timeline(MonarchGanttTimeline(items, today: day(9)))
        },
    ]
}

@MainActor
@Suite("MonarchGanttTimeline")
struct MonarchGanttTimelineTests {
    private let days = GanttTimelineTestClock.days
    private let calendar = GanttTimelineTestClock.calendar
    private func day(_ offset: Int) -> Date { GanttTimelineTestClock.day(offset) }
    private var jan5: Int { days.day(GanttTimelineTestClock.jan5) }

    @Test("The model carries the meta slots, so the chart's labels and the list draw them")
    func metaSlots() {
        let item = MonarchGanttTimelineItem(id: "a", label: "Design")
        let group = MonarchGanttTimelineGroup(id: "g", label: "Team")
        let without = MonarchGanttTimelineModel.make(
            items: [item],
            today: nil,
            calendar: calendar,
            locale: Locale(identifier: "en_US")
        )
        #expect(without.itemMeta == nil && without.groupMeta == nil)
        let with = MonarchGanttTimelineModel.make(
            items: [item],
            groups: [group],
            today: nil,
            calendar: calendar,
            locale: Locale(identifier: "en_US"),
            itemMeta: { _ in AnyView(EmptyView()) },
            groupMeta: { _ in AnyView(EmptyView()) }
        )
        #expect(with.itemMeta != nil && with.groupMeta != nil)
    }
    private func dayOf(_ date: Date?) -> Int? { date.map { days.day($0) } }

    private func model(
        _ items: [MonarchGanttTimelineItem] = MonarchGanttTimelineFixtures.items,
        state: MonarchGanttTimelineState = MonarchGanttTimelineState(start: GanttTimelineTestClock.jan5),
        today: Date? = GanttTimelineTestClock.day(9),
        labels: MonarchGanttTimelineLabels = MonarchGanttTimelineLabels(),
        isEditable: Bool = true,
        undated: MonarchGanttTimelineUndated = .list
    ) -> MonarchGanttTimelineModel {
        var change: (@MainActor (MonarchGanttTimelineItem, MonarchGanttTimelineDates) -> Void)?
        if isEditable {
            change = { item, dates in _ = (item, dates) }
        }
        return .make(
            items: items,
            dependencies: MonarchGanttTimelineFixtures.dependencies,
            milestones: MonarchGanttTimelineFixtures.milestones,
            state: state,
            undated: undated,
            today: today,
            calendar: calendar,
            locale: GanttTimelineTestClock.locale,
            labels: labels,
            onChange: change
        )
    }

    // MARK: Days and dates

    @Test func daysRoundTripAndReadAnyTimeOfDay() {
        let late = calendar.date(byAdding: .hour, value: 11, to: day(0)) ?? day(0)
        #expect(days.day(late) == jan5)
        #expect(days.date(jan5) == calendar.startOfDay(for: day(0)))
        #expect(days.startOfWeek(jan5 + 3) == jan5)
        #expect(days.isStartOfWeek(jan5))
        #expect(days.isWeekend(jan5 + 5))
        #expect(!days.isWeekend(jan5))
        #expect(days.dayAndMonth(jan5).day == 5)
        #expect(days.dayAndMonth(jan5).month == 1)
    }

    @Test func shiftedMovesBothDatesOrOneEndWithoutCrossing() {
        let dates = MonarchGanttTimelineDates(start: day(0), end: day(2))
        let moved = dates.shifted(.move, by: 3, in: calendar)
        #expect(dayOf(moved.start) == jan5 + 3)
        #expect(dayOf(moved.end) == jan5 + 5)
        #expect(dayOf(dates.shifted(.start, by: -1, in: calendar).start) == jan5 - 1)
        #expect(dayOf(dates.shifted(.start, by: 5, in: calendar).start) == jan5 + 2)
        #expect(dayOf(dates.shifted(.end, by: -5, in: calendar).end) == jan5)
        #expect(dayOf(dates.shifted(.end, by: 1, in: calendar).end) == jan5 + 3)
        let zero = dates.shifted(.move, by: 0, in: calendar)
        #expect(zero.start == calendar.startOfDay(for: day(0)))
    }

    @Test func shiftedKeepsAOneDayMarkerAMarker() {
        let point = MonarchGanttTimelineDates(start: nil, end: day(4))
        let moved = point.shifted(.move, by: 1, in: calendar)
        #expect(moved.start == nil)
        #expect(dayOf(moved.end) == jan5 + 5)
        #expect(MonarchGanttTimelineRules.isPoint(point))
        #expect(!MonarchGanttTimelineRules.isPoint(MonarchGanttTimelineDates(start: nil, end: nil)))
    }

    @Test func spanSwapsReversedDatesAndHandlesMissingOnes() {
        #expect(MonarchGanttTimelineRules.span(day(5), day(1), days: days) == .init(start: jan5 + 1, end: jan5 + 5))
        #expect(MonarchGanttTimelineRules.span(nil, day(2), days: days)?.days == 1)
        #expect(MonarchGanttTimelineRules.span(nil, nil, days: days) == nil)
    }

    // MARK: Rows

    @Test func linesNestChildrenAndListUnscheduledItemsApart() {
        let lines = MonarchGanttTimelineRules.lines(
            MonarchGanttTimelineFixtures.items,
            groups: [],
            collapsed: [],
            undated: .list,
            days: days
        )
        #expect(lines.lines.map(\.key) == ["i:design", "i:build", "i:api", "i:launch"])
        #expect(lines.unscheduled.map(\.id) == ["docs"])
        guard case .item(_, let depth, _, _) = lines.lines[2], case .item(_, _, let children, _) = lines.lines[1] else {
            Issue.record("expected items")
            return
        }
        #expect(depth == 1)
        #expect(children == 1)
    }

    @Test func linesHideCollapsedChildrenAndKeepUndatedRows() {
        let lines = MonarchGanttTimelineRules.lines(
            MonarchGanttTimelineFixtures.items,
            groups: [],
            collapsed: ["build"],
            undated: .row,
            days: days
        )
        #expect(lines.lines.map(\.key) == ["i:design", "i:build", "i:launch", "i:docs"])
        #expect(lines.unscheduled.isEmpty)
    }

    @Test func linesPutGroupsFirstAndIgnoreCyclesAndUnknownParents() {
        let items = [
            MonarchGanttTimelineItem(id: "x", label: "X", start: day(0), parentID: "y"),
            MonarchGanttTimelineItem(id: "y", label: "Y", start: day(0), parentID: "x"),
            MonarchGanttTimelineItem(id: "z", label: "Z", start: day(0), parentID: "missing", groupID: "g"),
            MonarchGanttTimelineItem(id: "self", label: "Self", start: day(0), parentID: "self", groupID: "nope"),
        ]
        let lines = MonarchGanttTimelineRules.lines(
            items,
            groups: [MonarchGanttTimelineGroup(id: "g", label: "G")],
            collapsed: [],
            undated: .list,
            days: days
        )
        #expect(lines.lines.map(\.key) == ["g:g", "i:z", "i:self"])
    }

    @Test func aParentWithoutDatesRollsUpItsChildren() {
        let items = [
            MonarchGanttTimelineItem(id: "p", label: "Parent"),
            MonarchGanttTimelineItem(id: "c1", label: "One", start: day(2), end: day(4), parentID: "p"),
            MonarchGanttTimelineItem(id: "c2", label: "Two", start: day(8), parentID: "p"),
        ]
        let lines = MonarchGanttTimelineRules.lines(items, groups: [], collapsed: [], undated: .list, days: days)
        guard case .item(_, _, _, let rollup) = lines.lines.first else {
            Issue.record("expected the parent")
            return
        }
        #expect(rollup == MonarchGanttTimelineSpan(start: jan5 + 2, end: jan5 + 8))
    }

    // MARK: Window and ticks

    @Test func anchorStartsAWeekBeforeTodayOnTheFirstWeekday() {
        #expect(MonarchGanttTimelineRules.anchor(start: day(3), today: nil, items: [], days: days) == jan5 + 3)
        #expect(MonarchGanttTimelineRules.anchor(start: nil, today: jan5 + 9, items: [], days: days) == jan5)
        let items = [MonarchGanttTimelineItem(id: "a", label: "A", start: day(16))]
        #expect(MonarchGanttTimelineRules.anchor(start: nil, today: nil, items: items, days: days) == jan5 + 7)
        let fallback = MonarchGanttTimelineRules.anchor(start: nil, today: nil, items: [], days: days)
        #expect(days.isStartOfWeek(fallback))
    }

    @Test func ticksFollowTheZoom() {
        let week = MonarchGanttTimelineRules.ticks(anchor: jan5, zoom: .week, days: days)
        #expect(week.minor.count == MonarchGanttTimelineTokens.span(.week))
        #expect(week.major.first == MonarchGanttTimelineTick(day: jan5, withYear: true))
        #expect(week.major.count == 2)
        let month = MonarchGanttTimelineRules.ticks(anchor: jan5, zoom: .month, days: days)
        #expect(month.minor.count == 13)
        let weekStarts = month.minor.filter { days.isStartOfWeek($0) }
        #expect(weekStarts.count == month.minor.count)
        let year = MonarchGanttTimelineRules.ticks(anchor: jan5, zoom: .year, days: days)
        #expect(year.minor.isEmpty)
        let quarterly = year.major.dropFirst().filter { (days.dayAndMonth($0.day).month - 1) % 3 == 0 }
        #expect(quarterly.count == year.major.count - 1)
        let withYear = year.major.filter(\.withYear)
        #expect(withYear.count == year.major.count)
    }

    @Test func zoomTokensCoverTheirWindows() {
        for zoom in MonarchGanttTimelineZoom.allCases {
            #expect(MonarchGanttTimelineTokens.span(zoom) > MonarchGanttTimelineTokens.step(zoom))
            #expect(MonarchGanttTimelineTokens.dayWidth(zoom) > 0)
        }
        #expect(MonarchGanttTimelineTokens.usesMinorGrid(.week))
        #expect(!MonarchGanttTimelineTokens.usesMinorGrid(.year))
    }

    // MARK: Geometry and gestures

    @Test func dependencyPathsLeaveAndEnterTheRightEnds() {
        let scale = MonarchGanttTimelineScale(anchor: 0, dayWidth: 10)
        let from = MonarchGanttTimelineSpan(start: 0, end: 4)
        let to = MonarchGanttTimelineSpan(start: 10, end: 12)
        let forward = MonarchGanttTimelineRules.dependencyPath(
            kind: .finishToStart,
            from: from,
            to: to,
            fromY: 0,
            toY: 40,
            scale: scale,
            gap: 5,
            tip: 1
        )
        #expect(forward.first == CGPoint(x: 50, y: 0))
        #expect(forward.last == CGPoint(x: 99, y: 40))
        #expect(forward.count == 4)
        let start = MonarchGanttTimelineRules.dependencyPath(
            kind: .startToStart,
            from: from,
            to: to,
            fromY: 0,
            toY: 40,
            scale: scale,
            gap: 5,
            tip: 1
        )
        #expect(start.first == CGPoint(x: 0, y: 0))
        let finish = MonarchGanttTimelineRules.dependencyPath(
            kind: .finishToFinish,
            from: from,
            to: to,
            fromY: 0,
            toY: 40,
            scale: scale,
            gap: 5,
            tip: 1
        )
        #expect(finish.last == CGPoint(x: 131, y: 40))
        let back = MonarchGanttTimelineRules.dependencyPath(
            kind: .finishToStart,
            from: to,
            to: from,
            fromY: 0,
            toY: 40,
            scale: scale,
            gap: 5,
            tip: 1
        )
        #expect(back.count == 6)
        #expect(back.last == CGPoint(x: -1, y: 40))
    }

    @Test func dragsPickTheEndsOrTheWholeBar() {
        let edge = MonarchGanttTimelineTokens.edgeZone
        #expect(MonarchGanttTimelineRules.dragShift(x: 2, width: 200, edge: edge, isPoint: false) == .start)
        #expect(MonarchGanttTimelineRules.dragShift(x: 198, width: 200, edge: edge, isPoint: false) == .end)
        #expect(MonarchGanttTimelineRules.dragShift(x: 100, width: 200, edge: edge, isPoint: false) == .move)
        #expect(MonarchGanttTimelineRules.dragShift(x: 2, width: 20, edge: edge, isPoint: false) == .move)
        #expect(MonarchGanttTimelineRules.dragShift(x: 2, width: 200, edge: edge, isPoint: true) == .move)
        #expect(MonarchGanttTimelineRules.dragDays(39, dayWidth: 16) == 2)
        #expect(MonarchGanttTimelineRules.dragDays(-25, dayWidth: 16) == -2)
        #expect(MonarchGanttTimelineRules.dragDays(5, dayWidth: 0) == 0)
    }

    @Test func keysMoveTheBarOrOneEnd() {
        #expect(MonarchGanttTimelineBar.keyShift([]) == .move)
        #expect(MonarchGanttTimelineBar.keyShift(.shift) == .end)
        #expect(MonarchGanttTimelineBar.keyShift(.option) == .start)
    }

    @Test func barBoxesKeepA44PointTouchArea() {
        let scale = MonarchGanttTimelineScale(anchor: 0, dayWidth: 2)
        let bar = MonarchGanttTimelineTokens.barBox(.init(start: 10, end: 12), isPoint: false, scale: scale)
        #expect(bar.visibleLeft == 20)
        #expect(bar.visibleWidth == MonarchGanttTimelineTokens.barMinWidth)
        #expect(bar.nodeWidth == MonarchAccessibility.minimumHitTarget)
        #expect(bar.nodeLeft + bar.nodeWidth / 2 == bar.visibleLeft + bar.visibleWidth / 2)
        let point = MonarchGanttTimelineTokens.barBox(.init(start: 10, end: 10), isPoint: true, scale: scale)
        #expect(point.visibleWidth == MonarchGanttTimelineTokens.pointSize)
        #expect(point.visibleLeft + point.visibleWidth / 2 == 21)
        let wide = MonarchGanttTimelineTokens.barBox(.init(start: 0, end: 99), isPoint: false, scale: scale)
        #expect(wide.nodeWidth == 200)
        #expect(MonarchGanttTimelineTokens.showsBarLabel(width: 200))
        #expect(!MonarchGanttTimelineTokens.showsBarLabel(width: 20))
    }

    @Test func labelColumnAndIndentAreBounded() {
        #expect(MonarchGanttTimelineTokens.labelWidth(0) == MonarchGanttTimelineTokens.labelMaxWidth)
        #expect(MonarchGanttTimelineTokens.labelWidth(300) == 108)
        #expect(MonarchGanttTimelineTokens.labelWidth(2000) == MonarchGanttTimelineTokens.labelMaxWidth)
        #expect(MonarchGanttTimelineTokens.indent(depth: 9) == MonarchGanttTimelineTokens.indent(depth: 3))
        #expect(MonarchGanttTimelineTokens.indent(depth: 1) > MonarchGanttTimelineTokens.indent(depth: 0))
    }

    // MARK: Colours

    @Test func barColorsFollowTheToneAndConflict() {
        let accent = MonarchGanttTimelineTokens.barColors(tone: nil, hasConflict: false)
        #expect(accent.container == \MonarchColorTokens.accentSubtle)
        #expect(accent.border == \MonarchColorTokens.accentSolid)
        #expect(accent.content == \MonarchColorTokens.textDefault)
        #expect(!accent.isDashed)
        let neutral = MonarchGanttTimelineTokens.barColors(tone: .neutral, hasConflict: false)
        #expect(neutral.container == \MonarchColorTokens.bgMuted)
        #expect(neutral.progress == \MonarchColorTokens.borderStrong)
        let info = MonarchGanttTimelineTokens.barColors(tone: .info, hasConflict: false)
        #expect(info.container == \MonarchColorTokens.infoSubtle)
        #expect(info.border == \MonarchColorTokens.infoSolid)
        let conflict = MonarchGanttTimelineTokens.barColors(tone: .success, hasConflict: true)
        #expect(conflict.container == \MonarchColorTokens.dangerSubtle)
        #expect(conflict.isDashed)
        let tones: [MonarchTone?] = [nil] + MonarchTone.allCases.map { $0 }
        let containers = Set(tones.map { MonarchGanttTimelineTokens.barColors(tone: $0, hasConflict: false).container })
        // No tone is the accent tone, so nil and .accent share a container.
        #expect(containers.count == MonarchTone.allCases.count)
    }

    @Test func milestoneAndArrowColors() {
        #expect(MonarchGanttTimelineTokens.milestoneColors(isDone: true).fill == \MonarchColorTokens.accentSolid)
        #expect(MonarchGanttTimelineTokens.milestoneColors(isDone: false).fill == \MonarchColorTokens.bgSurface)
        #expect(MonarchGanttTimelineTokens.milestoneColors(isDone: false).border == \MonarchColorTokens.textMuted)
        #expect(MonarchGanttTimelineTokens.dependencyColor(isConflict: true) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchGanttTimelineTokens.dependencyColor(isConflict: false) == \MonarchColorTokens.textSubtle)
    }

    // MARK: Accessibility and words

    @Test func barsAreReadWithDatesProgressAndConflict() {
        let model = model()
        let items = MonarchGanttTimelineFixtures.items
        let design = items[0]
        let span = model.span(design.dates) ?? .init(start: 0, end: 0)
        #expect(model.describe(design, span: span) == "Design, Jan 12, 2026 to Jan 23, 2026, 60% complete")
        let build = items[1]
        let buildSpan = model.span(build.dates) ?? .init(start: 0, end: 0)
        #expect(
            model.describe(build, span: buildSpan)
                == "Build, Jan 21, 2026 to Feb 13, 2026. Conflict: Starts before Design ends"
        )
        let launch = items[3]
        let point = model.span(launch.dates) ?? .init(start: 0, end: 0)
        #expect(model.describe(launch, span: point) == "Launch, Feb 20, 2026")
    }

    @Test func milestonesAreReadWithTheirDateAndState() {
        let model = model()
        #expect(
            model.describe(MonarchGanttTimelineFixtures.milestones[0]) == "Milestone Design review, Jan 23, 2026, done"
        )
        #expect(model.describe(MonarchGanttTimelineFixtures.milestones[1]) == "Milestone API frozen, Feb 4, 2026")
    }

    @Test func listFactsWriteEveryDateLengthProgressAndDependency() {
        let model = model()
        let items = MonarchGanttTimelineFixtures.items
        #expect(model.facts(items[0]) == ["Jan 12, 2026 – Jan 23, 2026", "12 days · 60% complete"])
        #expect(
            model.facts(items[1])
                == ["Jan 21, 2026 – Feb 13, 2026", "24 days · In progress", "Depends on Design, API changes"]
        )
        #expect(model.facts(items[3]) == ["Feb 20, 2026", "1 day"])
        #expect(model.facts(items[4]) == ["No dates"])
    }

    @Test func editableBarsOfferSixActionsAndMarkersTwo() {
        let model = model()
        let items = MonarchGanttTimelineFixtures.items
        #expect(
            model.actions(items[0], isPoint: false).map(\.name) == [
                "Move a day earlier", "Move a day later", "Start a day earlier", "Start a day later",
                "End a day earlier", "End a day later",
            ]
        )
        #expect(model.actions(items[0], isPoint: true).count == 2)
        #expect(model.actions(items[3], isPoint: true).isEmpty)
        #expect(self.model(isEditable: false).actions(items[0], isPoint: false).isEmpty)
    }

    @Test func changesAreReportedOnlyWhenDatesChange() {
        let model = model()
        let design = MonarchGanttTimelineFixtures.items[0]
        #expect(model.change(design, to: design.dates) == nil)
        let later = design.dates.shifted(.move, by: 1, in: calendar)
        #expect(model.change(design, to: later) == later)
        #expect(model.change(MonarchGanttTimelineFixtures.items[3], to: later) == nil)
        #expect(self.model(isEditable: false).change(design, to: later) == nil)
        #expect(model.announcement(design, dates: later) == "Design: Jan 13, 2026 to Jan 24, 2026")
        #expect(
            model.announcement(design, dates: MonarchGanttTimelineDates(start: day(1), end: day(1)))
                == "Design: Jan 6, 2026"
        )
    }

    @Test func rowNotesSayWhenItemsAreOutsideTheWindow() {
        let model = model()
        func note(_ span: MonarchGanttTimelineSpan?, rollup: Bool = false) -> String? {
            MonarchGanttTimelineRules.rowNote(
                span: span,
                hasRollup: rollup,
                anchor: model.anchor,
                rangeEnd: model.rangeEnd,
                formats: model.formats,
                text: model.text
            )
        }
        #expect(note(.init(start: jan5 - 10, end: jan5 - 1)) == "Ended Jan 4, 2026")
        #expect(note(.init(start: jan5 + 200, end: jan5 + 201))?.hasPrefix("Starts ") == true)
        #expect(note(nil) == "No dates")
        #expect(note(nil, rollup: true) == nil)
        #expect(note(.init(start: jan5, end: jan5 + 1)) == nil)
    }

    @Test func toolbarStepsTheWindowAndGoesToToday() {
        let model = model(state: MonarchGanttTimelineState(start: day(0), zoom: .week))
        #expect(dayOf(model.stepped(forward: true).start) == jan5 + 7)
        #expect(dayOf(model.stepped(forward: false).start) == jan5 - 7)
        let far = self.model(state: MonarchGanttTimelineState(start: day(100)))
        #expect(dayOf(far.todayState?.start) == jan5)
        #expect(self.model(today: nil).todayState == nil)
        #expect(model.range == "Jan 5, 2026 – Feb 1, 2026")
        #expect(model.showsToday)
        #expect(far.showsToday == false)
        #expect(MonarchGanttTimelineToolbar<EmptyView>.showsZoomMenu([.month, .year]))
        #expect(!MonarchGanttTimelineToolbar<EmptyView>.showsZoomMenu([.year]))
    }

    @Test func stateTogglesCollapsedParents() {
        var state = MonarchGanttTimelineState()
        state.toggle("build")
        #expect(state.collapsed == ["build"])
        state.toggle("build")
        #expect(state.collapsed.isEmpty)
    }

    @Test func dateSheetStartsOnTheItemOrToday() {
        let model = model()
        let design = MonarchGanttTimelineDatesSheet.initialDates(
            model: model,
            item: MonarchGanttTimelineFixtures.items[0]
        )
        #expect(days.day(design.start) == jan5 + 7)
        #expect(days.day(design.end) == jan5 + 18)
        let docs = MonarchGanttTimelineDatesSheet.initialDates(
            model: model,
            item: MonarchGanttTimelineFixtures.items[4]
        )
        #expect(days.day(docs.start) == jan5 + 9)
    }

    // MARK: Strings

    @Test func builtInStringsResolve() {
        let text = MonarchGanttTimelineText(labels: MonarchGanttTimelineLabels())
        let all =
            [
                text.timeline, text.rowHeader, text.zoom, text.earlier, text.later, text.today, text.view, text.chart,
                text.list, text.milestones, text.hint, text.noDates, text.changeDates, text.startDate, text.endDate,
                text.save, text.cancel, text.expanded, text.collapsed, text.moveEarlier, text.moveLater,
                text.startEarlier, text.startLater, text.endEarlier, text.endLater, text.unscheduled(1),
                text.unscheduled(3), text.duration(1), text.duration(4), text.progress("5%"), text.conflict("x"),
                text.ended("x"), text.starts("x"), text.dependsOn("x"), text.children(1, isExpanded: true),
                text.children(2, isExpanded: false), text.range("a", "b"),
            ] + MonarchGanttTimelineZoom.allCases.map(text.zoomName)
        for string in all {
            #expect(!string.hasPrefix("monarch."), "unresolved: \(string)")
            #expect(!string.isEmpty)
        }
        #expect(text.unscheduled(3) == "Unscheduled · 3 without dates")
        #expect(text.children(2, isExpanded: false) == "Show 2 sub-items")
        #expect(text.children(1, isExpanded: true) == "Hide 1 sub-item")
        #expect(text.zoomName(.quarter) == "Quarters")
    }

    @Test func labelsReplaceBuiltInStrings() {
        let labels = MonarchGanttTimelineLabels(
            timeline: "Plan",
            zoomName: { _ in "Z" },
            bar: { label, start, end, _ in "\(label) \(start)-\(end)" },
            moveEarlier: "Back"
        )
        let model = model(labels: labels)
        #expect(model.text.timeline == "Plan")
        #expect(model.text.zoomName(.week) == "Z")
        #expect(model.text.bar("A", start: "1", end: "2", progress: nil) == "A 1-2")
        #expect(model.actions(MonarchGanttTimelineFixtures.items[0], isPoint: true).first?.name == "Back")
    }

    @Test func percentagesAreRoundedAndClamped() {
        let model = model()
        var item = MonarchGanttTimelineFixtures.items[0]
        item.progress = 1.7
        #expect(model.percent(item) == "100%")
        item.progress = nil
        #expect(model.percent(item) == nil)
    }

    #if os(iOS)
        @Test func chartRowsKeepTheMinimumTouchHeight() {
            let size = Host.layout(
                MonarchGanttTimelineFixtures.timeline(
                    MonarchGanttTimeline(
                        MonarchGanttTimelineFixtures.items,
                        state: MonarchGanttTimelineFixtures.state(),
                        today: nil,
                        showsToolbar: false
                    )
                )
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget * 4)
        }
    #endif
}
