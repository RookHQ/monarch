import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

/// A fixed calendar, locale and dates, so nothing depends on the machine or the clock.
private enum TeamClock {
    static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        calendar.locale = Locale(identifier: "en_US")
        return calendar
    }()

    static let locale = Locale(identifier: "en_US")

    /// A day of November 2026 (November 1 is a Sunday), at an hour.
    static func date(_ day: Int, _ hour: Int = 0, month: Int = 11) -> Date {
        let components = DateComponents(year: 2026, month: month, day: day, hour: hour)
        return calendar.date(from: components) ?? Date(timeIntervalSince1970: 0)
    }
}

private var teamMembers: [MonarchTeamCalendarMember] {
    [
        MonarchTeamCalendarMember("Alex Kim", id: "alex", description: "Design"),
        MonarchTeamCalendarMember("Sam Lee", id: "sam", description: "Engineering"),
        MonarchTeamCalendarMember("Priya Nair", id: "priya"),
    ]
}

private let teamEntries = [
    MonarchTeamCalendarEntry(
        "Annual leave",
        id: "1",
        memberID: "alex",
        start: TeamClock.date(2, 9),
        end: TeamClock.date(5)
    ),
    MonarchTeamCalendarEntry(
        "Doctor",
        id: "2",
        memberID: "sam",
        start: TeamClock.date(3),
        portion: .firstHalf,
        isPending: true
    ),
    MonarchTeamCalendarEntry(
        "Conference",
        id: "3",
        memberID: "priya",
        start: TeamClock.date(9),
        end: TeamClock.date(11),
        portions: [TeamClock.date(11, 15): .secondHalf]
    ),
    MonarchTeamCalendarEntry("Ghost", id: "4", memberID: "nobody", start: TeamClock.date(4)),
]

enum MonarchTeamCalendarFixtures: ComponentFixtures {
    static let id = "team-calendar"
    static let fixtures = [
        Fixture("month with bars, half days, pending, a holiday and today, tappable") {
            MonarchTeamCalendar(
                members: teamMembers,
                entries: teamEntries,
                month: TeamClock.date(15),
                holidays: [MonarchTeamCalendarHoliday("Thanksgiving", date: TeamClock.date(26))],
                weekendDays: [1, 7],
                today: TeamClock.date(3),
                calendar: TeamClock.calendar,
                onEntryTap: { _, _ in },
                onCellTap: { _, _ in }
            )
            .frame(height: MonarchTokens.Space.space24 * 3)
        },
        Fixture("two-week range, read only, no legend, calendar weekend") {
            MonarchTeamCalendar(
                members: [MonarchTeamCalendarMember("Priya Nair", id: "priya")],
                entries: teamEntries,
                from: TeamClock.date(1),
                to: TeamClock.date(14),
                today: TeamClock.date(1),
                calendar: TeamClock.calendar,
                showsLegend: false
            )
            .frame(height: MonarchTokens.Space.space24 * 2)
        },
        Fixture("custom colours, long names and custom labels") {
            MonarchTeamCalendar(
                members: [
                    MonarchTeamCalendarMember(
                        "Maximiliana Alexandrovna Konstantinopoulou",
                        id: "max",
                        description: "Principal engineer, platform reliability and infrastructure"
                    )
                ],
                entries: [
                    MonarchTeamCalendarEntry(
                        "Parental leave with a very long label that does not fit",
                        id: "p",
                        memberID: "max",
                        start: TeamClock.date(20, month: 10),
                        end: TeamClock.date(8),
                        color: MonarchTheme.light.colors.successSolid
                    )
                ],
                month: TeamClock.date(1),
                today: TeamClock.date(20, month: 12),
                calendar: TeamClock.calendar,
                labels: MonarchTeamCalendarLabels(person: "Who", legend: "Grey days are off.")
            )
            .frame(height: MonarchTokens.Space.space24 * 2)
        },
        Fixture("no members") {
            MonarchTeamCalendar(members: [], entries: [], calendar: TeamClock.calendar)
                .frame(height: MonarchTokens.Space.space24 * 3)
        },
        Fixture("loading") {
            MonarchTeamCalendar(members: teamMembers, entries: [], calendar: TeamClock.calendar, isLoading: true)
        },
        Fixture("error") {
            MonarchTeamCalendar(
                members: teamMembers,
                entries: [],
                calendar: TeamClock.calendar,
                errorMessage: "Could not load time away."
            )
        },
    ]
}

@Suite("MonarchTeamCalendar")
struct MonarchTeamCalendarTests {
    private typealias Tokens = MonarchTeamCalendarTokens
    private let calendar = TeamClock.calendar

    private var formats: MonarchTeamCalendarFormats {
        MonarchTeamCalendarFormats(locale: TeamClock.locale, calendar: calendar)
    }

    private var november: [Date] {
        Tokens.days(from: TeamClock.date(1), through: TeamClock.date(30), calendar: calendar)
    }

    @Test("A month runs from its first to its last day, from month or from today")
    func monthRange() {
        let range = Tokens.range(
            month: TeamClock.date(15, 13),
            from: nil,
            to: nil,
            today: TeamClock.date(3, month: 2),
            calendar: calendar
        )
        #expect(range.first == TeamClock.date(1))
        #expect(range.last == TeamClock.date(30))
        let fromToday = Tokens.range(
            month: nil,
            from: nil,
            to: nil,
            today: TeamClock.date(10, month: 2),
            calendar: calendar
        )
        #expect(fromToday.first == TeamClock.date(1, month: 2))
        #expect(fromToday.last == TeamClock.date(28, month: 2))
        #expect(Tokens.days(from: range.first, through: range.last, calendar: calendar).count == 30)
    }

    @Test("A custom range needs both ends, a backwards range is one day, and it is capped")
    func customRange() {
        let range = Tokens.range(
            month: TeamClock.date(1, month: 3),
            from: TeamClock.date(2, 8),
            to: TeamClock.date(8, 20),
            today: TeamClock.date(1),
            calendar: calendar
        )
        #expect(range.first == TeamClock.date(2))
        #expect(range.last == TeamClock.date(8))
        let onlyFrom = Tokens.range(
            month: nil,
            from: TeamClock.date(2),
            to: nil,
            today: TeamClock.date(1),
            calendar: calendar
        )
        #expect(onlyFrom.last == TeamClock.date(30))
        let backwards = Tokens.range(
            month: nil,
            from: TeamClock.date(8),
            to: TeamClock.date(2),
            today: TeamClock.date(1),
            calendar: calendar
        )
        #expect(backwards.first == backwards.last)
        let long = Tokens.range(
            month: nil,
            from: TeamClock.date(1),
            to: TeamClock.date(1, month: 11) + 86_400 * 800,
            today: TeamClock.date(1),
            calendar: calendar
        )
        #expect(Tokens.days(from: long.first, through: long.last, calendar: calendar).count == Tokens.maximumDays)
    }

    @Test("Weekends come from the set or from the calendar, and holidays are shaded")
    func shading() {
        let holidays = Tokens.holidayNames(
            [MonarchTeamCalendarHoliday("Thanksgiving", date: TeamClock.date(26, 18))],
            calendar: calendar
        )
        #expect(holidays[TeamClock.date(26)] == "Thanksgiving")
        // November 1, 2026 is a Sunday; the 7th a Saturday; the 2nd a Monday.
        #expect(Tokens.isShaded(TeamClock.date(1), weekendDays: [1, 7], holidays: [:], calendar: calendar))
        #expect(Tokens.isShaded(TeamClock.date(7), weekendDays: nil, holidays: [:], calendar: calendar))
        #expect(!Tokens.isShaded(TeamClock.date(2), weekendDays: nil, holidays: [:], calendar: calendar))
        #expect(Tokens.isShaded(TeamClock.date(6), weekendDays: [6], holidays: [:], calendar: calendar))
        #expect(!Tokens.isShaded(TeamClock.date(7), weekendDays: [6], holidays: [:], calendar: calendar))
        #expect(Tokens.isShaded(TeamClock.date(26), weekendDays: [], holidays: holidays, calendar: calendar))
    }

    @Test("Cells follow entries; a later entry wins, per-day portions apply, an end before the start is the start")
    func cells() {
        let entries = [
            MonarchTeamCalendarEntry("Leave", id: "a", memberID: "m", start: TeamClock.date(2), end: TeamClock.date(4)),
            MonarchTeamCalendarEntry(
                "Sick",
                id: "b",
                memberID: "m",
                start: TeamClock.date(4, 10),
                portions: [TeamClock.date(4): .secondHalf]
            ),
            MonarchTeamCalendarEntry("Odd", id: "c", memberID: "m", start: TeamClock.date(10), end: TeamClock.date(8)),
        ]
        let cells = Tokens.cells(entries, days: november, calendar: calendar)
        #expect(cells.count == 30)
        #expect(cells[0] == nil)
        #expect(cells[1]?.entry.id == "a")
        #expect(cells[2]?.entry.id == "a")
        #expect(cells[3]?.entry.id == "b")
        #expect(cells[3]?.portion == .secondHalf)
        #expect(cells[9]?.entry.id == "c")
        #expect(cells[8] == nil && cells[10] == nil)
        #expect(Tokens.lastDay(of: entries[2], calendar: calendar) == TeamClock.date(10))
    }

    @Test("Whole days of one entry join into one bar; half days stand alone; cut bars lose their edge")
    func segments() {
        let entries = [
            MonarchTeamCalendarEntry(
                "Trip",
                id: "t",
                memberID: "m",
                start: TeamClock.date(25, month: 10),
                end: TeamClock.date(3)
            ),
            MonarchTeamCalendarEntry(
                "Halves",
                id: "h",
                memberID: "m",
                start: TeamClock.date(10),
                end: TeamClock.date(11),
                portion: .firstHalf
            ),
            MonarchTeamCalendarEntry(
                "Week",
                id: "w",
                memberID: "m",
                start: TeamClock.date(28),
                end: TeamClock.date(4, month: 12)
            ),
        ]
        let segments = Tokens.segments(
            Tokens.cells(entries, days: november, calendar: calendar),
            days: november,
            calendar: calendar
        )
        #expect(segments.map(\.first) == [0, 9, 10, 27])
        #expect(segments.map(\.count) == [3, 1, 1, 3])
        #expect(segments[0].startsEntry == false && segments[0].endsEntry == true)
        #expect(segments[1].startsEntry && !segments[1].endsEntry)
        #expect(!segments[2].startsEntry && segments[2].endsEntry)
        #expect(segments[3].startsEntry && !segments[3].endsEntry)
        let cut = Tokens.corners(segments[0])
        #expect(cut.leading == 0)
        #expect(cut.trailing == Tokens.barRadius)
        let half = Tokens.corners(segments[1])
        #expect(half.leading == Tokens.barRadius)
        #expect(half.trailing == Tokens.barRadius)
        #expect(Tokens.showsLabel(segments[0]))
        #expect(!Tokens.showsLabel(segments[1]))
        #expect(Tokens.segments([], days: [], calendar: calendar).isEmpty)
    }

    @Test("The model ignores entries of unknown members and finds today and the scroll target")
    func model() {
        let model = MonarchTeamCalendarModel.make(
            members: teamMembers,
            entries: teamEntries,
            month: TeamClock.date(15),
            from: nil,
            to: nil,
            holidays: [],
            weekendDays: nil,
            today: TeamClock.date(3, 15),
            formats: formats
        )
        #expect(model.days.count == 30)
        #expect(model.cells["nobody"] == nil)
        #expect(model.segments["alex"]?.map(\.count) == [4])
        #expect(model.segments["priya"]?.map(\.count) == [2, 1])
        #expect(model.segments["priya"]?.last?.portion == .secondHalf)
        #expect(model.todayIndex == 2)
        #expect(model.scrollTarget == 1)
        #expect(model.rangeName == "November 2026")
        #expect(Tokens.scrollTarget(today: TeamClock.date(1), days: november, calendar: calendar) == nil)
        #expect(Tokens.scrollTarget(today: TeamClock.date(1, month: 12), days: november, calendar: calendar) == nil)
    }

    @Test("Bars: solid in the entry colour, or faded when pending")
    func barAppearance() {
        let solid = Tokens.barAppearance(isPending: false)
        #expect(solid.base == \MonarchColorTokens.bgSurface)
        #expect(solid.tint == Tokens.barTintRatio)
        #expect(!solid.hasStripe && !solid.isOutlined)
        #expect(solid.content == \MonarchColorTokens.accentOnSolid)
        let pending = Tokens.barAppearance(isPending: true)
        #expect(pending.tint == Tokens.pendingTintRatio)
        #expect(pending.tint < solid.tint)
        #expect(!pending.hasStripe && !pending.isOutlined)
        #expect(pending.content == \MonarchColorTokens.textDefault)
        #expect(Tokens.defaultBarColor == \MonarchColorTokens.textMuted)
    }

    @Test("Today is ringed in the accent; shaded columns use the muted fill")
    func headerAndColumns() {
        #expect(
            Tokens.headerAppearance(isToday: true)
                == MonarchTeamCalendarHeaderAppearance(text: \.accentStrong, ring: \.accentBorder)
        )
        #expect(
            Tokens.headerAppearance(isToday: false) == MonarchTeamCalendarHeaderAppearance(text: \.textMuted, ring: nil)
        )
        #expect(Tokens.columnColor(isShaded: true) == \MonarchColorTokens.bgMuted)
        #expect(Tokens.columnColor(isShaded: false) == nil)
        #expect(Tokens.surfaceColor == \MonarchColorTokens.bgSurface)
        #expect(Tokens.dividerColor == \MonarchColorTokens.borderSubtle)
    }

    @Test("The people column keeps its width but never more than a share of the calendar")
    func personWidth() {
        #expect(Tokens.personColumnWidth(scaled: 120, available: 400) == 120)
        #expect(Tokens.personColumnWidth(scaled: 300, available: 400) == 400 * Tokens.personMaximumShare)
        #expect(Tokens.personColumnWidth(scaled: 120, available: 0) == 120)
        #expect(Tokens.dayWidth(scaled: 44, dayCount: 14, available: 248) == 248.0 / 14)
        #expect(Tokens.dayWidth(scaled: 44, dayCount: 3, available: 248) == 44)
        #expect(Tokens.dayWidth(scaled: 44, dayCount: 31, available: 100) == Tokens.minimumDayWidth)
        #expect(Tokens.dayWidth(scaled: 44, dayCount: 14, available: 0) == 44)
        #expect(Tokens.cellSize >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("Loading comes first, then the error, then the empty state")
    func phase() {
        #expect(Tokens.phase(isLoading: true, errorMessage: "x", memberCount: 0) == .loading)
        #expect(Tokens.phase(isLoading: false, errorMessage: "x", memberCount: 3) == .failed("x"))
        #expect(Tokens.phase(isLoading: false, errorMessage: nil, memberCount: 0) == .empty)
        #expect(Tokens.phase(isLoading: false, errorMessage: nil, memberCount: 1) == .grid)
    }

    @Test("Day headers read Today, the date and the holiday")
    func dayHeaderLabels() {
        let labels = MonarchTeamCalendarLabels()
        #expect(
            Tokens.dayHeaderLabel(date: "Tuesday, November 3", isToday: true, holiday: nil, labels: labels)
                == "Today, Tuesday, November 3"
        )
        #expect(
            Tokens.dayHeaderLabel(
                date: "Thursday, November 26",
                isToday: false,
                holiday: "Thanksgiving",
                labels: labels
            )
                == "Thursday, November 26, Holiday: Thanksgiving"
        )
        let custom = MonarchTeamCalendarLabels(today: "Now", holiday: { "Closed for \($0)" })
        #expect(Tokens.dayHeaderLabel(date: "D", isToday: true, holiday: "H", labels: custom) == "Now, D, Closed for H")
    }

    @Test("Bars read the label, the person, the days, the half and pending")
    func barLabels() {
        let labels = MonarchTeamCalendarLabels()
        let leave = MonarchTeamCalendarSegment(
            entry: teamEntries[0],
            first: 1,
            count: 4,
            portion: .full,
            startsEntry: true,
            endsEntry: true
        )
        let dates = Tokens.dates(first: "Monday, November 2", last: "Thursday, November 5", count: 4, labels: labels)
        #expect(
            Tokens.barLabel(leave, member: "Alex Kim", dates: dates, labels: labels)
                == "Annual leave, Alex Kim, Monday, November 2 to Thursday, November 5"
        )
        let doctor = MonarchTeamCalendarSegment(
            entry: teamEntries[1],
            first: 2,
            count: 1,
            portion: .firstHalf,
            startsEntry: true,
            endsEntry: true
        )
        let single = Tokens.dates(first: "Tuesday, November 3", last: "Tuesday, November 3", count: 1, labels: labels)
        #expect(
            Tokens.barLabel(doctor, member: "Sam Lee", dates: single, labels: labels)
                == "Doctor, Sam Lee, Tuesday, November 3, first half, pending"
        )
        let custom = MonarchTeamCalendarLabels(
            range: { "\($0) until \($1)" },
            firstHalf: "morning",
            pending: "awaiting approval"
        )
        #expect(Tokens.dates(first: "A", last: "B", count: 2, labels: custom) == "A until B")
        #expect(
            Tokens.barLabel(doctor, member: "S", dates: "D", labels: custom)
                == "Doctor, S, D, morning, awaiting approval"
        )
        #expect(Tokens.portionText(.secondHalf, labels: labels) == "second half")
        #expect(Tokens.portionText(.full, labels: labels) == nil)
    }

    @Test("People, empty days and the grid name")
    func otherLabels() {
        #expect(Tokens.personLabel(name: "Alex Kim", description: "Design") == "Alex Kim, Design")
        #expect(Tokens.personLabel(name: "Priya Nair", description: nil) == "Priya Nair")
        #expect(Tokens.personLabel(name: "Priya Nair", description: "  ") == "Priya Nair")
        #expect(Tokens.cellLabel(member: "Sam Lee", date: "Wednesday, November 4") == "Sam Lee, Wednesday, November 4")
        #expect(Tokens.gridName("November 2026", labels: MonarchTeamCalendarLabels()) == "Team calendar, November 2026")
        #expect(Tokens.gridName("N", labels: MonarchTeamCalendarLabels(grid: { "Leave in \($0)" })) == "Leave in N")
    }

    @Test("Built-in strings come from the table and can be replaced")
    func strings() {
        let labels = MonarchTeamCalendarLabels()
        #expect(Tokens.personHeader(labels) == "Person")
        #expect(Tokens.emptyTitle(labels) == "Nobody to show")
        #expect(Tokens.emptyMessage(labels) == "People and their time away appear here.")
        #expect(Tokens.loadingLabel(labels) == "Loading team calendar")
        #expect(
            Tokens.legend(labels, showsLegend: true)?.hasPrefix("Shaded columns are weekends and holidays.") == true
        )
        #expect(Tokens.legend(labels, showsLegend: false) == nil)
        #expect(MonarchTeamCalendarStrings.today == "Today")
        #expect(MonarchTeamCalendarStrings.holiday("X") == "Holiday: X")
        let custom = MonarchTeamCalendarLabels(
            person: "Who",
            emptyTitle: "Empty",
            emptyMessage: "None",
            legend: "L",
            loading: "Wait"
        )
        #expect(Tokens.personHeader(custom) == "Who")
        #expect(Tokens.emptyTitle(custom) == "Empty")
        #expect(Tokens.emptyMessage(custom) == "None")
        #expect(Tokens.legend(custom, showsLegend: true) == "L")
        #expect(Tokens.loadingLabel(custom) == "Wait")
    }

    @Test("Dates are named in the given locale and the calendar's time zone")
    func formatting() {
        #expect(formats.month(TeamClock.date(2)) == "November 2026")
        #expect(formats.day(TeamClock.date(2, 23)) == "Monday, November 2")
        #expect(formats.number(TeamClock.date(2)) == "2")
        #expect(formats.narrowWeekday(TeamClock.date(2)) == "M")
        let range = formats.range(TeamClock.date(2), TeamClock.date(8))
        #expect(range.contains("Nov") && range.contains("2026"))
        #expect(
            formats.rangeName(first: TeamClock.date(1), last: TeamClock.date(30), isCustom: false) == "November 2026"
        )
        #expect(formats.range(TeamClock.date(2), TeamClock.date(2)).contains("2026"))
    }
}
