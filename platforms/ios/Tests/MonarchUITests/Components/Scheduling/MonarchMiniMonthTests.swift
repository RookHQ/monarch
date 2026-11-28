import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// A fixed calendar and fixed days, so the tests do not depend on the
/// machine's time zone or on today's date.
private enum Days {
    static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Europe/Amsterdam") ?? .gmt
        calendar.firstWeekday = 2
        return calendar
    }()

    /// The Hebrew calendar in the same zone, for the fixture of another calendar.
    static let hebrew: Calendar = {
        var calendar = Calendar(identifier: .hebrew)
        calendar.timeZone = Days.calendar.timeZone
        return calendar
    }()

    static func date(_ year: Int, _ month: Int, _ day: Int, hour: Int = 0) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day, hour: hour)) ?? .distantPast
    }

    static let november4 = date(2026, 11, 4)
    static let november13 = date(2026, 11, 13)
    static let november30 = date(2026, 11, 30)
}

enum MonarchMiniMonthFixtures: ComponentFixtures {
    static let id = "mini-month"
    static let fixtures = [
        Fixture("no selection") {
            MonarchMiniMonth(selection: .constant(nil), visibleMonth: .constant(Days.november13))
                .environment(\.calendar, Days.calendar)
        },
        Fixture("selected day") {
            MonarchMiniMonth(selection: .constant(Days.november13), visibleMonth: .constant(Days.november13))
                .environment(\.calendar, Days.calendar)
        },
        Fixture("bounds") {
            MonarchMiniMonth(
                selection: .constant(Days.november13),
                minimumDate: Days.november4,
                maximumDate: Days.november30
            )
            .environment(\.calendar, Days.calendar)
        },
        Fixture("selection outside the bounds") {
            MonarchMiniMonth(
                selection: .constant(Days.date(2027, 3, 1)),
                visibleMonth: .constant(Days.date(2020, 1, 1)),
                minimumDate: Days.november4,
                maximumDate: Days.november30
            )
            .environment(\.calendar, Days.calendar)
        },
        Fixture("latest day before earliest day") {
            MonarchMiniMonth(
                selection: .constant(nil),
                minimumDate: Days.november30,
                maximumDate: Days.november4
            )
            .environment(\.calendar, Days.calendar)
        },
        Fixture("disabled weekends and dots") {
            MonarchMiniMonth(
                selection: .constant(Days.november13),
                visibleMonth: .constant(Days.november13),
                isDateDisabled: { Days.calendar.isDateInWeekend($0) },
                dayInfo: { date in
                    switch Days.calendar.component(.day, from: date) % 6 {
                    case 0: MonarchMiniMonthDayInfo(dotCount: 1, description: "1 event")
                    case 1: MonarchMiniMonthDayInfo(dots: [.success], description: "Available")
                    case 2: MonarchMiniMonthDayInfo(dots: [.danger, .warning, .info, .neutral], description: "Busy")
                    case 3: MonarchMiniMonthDayInfo(dotCount: 0)
                    default: nil
                    }
                }
            )
            .environment(\.calendar, Days.calendar)
        },
        Fixture("another calendar and locale") {
            MonarchMiniMonth(selection: .constant(Days.november13))
                .environment(\.calendar, Days.hebrew)
                .environment(\.locale, Locale(identifier: "he_IL"))
        },
    ]
}

@Suite("MonarchMiniMonth")
@MainActor
struct MonarchMiniMonthTests {
    private let calendar = Days.calendar

    // MARK: Dots

    @Test("A dot with no tone is the accent; a toned dot is the tone's solid colour")
    func dotColours() {
        #expect(MonarchMiniMonthTokens.dotRole(nil) == \MonarchColorTokens.accentSolid)
        #expect(MonarchMiniMonthTokens.dotRole(.neutral) == \MonarchColorTokens.textSubtle)
        #expect(MonarchMiniMonthTokens.dotRole(.info) == \MonarchColorTokens.infoSolid)
        #expect(MonarchMiniMonthTokens.dotRole(.success) == \MonarchColorTokens.successSolid)
        #expect(MonarchMiniMonthTokens.dotRole(.warning) == \MonarchColorTokens.warningSolid)
        #expect(MonarchMiniMonthTokens.dotRole(.danger) == \MonarchColorTokens.dangerSolid)
    }

    @Test("Every tone has its own dot colour")
    func dotColoursAreDistinct() {
        let roles =
            MonarchTone.allCases.map { MonarchMiniMonthTokens.dotRole($0) } + [MonarchMiniMonthTokens.dotRole(nil)]
        // A dot with no tone is the accent, so nil and .accent share a colour.
        #expect(Set(roles).count == MonarchTone.allCases.count)
        #expect(MonarchMiniMonthTokens.dotRole(nil) == MonarchMiniMonthTokens.dotRole(.accent))
    }

    @Test("At most three dots are drawn, in order")
    func dotLimit() {
        let busy = MonarchMiniMonthDayInfo(dots: [.danger, .warning, .info, .neutral])
        #expect(
            MonarchMiniMonthTokens.dotRoles(busy) == [
                \MonarchColorTokens.dangerSolid, \MonarchColorTokens.warningSolid, \MonarchColorTokens.infoSolid,
            ]
        )
        #expect(MonarchMiniMonthTokens.dotRoles(MonarchMiniMonthDayInfo(dotCount: 7)).count == 3)
        #expect(
            MonarchMiniMonthTokens.dotRoles(MonarchMiniMonthDayInfo(dotCount: 2)) == [
                \MonarchColorTokens.accentSolid, \MonarchColorTokens.accentSolid,
            ]
        )
    }

    @Test("No information, no dots, and a negative count are all no dots")
    func noDots() {
        #expect(MonarchMiniMonthTokens.dotRoles(nil).isEmpty)
        #expect(MonarchMiniMonthTokens.dotRoles(MonarchMiniMonthDayInfo(dots: [])).isEmpty)
        #expect(MonarchMiniMonthTokens.dotRoles(MonarchMiniMonthDayInfo(dotCount: 0)).isEmpty)
        #expect(MonarchMiniMonthTokens.dotRoles(MonarchMiniMonthDayInfo(dotCount: -2)).isEmpty)
    }

    @Test("The description of a day is kept for assistive technology")
    func description() {
        #expect(MonarchMiniMonthDayInfo(dotCount: 3, description: "3 events").description == "3 events")
        #expect(MonarchMiniMonthDayInfo(dots: [.success]).description == nil)
    }

    // MARK: Days

    @Test("A date is reduced to the first moment of its day in the calendar")
    func day() {
        let afternoon = Days.date(2026, 11, 13, hour: 15)
        #expect(MonarchMiniMonthTokens.day(afternoon, calendar: calendar) == Days.november13)
        #expect(MonarchMiniMonthTokens.isSameDay(afternoon, Days.november13, calendar: calendar))
        #expect(!MonarchMiniMonthTokens.isSameDay(afternoon, Days.november4, calendar: calendar))
        #expect(MonarchMiniMonthTokens.isSameDay(nil, nil, calendar: calendar))
        #expect(!MonarchMiniMonthTokens.isSameDay(nil, Days.november4, calendar: calendar))
    }

    @Test("Day components carry the calendar and return the same day")
    func dayComponents() {
        let components = MonarchMiniMonthTokens.dayComponents(Days.date(2026, 11, 13, hour: 15), calendar: calendar)
        #expect(components.year == 2026 && components.month == 11 && components.day == 13)
        #expect(components.calendar == calendar)
        #expect(components.hour == nil)
        #expect(calendar.date(from: components) == Days.november13)
    }

    @Test("Month components have no day")
    func monthComponents() {
        let components = MonarchMiniMonthTokens.monthComponents(Days.november13, calendar: calendar)
        #expect(components.year == 2026 && components.month == 11)
        #expect(components.day == nil)
        #expect(MonarchMiniMonthTokens.isSameMonth(Days.november4, Days.november30, calendar: calendar))
        #expect(!MonarchMiniMonthTokens.isSameMonth(Days.november30, Days.date(2026, 12, 1), calendar: calendar))
        #expect(!MonarchMiniMonthTokens.isSameMonth(Days.november13, Days.date(2027, 11, 13), calendar: calendar))
    }

    @Test("Every day of the month is listed once, including across a clock change")
    func daysOfMonth() {
        let november = MonarchMiniMonthTokens.daysOfMonth(containing: Days.november13, calendar: calendar)
        #expect(november.count == 30)
        #expect(november.first == Days.date(2026, 11, 1))
        #expect(november.last == Days.november30)
        // Clocks go back on 25 October 2026 in this time zone.
        let october = MonarchMiniMonthTokens.daysOfMonth(containing: Days.date(2026, 10, 10), calendar: calendar)
        #expect(october.count == 31)
        #expect(Set(october.map { calendar.component(.day, from: $0) }).count == 31)
        #expect(MonarchMiniMonthTokens.daysOfMonth(containing: Days.date(2028, 2, 3), calendar: calendar).count == 29)
    }

    // MARK: Bounds

    @Test("Without bounds every day is available")
    func unbounded() {
        let range = MonarchMiniMonthTokens.availableRange(minimumDate: nil, maximumDate: nil, calendar: calendar)
        #expect(range.start == .distantPast)
        #expect(range.end == .distantFuture)
        #expect(
            MonarchMiniMonthTokens.isWithinBounds(
                Days.november13,
                minimumDate: nil,
                maximumDate: nil,
                calendar: calendar
            )
        )
    }

    @Test("The earliest and the latest day are included, whatever time the bounds carry")
    func bounds() {
        let minimum = Days.date(2026, 11, 4, hour: 17)
        let maximum = Days.date(2026, 11, 30, hour: 9)
        func isWithin(_ date: Date) -> Bool {
            MonarchMiniMonthTokens.isWithinBounds(
                date,
                minimumDate: minimum,
                maximumDate: maximum,
                calendar: calendar
            )
        }
        #expect(isWithin(Days.november4))
        #expect(isWithin(Days.date(2026, 11, 4, hour: 1)))
        #expect(isWithin(Days.november13))
        #expect(isWithin(Days.date(2026, 11, 30, hour: 23)))
        #expect(!isWithin(Days.date(2026, 11, 3, hour: 23)))
        #expect(!isWithin(Days.date(2026, 12, 1)))

        let range = MonarchMiniMonthTokens.availableRange(
            minimumDate: minimum,
            maximumDate: maximum,
            calendar: calendar
        )
        #expect(range.start == Days.november4)
        #expect(range.end == Days.date(2026, 12, 1).addingTimeInterval(-1))
    }

    @Test("A latest day before the earliest day leaves only the earliest day")
    func invertedBounds() {
        let range = MonarchMiniMonthTokens.availableRange(
            minimumDate: Days.november30,
            maximumDate: Days.november4,
            calendar: calendar
        )
        #expect(range.start == Days.november30)
        #expect(range.end >= range.start)
        #expect(calendar.isDate(range.end, inSameDayAs: Days.november30))
    }

    @Test("A date outside the bounds is moved to the nearest bound")
    func clamped() {
        func clamp(_ date: Date) -> Date {
            MonarchMiniMonthTokens.clamped(
                date,
                minimumDate: Days.november4,
                maximumDate: Days.november30,
                calendar: calendar
            )
        }
        #expect(clamp(Days.november13) == Days.november13)
        #expect(clamp(Days.date(2020, 1, 1)) == Days.november4)
        #expect(calendar.isDate(clamp(Days.date(2027, 3, 1)), inSameDayAs: Days.november30))
    }

    @Test("A day can be chosen when it is inside the bounds and the app does not disable it")
    func selectable() {
        func isSelectable(_ date: Date, disabled: (@MainActor (Date) -> Bool)? = nil) -> Bool {
            MonarchMiniMonthTokens.isSelectable(
                date,
                minimumDate: Days.november4,
                maximumDate: Days.november30,
                isDateDisabled: disabled,
                calendar: calendar
            )
        }
        let weekend: @MainActor (Date) -> Bool = { Days.calendar.isDateInWeekend($0) }
        // 14 November 2026 is a Saturday.
        let saturday = Days.date(2026, 11, 14)
        let weekday = isSelectable(Days.november13)
        let weekdayWithRule = isSelectable(Days.november13, disabled: weekend)
        let saturdayWithRule = isSelectable(saturday, disabled: weekend)
        let saturdayWithoutRule = isSelectable(saturday)
        let beforeBounds = isSelectable(Days.date(2026, 11, 3))
        let afterBounds = isSelectable(Days.date(2026, 12, 5), disabled: { _ in false })
        #expect(weekday)
        #expect(weekdayWithRule)
        #expect(!saturdayWithRule)
        #expect(saturdayWithoutRule)
        #expect(!beforeBounds)
        #expect(!afterBounds)
    }

    @Test("The app's rule is asked about the first moment of the day")
    func disabledRuleReceivesTheDay() {
        var asked: [Date] = []
        _ = MonarchMiniMonthTokens.isSelectable(
            Days.date(2026, 11, 13, hour: 15),
            minimumDate: nil,
            maximumDate: nil,
            isDateDisabled: { date in
                asked.append(date)
                return false
            },
            calendar: calendar
        )
        #expect(asked == [Days.november13])
    }

    #if os(iOS)
        @Test("The system month grid takes the offered width and a height for six weeks of 44-point rows")
        func gridSize() {
            let size = Host.layout(
                MonarchMiniMonth(selection: .constant(Days.november13), visibleMonth: .constant(Days.november13))
                    .environment(\.calendar, Days.calendar)
            )
            #expect(size.width == Host.width)
            #expect(size.height >= MonarchAccessibility.minimumHitTarget * 5)
        }

        /// The calendar with two lines under it, as the slot picker shows it
        /// before a day is chosen.
        @MainActor
        private func stack(_ size: DynamicTypeSize) -> some View {
            VStack(alignment: .leading, spacing: 16) {
                MonarchMiniMonth(selection: .constant(nil), visibleMonth: .constant(Days.november13))
                Text("Choose a day to see the open times.").monarchTextStyle(.caption)
                Text("Times are shown in Coordinated Universal Time.").monarchTextStyle(.caption)
            }
            .environment(\.calendar, Days.calendar)
            .environment(\.locale, Locale(identifier: "en_US"))
            .environment(\.dynamicTypeSize, size)
            .padding()
        }

        // Before, measuring laid the visible UICalendarView out again. In this
        // stack, taller than the 844-point window at the largest text size,
        // SwiftUI measured again on every pass and layout never finished
        // (the slot picker's "no day yet" fixture hung the simulator run).
        // The window clips the stack, so its fitting height is the window's.
        @Test("A calendar in a stack taller than the window lays out, and the same each time")
        @MainActor
        func tallStack() {
            let tall = stack(.accessibility5)
            let first = Host.layout(tall)
            #expect(first.height >= 844)
            #expect(Host.layout(tall) == first)
            let natural = Host.layout(tall.fixedSize(horizontal: false, vertical: true))
            #expect(natural.height > 844, "The stack must be taller than the hosting window")
        }

        @Test("The grid is measured at the environment's text size")
        @MainActor
        func measuredAtTextSize() {
            let calendar = { (size: DynamicTypeSize) in
                Host.layout(
                    MonarchMiniMonth(selection: .constant(nil), visibleMonth: .constant(Days.november13))
                        .environment(\.calendar, Days.calendar)
                        .environment(\.dynamicTypeSize, size)
                ).height
            }
            #expect(calendar(.accessibility5) > calendar(.large))
        }
    #endif
}
