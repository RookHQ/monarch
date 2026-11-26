import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

/// A Gregorian calendar in UTC whose weeks start on Monday, so dates do not depend on the machine.
private let periodCalendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
    calendar.locale = Locale(identifier: "en_US")
    calendar.firstWeekday = 2
    return calendar
}()

private func periodDay(_ year: Int, _ month: Int, _ day: Int) -> Date {
    periodCalendar.date(from: DateComponents(year: year, month: month, day: day)) ?? Date(timeIntervalSince1970: 0)
}

/// Saturday 7 November 2026.
private let periodToday = periodDay(2026, 11, 7)

enum MonarchPeriodSelectorFixtures: ComponentFixtures {
    static let id = "period-selector"
    static let fixtures = [
        Fixture("segmented, default presets, with dates") {
            MonarchPeriodSelector(
                selection: .constant(MonarchPeriod(.last30Days)),
                showsRange: true,
                today: periodToday
            )
            .environment(\.calendar, periodCalendar)
        },
        Fixture("menu, report presets") {
            MonarchPeriodSelector(
                "Report period",
                selection: .constant(MonarchPeriod(.lastQuarter)),
                presets: [.last30Days, .last90Days, .last180Days, .last365Days, .thisYear],
                variant: .menu,
                showsRange: true,
                today: periodToday
            )
            .environment(\.calendar, periodCalendar)
        },
        Fixture("custom with a range") {
            MonarchPeriodSelector(
                selection: .constant(MonarchPeriod(.custom, range: periodDay(2026, 10, 1)...periodDay(2026, 10, 30))),
                today: periodToday,
                maximumDays: 366
            )
            .environment(\.calendar, periodCalendar)
        },
        Fixture("custom without a range, disabled, custom labels") {
            MonarchPeriodSelector(
                selection: .constant(MonarchPeriod(.custom)),
                presets: MonarchPeriodPreset.allCases,
                today: periodToday,
                labels: MonarchPeriodSelectorLabels(presets: [.custom: "Choose dates"], customRange: "Dates")
            )
            .environment(\.calendar, periodCalendar)
            .disabled(true)
        },
    ]
}

@Suite("MonarchPeriodSelector")
struct MonarchPeriodSelectorTests {
    private typealias Tokens = MonarchPeriodSelectorTokens

    private func resolved(_ preset: MonarchPeriodPreset) -> ClosedRange<Date>? {
        MonarchPeriod(preset).resolved(today: periodToday, calendar: periodCalendar)
    }

    @Test("Rolling presets end today and count today")
    func rolling() {
        #expect(resolved(.today) == periodToday...periodToday)
        #expect(resolved(.yesterday) == periodDay(2026, 11, 6)...periodDay(2026, 11, 6))
        #expect(resolved(.last7Days) == periodDay(2026, 11, 1)...periodToday)
        #expect(resolved(.last14Days) == periodDay(2026, 10, 25)...periodToday)
        #expect(resolved(.last30Days) == periodDay(2026, 10, 9)...periodToday)
        #expect(resolved(.last90Days) == periodDay(2026, 8, 10)...periodToday)
        #expect(resolved(.last180Days) == periodDay(2026, 5, 12)...periodToday)
        #expect(resolved(.last365Days) == periodDay(2025, 11, 8)...periodToday)
    }

    @Test("Calendar presets start on their first day; last month and quarter are whole")
    func calendarPresets() {
        // Weeks start on Monday in this calendar: Monday 2 November.
        #expect(resolved(.thisWeek) == periodDay(2026, 11, 2)...periodToday)
        #expect(resolved(.thisMonth) == periodDay(2026, 11, 1)...periodToday)
        #expect(resolved(.lastMonth) == periodDay(2026, 10, 1)...periodDay(2026, 10, 31))
        #expect(resolved(.thisQuarter) == periodDay(2026, 10, 1)...periodToday)
        #expect(resolved(.lastQuarter) == periodDay(2026, 7, 1)...periodDay(2026, 9, 30))
        #expect(resolved(.thisYear) == periodDay(2026, 1, 1)...periodToday)
        // February in a leap year, from March.
        let march = periodDay(2024, 3, 15)
        #expect(
            MonarchPeriod(.lastMonth).resolved(today: march, calendar: periodCalendar)
                == periodDay(2024, 2, 1)...periodDay(2024, 2, 29)
        )
        // The last quarter of the previous year, from January.
        #expect(
            MonarchPeriod(.lastQuarter).resolved(today: periodDay(2027, 1, 5), calendar: periodCalendar)
                == periodDay(2026, 10, 1)...periodDay(2026, 12, 31)
        )
        var sunday = periodCalendar
        sunday.firstWeekday = 1
        #expect(
            MonarchPeriod(.thisWeek).resolved(today: periodToday, calendar: sunday) == periodDay(
                2026,
                11,
                1
            )...periodToday
        )
    }

    @Test("A custom period is its range, or nothing yet")
    func custom() {
        let range = periodDay(2026, 10, 1)...periodDay(2026, 10, 30)
        #expect(MonarchPeriod(.custom, range: range).resolved(today: periodToday, calendar: periodCalendar) == range)
        #expect(resolved(.custom) == nil)
        // A range kept on another preset is ignored.
        #expect(
            MonarchPeriod(.today, range: range).resolved(today: periodToday, calendar: periodCalendar)
                == periodToday...periodToday
        )
    }

    @Test("Picking Custom starts from the dates shown; picking the same preset changes nothing")
    func picking() {
        let current = MonarchPeriod(.last7Days)
        let custom = Tokens.picking(.custom, current: current, today: periodToday, calendar: periodCalendar)
        #expect(custom == MonarchPeriod(.custom, range: periodDay(2026, 11, 1)...periodToday))
        #expect(Tokens.picking(.last7Days, current: current, today: periodToday, calendar: periodCalendar) == current)
        let back = Tokens.picking(.thisMonth, current: custom, today: periodToday, calendar: periodCalendar)
        #expect(back.preset == .thisMonth)
        // The custom dates are remembered for the next time Custom is picked.
        let again = Tokens.picking(.custom, current: back, today: periodToday, calendar: periodCalendar)
        #expect(again.range == periodDay(2026, 11, 1)...periodToday)
    }

    @Test("Dates show when asked for and not for a custom period")
    func showsDates() {
        let range = periodToday...periodToday
        #expect(Tokens.showsDates(MonarchPeriod(.today), showsRange: true, resolved: range))
        #expect(!Tokens.showsDates(MonarchPeriod(.today), showsRange: false, resolved: range))
        #expect(!Tokens.showsDates(MonarchPeriod(.custom, range: range), showsRange: true, resolved: range))
        #expect(!Tokens.showsDates(MonarchPeriod(.custom), showsRange: true, resolved: nil))
    }

    @Test("The dates are written in the locale, one day once")
    func rangeText() {
        let locale = Locale(identifier: "en_US")
        let text = Tokens.rangeText(
            periodDay(2026, 10, 1)...periodDay(2026, 10, 30),
            locale: locale,
            calendar: periodCalendar
        )
        #expect(text.contains("Oct"))
        #expect(text.contains("1"))
        #expect(text.contains("30"))
        #expect(text.contains("2026"))
        let one = Tokens.rangeText(periodToday...periodToday, locale: locale, calendar: periodCalendar)
        #expect(one == "Nov 7, 2026")
        let german = Tokens.rangeText(
            periodToday...periodToday,
            locale: Locale(identifier: "de_DE"),
            calendar: periodCalendar
        )
        #expect(german.contains("Nov") && german.contains("2026") && german != one)
    }

    @Test("Segments become a menu at the accessibility text sizes")
    func variant() {
        #expect(Tokens.resolvedVariant(.segmented, isAccessibilitySize: false) == .segmented)
        #expect(Tokens.resolvedVariant(.segmented, isAccessibilitySize: true) == .menu)
        #expect(Tokens.resolvedVariant(.menu, isAccessibilitySize: false) == .menu)
    }

    @Test("Preset names are built in and can be replaced one by one")
    func names() {
        let labels = MonarchPeriodSelectorLabels(presets: [.last7Days: "7 derniers jours"])
        #expect(Tokens.name(.last7Days, labels: labels) == "7 derniers jours")
        #expect(Tokens.name(.last30Days, labels: labels) == "Last 30 days")
        #expect(MonarchPeriodPreset.last365Days.rawValue == "365d")
        #expect(MonarchPeriodPreset.thisQuarter.id == "this-quarter")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchPeriodSelectorStrings.label == "Period")
        #expect(MonarchPeriodSelectorStrings.customRange == "Custom period")
        let expected: [MonarchPeriodPreset: String] = [
            .today: "Today", .yesterday: "Yesterday", .last7Days: "Last 7 days", .last14Days: "Last 14 days",
            .last30Days: "Last 30 days", .last90Days: "Last 90 days", .last180Days: "Last 180 days",
            .last365Days: "Last 12 months", .thisWeek: "This week", .thisMonth: "This month",
            .lastMonth: "Last month", .thisQuarter: "This quarter", .lastQuarter: "Last quarter",
            .thisYear: "This year", .custom: "Custom",
        ]
        for preset in MonarchPeriodPreset.allCases {
            #expect(MonarchPeriodSelectorStrings.preset(preset) == expected[preset])
        }
    }
}
