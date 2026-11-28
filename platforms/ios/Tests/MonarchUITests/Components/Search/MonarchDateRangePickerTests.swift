import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// A fixed calendar and dates, so the rules do not depend on the machine.
private enum RangeClock {
    static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? TimeZone(secondsFromGMT: 0) ?? .gmt
        return calendar
    }()

    static func day(_ day: Int, month: Int = 11, year: Int = 2026, hour: Int = 0) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day, hour: hour))
            ?? Date(timeIntervalSince1970: 0)
    }

    /// Saturday 14 November 2026.
    static let today = day(14)
}

private func limits(
    minimumDate: Date? = nil,
    maximumDate: Date? = nil,
    minimumDays: Int = 1,
    maximumDays: Int? = nil
) -> MonarchDateRangeLimits {
    MonarchDateRangeLimits(
        minimumDate: minimumDate,
        maximumDate: maximumDate,
        minimumDays: minimumDays,
        maximumDays: maximumDays,
        calendar: RangeClock.calendar
    )
}

enum MonarchDateRangePickerFixtures: ComponentFixtures {
    static let id = "date-range-picker"
    static let fixtures = [
        Fixture("range with presets, helper and clear") {
            MonarchDateRangePicker(
                "Report period",
                selection: .constant(RangeClock.day(1)...RangeClock.day(14)),
                helper: "Up to a year.",
                maximumDate: RangeClock.today,
                maximumDays: 366,
                presets: [
                    .lastDays(7, label: "Last 7 days"),
                    .month(0, label: "This month"),
                    .month(-1, label: "Last month"),
                ],
                today: RangeClock.today
            )
            .environment(\.calendar, RangeClock.calendar)
        },
        Fixture("no range yet, required") {
            MonarchDateRangePicker("Leave", selection: .constant(nil), today: RangeClock.today, isRequired: true)
                .environment(\.calendar, RangeClock.calendar)
        },
        Fixture("unavailable end day shows the error") {
            MonarchDateRangePicker(
                "Booking",
                selection: .constant(RangeClock.day(16)...RangeClock.day(21)),
                minimumDays: 2,
                today: RangeClock.today,
                isClearable: false,
                isDateDisabled: { RangeClock.calendar.isDateInWeekend($0) }
            )
            .environment(\.calendar, RangeClock.calendar)
        },
        Fixture("disabled, custom labels and a long label") {
            MonarchDateRangePicker(
                "Dates the whole northern region team will be away at the annual planning offsite",
                selection: .constant(RangeClock.day(2)...RangeClock.day(2)),
                today: RangeClock.today,
                labels: MonarchDateRangePickerLabels(
                    start: "From",
                    end: "Until",
                    clear: "Reset",
                    dayCount: { "\($0) d" }
                )
            )
            .environment(\.calendar, RangeClock.calendar)
            .disabled(true)
        },
    ]
}

@Suite("MonarchDateRangePicker")
struct MonarchDateRangePickerTests {
    private let calendar = RangeClock.calendar

    @Test func dayCountCountsBothEnds() {
        #expect(MonarchDateRangePickerTokens.dayCount(RangeClock.day(1)...RangeClock.day(1), calendar: calendar) == 1)
        #expect(
            MonarchDateRangePickerTokens.dayCount(RangeClock.day(1)...RangeClock.day(7, hour: 18), calendar: calendar)
                == 7
        )
        #expect(
            MonarchDateRangePickerTokens.dayCount(
                RangeClock.day(28, month: 2)...RangeClock.day(1, month: 3),
                calendar: calendar
            )
                == 2
        )
    }

    @Test func startCannotLeaveTooLittleRoom() {
        let range = MonarchDateRangePickerTokens.startRange(
            limits(minimumDate: RangeClock.day(1), maximumDate: RangeClock.day(30), minimumDays: 3)
        )
        #expect(range == RangeClock.day(1)...RangeClock.day(28))
        let open = MonarchDateRangePickerTokens.startRange(limits())
        #expect(open.lowerBound == .distantPast && open.upperBound == .distantFuture)
    }

    @Test func endRangeKeepsLengthAndLatestDay() {
        let range = MonarchDateRangePickerTokens.endRange(
            start: RangeClock.day(10),
            limits: limits(maximumDate: RangeClock.day(20), minimumDays: 2, maximumDays: 7)
        )
        #expect(range == RangeClock.day(11)...RangeClock.day(16))
        let capped = MonarchDateRangePickerTokens.endRange(
            start: RangeClock.day(18),
            limits: limits(maximumDate: RangeClock.day(20), maximumDays: 7)
        )
        #expect(capped == RangeClock.day(18)...RangeClock.day(20))
    }

    @Test func movingTheStartKeepsTheLength() {
        let current = RangeClock.day(2)...RangeClock.day(8)
        let moved = MonarchDateRangePickerTokens.settingStart(
            RangeClock.day(10, hour: 15),
            current: current,
            limits: limits()
        )
        #expect(moved == RangeClock.day(10)...RangeClock.day(16))
        // Near the latest day the end is cut, but never before the shortest range.
        let cut = MonarchDateRangePickerTokens.settingStart(
            RangeClock.day(27),
            current: current,
            limits: limits(maximumDate: RangeClock.day(30), minimumDays: 2)
        )
        #expect(cut == RangeClock.day(27)...RangeClock.day(30))
        // A start past the last possible start is pulled back.
        let pulled = MonarchDateRangePickerTokens.settingStart(
            RangeClock.day(30),
            current: nil,
            limits: limits(maximumDate: RangeClock.day(30), minimumDays: 3)
        )
        #expect(pulled == RangeClock.day(28)...RangeClock.day(30))
    }

    @Test func endIsKeptInsideItsBounds() {
        let rules = limits(minimumDays: 3, maximumDays: 10)
        #expect(
            MonarchDateRangePickerTokens.settingEnd(RangeClock.day(5), start: RangeClock.day(5), limits: rules)
                == RangeClock.day(5)...RangeClock.day(7)
        )
        #expect(
            MonarchDateRangePickerTokens.settingEnd(RangeClock.day(30), start: RangeClock.day(5), limits: rules)
                == RangeClock.day(5)...RangeClock.day(14)
        )
        #expect(
            MonarchDateRangePickerTokens.settingEnd(RangeClock.day(9), start: RangeClock.day(5), limits: rules)
                == RangeClock.day(5)...RangeClock.day(9)
        )
    }

    @Test func chooseDatesStartsTodayInsideTheBounds() {
        #expect(
            MonarchDateRangePickerTokens.initialRange(today: RangeClock.today, limits: limits(minimumDays: 2))
                == RangeClock.day(14)...RangeClock.day(15)
        )
        #expect(
            MonarchDateRangePickerTokens.initialRange(
                today: RangeClock.today,
                limits: limits(minimumDate: RangeClock.day(20))
            )
                == RangeClock.day(20)...RangeClock.day(20)
        )
    }

    @Test func presetsResolveAgainstToday() {
        let today = RangeClock.today
        func resolve(_ preset: MonarchDateRangePreset) -> ClosedRange<Date> {
            MonarchDateRangePickerTokens.resolve(preset, today: today, calendar: calendar)
        }
        #expect(resolve(.lastDays(7, label: "")) == RangeClock.day(8)...RangeClock.day(14))
        #expect(resolve(.nextDays(3, label: "")) == RangeClock.day(14)...RangeClock.day(16))
        #expect(resolve(.lastDays(0, label: "")) == today...today)
        #expect(resolve(.month(0, label: "")) == RangeClock.day(1)...RangeClock.day(30))
        #expect(resolve(.month(-9, label: "")) == RangeClock.day(1, month: 2)...RangeClock.day(28, month: 2))
        #expect(
            resolve(.month(2, label: "")) == RangeClock.day(
                1,
                month: 1,
                year: 2027
            )...RangeClock.day(31, month: 1, year: 2027)
        )
        #expect(
            resolve(.range(RangeClock.day(9, hour: 8)...RangeClock.day(12, hour: 20), label: ""))
                == RangeClock.day(9)...RangeClock.day(12)
        )
    }

    @Test func presetsThatBreakARuleDoNotFit() {
        let rules = limits(maximumDate: RangeClock.today, maximumDays: 10)
        #expect(MonarchDateRangePickerTokens.fits(RangeClock.day(8)...RangeClock.day(14), limits: rules))
        #expect(!MonarchDateRangePickerTokens.fits(RangeClock.day(1)...RangeClock.day(14), limits: rules))
        #expect(!MonarchDateRangePickerTokens.fits(RangeClock.day(14)...RangeClock.day(16), limits: rules))
        #expect(
            !MonarchDateRangePickerTokens.fits(RangeClock.day(1)...RangeClock.day(1), limits: limits(minimumDays: 2))
        )
        #expect(
            !MonarchDateRangePickerTokens.fits(
                RangeClock.day(1)...RangeClock.day(1),
                limits: limits(minimumDate: RangeClock.day(2))
            )
        )
    }

    @Test func matchingPresetIsFoundByDays() {
        let presets: [MonarchDateRangePreset] = [.lastDays(7, label: "Week"), .month(0, label: "Month")]
        #expect(
            MonarchDateRangePickerTokens.matchingPreset(
                RangeClock.day(1, hour: 9)...RangeClock.day(30),
                presets: presets,
                today: RangeClock.today,
                calendar: calendar
            ) == 1
        )
        #expect(
            MonarchDateRangePickerTokens.matchingPreset(
                RangeClock.day(2)...RangeClock.day(3),
                presets: presets,
                today: RangeClock.today,
                calendar: calendar
            ) == nil
        )
        #expect(
            MonarchDateRangePickerTokens.matchingPreset(
                nil,
                presets: presets,
                today: RangeClock.today,
                calendar: calendar
            ) == nil
        )
        #expect(MonarchDateRangePreset.lastDays(7, label: "Week").id == "Week")
    }

    @Test func disabledEndsAreAnError() {
        let weekend: (Date) -> Bool = { RangeClock.calendar.isDateInWeekend($0) }
        let labels = MonarchDateRangePickerLabels()
        // Saturday 21 November.
        #expect(
            MonarchDateRangePickerTokens.error(
                RangeClock.day(16)...RangeClock.day(21),
                isDisabled: weekend,
                labels: labels,
                calendar: calendar
            ) == "That date is not available."
        )
        // A weekend inside the range is allowed.
        #expect(
            MonarchDateRangePickerTokens.error(
                RangeClock.day(16)...RangeClock.day(23),
                isDisabled: weekend,
                labels: labels,
                calendar: calendar
            ) == nil
        )
        #expect(MonarchDateRangePickerTokens.error(nil, isDisabled: weekend, labels: labels, calendar: calendar) == nil)
        #expect(
            MonarchDateRangePickerTokens.error(
                RangeClock.day(21)...RangeClock.day(21),
                isDisabled: weekend,
                labels: MonarchDateRangePickerLabels(unavailable: "Closed"),
                calendar: calendar
            ) == "Closed"
        )
    }

    @Test func spokenAndVisibleText() {
        let labels = MonarchDateRangePickerLabels()
        let locale = Locale(identifier: "en_US")
        #expect(MonarchDateRangePickerTokens.countText(1, labels: labels, locale: locale) == "1 day")
        #expect(MonarchDateRangePickerTokens.countText(1_200, labels: labels, locale: locale) == "1,200 days")
        #expect(
            MonarchDateRangePickerTokens.countText(
                3,
                labels: MonarchDateRangePickerLabels(dayCount: { "\($0)d" }),
                locale: locale
            ) == "3d"
        )
        #expect(MonarchDateRangePickerTokens.pickerLabel(field: "Trip dates", part: "Start") == "Trip dates, Start")
        #expect(MonarchDateRangePickerTokens.pickerLabel(field: "", part: "End") == "End")
        #expect(MonarchDateRangePickerTokens.start(labels) == "Start")
        #expect(MonarchDateRangePickerTokens.end(MonarchDateRangePickerLabels(end: "Until")) == "Until")
    }

    @Test func builtInStringsResolve() {
        let strings = [
            MonarchDateRangePickerStrings.start,
            MonarchDateRangePickerStrings.end,
            MonarchDateRangePickerStrings.chooseDates,
            MonarchDateRangePickerStrings.clear,
            MonarchDateRangePickerStrings.presets,
            MonarchDateRangePickerStrings.unavailable,
            MonarchDateRangePickerStrings.dayCountOne,
            MonarchDateRangePickerStrings.dayCountOther("2"),
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchDateRangePickerStrings.dayCountOther("2") == "2 days")
    }
}
