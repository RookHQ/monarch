import Foundation
import MonarchTokens
import SwiftUI

/// The bounds a range must keep: the earliest and latest day and the shortest
/// and longest length, in days counting both ends.
struct MonarchDateRangeLimits {
    var minimumDate: Date?
    var maximumDate: Date?
    var minimumDays: Int
    var maximumDays: Int?
    var calendar: Calendar

    init(minimumDate: Date?, maximumDate: Date?, minimumDays: Int, maximumDays: Int?, calendar: Calendar) {
        self.calendar = calendar
        self.minimumDate = minimumDate.map { calendar.startOfDay(for: $0) }
        self.maximumDate = maximumDate.map { calendar.startOfDay(for: $0) }
        self.minimumDays = max(1, minimumDays)
        self.maximumDays = maximumDays.map { max(self.minimumDays, $0) }
    }
}

/// Component tokens and pure logic of ``MonarchDateRangePicker``. Every rule
/// is decided here, on whole days in the given calendar, and tested.
enum MonarchDateRangePickerTokens {
    /// The space between the label, the pickers and the messages.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The space between the quick picks and the clear button.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the Choose dates button.
    static let calendarSymbol = "calendar"
    /// The SF Symbol of the quick picks menu.
    static let presetsSymbol = "list.bullet"
    /// The size of the buttons under the pickers.
    static let buttonSize: MonarchButtonSize = .sm
    /// The colour of the day count.
    static var countColor: MonarchColorRole { \.textMuted }

    /// The first moment of the day `count` days after `day`.
    static func adding(_ count: Int, to day: Date, calendar: Calendar) -> Date {
        calendar.date(byAdding: .day, value: count, to: calendar.startOfDay(for: day)) ?? day
    }

    /// The number of days in a range, counting both ends.
    static func dayCount(_ range: ClosedRange<Date>, calendar: Calendar) -> Int {
        let start = calendar.startOfDay(for: range.lowerBound)
        let end = calendar.startOfDay(for: range.upperBound)
        return (calendar.dateComponents([.day], from: start, to: end).day ?? 0) + 1
    }

    /// The days the first date can be: from the earliest day to the last day
    /// that still leaves room for the shortest range.
    static func startRange(_ limits: MonarchDateRangeLimits) -> ClosedRange<Date> {
        let lower = limits.minimumDate ?? .distantPast
        guard let maximum = limits.maximumDate else { return lower...Date.distantFuture }
        let upper = adding(-(limits.minimumDays - 1), to: maximum, calendar: limits.calendar)
        return lower...max(lower, upper)
    }

    /// The days the last date can be for a first date: from the end of the
    /// shortest range to the end of the longest, within the latest day.
    static func endRange(start: Date, limits: MonarchDateRangeLimits) -> ClosedRange<Date> {
        let calendar = limits.calendar
        let lower = adding(limits.minimumDays - 1, to: start, calendar: calendar)
        var upper = limits.maximumDays.map { adding($0 - 1, to: start, calendar: calendar) } ?? .distantFuture
        if let maximum = limits.maximumDate { upper = min(upper, maximum) }
        return lower...max(lower, upper)
    }

    /// A date moved into a range of days.
    static func clamp(_ date: Date, to range: ClosedRange<Date>, calendar: Calendar) -> Date {
        min(max(calendar.startOfDay(for: date), range.lowerBound), range.upperBound)
    }

    /// The range after the first date changes: the first date inside its
    /// bounds and the length kept, as Calendar does, then the last date
    /// inside its bounds.
    static func settingStart(_ start: Date, current: ClosedRange<Date>?, limits: MonarchDateRangeLimits) -> ClosedRange<
        Date
    > {
        let calendar = limits.calendar
        let newStart = clamp(start, to: startRange(limits), calendar: calendar)
        let length = current.map { dayCount($0, calendar: calendar) } ?? limits.minimumDays
        let wanted = adding(length - 1, to: newStart, calendar: calendar)
        return newStart...clamp(wanted, to: endRange(start: newStart, limits: limits), calendar: calendar)
    }

    /// The range after the last date changes: the last date inside its bounds.
    static func settingEnd(_ end: Date, start: Date, limits: MonarchDateRangeLimits) -> ClosedRange<Date> {
        let calendar = limits.calendar
        let start = calendar.startOfDay(for: start)
        return start...clamp(end, to: endRange(start: start, limits: limits), calendar: calendar)
    }

    /// The range Choose dates sets: the shortest range from today, inside the bounds.
    static func initialRange(today: Date, limits: MonarchDateRangeLimits) -> ClosedRange<Date> {
        settingStart(today, current: nil, limits: limits)
    }

    /// The range a preset stands for on `today`.
    static func resolve(_ preset: MonarchDateRangePreset, today: Date, calendar: Calendar) -> ClosedRange<Date> {
        let today = calendar.startOfDay(for: today)
        switch preset.kind {
        case .range(let range):
            let start = calendar.startOfDay(for: range.lowerBound)
            return start...max(start, calendar.startOfDay(for: range.upperBound))
        case .lastDays(let count):
            return adding(-(max(1, count) - 1), to: today, calendar: calendar)...today
        case .nextDays(let count):
            return today...adding(max(1, count) - 1, to: today, calendar: calendar)
        case .month(let offset):
            let shifted = calendar.date(byAdding: .month, value: offset, to: today) ?? today
            guard let month = calendar.dateInterval(of: .month, for: shifted) else { return today...today }
            return month.start...adding(-1, to: month.end, calendar: calendar)
        }
    }

    /// Whether a range keeps every bound, so a preset that does not is disabled.
    static func fits(_ range: ClosedRange<Date>, limits: MonarchDateRangeLimits) -> Bool {
        let count = dayCount(range, calendar: limits.calendar)
        if let minimum = limits.minimumDate, range.lowerBound < minimum { return false }
        if let maximum = limits.maximumDate, range.upperBound > maximum { return false }
        if count < limits.minimumDays { return false }
        if let maximumDays = limits.maximumDays, count > maximumDays { return false }
        return true
    }

    /// The index of the preset that stands for the range, if any.
    static func matchingPreset(
        _ range: ClosedRange<Date>?,
        presets: [MonarchDateRangePreset],
        today: Date,
        calendar: Calendar
    ) -> Int? {
        guard let range else { return nil }
        let start = calendar.startOfDay(for: range.lowerBound)
        let end = calendar.startOfDay(for: range.upperBound)
        return presets.firstIndex { resolve($0, today: today, calendar: calendar) == start...end }
    }

    /// The error of a range: a first or last day the app disabled. The other
    /// rules cannot be broken, because the pickers only offer days that keep them.
    static func error(
        _ range: ClosedRange<Date>?,
        isDisabled: (Date) -> Bool,
        labels: MonarchDateRangePickerLabels,
        calendar: Calendar
    ) -> String? {
        guard let range else { return nil }
        let ends = [range.lowerBound, range.upperBound].map { calendar.startOfDay(for: $0) }
        guard ends.contains(where: isDisabled) else { return nil }
        return labels.unavailable ?? MonarchDateRangePickerStrings.unavailable
    }

    /// The length of the range as shown under the pickers: "1 day", "7 days".
    static func countText(_ count: Int, labels: MonarchDateRangePickerLabels, locale: Locale) -> String {
        if let custom = labels.dayCount { return custom(count) }
        return count == 1
            ? MonarchDateRangePickerStrings.dayCountOne
            : MonarchDateRangePickerStrings.dayCountOther(count.formatted(.number.locale(locale)))
    }

    /// The spoken name of one of the two pickers: "Trip dates, Start".
    static func pickerLabel(field: String, part: String) -> String {
        field.isEmpty ? part : "\(field), \(part)"
    }

    /// The label of the first date.
    static func start(_ labels: MonarchDateRangePickerLabels) -> String {
        labels.start ?? MonarchDateRangePickerStrings.start
    }

    /// The label of the last date.
    static func end(_ labels: MonarchDateRangePickerLabels) -> String {
        labels.end ?? MonarchDateRangePickerStrings.end
    }
}
