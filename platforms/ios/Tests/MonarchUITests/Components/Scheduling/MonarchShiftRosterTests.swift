import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// A fixed calendar, locale and days, so nothing depends on the machine or the clock.
private enum RosterClock {
    static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        return calendar
    }()
    static let locale = Locale(identifier: "en_US")

    /// November 2026: Monday the 2nd.
    static func date(_ day: Int, _ hour: Int = 0) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 11, day: day, hour: hour))
            ?? Date(timeIntervalSince1970: 0)
    }
}

private let rosterShifts = [
    MonarchShiftRosterShift(id: "m", name: "Morning", start: 6 * 60, end: 14 * 60, shortName: "M"),
    MonarchShiftRosterShift(id: "e", name: "Evening", start: 14 * 60, end: 22 * 60),
    MonarchShiftRosterShift(id: "n", name: "Night", start: 22 * 60, end: 6 * 60, shortName: "N"),
]

/// Computed, because `MonarchShiftRosterMember` holds an `Image` and is not `Sendable`.
private var rosterMembers: [MonarchShiftRosterMember] {
    [
        MonarchShiftRosterMember(id: "a", name: "Alex Kim", description: "Support"),
        MonarchShiftRosterMember(id: "b", name: "Sam Lee"),
        MonarchShiftRosterMember(
            id: "c",
            name: "Maximiliana Montgomery-Fitzgerald",
            description: "Customer success, northern region, weekend cover"
        ),
    ]
}

private let rosterValue: [String: [Date: MonarchShiftRosterAssignment]] = [
    "a": [RosterClock.date(2, 9): .shift("m"), RosterClock.date(3): .shift("n"), RosterClock.date(4): .off],
    "b": [RosterClock.date(2): .off, RosterClock.date(3): .shift("e")],
    "c": [RosterClock.date(2): .shift("unknown")],
]

@MainActor
private func roster(
    members: [MonarchShiftRosterMember] = rosterMembers,
    layout: MonarchShiftRosterLayout = .byDay,
    days: Int = 7,
    isReadOnly: Bool = false,
    isLoading: Bool = false,
    showsTotals: Bool = true,
    error: String? = nil,
    labels: MonarchShiftRosterLabels = MonarchShiftRosterLabels(),
    onSelectMember: (@MainActor (MonarchShiftRosterMember) -> Void)? = nil
) -> some View {
    MonarchShiftRoster(
        members: members,
        shifts: rosterShifts,
        from: RosterClock.date(2, 15),
        value: .constant(rosterValue),
        days: days,
        layout: layout,
        isReadOnly: isReadOnly,
        isLoading: isLoading,
        showsTotals: showsTotals,
        today: RosterClock.date(3, 10),
        calendar: RosterClock.calendar,
        uses24HourClock: true,
        error: error,
        labels: labels,
        onSelectMember: onSelectMember
    )
    .environment(\.locale, RosterClock.locale)
}

enum MonarchShiftRosterFixtures: ComponentFixtures {
    static let id = "shift-roster"
    static let fixtures = [
        Fixture("one day at a time, editable, opened on today") { roster() },
        Fixture("one person at a time, read only") { roster(layout: .byPerson, days: 4, isReadOnly: true) },
        Fixture("grid, scrolled sideways") { roster(layout: .grid) },
        Fixture("automatic, two weeks") { roster(layout: .automatic, days: 14) },
        Fixture("grid without totals, people are buttons") {
            roster(layout: .grid, days: 3, showsTotals: false) { _ in }
        },
        Fixture("disabled") { roster().disabled(true) },
        Fixture("loading") { roster(isLoading: true) },
        Fixture("error") { roster(error: "The roster could not be loaded. Try again in a moment.") },
        Fixture("empty, built in") { roster(members: []) },
        Fixture("empty, app's own") {
            MonarchShiftRoster(
                members: [],
                shifts: rosterShifts,
                from: RosterClock.date(2),
                value: .constant([:]),
                calendar: RosterClock.calendar
            ) {
                Text("Invite your team to start scheduling")
            }
        },
        Fixture("custom labels") {
            roster(labels: MonarchShiftRosterLabels(off: "Rest day", unassigned: "Open", today: "Now"))
        },
    ]
}

@Suite("MonarchShiftRoster")
@MainActor
struct MonarchShiftRosterTests {
    private let calendar = RosterClock.calendar

    /// A model over a mutable value, recording every change.
    private final class Box {
        var value: [String: [Date: MonarchShiftRosterAssignment]]
        var changes: [MonarchShiftRosterChange] = []
        init(_ value: [String: [Date: MonarchShiftRosterAssignment]]) { self.value = value }
    }

    private func model(
        _ box: Box = Box(rosterValue),
        isReadOnly: Bool = false,
        showsTotals: Bool = true,
        today: Date? = RosterClock.date(3, 10),
        labels: MonarchShiftRosterLabels = MonarchShiftRosterLabels(),
        uses24HourClock: Bool? = true
    ) -> MonarchShiftRosterModel {
        MonarchShiftRosterModel(
            members: rosterMembers,
            shifts: rosterShifts,
            range: MonarchShiftRosterTokens.range(from: RosterClock.date(2, 15), days: 7, calendar: calendar),
            value: Binding(get: { box.value }, set: { box.value = $0 }),
            isReadOnly: isReadOnly,
            showsTotals: showsTotals,
            today: today,
            calendar: calendar,
            text: MonarchShiftRosterText(labels: labels),
            formats: MonarchShiftRosterFormats(
                locale: RosterClock.locale,
                calendar: calendar,
                uses24HourClock: uses24HourClock
            ),
            onChange: { box.changes.append($0) },
            onSelectMember: nil
        )
    }

    // MARK: Rules

    @Test("The range starts at the start of the first day and is clamped to 1…62 days")
    func range() {
        let week = MonarchShiftRosterTokens.range(from: RosterClock.date(2, 15), days: 7, calendar: calendar)
        #expect(week == (2...8).map { RosterClock.date($0) })
        #expect(MonarchShiftRosterTokens.range(from: RosterClock.date(2), days: 0, calendar: calendar).count == 1)
        #expect(MonarchShiftRosterTokens.range(from: RosterClock.date(2), days: -3, calendar: calendar).count == 1)
        #expect(MonarchShiftRosterTokens.range(from: RosterClock.date(2), days: 500, calendar: calendar).count == 62)
    }

    @Test("Assignments are matched by day, whatever the time of the key")
    func matching() {
        let value = rosterValue
        #expect(
            MonarchShiftRosterTokens.assignment(in: value, memberId: "a", day: RosterClock.date(2), calendar: calendar)
                == .shift("m")
        )
        #expect(
            MonarchShiftRosterTokens.assignment(
                in: value,
                memberId: "a",
                day: RosterClock.date(4, 23),
                calendar: calendar
            )
                == .off
        )
        #expect(
            MonarchShiftRosterTokens.assignment(in: value, memberId: "a", day: RosterClock.date(5), calendar: calendar)
                == nil
        )
        #expect(
            MonarchShiftRosterTokens.assignment(in: value, memberId: "z", day: RosterClock.date(2), calendar: calendar)
                == nil
        )
    }

    @Test("Assigning replaces any key on the same day with the start of the day, and clearing removes it")
    func assigning() {
        let next = MonarchShiftRosterTokens.assigning(
            .off,
            in: rosterValue,
            memberId: "a",
            day: RosterClock.date(2, 20),
            calendar: calendar
        )
        #expect(next["a"]?[RosterClock.date(2)] == .off)
        #expect(next["a"]?[RosterClock.date(2, 9)] == nil)
        #expect(next["a"]?.count == 3)
        #expect(next["b"] == rosterValue["b"])
        let cleared = MonarchShiftRosterTokens.assigning(
            nil,
            in: next,
            memberId: "a",
            day: RosterClock.date(2),
            calendar: calendar
        )
        #expect(cleared["a"]?.count == 2)
        let added = MonarchShiftRosterTokens.assigning(
            .shift("e"),
            in: [:],
            memberId: "new",
            day: RosterClock.date(6, 7),
            calendar: calendar
        )
        #expect(added == ["new": [RosterClock.date(6): .shift("e")]])
    }

    @Test("Shift lengths, overnight shifts and unknown ids")
    func shifts() {
        #expect(MonarchShiftRosterTokens.durationMinutes(rosterShifts[0]) == 480)
        #expect(MonarchShiftRosterTokens.durationMinutes(rosterShifts[2]) == 480)
        let allDay = MonarchShiftRosterShift(id: "d", name: "Day", start: 8 * 60, end: 8 * 60)
        #expect(MonarchShiftRosterTokens.durationMinutes(allDay) == 1440)
        #expect(MonarchShiftRosterTokens.isOvernight(allDay))
        #expect(MonarchShiftRosterTokens.isOvernight(rosterShifts[2]))
        #expect(!MonarchShiftRosterTokens.isOvernight(rosterShifts[0]))
        #expect(MonarchShiftRosterTokens.shift(of: .shift("unknown"), in: rosterShifts) == nil)
        #expect(MonarchShiftRosterTokens.shift(of: .off, in: rosterShifts) == nil)
        #expect(MonarchShiftRosterTokens.shift(of: .shift("n"), in: rosterShifts)?.name == "Night")
    }

    @Test("Hours per person and people working per day count known shifts only")
    func totals() {
        let range = MonarchShiftRosterTokens.range(from: RosterClock.date(2), days: 7, calendar: calendar)
        let hours = { (id: String) in
            MonarchShiftRosterTokens.hours(
                in: rosterValue,
                memberId: id,
                range: range,
                shifts: rosterShifts,
                calendar: calendar
            )
        }
        #expect(hours("a") == 16)
        #expect(hours("b") == 8)
        #expect(hours("c") == 0)
        let working = { (day: Int) in
            MonarchShiftRosterTokens.working(
                in: rosterValue,
                members: rosterMembers,
                day: RosterClock.date(day),
                shifts: rosterShifts,
                calendar: calendar
            )
        }
        #expect(working(2) == 1)
        #expect(working(3) == 2)
        #expect(working(4) == 0)
    }

    @Test("The day layout opens on the chosen day, then today, then the first day")
    func shownDay() {
        let range = MonarchShiftRosterTokens.range(from: RosterClock.date(2), days: 7, calendar: calendar)
        let shown = { (chosen: Date?, today: Date?) in
            MonarchShiftRosterTokens.shownDay(chosen: chosen, range: range, today: today, calendar: calendar)
        }
        #expect(shown(nil, RosterClock.date(4, 13)) == RosterClock.date(4))
        #expect(shown(RosterClock.date(6), RosterClock.date(4)) == RosterClock.date(6))
        #expect(shown(nil, RosterClock.date(20)) == RosterClock.date(2))
        #expect(shown(RosterClock.date(20), nil) == RosterClock.date(2))
        #expect(MonarchShiftRosterTokens.shownDay(chosen: nil, range: [], today: nil, calendar: calendar) == nil)
        #expect(MonarchShiftRosterTokens.shownMember(chosen: "b", members: rosterMembers) == "b")
        #expect(MonarchShiftRosterTokens.shownMember(chosen: "gone", members: rosterMembers) == "a")
        #expect(MonarchShiftRosterTokens.shownMember(chosen: nil, members: []) == nil)
    }

    @Test("The automatic layout allows the grid up to xxLarge text, and rows stack at accessibility sizes")
    func layoutRules() {
        #expect(MonarchShiftRosterTokens.allowsGrid(.large))
        #expect(MonarchShiftRosterTokens.allowsGrid(.xxLarge))
        #expect(!MonarchShiftRosterTokens.allowsGrid(.xxxLarge))
        #expect(!MonarchShiftRosterTokens.allowsGrid(.accessibility1))
        #expect(!MonarchShiftRosterTokens.stacksRows(.xxxLarge))
        #expect(MonarchShiftRosterTokens.stacksRows(.accessibility1))
        let width = MonarchShiftRosterTokens.gridWidth(person: 160, day: 112, hours: 80, days: 7, showsTotals: true)
        let expected: CGFloat = 1024
        #expect(width == expected)
        #expect(
            MonarchShiftRosterTokens.gridWidth(person: 160, day: 112, hours: 80, days: 7, showsTotals: false)
                == CGFloat(944)
        )
    }

    // MARK: Colours

    @Test("A day off is a muted fill, a day not assigned a dashed border, today the strong accent")
    func colours() {
        let off = MonarchShiftRosterTokens.slotAppearance(isOff: true)
        #expect(off.fill == \MonarchColorTokens.bgMuted)
        #expect(off.border == nil)
        #expect(off.content == \MonarchColorTokens.textMuted)
        let open = MonarchShiftRosterTokens.slotAppearance(isOff: false)
        #expect(open.fill == nil)
        #expect(open.border == \MonarchColorTokens.borderDefault)
        #expect(off != open)
        #expect(MonarchShiftRosterTokens.dayColor(isToday: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchShiftRosterTokens.dayColor(isToday: false) == \MonarchColorTokens.textDefault)
    }

    // MARK: Editing

    @Test("Assigning updates the value and reports one change with the start of the day")
    func assign() {
        let box = Box(rosterValue)
        let model = model(box)
        model.assign(.shift("e"), to: rosterMembers[1], on: RosterClock.date(5, 18))
        #expect(box.value["b"]?[RosterClock.date(5)] == .shift("e"))
        #expect(
            box.changes == [MonarchShiftRosterChange(memberId: "b", day: RosterClock.date(5), assignment: .shift("e"))]
        )
        model.assign(nil, to: rosterMembers[1], on: RosterClock.date(5))
        #expect(box.value["b"]?[RosterClock.date(5)] == nil)
        #expect(box.changes.last?.assignment == nil)
        #expect(box.changes.count == 2)
    }

    @Test("Choosing the current assignment again, or editing read only, changes nothing")
    func noOp() {
        let box = Box(rosterValue)
        model(box).assign(.shift("m"), to: rosterMembers[0], on: RosterClock.date(2))
        #expect(box.changes.isEmpty)
        #expect(box.value == rosterValue)
        let readOnly = Box(rosterValue)
        model(readOnly, isReadOnly: true).assign(.off, to: rosterMembers[0], on: RosterClock.date(6))
        #expect(readOnly.changes.isEmpty)
        #expect(readOnly.value == rosterValue)
    }

    // MARK: Accessibility text

    @Test("A cell is read as person, day and assignment, with Today and overnight")
    func cellLabels() {
        let model = model()
        let alex = rosterMembers[0]
        #expect(
            model.cellLabel(alex, on: RosterClock.date(2), isCompact: false)
                == "Alex Kim, Monday, November 2: Morning, 06:00 to 14:00"
        )
        #expect(
            model.cellLabel(alex, on: RosterClock.date(3), isCompact: true)
                == "Alex Kim, Tuesday, November 3, Today: Night, N, 22:00 to 06:00, overnight"
        )
        #expect(
            model.cellLabel(alex, on: RosterClock.date(4), isCompact: true) == "Alex Kim, Wednesday, November 4: Off"
        )
        #expect(
            model.cellLabel(alex, on: RosterClock.date(5), isCompact: true)
                == "Alex Kim, Thursday, November 5: Not assigned"
        )
        #expect(
            model.cellLabel(rosterMembers[2], on: RosterClock.date(2), isCompact: false)
                == "Maximiliana Montgomery-Fitzgerald, Monday, November 2: Not assigned"
        )
        // The short name is only added when it differs from the name and is shown.
        #expect(model.spokenShift(rosterShifts[0], isCompact: false) == "Morning, 06:00 to 14:00")
        #expect(model.spokenShift(rosterShifts[1], isCompact: true) == "Evening, 14:00 to 22:00")
    }

    @Test("People, totals, menus and days read in full")
    func otherLabels() {
        let model = model()
        #expect(model.personLabel(rosterMembers[0]) == "Alex Kim, Support, Hours: 16 h")
        #expect(model.personLabel(rosterMembers[1]) == "Sam Lee, Hours: 8 h")
        #expect(model.supporting(rosterMembers[0]) == "Support · 16 h")
        #expect(model.menuTitle(rosterMembers[0], on: RosterClock.date(4)) == "Alex Kim, Wednesday, November 4")
        #expect(model.menuItem(rosterShifts[2]) == "Night · 22:00 – 06:00")
        #expect(model.workingValue(RosterClock.date(2)) == "1 working")
        #expect(model.workingValue(RosterClock.date(3)) == "2 working")
        #expect(model.workingLabel(RosterClock.date(4)) == "Wednesday, November 4: 0 working")
        #expect(model.tabTitle(RosterClock.date(3)) == "Today")
        #expect(model.tabTitle(RosterClock.date(4)) == "Wed 4")
        #expect(model.spokenDay(RosterClock.date(3)) == "Tuesday, November 3, Today")
        let noTotals = self.model(showsTotals: false)
        #expect(noTotals.personLabel(rosterMembers[0]) == "Alex Kim, Support")
        #expect(noTotals.supporting(rosterMembers[1]) == nil)
        #expect(self.model(today: nil).tabTitle(RosterClock.date(3)) == "Tue 3")
    }

    @Test("Hours are rounded to one decimal in the locale")
    func hoursFormat() {
        let formats = MonarchShiftRosterFormats(
            locale: Locale(identifier: "de_DE"),
            calendar: calendar,
            uses24HourClock: true
        )
        #expect(formats.hours(37.5) == "37,5")
        #expect(formats.hours(8) == "8")
        #expect(formats.hours(7.25) == "7,2" || formats.hours(7.25) == "7,3")
    }

    @Test("Shift times follow the clock setting, and the locale when it is not set")
    func times() {
        let twelve = MonarchShiftRosterFormats(locale: RosterClock.locale, calendar: calendar, uses24HourClock: false)
        #expect(twelve.time(22 * 60).hasPrefix("10:00"))
        #expect(twelve.time(22 * 60).hasSuffix("PM"))
        let twentyFour = MonarchShiftRosterFormats(
            locale: RosterClock.locale,
            calendar: calendar,
            uses24HourClock: true
        )
        #expect(twentyFour.time(6 * 60 + 5) == "06:05")
        #expect(twentyFour.time(1440 + 60) == "01:00")
        #expect(twentyFour.time(-60) == "23:00")
        let british = MonarchShiftRosterFormats(
            locale: Locale(identifier: "en_GB"),
            calendar: calendar,
            uses24HourClock: nil
        )
        #expect(british.time(14 * 60) == "14:00")
    }

    @Test("Labels replace every built-in string")
    func overrides() {
        let labels = MonarchShiftRosterLabels(
            off: "Rest",
            unassigned: "Open",
            menuTitle: { "\($1) – \($0)" },
            cell: { "\($2) | \($0) | \($1)" },
            range: { "\($0)-\($1)" },
            overnight: "next day",
            hours: "Total",
            hoursValue: { "\($0) hrs" },
            workingValue: { "\($0) on shift" },
            today: "Now"
        )
        let model = model(labels: labels)
        #expect(
            model.cellLabel(rosterMembers[0], on: RosterClock.date(4), isCompact: false)
                == "Rest | Alex Kim | Wednesday, November 4"
        )
        #expect(model.cellLabel(rosterMembers[0], on: RosterClock.date(5), isCompact: false).hasPrefix("Open"))
        #expect(model.spokenShift(rosterShifts[2]) == "Night, 22:00-06:00, next day")
        #expect(model.personLabel(rosterMembers[1]) == "Sam Lee, Total: 8 hrs")
        #expect(model.workingValue(RosterClock.date(3)) == "2 on shift")
        #expect(model.menuTitle(rosterMembers[1], on: RosterClock.date(2)) == "Monday, November 2 – Sam Lee")
        #expect(model.tabTitle(RosterClock.date(3)) == "Now")
        let text = MonarchShiftRosterText(
            labels: MonarchShiftRosterLabels(
                roster: "Rota",
                person: "Who",
                day: "When",
                clear: "Remove",
                working: "On",
                emptyTitle: "Nobody",
                emptyDescription: "Add people",
                loading: "Wait"
            )
        )
        #expect(
            [
                text.roster, text.person, text.day, text.clear, text.working, text.emptyTitle, text.emptyDescription,
                text.loading,
            ]
                == ["Rota", "Who", "When", "Remove", "On", "Nobody", "Add people", "Wait"]
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        let plain = [
            MonarchShiftRosterStrings.roster, MonarchShiftRosterStrings.person, MonarchShiftRosterStrings.day,
            MonarchShiftRosterStrings.off, MonarchShiftRosterStrings.unassigned, MonarchShiftRosterStrings.clear,
            MonarchShiftRosterStrings.overnight, MonarchShiftRosterStrings.hours, MonarchShiftRosterStrings.working,
            MonarchShiftRosterStrings.today, MonarchShiftRosterStrings.emptyTitle,
            MonarchShiftRosterStrings.emptyDescription, MonarchShiftRosterStrings.loading,
            MonarchShiftRosterStrings.workingOne,
        ]
        for string in plain {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchShiftRosterStrings.workingOther("3") == "3 working")
        #expect(MonarchShiftRosterStrings.menuTitle("A", "B") == "A, B")
        #expect(MonarchShiftRosterStrings.cell("A", "B", "C") == "A, B: C")
        #expect(MonarchShiftRosterStrings.range("1", "2") == "1 to 2")
        #expect(MonarchShiftRosterStrings.hoursValue("40") == "40 h")
    }

    #if os(iOS)
        @Test("An editable cell keeps a 44-point touch target at the default size")
        func touchTarget() {
            let size = Host.layout(
                MonarchShiftRosterCell(
                    model: model(),
                    member: rosterMembers[1],
                    day: RosterClock.date(5),
                    isCompact: true
                )
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
