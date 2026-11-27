import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCalendarMonthViewFixtures: ComponentFixtures {
    static let id = "calendar-month-view"

    private static var calendar: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        calendar.locale = Locale(identifier: "en_US")
        return calendar
    }

    // Saturday, November 14, 2026, at midnight UTC.
    private static let today = Date(timeIntervalSince1970: 1_794_614_400)
    private static let hour: TimeInterval = 3_600
    private static let events = [
        MonarchAgendaEvent(
            "Stand-up",
            id: "1",
            start: today.addingTimeInterval(9 * hour),
            end: today.addingTimeInterval(9.5 * hour)
        ),
        MonarchAgendaEvent("Design review", id: "2", start: today.addingTimeInterval(14 * hour), status: .tentative),
        MonarchAgendaEvent(
            "Lunch with a very long title that cannot fit",
            id: "3",
            start: today.addingTimeInterval(12 * hour)
        ),
        MonarchAgendaEvent("Cancelled call", id: "4", start: today.addingTimeInterval(16 * hour), status: .cancelled),
        MonarchAgendaEvent(
            "Company offsite",
            id: "5",
            start: today.addingTimeInterval(3 * 86_400),
            end: today.addingTimeInterval(5 * 86_400),
            isAllDay: true
        ),
    ]
    private static let wide = MonarchTokens.Space.space24 * 8

    static let fixtures = [
        Fixture("automatic on a phone: dots") {
            MonarchCalendarMonthView(month: .constant(today), events: events, selection: .constant(today), today: today)
                .environment(\.calendar, calendar)
        },
        Fixture("events with overflow and day states") {
            MonarchCalendarMonthView(
                month: .constant(today),
                events: events,
                today: today,
                layout: .events,
                dayState: { day in
                    calendar.component(.day, from: day) == 25
                        ? MonarchCalendarMonthViewDayState(isUnavailable: true, badge: "Holiday") : nil
                },
                onMoveEvent: { _, _ in },
                onSelectEvent: { _ in }
            )
            .environment(\.calendar, calendar)
        },
        Fixture("events, wide") {
            MonarchCalendarMonthView(month: .constant(today), events: events, today: today, layout: .events)
                .environment(\.calendar, calendar)
                .frame(width: wide)
        },
        Fixture("empty month with flexible weeks") {
            MonarchCalendarMonthView(month: .constant(today), today: today, isFixedWeeks: false, layout: .dots)
                .environment(\.calendar, calendar)
        },
    ]
}

@Suite("MonarchCalendarMonthView")
@MainActor
struct MonarchCalendarMonthViewTests {
    private let us = Locale(identifier: "en_US")

    private func calendar(firstWeekday: Int = 1) -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        calendar.locale = us
        calendar.firstWeekday = firstWeekday
        return calendar
    }

    private func day(_ year: Int, _ month: Int, _ day: Int, hour: Int = 0) -> Date {
        calendar().date(from: DateComponents(year: year, month: month, day: day, hour: hour)) ?? .distantPast
    }

    @Test("Moving an event has VoiceOver actions for a day and a week, and they land on the same days a drag does")
    func moveActions() {
        let actions = MonarchCalendarMonthViewTokens.moveActions(labels: MonarchCalendarMonthViewLabels())
        #expect(actions.map(\.days) == [-1, 1, -7, 7])
        #expect(actions.map(\.name).first == "Move one day earlier")
        #expect(Set(actions.map(\.name)).count == 4)
        let start = day(2026, 11, 30)
        let landed = actions.compactMap {
            MonarchCalendarMonthViewTokens.movedDay(from: start, days: $0.days, calendar: calendar())
        }
        #expect(landed == [day(2026, 11, 29), day(2026, 12, 1), day(2026, 11, 23), day(2026, 12, 7)])
        let custom = MonarchCalendarMonthViewTokens.moveActions(
            labels: MonarchCalendarMonthViewLabels(moveDayLater: "Next day")
        )
        #expect(custom[1].name == "Next day")
    }

    @Test("Days are whole weeks from the calendar's first weekday")
    func days() {
        let sunday = MonarchCalendarMonthViewTokens.days(
            month: day(2026, 11, 14),
            calendar: calendar(),
            isFixedWeeks: true
        )
        #expect(sunday.count == 42)
        #expect(sunday.first == day(2026, 11, 1), "November 2026 starts on a Sunday")
        let monday = MonarchCalendarMonthViewTokens.days(
            month: day(2026, 11, 14),
            calendar: calendar(firstWeekday: 2),
            isFixedWeeks: false
        )
        #expect(monday.first == day(2026, 10, 26))
        #expect(monday.count == 42, "November 2026 needs six Monday weeks")
        let february = MonarchCalendarMonthViewTokens.days(
            month: day(2026, 2, 10),
            calendar: calendar(),
            isFixedWeeks: false
        )
        #expect(february.count == 28, "February 2026 starts on a Sunday and fills four weeks")
        #expect(
            MonarchCalendarMonthViewTokens.isOutside(day(2026, 10, 31), month: day(2026, 11, 1), calendar: calendar())
        )
        #expect(
            !MonarchCalendarMonthViewTokens.isOutside(day(2026, 11, 30), month: day(2026, 11, 1), calendar: calendar())
        )
    }

    @Test("Months around an anchor start on the first of the month")
    func months() {
        let months = MonarchCalendarMonthViewTokens.months(around: day(2026, 11, 14), radius: 2, calendar: calendar())
        #expect(months == [day(2026, 9, 1), day(2026, 10, 1), day(2026, 11, 1), day(2026, 12, 1), day(2027, 1, 1)])
    }

    @Test("Events land on every day they touch, all-day first, then by time")
    func eventsByDay() {
        let cal = calendar()
        let days = MonarchCalendarMonthViewTokens.days(month: day(2026, 11, 1), calendar: cal, isFixedWeeks: true)
        let events = [
            MonarchAgendaEvent("Late", id: "late", start: day(2026, 11, 14, hour: 15)),
            MonarchAgendaEvent("Early", id: "early", start: day(2026, 11, 14, hour: 9)),
            MonarchAgendaEvent("Offsite", id: "off", start: day(2026, 11, 13), end: day(2026, 11, 15), isAllDay: true),
            MonarchAgendaEvent("Overnight", id: "night", start: day(2026, 11, 14, hour: 22), end: day(2026, 11, 15)),
            MonarchAgendaEvent("Long ago", id: "old", start: day(2025, 1, 1)),
            MonarchAgendaEvent("Spill", id: "spill", start: day(2026, 10, 20), end: day(2026, 10, 27), isAllDay: true),
        ]
        let map = MonarchCalendarMonthViewTokens.eventsByDay(events, days: days, calendar: cal)
        #expect(map[day(2026, 11, 14)]?.map(\.id) == ["off", "early", "late", "night"])
        #expect(map[day(2026, 11, 13)]?.map(\.id) == ["off"])
        #expect(map[day(2026, 11, 15)]?.map(\.id) == ["off"], "an event ending at midnight does not touch that day")
        #expect(map[day(2026, 10, 25)] == nil, "the grid starts on November 1")
        #expect(map.values.flatMap { $0 }.allSatisfy { $0.id != "old" })
        #expect(map[day(2026, 11, 1)] == nil)
    }

    private func pair(_ value: (shown: Int, hidden: Int)) -> [Int] { [value.shown, value.hidden] }

    @Test("A day lists events up to the limit, the overflow button taking the last row")
    func visibleCount() {
        #expect(pair(MonarchCalendarMonthViewTokens.visibleCount(2, maxEvents: 3)) == [2, 0])
        #expect(pair(MonarchCalendarMonthViewTokens.visibleCount(3, maxEvents: 3)) == [3, 0])
        #expect(pair(MonarchCalendarMonthViewTokens.visibleCount(5, maxEvents: 3)) == [2, 3])
        #expect(pair(MonarchCalendarMonthViewTokens.visibleCount(2, maxEvents: 0)) == [0, 2])
        #expect(pair(MonarchCalendarMonthViewTokens.visibleCount(0, maxEvents: 3)) == [0, 0])
    }

    @Test("The automatic layout shows chips only on wide days at standard text sizes")
    func layout() {
        #expect(!MonarchCalendarMonthViewTokens.showsEvents(.automatic, cellWidth: 390 / 7, isAccessibilitySize: false))
        #expect(MonarchCalendarMonthViewTokens.showsEvents(.automatic, cellWidth: 120, isAccessibilitySize: false))
        #expect(!MonarchCalendarMonthViewTokens.showsEvents(.automatic, cellWidth: 120, isAccessibilitySize: true))
        #expect(MonarchCalendarMonthViewTokens.showsEvents(.events, cellWidth: 10, isAccessibilitySize: true))
        #expect(!MonarchCalendarMonthViewTokens.showsEvents(.dots, cellWidth: 500, isAccessibilitySize: false))
    }

    @Test("Spoken names, weekday header and chip times follow the calendar and locale")
    func text() {
        let cal = calendar()
        let date = MonarchCalendarMonthViewTokens.longDate(day(2026, 11, 4), calendar: cal, locale: us)
        #expect(date == "Wednesday, November 4, 2026")
        let none = MonarchCalendarMonthViewLabels()
        #expect(
            MonarchCalendarMonthViewTokens.dayLabel(
                date: date,
                count: 2,
                isToday: true,
                badge: "Holiday",
                locale: us,
                labels: none
            )
                == "Wednesday, November 4, 2026, 2 events, today, Holiday"
        )
        #expect(
            MonarchCalendarMonthViewTokens.dayLabel(
                date: "D",
                count: 1,
                isToday: false,
                badge: nil,
                locale: us,
                labels: none
            ) == "D, 1 event"
        )
        #expect(
            MonarchCalendarMonthViewTokens.dayLabel(
                date: "D",
                count: 0,
                isToday: false,
                badge: "",
                locale: us,
                labels: none
            ) == "D, 0 events"
        )
        let custom = MonarchCalendarMonthViewLabels(more: { "\($0) others" }, day: { "\($0) (\($1))" }, today: "now")
        #expect(
            MonarchCalendarMonthViewTokens.dayLabel(
                date: "D",
                count: 3,
                isToday: true,
                badge: nil,
                locale: us,
                labels: custom
            ) == "D (3), now"
        )
        #expect(MonarchCalendarMonthViewTokens.more(2, locale: us, labels: none) == "+2 more")
        #expect(MonarchCalendarMonthViewTokens.more(2, locale: us, labels: custom) == "2 others")
        #expect(
            MonarchCalendarMonthViewTokens.weekdays(calendar: cal).map(\.short) == [
                "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT",
            ]
        )
        #expect(MonarchCalendarMonthViewTokens.weekdays(calendar: calendar(firstWeekday: 2)).first?.full == "Monday")
        #expect(MonarchCalendarMonthViewTokens.dayNumber(day(2026, 11, 4), calendar: cal, locale: us) == "4")
        #expect(
            MonarchCalendarMonthViewTokens.monthTitle(day(2026, 11, 4), calendar: cal, locale: us) == "November 2026"
        )
        let timed = MonarchAgendaEvent("T", id: "t", start: day(2026, 11, 4, hour: 9))
        #expect(
            MonarchCalendarMonthViewTokens.chipTime(timed, on: day(2026, 11, 4), calendar: cal, locale: us)
                == "9:00\u{202F}AM"
        )
        #expect(MonarchCalendarMonthViewTokens.chipTime(timed, on: day(2026, 11, 5), calendar: cal, locale: us) == nil)
        let allDay = MonarchAgendaEvent("A", id: "a", start: day(2026, 11, 4), isAllDay: true)
        #expect(MonarchCalendarMonthViewTokens.chipTime(allDay, on: day(2026, 11, 4), calendar: cal, locale: us) == nil)
    }

    @Test("Colours by state")
    func colors() {
        #expect(
            MonarchCalendarMonthViewTokens.dateForeground(isToday: true, isOutside: true, isUnavailable: false)
                == \.accentOnSolid
        )
        #expect(
            MonarchCalendarMonthViewTokens.dateForeground(isToday: false, isOutside: true, isUnavailable: false)
                == \.textSubtle
        )
        #expect(
            MonarchCalendarMonthViewTokens.dateForeground(isToday: false, isOutside: false, isUnavailable: true)
                == \.textSubtle
        )
        #expect(
            MonarchCalendarMonthViewTokens.dateForeground(isToday: false, isOutside: false, isUnavailable: false)
                == \.textDefault
        )
        #expect(
            MonarchCalendarMonthViewTokens.cellBackground(isSelected: true, isTargeted: false, isUnavailable: true)
                == \.bgEmphasis
        )
        #expect(
            MonarchCalendarMonthViewTokens.cellBackground(isSelected: false, isTargeted: true, isUnavailable: false)
                == \.accentSubtle
        )
        #expect(
            MonarchCalendarMonthViewTokens.cellBackground(isSelected: false, isTargeted: false, isUnavailable: true)
                == \.bgMuted
        )
        #expect(
            MonarchCalendarMonthViewTokens.cellBackground(isSelected: false, isTargeted: false, isUnavailable: false)
                == nil
        )
        #expect(MonarchCalendarMonthViewTokens.dotRole(.cancelled) == \.textDisabled)
        #expect(MonarchCalendarMonthViewTokens.dotRole(.confirmed) == \.accentSolid)
    }

    @Test("Built-in strings resolve")
    func strings() {
        for string in [
            MonarchCalendarMonthViewStrings.today, MonarchCalendarMonthViewStrings.countOne,
            MonarchCalendarMonthViewStrings.countOther("2"), MonarchCalendarMonthViewStrings.more("2"),
        ] {
            #expect(!string.hasPrefix("monarch."))
        }
    }
}
