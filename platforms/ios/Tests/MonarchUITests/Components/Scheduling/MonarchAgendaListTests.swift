import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// A fixed calendar and dates in it, so grouping does not depend on the machine.
private enum AgendaClock {
    static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        return calendar
    }()

    static func date(_ day: Int, _ hour: Int = 0, _ minute: Int = 0) -> Date {
        let components = DateComponents(year: 2026, month: 11, day: day, hour: hour, minute: minute)
        return calendar.date(from: components) ?? Date(timeIntervalSince1970: 0)
    }
}

private let agendaEvents = [
    MonarchAgendaEvent(
        "Design review",
        id: "review",
        start: AgendaClock.date(4, 14),
        end: AgendaClock.date(4, 15),
        detail: "Room 4"
    ),
    MonarchAgendaEvent("Stand-up", id: "standup", start: AgendaClock.date(4, 9, 30), end: AgendaClock.date(4, 9, 45)),
    MonarchAgendaEvent(
        "Offsite",
        id: "offsite",
        start: AgendaClock.date(4),
        end: AgendaClock.date(5),
        isAllDay: true,
        status: .tentative
    ),
    MonarchAgendaEvent("Release night", id: "release", start: AgendaClock.date(5, 22), end: AgendaClock.date(7, 2)),
    MonarchAgendaEvent("Cancelled call", id: "call", start: AgendaClock.date(8, 10), status: .cancelled),
]

enum MonarchAgendaListFixtures: ComponentFixtures {
    static let id = "agenda-list"
    static let fixtures = [
        Fixture("days with a Today marker, tappable") {
            MonarchAgendaList(agendaEvents, today: AgendaClock.date(4), calendar: AgendaClock.calendar) { _ in }
                .frame(height: MonarchTokens.Space.space24 * 4)
        },
        Fixture("empty days and notes, not tappable") {
            MonarchAgendaList(
                agendaEvents,
                from: AgendaClock.date(3),
                days: 7,
                today: nil,
                showsEmptyDays: true,
                calendar: AgendaClock.calendar,
                dayNotes: [AgendaClock.date(3, 8): ["Ada is off"], AgendaClock.date(4, 9): ["Grace is off"]]
            )
            .frame(height: MonarchTokens.Space.space24 * 4)
        },
        Fixture("nothing scheduled") {
            MonarchAgendaList([], calendar: AgendaClock.calendar)
                .frame(height: MonarchTokens.Space.space24 * 3)
        },
        Fixture("custom labels and long titles") {
            MonarchAgendaList(
                [
                    MonarchAgendaEvent(
                        "Quarterly planning with the whole northern region sales team",
                        id: "1",
                        start: AgendaClock.date(4, 9),
                        end: AgendaClock.date(4, 17),
                        detail: "The large meeting room on the third floor"
                    )
                ],
                today: AgendaClock.date(4),
                calendar: AgendaClock.calendar,
                labels: MonarchAgendaListLabels(today: "Now", allDay: "Whole day", count: { "\($0) item(s)" })
            )
            .frame(height: MonarchTokens.Space.space24 * 3)
        },
    ]
}

@Suite("MonarchAgendaList")
struct MonarchAgendaListTests {
    private let calendar = AgendaClock.calendar

    private func days(
        _ events: [MonarchAgendaEvent] = agendaEvents,
        from: Date? = nil,
        days: Int? = nil,
        showsEmptyDays: Bool = false,
        notes: @escaping (Date) -> [String] = { _ in [] }
    ) -> [MonarchAgendaDay] {
        MonarchAgendaListTokens.days(
            events: events,
            from: from,
            days: days,
            showsEmptyDays: showsEmptyDays,
            calendar: calendar,
            notes: notes
        )
    }

    @Test("Events are grouped by day, all-day first, then by start time")
    func grouping() {
        let agenda = days()
        #expect(agenda.map(\.day) == [4, 5, 6, 7, 8].map { AgendaClock.date($0) })
        #expect(agenda[0].events.map(\.id) == ["offsite", "standup", "review"])
        #expect(agenda[1].events.map(\.id) == ["offsite", "release"])
        #expect(agenda[2].events.map(\.id) == ["release"])
        #expect(agenda[3].events.map(\.id) == ["release"])
        #expect(agenda[4].events.map(\.id) == ["call"])
    }

    @Test("An event ending exactly at midnight does not touch the next day")
    func midnight() {
        let event = MonarchAgendaEvent("Late", id: "late", start: AgendaClock.date(4, 22), end: AgendaClock.date(5))
        let span = MonarchAgendaListTokens.span(of: event, calendar: calendar)
        #expect(span.first == AgendaClock.date(4))
        #expect(span.last == AgendaClock.date(4))
    }

    @Test("An all-day event covers its last day, and an end before the start is ignored")
    func allDaySpan() {
        let event = MonarchAgendaEvent(
            "Trip",
            id: "trip",
            start: AgendaClock.date(4),
            end: AgendaClock.date(6),
            isAllDay: true
        )
        #expect(MonarchAgendaListTokens.span(of: event, calendar: calendar).last == AgendaClock.date(6))
        let backwards = MonarchAgendaEvent("Odd", id: "odd", start: AgendaClock.date(6, 10), end: AgendaClock.date(4))
        let span = MonarchAgendaListTokens.span(of: backwards, calendar: calendar)
        #expect(span.first == AgendaClock.date(6))
        #expect(span.last == AgendaClock.date(6))
    }

    @Test("from and days set the range; running events appear on the first day")
    func range() {
        let agenda = days(from: AgendaClock.date(6), days: 2)
        #expect(agenda.map(\.day) == [AgendaClock.date(6), AgendaClock.date(7)])
        #expect(agenda.allSatisfy { $0.events.map(\.id) == ["release"] })
        #expect(days(from: AgendaClock.date(6), days: 0).isEmpty)
    }

    @Test("Empty days are listed only when asked, and a day with notes is always listed")
    func emptyDays() {
        let all = days(from: AgendaClock.date(3), days: 7, showsEmptyDays: true)
        #expect(all.count == 7)
        #expect(all[0].events.isEmpty)
        #expect(!days(from: AgendaClock.date(3), days: 7).map(\.day).contains(AgendaClock.date(3)))
        let noted = days(from: AgendaClock.date(4), days: 6) { $0 == AgendaClock.date(9) ? ["Holiday"] : [] }
        #expect(noted.last?.day == AgendaClock.date(9))
        #expect(noted.last?.notes == ["Holiday"])
        #expect(noted.last?.events.isEmpty == true)
    }

    @Test("Notes are matched to their day by any time on it")
    func notesByDay() {
        let notes = [
            AgendaClock.date(4, 18): ["Late note"], AgendaClock.date(4, 6): ["Early note"],
            AgendaClock.date(5): ["Other"],
        ]
        #expect(
            MonarchAgendaListTokens.notes(on: AgendaClock.date(4), from: notes, calendar: calendar) == [
                "Early note", "Late note",
            ]
        )
        #expect(MonarchAgendaListTokens.notes(on: AgendaClock.date(6), from: notes, calendar: calendar).isEmpty)
    }

    @Test("Nothing to list gives no days")
    func empty() {
        #expect(days([]).isEmpty)
        #expect(days([], from: AgendaClock.date(4)).isEmpty)
        #expect(days([], from: AgendaClock.date(4), showsEmptyDays: true).count == 1)
    }

    @Test("Times: a range, an end only on the last day, All day on a day covered whole")
    func times() {
        let release = agendaEvents[3]
        let format: (Date) -> String = { date in
            let hour = AgendaClock.calendar.component(.hour, from: date)
            return String(format: "%02d:00", hour)
        }
        let labels = MonarchAgendaListLabels()
        let first = MonarchAgendaListTokens.time(
            of: release,
            on: AgendaClock.date(5),
            calendar: calendar,
            labels: labels,
            format: format
        )
        #expect(first == MonarchAgendaTime(text: "22:00 – 02:00", spoken: "22:00 to 02:00"))
        let middle = MonarchAgendaListTokens.time(
            of: release,
            on: AgendaClock.date(6),
            calendar: calendar,
            labels: labels,
            format: format
        )
        #expect(middle == MonarchAgendaTime(text: "All day", spoken: "All day"))
        let late = MonarchAgendaEvent("Late", id: "l", start: AgendaClock.date(5, 22), end: AgendaClock.date(6, 3))
        let last = MonarchAgendaListTokens.time(
            of: late,
            on: AgendaClock.date(6),
            calendar: calendar,
            labels: labels,
            format: format
        )
        #expect(last == MonarchAgendaTime(text: "– 03:00", spoken: "until 03:00"))
        let moment = MonarchAgendaListTokens.time(
            of: agendaEvents[4],
            on: AgendaClock.date(8),
            calendar: calendar,
            labels: labels,
            format: format
        )
        #expect(moment == MonarchAgendaTime(text: "10:00", spoken: "10:00"))
        let allDay = MonarchAgendaListTokens.time(
            of: agendaEvents[2],
            on: AgendaClock.date(4),
            calendar: calendar,
            labels: MonarchAgendaListLabels(allDay: "Whole day"),
            format: format
        )
        #expect(allDay == MonarchAgendaTime(text: "Whole day", spoken: "Whole day"))
    }

    @Test("Spoken times can be replaced")
    func spokenOverrides() {
        let labels = MonarchAgendaListLabels(until: { "ends \($0)" }, range: { "from \($0) until \($1)" })
        let format: (Date) -> String = { "\(AgendaClock.calendar.component(.hour, from: $0))" }
        let late = MonarchAgendaEvent("Late", id: "l", start: AgendaClock.date(5, 22), end: AgendaClock.date(6, 3))
        #expect(
            MonarchAgendaListTokens.time(
                of: late,
                on: AgendaClock.date(5),
                calendar: calendar,
                labels: labels,
                format: format
            ).spoken
                == "from 22 until 3"
        )
        #expect(
            MonarchAgendaListTokens.time(
                of: late,
                on: AgendaClock.date(6),
                calendar: calendar,
                labels: labels,
                format: format
            ).spoken
                == "ends 3"
        )
    }

    @Test("The count is singular for one event and can be replaced")
    func count() {
        let english = Locale(identifier: "en_US")
        #expect(MonarchAgendaListTokens.count(1, labels: MonarchAgendaListLabels(), locale: english) == "1 event")
        #expect(MonarchAgendaListTokens.count(3, labels: MonarchAgendaListLabels(), locale: english) == "3 events")
        #expect(MonarchAgendaListTokens.count(0, labels: MonarchAgendaListLabels(), locale: english) == "0 events")
        #expect(
            MonarchAgendaListTokens.count(2, labels: MonarchAgendaListLabels(count: { "\($0) items" }), locale: english)
                == "2 items"
        )
    }

    @Test("A day heading reads the date, Today when it is today, and the count")
    func heading() {
        let labels = MonarchAgendaListLabels()
        #expect(
            MonarchAgendaListTokens.headingLabel(
                date: "Wednesday 4 November",
                isToday: true,
                count: "3 events",
                labels: labels
            )
                == "Wednesday 4 November, Today, 3 events"
        )
        #expect(
            MonarchAgendaListTokens.headingLabel(
                date: "Thursday 5 November",
                isToday: false,
                count: "1 event",
                labels: labels
            )
                == "Thursday 5 November, 1 event"
        )
        #expect(
            MonarchAgendaListTokens.headingLabel(
                date: "D",
                isToday: true,
                count: "C",
                labels: MonarchAgendaListLabels(today: "Now")
            ) == "D, Now, C"
        )
    }

    @Test("Built-in strings come from the table")
    func strings() {
        #expect(MonarchAgendaListTokens.emptyTitle(MonarchAgendaListLabels()) == "Nothing scheduled")
        #expect(MonarchAgendaListTokens.emptyTitle(MonarchAgendaListLabels(emptyTitle: "Free")) == "Free")
        #expect(MonarchAgendaListStrings.today == "Today")
        #expect(MonarchAgendaListStrings.allDay == "All day")
    }

    @Test("Headings and notes use theme roles; Today is a word as well as a colour")
    func roles() {
        #expect(MonarchAgendaListTokens.headingColor == \MonarchColorTokens.textDefault)
        #expect(MonarchAgendaListTokens.secondaryColor == \MonarchColorTokens.textMuted)
        #expect(MonarchAgendaListTokens.todayTone == .accent)
    }
}
