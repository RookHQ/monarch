import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// A fixed calendar, time zone and locale, so dates and words never depend on the machine.
private enum RoadmapTestClock {
    static let locale = Locale(identifier: "en_US")

    static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        calendar.locale = locale
        calendar.firstWeekday = 2
        return calendar
    }()

    /// Thursday 1 January 2026, at noon so any time of day is read as its date.
    static let jan1: Date = calendar.date(from: DateComponents(year: 2026, month: 1, day: 1, hour: 12)) ?? .distantPast

    static func day(_ offset: Int) -> Date {
        calendar.date(byAdding: .day, value: offset, to: jan1) ?? jan1
    }
}

enum MonarchRoadmapFixtures: ComponentFixtures {
    static let id = "roadmap"

    private static func day(_ offset: Int) -> Date { RoadmapTestClock.day(offset) }

    static let items = [
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
            id: "p2",
            label: "Referral page",
            start: day(59),
            end: day(134),
            groupID: "growth",
            progress: 0.1,
            progressText: "1/10",
            health: .notReported
        ),
        MonarchRoadmapItem(
            id: "p3",
            label: "Help centre search",
            start: day(32),
            end: day(119),
            groupID: "quality",
            progress: 0.25,
            progressText: "3/12",
            health: .atRisk
        ),
        MonarchRoadmapItem(
            id: "p4",
            label: "Crash fixes",
            start: day(11),
            end: day(57),
            groupID: "quality",
            progress: 0.7,
            progressText: "14/20",
            health: .offTrack
        ),
        MonarchRoadmapItem(id: "p5", label: "Billing revamp", progressText: "0/6"),
    ]

    static let groups = [
        MonarchRoadmapGroup(
            id: "growth",
            label: "Grow repeat customers",
            start: day(0),
            end: day(180),
            health: .onTrack
        ),
        MonarchRoadmapGroup(
            id: "quality",
            label: "Fewer support tickets",
            start: day(31),
            end: day(242),
            health: .atRisk
        ),
        MonarchRoadmapGroup(id: "platform", label: "Platform"),
    ]

    static let dependencies = [MonarchGanttTimelineDependency(id: "d1", from: "p4", to: "p3")]

    static let milestones = [
        MonarchGanttTimelineMilestone(id: "m1", label: "Launch", date: day(78), itemID: "p1"),
        MonarchGanttTimelineMilestone(id: "m2", label: "Board review", date: day(90), isDone: true),
    ]

    static func state(_ zoom: MonarchGanttTimelineZoom = .quarter, view: MonarchGanttTimelineView = .chart)
        -> Binding<MonarchGanttTimelineState>
    {
        .constant(MonarchGanttTimelineState(start: RoadmapTestClock.jan1, zoom: zoom, view: view))
    }

    @MainActor static func roadmap(_ content: some View) -> some View {
        content
            .environment(\.calendar, RoadmapTestClock.calendar)
            .environment(\.timeZone, RoadmapTestClock.calendar.timeZone)
            .environment(\.locale, RoadmapTestClock.locale)
    }

    static let healthItems: [MonarchRoadmapItem] =
        MonarchRoadmapHealth.allCases.enumerated().map { index, health in
            MonarchRoadmapItem(
                id: "h\(index)",
                label: "Health \(index)",
                start: day(index * 20),
                end: day(index * 20 + 60),
                progress: Double(index) / 4,
                health: health
            )
        } + [
            MonarchRoadmapItem(
                id: "tone",
                label: "Explicit tone",
                start: day(10),
                end: day(40),
                health: .offTrack,
                tone: .info
            )
        ]

    static let edgeItems = [
        MonarchRoadmapItem(
            id: "long",
            label: "A project with a very long name that wraps over several lines in the heading",
            start: day(5),
            end: day(200),
            groupID: "platform",
            progressText: "12 of 40 issues closed",
            health: .atRisk
        ),
        MonarchRoadmapItem(id: "undated", label: "Not planned yet", groupID: "platform"),
        MonarchRoadmapItem(id: "stray", label: "Unknown group", start: day(3), groupID: "missing"),
        MonarchRoadmapItem(id: "old", label: "Finished last year", start: day(-200), end: day(-120)),
    ]

    static let fixtures = [
        Fixture("read-only chart with groups, health, dependencies, milestones and today") {
            roadmap(
                MonarchRoadmap(
                    items,
                    state: state(),
                    groups: groups,
                    dependencies: dependencies,
                    milestones: milestones,
                    today: day(13),
                    onOpen: { _ in }
                )
            )
        },
        Fixture("editable chart with toolbar actions") {
            roadmap(
                MonarchRoadmap(
                    items,
                    state: state(.month),
                    groups: groups,
                    today: day(13),
                    onChange: { _, _ in },
                    toolbarActions: { Button("Filter") {} }
                )
            )
        },
        Fixture("list view with dates, health and dependencies") {
            roadmap(
                MonarchRoadmap(
                    items,
                    state: state(view: .list),
                    groups: groups,
                    dependencies: dependencies,
                    milestones: milestones,
                    today: nil,
                    onChange: { _, _ in }
                )
            )
        },
        Fixture("every health and an explicit tone, year zoom, no toolbar") {
            roadmap(MonarchRoadmap(healthItems, state: state(.year), today: day(30), showsToolbar: false))
        },
        Fixture("long names, no dates, unknown group and an empty group") {
            roadmap(
                MonarchRoadmap(
                    edgeItems,
                    state: state(),
                    groups: [MonarchRoadmapGroup(id: "platform", label: "Platform"), groups[1]],
                    today: day(13)
                )
            )
        },
        Fixture("empty roadmap") {
            roadmap(MonarchRoadmap([], state: state(), today: day(13)))
        },
    ]
}

@Suite("MonarchRoadmap")
struct MonarchRoadmapTests {
    private let text = MonarchRoadmapText(labels: MonarchRoadmapLabels())

    // MARK: Tokens

    @Test func barToneFollowsHealthUnlessAToneIsGiven() {
        #expect(MonarchRoadmapTokens.barTone(nil, health: .atRisk) == .warning)
        #expect(MonarchRoadmapTokens.barTone(nil, health: .offTrack) == .danger)
        #expect(MonarchRoadmapTokens.barTone(nil, health: .onTrack) == nil)
        #expect(MonarchRoadmapTokens.barTone(nil, health: .notReported) == nil)
        #expect(MonarchRoadmapTokens.barTone(nil, health: nil) == nil)
        for health in MonarchRoadmapHealth.allCases {
            #expect(MonarchRoadmapTokens.barTone(.info, health: health) == .info)
        }
        #expect(MonarchRoadmapTokens.defaultZoom == .quarter)
    }

    // MARK: Headings

    @Test func itemMetaJoinsProgressTextAndHealth() {
        var item = MonarchRoadmapItem(id: "a", label: "A", progressText: "8/20", health: .onTrack)
        #expect(MonarchRoadmapRules.itemMeta(item, text: text) == "8/20 · On track")
        item.health = nil
        #expect(MonarchRoadmapRules.itemMeta(item, text: text) == "8/20")
        item.progressText = nil
        #expect(MonarchRoadmapRules.itemMeta(item, text: text) == nil)
        item.health = .notReported
        #expect(MonarchRoadmapRules.itemMeta(item, text: text) == "No health reported")
    }

    @Test func groupMetaCountsProjectsAndAddsHealth() {
        let healthy = MonarchRoadmapGroup(id: "g", label: "G", health: .atRisk)
        let plain = MonarchRoadmapGroup(id: "g", label: "G")
        #expect(MonarchRoadmapRules.groupMeta(healthy, count: 1, text: text) == "1 project · At risk")
        #expect(MonarchRoadmapRules.groupMeta(plain, count: 0, text: text) == "0 projects")
        #expect(MonarchRoadmapRules.groupMeta(plain, count: 3, text: text) == "3 projects")
    }

    // MARK: Rows

    @Test func rowsKeepOrderDatesAndProgressAndTintByHealth() {
        let rows = MonarchRoadmapRules.rows(
            MonarchRoadmapFixtures.items,
            groups: MonarchRoadmapFixtures.groups,
            text: text
        )
        #expect(rows.items.map(\.id) == ["p1", "p2", "p3", "p4", "p5"])
        let first = rows.items[0]
        let source = MonarchRoadmapFixtures.items[0]
        #expect(first.label == source.label)
        #expect(first.start == source.start)
        #expect(first.end == source.end)
        #expect(first.groupID == "growth")
        #expect(first.progress == 0.4)
        #expect(first.meta == "8/20 · On track")
        #expect(first.isEditable)
        #expect(rows.items.map(\.tone) == [nil, nil, .warning, .danger, nil])
        #expect(rows.items[1].meta == "1/10 · No health reported")
        #expect(rows.items[4].meta == "0/6")
        #expect(rows.items[4].start == nil && rows.items[4].end == nil)
    }

    @Test func groupRowsCarryCountsHealthAndSpans() {
        let rows = MonarchRoadmapRules.rows(
            MonarchRoadmapFixtures.items,
            groups: MonarchRoadmapFixtures.groups,
            text: text
        )
        #expect(rows.groups.map(\.id) == ["growth", "quality", "platform"])
        #expect(rows.groups.map(\.meta) == ["2 projects · On track", "2 projects · At risk", "0 projects"])
        #expect(rows.groups[0].start == MonarchRoadmapFixtures.groups[0].start)
        #expect(rows.groups[2].start == nil)
    }

    @Test func duplicateIDsKeepTheFirstProjectForCallbacks() {
        let rows = MonarchRoadmapRules.rows(
            [MonarchRoadmapItem(id: "x", label: "First"), MonarchRoadmapItem(id: "x", label: "Second")],
            groups: [],
            text: text
        )
        #expect(rows.byID["x"]?.label == "First")
        #expect(rows.items.count == 2)
    }

    @Test func projectsWithoutDatesStayAsRowsInTheTimeline() {
        let rows = MonarchRoadmapRules.rows(
            MonarchRoadmapFixtures.items,
            groups: MonarchRoadmapFixtures.groups,
            text: text
        )
        let model = MonarchGanttTimelineModel.make(
            items: rows.items,
            groups: rows.groups,
            state: MonarchGanttTimelineState(start: RoadmapTestClock.jan1, zoom: .quarter),
            undated: .row,
            today: nil,
            calendar: RoadmapTestClock.calendar,
            locale: RoadmapTestClock.locale,
            labels: MonarchRoadmapRules.timelineLabels(MonarchGanttTimelineLabels())
        )
        #expect(model.lines.unscheduled.isEmpty)
        #expect(model.text.timeline == "Roadmap")
        #expect(model.text.rowHeader == "Project")
    }

    // MARK: Callbacks

    @MainActor @Test func callbacksReceiveTheRoadmapProject() {
        let rows = MonarchRoadmapRules.rows(MonarchRoadmapFixtures.items, groups: [], text: text)
        var changed: [MonarchRoadmapItem] = []
        var changedDates: [MonarchGanttTimelineDates] = []
        var opened: [MonarchRoadmapItem] = []
        let change = MonarchRoadmapRules.change(
            { item, dates in
                changed.append(item)
                changedDates.append(dates)
            },
            rows: rows
        )
        let open = MonarchRoadmapRules.open({ opened.append($0) }, rows: rows)
        let dates = MonarchGanttTimelineDates(start: RoadmapTestClock.day(5), end: RoadmapTestClock.day(9))
        change?(rows.items[2], dates)
        open?(rows.items[3])
        change?(MonarchGanttTimelineItem(id: "unknown", label: "Unknown"), dates)
        #expect(changed == [MonarchRoadmapFixtures.items[2]])
        #expect(changedDates == [dates])
        #expect(opened == [MonarchRoadmapFixtures.items[3]])
    }

    @Test func noCallbacksMakeTheTimelineReadOnly() {
        let rows = MonarchRoadmapRules.rows(MonarchRoadmapFixtures.items, groups: [], text: text)
        #expect(MonarchRoadmapRules.change(nil, rows: rows) == nil)
        #expect(MonarchRoadmapRules.open(nil, rows: rows) == nil)
    }

    // MARK: Strings

    @Test func builtInStringsResolve() {
        let healthWords = MonarchRoadmapHealth.allCases.map(text.health)
        let all =
            [
                MonarchRoadmapStrings.timeline, MonarchRoadmapStrings.rowHeader, text.count(1), text.count(4),
                text.meta("a", "b"),
            ] + healthWords
        for string in all {
            #expect(!string.hasPrefix("monarch."), "unresolved: \(string)")
            #expect(!string.isEmpty)
        }
        #expect(healthWords == ["On track", "At risk", "Off track", "No health reported"])
        #expect(text.count(1) == "1 project")
        #expect(text.count(4) == "4 projects")
        #expect(text.meta("a", "b") == "a · b")
    }

    @Test func labelsReplaceBuiltInStrings() {
        let custom = MonarchRoadmapText(
            labels: MonarchRoadmapLabels(
                health: { $0 == .onTrack ? "Green" : "Other" },
                count: { "\($0) epics" },
                meta: { "\($0) / \($1)" }
            )
        )
        let group = MonarchRoadmapGroup(id: "g", label: "G", health: .onTrack)
        #expect(MonarchRoadmapRules.groupMeta(group, count: 2, text: custom) == "2 epics / Green")
        let item = MonarchRoadmapItem(id: "a", label: "A", progressText: "1/2", health: .atRisk)
        #expect(MonarchRoadmapRules.itemMeta(item, text: custom) == "1/2 / Other")
    }

    @Test func timelineLabelsKeepTheAppsReplacements() {
        let merged = MonarchRoadmapRules.timelineLabels(
            MonarchGanttTimelineLabels(timeline: "Plan", noDates: "Unplanned")
        )
        #expect(merged.timeline == "Plan")
        #expect(merged.rowHeader == "Project")
        #expect(merged.noDates == "Unplanned")
    }

    #if os(iOS)
        @MainActor @Test func chartRowsKeepTheMinimumTouchHeight() {
            let size = Host.layout(
                MonarchRoadmapFixtures.roadmap(
                    MonarchRoadmap(
                        MonarchRoadmapFixtures.items,
                        state: MonarchRoadmapFixtures.state(),
                        today: nil,
                        showsToolbar: false
                    )
                )
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget * 5)
        }
    #endif
}
