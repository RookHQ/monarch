import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

/// A fixed calendar: Gregorian, UTC, weeks from Monday, English names.
private let utc: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
    calendar.firstWeekday = 2
    calendar.locale = Locale(identifier: "en_US")
    return calendar
}()

private let english = Locale(identifier: "en_US")

private func day(_ year: Int, _ month: Int, _ day: Int, hour: Int = 0) -> Date {
    utc.date(from: DateComponents(year: year, month: month, day: day, hour: hour)) ?? .distantPast
}

/// Half a year of daily values from Thursday 1 January 2026, as the web docs.
private let halfYear: [MonarchCalendarHeatmapDay] = (0..<181).map { offset in
    let date = utc.date(byAdding: .day, value: offset, to: day(2026, 1, 1)) ?? .distantPast
    let weekday = utc.component(.weekday, from: date)
    let weekend = weekday == 1 || weekday == 7
    return MonarchCalendarHeatmapDay(
        date,
        value: weekend ? Double(offset % 5 == 0 ? 1 : 0) : Double((offset * 7 + weekday * 3) % 11),
        isInactive: weekend
    )
}

private let march: [MonarchCalendarHeatmapDay] = [
    MonarchCalendarHeatmapDay(day(2026, 3, 2), value: 480, valueLabel: "8h"),
    MonarchCalendarHeatmapDay(day(2026, 3, 3), value: 450, valueLabel: "7h 30m"),
    MonarchCalendarHeatmapDay(day(2026, 3, 4), value: 0, note: "Missing hours", hasMarker: true),
    MonarchCalendarHeatmapDay(day(2026, 3, 5), value: 510, valueLabel: "8h 30m"),
    MonarchCalendarHeatmapDay(day(2026, 3, 6), value: 240, valueLabel: "4h"),
    MonarchCalendarHeatmapDay(day(2026, 3, 7), value: nil, isInactive: true),
    MonarchCalendarHeatmapDay(day(2026, 3, 8), value: nil, isInactive: true),
    MonarchCalendarHeatmapDay(day(2026, 3, 9), value: nil, note: "Holiday", isInactive: true),
    MonarchCalendarHeatmapDay(day(2026, 3, 10), value: 465, valueLabel: "7h 45m"),
    MonarchCalendarHeatmapDay(day(2026, 3, 12), value: 600, valueLabel: "10h"),
]

private func number(_ value: Double) -> String { value.formatted(.number.locale(english)) }

private func marchModel() -> MonarchCalendarHeatmapTokens.Model? {
    MonarchCalendarHeatmapTokens.model(days: march, from: day(2026, 3, 1), to: day(2026, 3, 31), calendar: utc)
}

enum MonarchCalendarHeatmapFixtures: ComponentFixtures {
    static let id = "calendar-heatmap"
    static let fixtures = [
        Fixture("year: half a year, today, legend with no data") {
            MonarchCalendarHeatmap(
                "Contributions",
                days: halfYear,
                from: day(2026, 1, 1),
                to: day(2026, 6, 30),
                description: "January to June 2026",
                today: day(2026, 6, 30)
            )
            .environment(\.calendar, utc)
        },
        Fixture("month: hours, notes, marker, today, colour 2, selectable") {
            MonarchCalendarHeatmap(
                "March 2026",
                days: march,
                from: day(2026, 3, 1),
                to: day(2026, 3, 31),
                description: "Hours logged per day",
                layout: .month,
                today: day(2026, 3, 12),
                color: .data2,
                footer: "Source: timesheets.",
                onSelect: { _ in }
            )
            .environment(\.calendar, utc)
        },
        Fixture("month without values or legend, thresholds, weeks from Sunday") {
            MonarchCalendarHeatmap(
                "March",
                days: march,
                layout: .month,
                color: .neutral,
                thresholds: [100, 300, 500],
                showsValues: false,
                showsLegend: false
            )
            .environment(
                \.calendar,
                {
                    var calendar = utc
                    calendar.firstWeekday = 1
                    return calendar
                }()
            )
        },
        Fixture("loading, empty, error, custom labels") {
            VStack {
                MonarchCalendarHeatmap("Activity", days: halfYear, status: .loading)
                MonarchCalendarHeatmap("Activity", days: [], emptyMessage: "Activity shows after the first sign-in.")
                MonarchCalendarHeatmap("Activity", days: halfYear, status: .error, onRetry: {})
                MonarchCalendarHeatmap(
                    "Aktivität",
                    days: Array(halfYear.prefix(10)),
                    labels: MonarchCalendarHeatmapLabels(less: "Weniger", more: "Mehr", summary: "%1$@ %2$@ %3$@ %4$@")
                )
            }
            .environment(\.calendar, utc)
        },
    ]
}

@Suite("MonarchCalendarHeatmap")
struct MonarchCalendarHeatmapTests {
    @Test("The model spans the given range and places the first day in its week")
    func model() throws {
        let model = try #require(marchModel())
        #expect(model.start == day(2026, 3, 1))
        #expect(model.count == 31)
        // 1 March 2026 is a Sunday: six places before it in a week from Monday.
        #expect(model.lead == 6)
        #expect(model.weeks == 6)
        #expect(model.days.count == march.count)
        #expect(model.days[1]?.valueLabel == "8h")
        #expect(model.offset(atPlace: 5) == nil)
        #expect(model.offset(atPlace: 6) == 0)
        #expect(model.offset(atPlace: 6 + 31) == nil)
    }

    @Test("Without from and to the range is the first to the last entry; any hour of a day is that day")
    func implicitRange() throws {
        let days = [
            MonarchCalendarHeatmapDay(day(2026, 3, 5, hour: 23), value: 1),
            MonarchCalendarHeatmapDay(day(2026, 3, 2, hour: 9), value: 2),
            MonarchCalendarHeatmapDay(day(2026, 3, 2, hour: 18), value: 3),
        ]
        let model = try #require(MonarchCalendarHeatmapTokens.model(days: days, from: nil, to: nil, calendar: utc))
        #expect(model.start == day(2026, 3, 2))
        #expect(model.count == 4)
        #expect(model.lead == 0)
        // A later entry for the same day wins.
        #expect(model.days[0]?.value == 3)
        #expect(model.days[3]?.value == 1)
    }

    @Test("No range, or a range that ends before it starts, has no model; entries outside are left out")
    func invalidRange() throws {
        #expect(MonarchCalendarHeatmapTokens.model(days: [], from: nil, to: nil, calendar: utc) == nil)
        #expect(
            MonarchCalendarHeatmapTokens.model(days: march, from: day(2026, 3, 10), to: day(2026, 3, 1), calendar: utc)
                == nil
        )
        let model = try #require(
            MonarchCalendarHeatmapTokens.model(days: march, from: day(2026, 3, 3), to: day(2026, 3, 5), calendar: utc)
        )
        #expect(model.days.keys.sorted() == [0, 1, 2])
    }

    @Test("The first weekday comes from the calendar")
    func firstWeekday() {
        var sunday = utc
        sunday.firstWeekday = 1
        #expect(MonarchCalendarHeatmapTokens.weekdayIndex(day(2026, 3, 1), calendar: sunday) == 0)
        #expect(MonarchCalendarHeatmapTokens.weekdayIndex(day(2026, 3, 1), calendar: utc) == 6)
        #expect(MonarchCalendarHeatmapTokens.weekdaySymbols(utc) == ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"])
        #expect(MonarchCalendarHeatmapTokens.weekdaySymbols(sunday).first == "Sun")
        #expect(MonarchCalendarHeatmapTokens.weekdaySymbols(utc, veryShort: true).first == "M")
    }

    @Test("Year places weeks in columns; month places weeks in rows")
    func positions() {
        let year = MonarchCalendarHeatmapTokens.position(0, lead: 6, layout: .year)
        #expect(year.column == 0 && year.row == 6)
        let next = MonarchCalendarHeatmapTokens.position(1, lead: 6, layout: .year)
        #expect(next.column == 1 && next.row == 0)
        let month = MonarchCalendarHeatmapTokens.position(1, lead: 6, layout: .month)
        #expect(month.column == 0 && month.row == 1)
        #expect(MonarchCalendarHeatmapTokens.showsWeekday(row: 1))
        #expect(!MonarchCalendarHeatmapTokens.showsWeekday(row: 0))
    }

    @Test("Month names sit at the first day and each first of a month, skipped when crowded")
    func monthLabels() throws {
        let model = try #require(
            MonarchCalendarHeatmapTokens.model(
                days: halfYear,
                from: day(2026, 1, 1),
                to: day(2026, 6, 30),
                calendar: utc
            )
        )
        let labels = MonarchCalendarHeatmapTokens.monthLabels(model, calendar: utc, minimumColumns: 3)
        #expect(labels.values.sorted() == ["Apr", "Feb", "Jan", "Jun", "Mar", "May"])
        #expect(labels[0] == "Jan")
        let crowded = MonarchCalendarHeatmapTokens.monthLabels(model, calendar: utc, minimumColumns: 6)
        #expect(crowded.count < labels.count)
        #expect(MonarchCalendarHeatmapTokens.minimumLabelColumns(labelWidth: 32, step: 16) == 2)
        #expect(MonarchCalendarHeatmapTokens.minimumLabelColumns(labelWidth: 33, step: 16) == 3)
        #expect(MonarchCalendarHeatmapTokens.minimumLabelColumns(labelWidth: 10, step: 0) == 1)
    }

    @Test("VoiceOver adjustment moves day by day and stops at the ends")
    func adjust() {
        #expect(MonarchCalendarHeatmapTokens.adjusted(nil, forward: true, count: 31) == 0)
        #expect(MonarchCalendarHeatmapTokens.adjusted(nil, forward: false, count: 31) == 30)
        #expect(MonarchCalendarHeatmapTokens.adjusted(4, forward: true, count: 31) == 5)
        #expect(MonarchCalendarHeatmapTokens.adjusted(0, forward: false, count: 31) == 0)
        #expect(MonarchCalendarHeatmapTokens.adjusted(30, forward: true, count: 31) == 30)
        #expect(MonarchCalendarHeatmapTokens.adjusted(nil, forward: true, count: 0) == nil)
    }

    @Test("Cell styles: inspected over today, dashed non-working days, outlined days without data")
    func styles() {
        typealias T = MonarchCalendarHeatmapTokens
        let active = T.cellStyle(level: 2, isInactive: false, isToday: true, isActive: true)
        #expect(active.fill == .level(2))
        #expect(active.stroke == T.activeRole)
        #expect(active.strokeWidth == T.thickStroke)
        let today = T.cellStyle(level: nil, isInactive: false, isToday: true, isActive: false)
        #expect(today.stroke == \.accentSolid)
        #expect(today.fill == .none)
        let quiet = T.cellStyle(level: nil, isInactive: true, isToday: false, isActive: false)
        #expect(quiet.fill == .inactive && quiet.isDashed && quiet.stroke == \.borderSubtle)
        let quietZero = T.cellStyle(level: 0, isInactive: true, isToday: false, isActive: false)
        #expect(quietZero.fill == .inactive && quietZero.isDashed)
        let workedWeekend = T.cellStyle(level: 3, isInactive: true, isToday: false, isActive: false)
        #expect(workedWeekend.fill == .level(3) && workedWeekend.stroke == nil)
        let empty = T.cellStyle(level: nil, isInactive: false, isToday: false, isActive: false)
        #expect(empty.fill == .none && !empty.isDashed && empty.stroke == \.borderSubtle)
        let zero = T.cellStyle(level: 0, isInactive: false, isToday: false, isActive: false)
        #expect(zero.fill == .level(0) && zero.stroke == nil)
    }

    @Test("Dates, values and notes as written and read")
    func text() throws {
        typealias T = MonarchCalendarHeatmapTokens
        #expect(T.dateText(day(2026, 3, 2), calendar: utc, locale: english) == "Mon, Mar 2, 2026")
        #expect(T.dayNumber(day(2026, 3, 9), calendar: utc, locale: english) == "9")
        let model = try #require(marchModel())
        let holiday = model.day(8, calendar: utc)
        #expect(T.valueText(holiday, format: number, noValue: "No value") == "No value")
        #expect(
            T.spokenValue(holiday, isToday: true, format: number, noValue: "No value", today: "Today")
                == "No value, Holiday, Today"
        )
        #expect(T.valueText(model.day(1, calendar: utc), format: number, noValue: "–") == "8h")
        let empty = model.day(20, calendar: utc)
        #expect(empty.value == nil && empty.date == day(2026, 3, 21))
        #expect(T.monthCellText(model.day(3, calendar: utc), format: number) == "Missing hours")
        #expect(T.monthCellText(MonarchCalendarHeatmapDay(day(2026, 3, 1), value: 3), format: number) == "3")
        #expect(T.monthCellText(MonarchCalendarHeatmapDay(day(2026, 3, 1), value: 0), format: number) == nil)
        #expect(T.describe(date: "Mon, Mar 2, 2026", value: "8h") == "Mon, Mar 2, 2026: 8h")
    }

    @Test("The summary counts the days, totals the values and names the highest day")
    func summary() throws {
        typealias T = MonarchCalendarHeatmapTokens
        let model = try #require(marchModel())
        #expect(T.total(model) == 2745)
        #expect(T.highest(model) == 11)
        #expect(T.hasMissingDays(model))
        let labels = MonarchCalendarHeatmapLabels()
        #expect(T.dayCount(1, text: "1", labels: labels) == "1 day")
        #expect(T.dayCount(31, text: "31", labels: labels) == "31 days")
        let text = T.summary(
            count: "31 days",
            first: "Sun, Mar 1, 2026",
            last: "Tue, Mar 31, 2026",
            total: "2,745",
            highest: "Thu, Mar 12, 2026: 10h",
            labels: labels
        )
        #expect(
            text == "31 days from Sun, Mar 1, 2026 to Tue, Mar 31, 2026. Total 2,745. Highest: Thu, Mar 12, 2026: 10h"
        )
        let custom = MonarchCalendarHeatmapLabels(summary: "%1$@|%2$@|%3$@|%4$@", highest: "max %1$@")
        #expect(T.summary(count: "a", first: "b", last: "c", total: "d", highest: nil, labels: custom) == "a|b|c|d")
        #expect(
            T.summary(count: "a", first: "b", last: "c", total: "d", highest: "e", labels: custom) == "a|b|c|d. max e"
        )
    }

    @Test("Ready without an entry in the range shows empty; other statuses pass through")
    func status() {
        typealias T = MonarchCalendarHeatmapTokens
        #expect(T.status(.ready, model: nil) == .empty)
        #expect(T.status(.ready, model: marchModel()) == .ready)
        #expect(T.status(.loading, model: nil) == .loading)
        #expect(T.status(.error, model: nil) == .error)
    }

    @Test("The tables: dates in order, values as written, notes when any day has one")
    func tables() throws {
        typealias T = MonarchCalendarHeatmapTokens
        let model = try #require(marchModel())
        let date: (Int) -> String = { "d\($0)" }
        let labels = MonarchCalendarHeatmapLabels()
        let display = T.displayTable(model, dateText: date, format: number, noValue: "No value", labels: labels)
        #expect(display.categoryTitle == "Date")
        #expect(display.categories == ["d1", "d2", "d3", "d4", "d5", "d6", "d7", "d8", "d9", "d11"])
        #expect(display.series.map(\.name) == ["Value", "Note"])
        #expect(display.series[0].formattedValues?[0] == "8h")
        #expect(display.series[0].values[5] == nil)
        #expect(display.series[1].formattedValues?[7] == "Holiday")
        #expect(display.series[1].values[7] == 0)
        #expect(display.series[1].values[0] == nil)
        let audio = T.audioGraphTable(model, dateText: date, format: number, noValue: "No value", labels: labels)
        #expect(audio.series.count == 1)
        #expect(audio.series[0].values[0] == 480)
        #expect(audio.series[0].values[7] == nil)
        #expect(audio.valueTitle == "Value")
        let plain = try #require(
            T.model(days: [MonarchCalendarHeatmapDay(day(2026, 3, 1), value: 1)], from: nil, to: nil, calendar: utc)
        )
        #expect(T.displayTable(plain, dateText: date, format: number, noValue: "", labels: labels).series.count == 1)
    }

    @Test("The legend uses the heatmap's strings unless replaced")
    func legend() {
        let labels = MonarchCalendarHeatmapTokens.legendLabels(MonarchCalendarHeatmapLabels(less: "Weniger"))
        #expect(labels.less == "Weniger")
        #expect(labels.more == nil)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchCalendarHeatmapStrings.date, MonarchCalendarHeatmapStrings.value,
            MonarchCalendarHeatmapStrings.note, MonarchCalendarHeatmapStrings.today,
            MonarchCalendarHeatmapStrings.dayCountOne, MonarchCalendarHeatmapStrings.dayCount,
            MonarchCalendarHeatmapStrings.summary, MonarchCalendarHeatmapStrings.highest,
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
    }
}
