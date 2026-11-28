import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// Monday 12 January 2026, 00:00 UTC, and the six days after it.
private let monday = Date(timeIntervalSince1970: 1_768_176_000)
private let week = (0..<7).map { monday.addingTimeInterval(Double($0) * 86_400) }

private let days: [MonarchTimesheetGridDay] = [
    MonarchTimesheetGridDay(week[0], lockReason: "Approved", note: "Payroll closed", expectedMinutes: 480),
    MonarchTimesheetGridDay(week[1], expectedMinutes: 480),
    MonarchTimesheetGridDay(week[2], isToday: true, expectedMinutes: 480),
    MonarchTimesheetGridDay(week[3], isLocked: true, expectedMinutes: 480),
    MonarchTimesheetGridDay(week[4], note: "Company holiday", expectedMinutes: 480),
    MonarchTimesheetGridDay(week[5], isNonWorking: true),
    MonarchTimesheetGridDay(week[6], label: "Sunday", isNonWorking: true),
]

private let rows: [MonarchTimesheetGridRow] = [
    MonarchTimesheetGridRow(
        id: "atlas",
        label: "Atlas website refresh",
        description: "Design · Billable",
        cells: [week[0]: 240, week[1]: 90, week[2]: 480]
    ),
    MonarchTimesheetGridRow(id: "pulse", label: "Pulse", cells: [week[2]: 1_200, week[5]: 30]),
    MonarchTimesheetGridRow(id: "timer", label: "Support (from the timer)", cells: [week[1]: 45], isReadOnly: true),
]

private let summaries = [
    MonarchTimesheetGridSummaryRow(
        id: "attendance",
        label: "Attendance",
        cells: [week[0]: "9:00–17:30", week[1]: "8:45–17:00"],
        total: "16h 45m"
    )
]

@MainActor private func save(_: String, _: Date, _: Int) async throws {}

enum MonarchTimesheetGridFixtures: ComponentFixtures {
    static let id = "timesheet-grid"

    static let fixtures = [
        Fixture("grid, editable, with locked, today, non-working and over-limit days") {
            MonarchTimesheetGrid(days: days, rows: rows, layout: .grid, onCellChange: save)
        },
        Fixture("day page, editable, starting on today") {
            MonarchTimesheetGrid(days: days, rows: rows, layout: .day, onCellChange: save)
        },
        Fixture("day page of a locked day") {
            MonarchTimesheetGrid(
                days: [MonarchTimesheetGridDay(week[0], isToday: true, lockReason: "Approved")],
                rows: rows,
                layout: .day,
                onCellChange: save
            )
        },
        Fixture("read-only with activatable cells and days") {
            VStack(alignment: .leading) {
                MonarchTimesheetGrid(
                    days: days,
                    rows: rows,
                    layout: .grid,
                    onDayActivate: { _ in },
                    onCellActivate: { _, _ in }
                )
                MonarchTimesheetGrid(
                    days: days,
                    rows: rows,
                    layout: .day,
                    onDayActivate: { _ in },
                    onCellActivate: { _, _ in }
                )
            }
        },
        Fixture("submitted: isReadOnly with a save handler") {
            MonarchTimesheetGrid(days: days, rows: rows, isReadOnly: true, onCellChange: save)
        },
        Fixture("summary rows and row actions") {
            VStack(alignment: .leading) {
                MonarchTimesheetGrid(
                    days: days,
                    rows: rows,
                    summaryRows: summaries,
                    layout: .grid,
                    rowActions: { _ in [MonarchDataTableRowAction("Clear row", isDestructive: true) {}] },
                    onCellChange: save
                )
                MonarchTimesheetGrid(
                    days: days,
                    rows: rows,
                    summaryRows: summaries,
                    layout: .day,
                    rowActions: { _ in [MonarchDataTableRowAction("Clear row", isDestructive: true) {}] },
                    onCellChange: save
                )
            }
        },
        Fixture("empty, in both layouts and without days") {
            VStack(alignment: .leading) {
                MonarchTimesheetGrid(days: days, rows: [], layout: .grid)
                MonarchTimesheetGrid(days: days, rows: [], layout: .day, emptyMessage: "Nothing logged this week.")
                MonarchTimesheetGrid(days: [], rows: rows, layout: .day)
            }
        },
        Fixture("automatic layout, a low daily maximum and custom strings") {
            MonarchTimesheetGrid(
                days: days,
                rows: rows,
                maximumMinutesPerDay: 600,
                labels: MonarchTimesheetGridLabels(
                    rowHeader: "Projekt",
                    total: "Summe",
                    dayTotal: "Tagessumme",
                    duration: { "\($0) min" },
                    shortDay: { _ in "Tag" }
                ),
                onCellChange: save
            )
        },
    ]
}

private struct SaveError: LocalizedError {
    var errorDescription: String? { "The period is closed." }
}

private struct SilentError: Error {}

@Suite("MonarchTimesheetGrid")
struct MonarchTimesheetGridTests {
    typealias Tokens = MonarchTimesheetGridTokens

    private let labels = MonarchTimesheetGridLabels()
    private let english = Locale(identifier: "en_US")
    private let utc = TimeZone(identifier: "UTC") ?? .gmt

    @Test("Totals add up per row, per day and for the period, ignoring days outside the grid")
    func totals() {
        #expect(Tokens.rowTotal(rows[0], days: days) == 810)
        #expect(Tokens.rowTotal(rows[1], days: days) == 1_230)
        #expect(Tokens.dayTotal(days[2], rows: rows) == 1_680)
        #expect(Tokens.dayTotal(days[6], rows: rows) == 0)
        #expect(Tokens.grandTotal(rows: rows, days: days) == 2_085)
        #expect(Tokens.rowTotal(rows[0], days: Array(days.prefix(1))) == 240)
        let negative = MonarchTimesheetGridRow(id: "x", label: "X", cells: [week[0]: -30])
        #expect(negative.minutes(on: week[0]) == 0)
        #expect(negative.minutes(on: week[1]) == 0)
    }

    @Test("A day is over the limit only above the maximum")
    func overLimit() {
        #expect(Tokens.isOverLimit(1_441, maximum: 1_440))
        #expect(!Tokens.isOverLimit(1_440, maximum: 1_440))
        #expect(Tokens.isOverLimit(Tokens.dayTotal(days[2], rows: rows), maximum: 1_440))
    }

    @Test("A lock reason locks the day")
    func locking() {
        #expect(days[0].isLocked)
        #expect(days[3].isLocked)
        #expect(!days[1].isLocked)
        #expect(Tokens.lockText(days[0], labels: labels) == "Approved")
        #expect(Tokens.lockText(days[3], labels: labels) == "Locked")
        #expect(Tokens.lockText(days[1], labels: labels) == nil)
        #expect(Tokens.lockText(days[3], labels: MonarchTimesheetGridLabels(locked: "Gesperrt")) == "Gesperrt")
    }

    @Test("A cell takes typing only in an editable grid, an editable row and an open day")
    func editability() {
        #expect(Tokens.isCellEditable(isEditable: true, row: rows[0], day: days[1]))
        #expect(!Tokens.isCellEditable(isEditable: false, row: rows[0], day: days[1]))
        #expect(!Tokens.isCellEditable(isEditable: true, row: rows[2], day: days[1]))
        #expect(!Tokens.isCellEditable(isEditable: true, row: rows[0], day: days[0]))
        #expect(!Tokens.isCellEditable(isEditable: true, row: rows[0], day: days[3]))
        #expect(Tokens.isCellEditable(isEditable: true, row: rows[0], day: days[5]))
    }

    @Test("Typed text is read as minutes, empty clears, and bad or too long text is refused")
    func reading() {
        #expect(Tokens.read("1:30", maximum: 1_440) == .save(90))
        #expect(Tokens.read("1.5h", maximum: 1_440) == .save(90))
        #expect(Tokens.read("1,5h", maximum: 1_440) == .save(90))
        #expect(Tokens.read("90m", maximum: 1_440) == .save(90))
        #expect(Tokens.read("2h 15m", maximum: 1_440) == .save(135))
        #expect(Tokens.read("  ", maximum: 1_440) == .save(0))
        #expect(Tokens.read("", maximum: 1_440) == .save(0))
        #expect(Tokens.read("soon", maximum: 1_440) == .invalid)
        #expect(Tokens.read("25h", maximum: 1_440) == .tooLong)
        #expect(Tokens.read("10h", maximum: 600) == .save(600))
        #expect(Tokens.read("10h 1m", maximum: 600) == .tooLong)
        #expect(Tokens.read("three", maximum: 600, parse: { $0 == "three" ? 180 : nil }) == .save(180))
        #expect(Tokens.read("four", maximum: 600, parse: { $0 == "three" ? 180 : nil }) == .invalid)
    }

    @Test("Problems are explained in words; a value to save has none")
    func problems() {
        #expect(Tokens.problem(.save(30), maximum: 1_440, labels: labels) == nil)
        #expect(Tokens.problem(.invalid, maximum: 1_440, labels: labels) == "Use a time such as 1:30, 1.5h or 90m.")
        #expect(Tokens.problem(.tooLong, maximum: 1_440, labels: labels) == "A day has at most 24h.")
        #expect(Tokens.problem(.tooLong, maximum: 450, labels: labels) == "A day has at most 7h 30m.")
        let custom = MonarchTimesheetGridLabels(invalid: "Nope", tooLong: { "max \($0)" })
        #expect(Tokens.problem(.invalid, maximum: 60, labels: custom) == "Nope")
        #expect(Tokens.problem(.tooLong, maximum: 60, labels: custom) == "max 1h")
    }

    @Test("A failed save shows the error's description, else the built-in message")
    func failures() {
        #expect(Tokens.failureMessage(for: SaveError(), labels: labels) == "The period is closed.")
        #expect(Tokens.failureMessage(for: SilentError(), labels: labels) == "That time could not be saved.")
        #expect(
            Tokens.failureMessage(for: SilentError(), labels: MonarchTimesheetGridLabels(saveFailed: "Failed"))
                == "Failed"
        )
    }

    @Test("Moving between cells skips cells that do not take typing and stops at the edges")
    func movement() {
        // Rows 0 and 1 editable, row 2 read-only; days 0 and 3 locked.
        let editable: (Int, Int) -> Bool = { row, day in row != 2 && day != 0 && day != 3 }
        func next(_ row: Int, _ day: Int, _ rowStep: Int, _ dayStep: Int, rowCount: Int = 3) -> [Int]? {
            Tokens.nextCell(
                row: row,
                day: day,
                rowStep: rowStep,
                dayStep: dayStep,
                rowCount: rowCount,
                dayCount: 7,
                isEditable: editable
            ).map { [$0.row, $0.day] }
        }
        #expect(next(0, 1, 1, 0) == [1, 1])
        #expect(next(1, 1, 1, 0) == nil)
        #expect(next(1, 1, -1, 0) == [0, 1])
        #expect(next(0, 1, -1, 0) == nil)
        #expect(next(0, 2, 0, 1) == [0, 4])
        #expect(next(0, 4, 0, -1) == [0, 2])
        #expect(next(0, 1, 0, -1) == nil)
        #expect(next(0, 6, 0, 1) == nil)
        #expect(next(1, 1, 1, 0, rowCount: 5) == [3, 1])
        #expect(next(0, 1, 0, 0) == nil)
    }

    @Test("The first page is the selected day, else today, else the first day")
    func firstPage() {
        #expect(Tokens.dayIndex(selected: nil, days: days) == 2)
        #expect(Tokens.dayIndex(selected: week[4], days: days) == 4)
        #expect(Tokens.dayIndex(selected: week[4].addingTimeInterval(60), days: days) == 2)
        #expect(Tokens.dayIndex(selected: nil, days: [days[0], days[1]]) == 0)
        #expect(Tokens.dayIndex(selected: nil, days: []) == 0)
    }

    @Test("Automatic layout is a grid at regular width below the accessibility sizes")
    func layouts() {
        #expect(Tokens.usesGrid(.automatic, isRegularWidth: true, isAccessibilitySize: false))
        #expect(!Tokens.usesGrid(.automatic, isRegularWidth: false, isAccessibilitySize: false))
        #expect(!Tokens.usesGrid(.automatic, isRegularWidth: true, isAccessibilitySize: true))
        #expect(Tokens.usesGrid(.grid, isRegularWidth: false, isAccessibilitySize: true))
        #expect(!Tokens.usesGrid(.day, isRegularWidth: true, isAccessibilitySize: false))
    }

    @Test("Durations are written as on the web and a cell with no time shows a dash")
    func durations() {
        #expect(Tokens.durationText(90, labels: labels) == "1h 30m")
        #expect(Tokens.durationText(480, labels: labels) == "8h")
        #expect(Tokens.durationText(0, labels: labels) == "0m")
        #expect(Tokens.valueText(0, labels: labels) == "\u{2013}")
        #expect(Tokens.valueText(45, labels: labels) == "45m")
        #expect(Tokens.valueText(45, labels: MonarchTimesheetGridLabels(duration: { "\($0) min" })) == "45 min")
    }

    @Test("Durations are read in words in the given locale")
    func spokenDurations() {
        #expect(Tokens.spokenDuration(90, labels: labels, locale: english) == "1 hour, 30 minutes")
        #expect(Tokens.spokenDuration(480, labels: labels, locale: english) == "8 hours")
        #expect(Tokens.spokenDuration(0, labels: labels, locale: english) == "No time")
        #expect(Tokens.spokenDuration(0, labels: MonarchTimesheetGridLabels(noTime: "Leer"), locale: english) == "Leer")
    }

    @Test("Days are written in the given locale and time zone, or by the overrides")
    func dayText() {
        #expect(Tokens.shortDayText(days[0], labels: labels, locale: english, timeZone: utc) == "Mon 12")
        #expect(Tokens.longDayText(days[0], labels: labels, locale: english, timeZone: utc) == "Mon, Jan 12")
        #expect(Tokens.shortDayText(days[6], labels: labels, locale: english, timeZone: utc) == "Sunday")
        let custom = MonarchTimesheetGridLabels(shortDay: { _ in "S" }, longDay: { _ in "L" })
        #expect(Tokens.shortDayText(days[0], labels: custom, locale: english, timeZone: utc) == "S")
        #expect(Tokens.shortDayText(days[6], labels: custom, locale: english, timeZone: utc) == "Sunday")
        #expect(Tokens.longDayText(days[6], labels: custom, locale: english, timeZone: utc) == "L")
        let german = Locale(identifier: "de_DE")
        #expect(Tokens.shortDayText(days[0], labels: labels, locale: german, timeZone: utc) != "Mon 12")
    }

    @Test("Expected time and a row's helper line")
    func supportingText() {
        #expect(Tokens.expectedText(days[1], labels: labels) == "of 8h")
        #expect(Tokens.expectedText(days[5], labels: labels) == nil)
        #expect(Tokens.pageRowHelper(rows[0], days: days, labels: labels) == "Design · Billable · Total 13h 30m")
        #expect(Tokens.pageRowHelper(rows[1], days: days, labels: labels) == "Total 20h 30m")
    }

    @Test("Cells, headings and totals are named and valued for VoiceOver")
    func accessibility() {
        let day = Tokens.longDayText(days[1], labels: labels, locale: english, timeZone: utc)
        #expect(Tokens.cellLabel(rowName: "Atlas", dayText: day, labels: labels) == "Atlas, Tue, Jan 13")
        #expect(
            Tokens.cellLabel(
                rowName: "Atlas",
                dayText: day,
                labels: MonarchTimesheetGridLabels(cellLabel: { "\($1): \($0)" })
            )
                == "Tue, Jan 13: Atlas"
        )
        #expect(Tokens.cellValue(90, lockText: nil, labels: labels, locale: english) == "1 hour, 30 minutes")
        #expect(Tokens.cellValue(0, lockText: "Approved", labels: labels, locale: english) == "No time, Approved")
        #expect(Tokens.cellHint(error: nil, isSaving: false, labels: labels).isEmpty)
        #expect(Tokens.cellHint(error: nil, isSaving: true, labels: labels) == "Saving")
        #expect(Tokens.cellHint(error: "Too long", isSaving: true, labels: labels) == "Error: Too long")
        #expect(
            Tokens.dayHeadingLabel(days[0], labels: labels, locale: english, timeZone: utc)
                == "Mon, Jan 12, Approved, Payroll closed"
        )
        #expect(Tokens.dayHeadingLabel(days[2], labels: labels, locale: english, timeZone: utc) == "Wed, Jan 14, Today")
        #expect(
            Tokens.dayHeadingLabel(days[3], labels: labels, locale: english, timeZone: utc) == "Thu, Jan 15, Locked"
        )
        #expect(
            Tokens.dayTotalValue(days[2], total: 1_680, maximum: 1_440, labels: labels, locale: english)
                == "28 hours, of 8h, Over the daily limit"
        )
        #expect(
            Tokens.dayTotalValue(days[5], total: 30, maximum: 1_440, labels: labels, locale: english) == "30 minutes"
        )
    }

    @Test("Cells are shaded when locked or non-working, bordered in danger on error, muted while saving")
    func appearance() {
        let open = Tokens.cellAppearance(
            isEditable: true,
            isLocked: false,
            isNonWorking: false,
            hasError: false,
            isSaving: false
        )
        #expect(
            open
                == MonarchTimesheetGridCellAppearance(
                    background: \.bgSurface,
                    border: \.borderInput,
                    foreground: \.textDefault
                )
        )
        let weekend = Tokens.cellAppearance(
            isEditable: true,
            isLocked: false,
            isNonWorking: true,
            hasError: false,
            isSaving: false
        )
        #expect(weekend.background == \.bgSubtle)
        let locked = Tokens.cellAppearance(
            isEditable: false,
            isLocked: true,
            isNonWorking: false,
            hasError: false,
            isSaving: false
        )
        #expect(
            locked
                == MonarchTimesheetGridCellAppearance(
                    background: \.bgSubtle,
                    border: \.borderSubtle,
                    foreground: \.textMuted
                )
        )
        let failed = Tokens.cellAppearance(
            isEditable: true,
            isLocked: false,
            isNonWorking: false,
            hasError: true,
            isSaving: false
        )
        #expect(failed.border == \.dangerStrong)
        let saving = Tokens.cellAppearance(
            isEditable: true,
            isLocked: false,
            isNonWorking: false,
            hasError: false,
            isSaving: true
        )
        #expect(saving.foreground == \.textMuted)
        #expect(Tokens.headingForeground(isToday: true) == \.accentStrong)
        #expect(Tokens.headingForeground(isToday: false) == \.textDefault)
        #expect(Tokens.totalForeground(isOverLimit: true) == \.dangerStrong)
        #expect(Tokens.totalForeground(isOverLimit: false) == \.textDefault)
    }

    @Test("Every built-in string resolves from the table")
    func strings() {
        typealias Strings = MonarchTimesheetGridStrings
        let all = [
            Strings.caption(labels), Strings.rowHeader(labels), Strings.total(labels), Strings.dayTotal(labels),
            Strings.rowActions("Atlas", labels), Strings.cellLabel("Atlas", "Mon", labels), Strings.locked(labels),
            Strings.noTime(labels), Strings.invalid(labels), Strings.tooLong("8h", labels), Strings.saveFailed(labels),
            Strings.expected("8h", labels), Strings.overLimit(labels), Strings.empty(labels), Strings.today(labels),
            Strings.saving(labels), Strings.previousDay(labels), Strings.nextDay(labels),
            Strings.rowTotal("8h", labels), Strings.error("Oops"),
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."), "\(string)")
            #expect(!string.isEmpty)
        }
        #expect(Strings.rowActions("Atlas", labels) == "Actions for Atlas")
        #expect(Strings.rowTotal("8h", labels) == "Total 8h")
    }

    @Test("Every string can be replaced")
    func overrides() {
        typealias Strings = MonarchTimesheetGridStrings
        let custom = MonarchTimesheetGridLabels(
            caption: "c",
            rowHeader: "rh",
            total: "t",
            dayTotal: "dt",
            rowActions: { "ra \($0)" },
            empty: "e",
            today: "td",
            saving: "s",
            previousDay: "p",
            nextDay: "n",
            rowTotal: { "rt \($0)" }
        )
        #expect(Strings.caption(custom) == "c")
        #expect(Strings.rowHeader(custom) == "rh")
        #expect(Strings.total(custom) == "t")
        #expect(Strings.dayTotal(custom) == "dt")
        #expect(Strings.rowActions("A", custom) == "ra A")
        #expect(Strings.empty(custom) == "e")
        #expect(Strings.today(custom) == "td")
        #expect(Strings.saving(custom) == "s")
        #expect(Strings.previousDay(custom) == "p")
        #expect(Strings.nextDay(custom) == "n")
        #expect(Strings.rowTotal("1h", custom) == "rt 1h")
        #expect(Strings.expected("8h", MonarchTimesheetGridLabels(expected: { "/\($0)" })) == "/8h")
        #expect(Strings.overLimit(MonarchTimesheetGridLabels(overLimit: "over")) == "over")
    }

    #if os(iOS)
        @MainActor
        @Test("Cells and day buttons keep a 44-point touch target")
        func touchTargets() {
            let size = Host.layout(
                MonarchTimesheetGrid(days: days, rows: rows, layout: .grid, onCellChange: save)
            )
            #expect(size.height >= Tokens.minCellHeight * Double(rows.count + 2))
        }
    #endif
}
