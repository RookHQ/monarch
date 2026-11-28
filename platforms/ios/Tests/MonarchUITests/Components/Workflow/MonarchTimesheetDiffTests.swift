import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// Monday 12 January 2026, 00:00 UTC.
private let monday = Date(timeIntervalSince1970: 1_768_176_000)
private let tuesday = monday.addingTimeInterval(86400)

private let version1 = [
    MonarchTimesheetDiffEntry(id: "a", date: monday, minutes: 120, project: "Atlas", activity: "Design", task: "ENG-1"),
    MonarchTimesheetDiffEntry(id: "b", date: monday, minutes: 45, project: "Pulse", isBillable: false),
    MonarchTimesheetDiffEntry(id: "c", date: tuesday, minutes: 30, project: "Atlas"),
]

private let version2 = [
    MonarchTimesheetDiffEntry(id: "a", date: monday, minutes: 150, project: "Atlas", activity: "Design", task: "ENG-1"),
    MonarchTimesheetDiffEntry(id: "c", date: tuesday, minutes: 30, project: "Pulse", note: "Moved to Pulse"),
    MonarchTimesheetDiffEntry(id: "d", date: tuesday, minutes: 60),
]

enum MonarchTimesheetDiffFixtures: ComponentFixtures {
    static let id = "timesheet-diff"

    static let fixtures = [
        Fixture("added, removed and changed, with totals") {
            MonarchTimesheetDiff(before: version1, after: version2, subtitle: "since version 3")
        },
        Fixture("no changes") {
            MonarchTimesheetDiff(before: version1, after: version1)
        },
        Fixture("without totals or billing, with a custom title") {
            VStack(alignment: .leading) {
                MonarchTimesheetDiff(before: version1, after: version2, title: "Corrections", showsTotals: false)
                MonarchTimesheetDiff(before: version1, after: version2, showsBillable: false)
            }
        },
        Fixture("a computed result with a custom field and long text") {
            MonarchTimesheetDiff(
                MonarchTimesheetDiffResult(
                    changed: [
                        MonarchTimesheetDiffChange(
                            id: "x",
                            fields: [.minutes, MonarchTimesheetDiffField("rate")],
                            before: MonarchTimesheetDiffEntry(
                                id: "x",
                                date: monday,
                                minutes: 480,
                                project: "A project with a very long name that wraps onto several lines",
                                activity: "Consulting",
                                task: "Quarterly planning workshop with the leadership team"
                            ),
                            after: MonarchTimesheetDiffEntry(id: "x", date: monday, minutes: 465, project: "")
                        )
                    ]
                )
            )
        },
        Fixture("custom strings") {
            MonarchTimesheetDiff(
                before: version1,
                after: version2,
                labels: MonarchTimesheetDiffLabels(
                    title: "Änderungen",
                    added: "Neu",
                    fields: [.minutes: "Zeit"],
                    entry: { "\($0.minutes) min" }
                )
            )
        },
    ]
}

@Suite("MonarchTimesheetDiff")
struct MonarchTimesheetDiffTests {
    typealias Tokens = MonarchTimesheetDiffTokens

    private let labels = MonarchTimesheetDiffLabels()
    private let english = Locale(identifier: "en_US")
    private let utc = TimeZone(identifier: "UTC") ?? .gmt

    // MARK: Comparing

    @Test("Entries are matched by id into added, removed and changed")
    func diff() {
        let diff = MonarchTimesheetDiffResult(before: version1, after: version2)
        #expect(diff.added.map(\.id) == ["d"])
        #expect(diff.removed.map(\.id) == ["b"])
        #expect(diff.changed.map(\.id) == ["a", "c"])
        #expect(diff.changed[0].fields == [.minutes])
        #expect(diff.changed[1].fields == [.project, .note])
        #expect(!diff.isEmpty)
        #expect(MonarchTimesheetDiffResult(before: version1, after: version1).isEmpty)
        #expect(MonarchTimesheetDiffResult(before: [], after: []).isEmpty)
    }

    @Test("Every compared field is detected, and nil differs from empty")
    func fields() {
        let base = MonarchTimesheetDiffEntry(id: "x", date: monday, minutes: 60)
        let other = MonarchTimesheetDiffEntry(
            id: "x",
            date: tuesday,
            minutes: 30,
            project: "",
            activity: "Build",
            task: "T-1",
            isBillable: false,
            note: "n"
        )
        #expect(
            Tokens.changedFields(base, other) == [.date, .minutes, .project, .activity, .task, .billable, .note]
        )
        #expect(Tokens.changedFields(base, base).isEmpty)
    }

    @Test("Totals add the time gained and subtract the time lost; non-billable time is not billable")
    func totals() {
        let totals = MonarchTimesheetDiffResult(before: version1, after: version2).totals
        // +60 added, -45 removed, +30 changed.
        #expect(totals.total == 45)
        // The removed 45 minutes were not billable.
        #expect(totals.billable == 90)
        #expect(MonarchTimesheetDiffResult().totals == MonarchTimesheetDiffTotals(total: 0, billable: 0))
    }

    // MARK: Text

    @Test("Durations are written as on the web")
    func durations() {
        #expect(Tokens.durationText(0) == "0m")
        #expect(Tokens.durationText(45) == "45m")
        #expect(Tokens.durationText(120) == "2h")
        #expect(Tokens.durationText(90) == "1h 30m")
        #expect(Tokens.durationText(-5) == "0m")
    }

    @Test("Changes are signed with a plus or a true minus")
    func deltas() {
        #expect(Tokens.deltaText(90, labels: labels) == "+1h 30m")
        #expect(Tokens.deltaText(-45, labels: labels) == "\u{2212}45m")
        #expect(Tokens.deltaText(0, labels: labels) == "0m")
        let custom = MonarchTimesheetDiffLabels(delta: { "\($0) min" })
        #expect(Tokens.deltaText(-45, labels: custom) == "-45 min")
    }

    @Test("Changes are read in words")
    func spoken() {
        #expect(Tokens.spokenDuration(90, locale: english) == "1 hour, 30 minutes")
        #expect(Tokens.spokenDuration(0, locale: english) == "0 minutes")
        #expect(Tokens.spokenDelta(90, locale: english) == "plus 1 hour, 30 minutes")
        #expect(Tokens.spokenDelta(-45, locale: english) == "minus 45 minutes")
        #expect(Tokens.spokenDelta(0, locale: english) == "no change in time")
    }

    @Test("An entry's line has the day, time, project, activity, task and billing")
    func entryLines() {
        #expect(
            Tokens.entryText(version1[0], labels: labels, locale: english, timeZone: utc)
                == "Mon, Jan 12 · 2h · Atlas · Design · ENG-1"
        )
        #expect(
            Tokens.entryText(version1[1], labels: labels, locale: english, timeZone: utc)
                == "Mon, Jan 12 · 45m · Pulse · non-billable"
        )
        let noProject = MonarchTimesheetDiffEntry(id: "z", date: tuesday, minutes: 60, project: "")
        #expect(
            Tokens.entryText(noProject, labels: labels, isSpoken: true, locale: english, timeZone: utc)
                == "Tue, Jan 13, 1 hour, No project"
        )
    }

    @Test("Kinds are named, with the changed fields in words")
    func kinds() {
        #expect(Tokens.kindText(.added, labels: labels) == "Added")
        #expect(Tokens.kindText(.removed, labels: labels) == "Removed")
        #expect(Tokens.kindText(.changed, fields: [.minutes, .project], labels: labels) == "Changed time, project")
        #expect(
            Tokens.kindText(.changed, fields: [MonarchTimesheetDiffField("rate")], labels: labels) == "Changed rate"
        )
        let custom = MonarchTimesheetDiffLabels(fields: [.minutes: "Zeit"])
        #expect(Tokens.kindText(.changed, fields: [.minutes], labels: custom) == "Changed Zeit")
    }

    @Test("The totals line has the billable change unless billing does not apply")
    func totalsLine() {
        let totals = MonarchTimesheetDiffTotals(total: 90, billable: -30)
        #expect(
            Tokens.totalsText(totals, showsBillable: true, labels: labels) == "Total +1h 30m · billable \u{2212}30m"
        )
        #expect(Tokens.totalsText(totals, showsBillable: false, labels: labels) == "Total +1h 30m")
    }

    // MARK: Appearance

    @Test("Each kind has its own tone and symbol")
    func appearance() {
        #expect(Tokens.appearance(.added).kind == \MonarchColorTokens.successStrong)
        #expect(Tokens.appearance(.removed).bar == \MonarchColorTokens.dangerBorder)
        #expect(Tokens.appearance(.changed).background == \MonarchColorTokens.warningSubtle)
        #expect(Set(MonarchTimesheetDiffKind.allCases.map(Tokens.symbol)).count == 3)
    }

    // MARK: Accessibility

    @Test("Added and removed entries are read with the kind, the change and the entry")
    func entryAccessibility() {
        #expect(
            Tokens.entryAccessibilityLabel(version1[1], kind: .removed, labels: labels, locale: english, timeZone: utc)
                == "Removed, minus 45 minutes, Mon, Jan 12, 45 minutes, Pulse, non-billable"
        )
        #expect(
            Tokens.entryAccessibilityLabel(version2[2], kind: .added, labels: labels, locale: english, timeZone: utc)
                == "Added, plus 1 hour, Tue, Jan 13, 1 hour, No project"
        )
    }

    @Test("Changed entries are read with Before and After")
    func changeAccessibility() {
        let diff = MonarchTimesheetDiffResult(before: version1, after: version2)
        #expect(
            Tokens.changeAccessibilityLabel(diff.changed[1], labels: labels, locale: english, timeZone: utc)
                == "Changed project, note, Before, Tue, Jan 13, 30 minutes, Atlas, "
                + "After, Tue, Jan 13, 30 minutes, Pulse"
        )
        #expect(
            Tokens.changeAccessibilityLabel(diff.changed[0], labels: labels, locale: english, timeZone: utc)
                .hasPrefix("Changed time, plus 30 minutes, Before")
        )
    }

    @Test("Every built-in string resolves")
    func strings() {
        let all = [
            MonarchTimesheetDiffStrings.title(labels),
            MonarchTimesheetDiffStrings.added(labels),
            MonarchTimesheetDiffStrings.removed(labels),
            MonarchTimesheetDiffStrings.changed(["x"], labels),
            MonarchTimesheetDiffStrings.before(labels),
            MonarchTimesheetDiffStrings.after(labels),
            MonarchTimesheetDiffStrings.noProject(labels),
            MonarchTimesheetDiffStrings.nonBillable(labels),
            MonarchTimesheetDiffStrings.noChanges(labels),
            MonarchTimesheetDiffStrings.totals("+1h", billable: nil, labels),
            MonarchTimesheetDiffStrings.increase("1 hour"),
            MonarchTimesheetDiffStrings.decrease("1 hour"),
            MonarchTimesheetDiffStrings.unchanged,
        ]
        let fields: [MonarchTimesheetDiffField] = [
            .date, .minutes, .project, .activity, .task, .billable, .note, .start, .end,
        ]
        for string in all + fields.map({ MonarchTimesheetDiffStrings.field($0, labels) }) {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
        #expect(MonarchTimesheetDiffStrings.field(.minutes, labels) == "time")
    }
}
