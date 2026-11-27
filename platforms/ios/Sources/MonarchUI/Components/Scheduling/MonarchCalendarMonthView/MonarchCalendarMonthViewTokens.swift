import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure rules for ``MonarchCalendarMonthView``.
enum MonarchCalendarMonthViewTokens {
    /// The padding inside a day.
    static let cellPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the parts of a day.
    static let cellSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between dots.
    static let dotSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The diameter of an event dot, before Dynamic Type.
    static let dotSize: CGFloat = MonarchTokens.Space.space1_5
    /// The least height of a day in the dots layout: the touch target.
    static let dotsCellHeight: CGFloat = MonarchAccessibility.minimumHitTarget + MonarchTokens.Space.space2
    /// The least height of a day in the events layout, before Dynamic Type.
    static let eventsCellHeight: CGFloat = MonarchTokens.Space.space24
    /// The least width of a day for event chips in the automatic layout.
    static let eventsMinimumCellWidth: CGFloat = MonarchTokens.Space.space20
    /// The diameter of the circle behind today's date, before Dynamic Type.
    static let dateMarkSize: CGFloat = MonarchTokens.Control.sm
    /// The gap below the weekday header.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// How many months the pager holds on each side of the first month shown.
    static let pagerRadius = 24
    /// How many dots a day shows at most in the dots layout.
    static let maxDots = 3

    /// The colour of a day's number.
    static func dateForeground(isToday: Bool, isOutside: Bool, isUnavailable: Bool) -> MonarchColorRole {
        if isToday { return \.accentOnSolid }
        if isOutside || isUnavailable { return \.textSubtle }
        return \.textDefault
    }

    /// The fill behind a day: selected, a drop target, unavailable, or none.
    static func cellBackground(isSelected: Bool, isTargeted: Bool, isUnavailable: Bool) -> MonarchColorRole? {
        if isTargeted { return \.accentSubtle }
        if isSelected { return \.bgEmphasis }
        if isUnavailable { return \.bgMuted }
        return nil
    }

    /// The colour of the lines between days.
    static var gridLine: MonarchColorRole { \.borderSubtle }
    /// The colour of the weekday header.
    static var weekday: MonarchColorRole { \.textMuted }
    /// The colour of a badge and of the overflow button.
    static var secondary: MonarchColorRole { \.textMuted }
    /// The colour of the "+3 more" link, which is the accent as on the web.
    static var more: MonarchColorRole { \.textLink }
    /// The fill of today's date mark.
    static var todayMark: MonarchColorRole { \.accentSolid }
    /// The border of the selected day.
    static var selectedBorder: MonarchColorRole { \.accentBorder }

    /// The colour of a dot: muted for cancelled and declined events, the
    /// accent when the event has no colour of its own.
    static func dotRole(_ status: MonarchEventChipStatus) -> MonarchColorRole {
        status == .cancelled || status == .declined ? \.textDisabled : \.accentSolid
    }

    // MARK: Dates

    /// The first moment of the month that contains `date`.
    static func monthStart(_ date: Date, calendar: Calendar) -> Date {
        calendar.dateInterval(of: .month, for: date)?.start ?? calendar.startOfDay(for: date)
    }

    /// The days shown for the month of `month`: whole weeks from the
    /// calendar's first weekday, six of them when `isFixedWeeks`.
    static func days(month: Date, calendar: Calendar, isFixedWeeks: Bool) -> [Date] {
        let first = monthStart(month, calendar: calendar)
        let length = calendar.range(of: .day, in: .month, for: first)?.count ?? 30
        let leading = (calendar.component(.weekday, from: first) - calendar.firstWeekday + 7) % 7
        let weeks = isFixedWeeks ? 6 : Int((Double(leading + length) / 7).rounded(.up))
        guard let start = calendar.date(byAdding: .day, value: -leading, to: first) else { return [] }
        return (0..<(weeks * 7)).compactMap { calendar.date(byAdding: .day, value: $0, to: start) }
    }

    /// The months the pager holds: `radius` months on each side of `anchor`.
    static func months(around anchor: Date, radius: Int, calendar: Calendar) -> [Date] {
        let start = monthStart(anchor, calendar: calendar)
        return (-radius...radius).compactMap { calendar.date(byAdding: .month, value: $0, to: start) }
    }

    /// Whether a day falls outside the month shown.
    static func isOutside(_ day: Date, month: Date, calendar: Calendar) -> Bool {
        !calendar.isDate(day, equalTo: month, toGranularity: .month)
    }

    /// The events of each shown day: every day an event touches, all-day
    /// events first, then by start time and title.
    static func eventsByDay(
        _ events: [MonarchAgendaEvent],
        days: [Date],
        calendar: Calendar
    ) -> [Date: [MonarchAgendaEvent]] {
        var map: [Date: [MonarchAgendaEvent]] = [:]
        guard let first = days.first, let last = days.last else { return map }
        for event in events {
            let span = MonarchAgendaListTokens.span(of: event, calendar: calendar)
            guard span.last >= first, span.first <= last else { continue }
            var day = max(span.first, first)
            while day <= min(span.last, last) {
                map[day, default: []].append(event)
                guard let next = calendar.date(byAdding: .day, value: 1, to: day) else { break }
                day = next
            }
        }
        for key in map.keys { map[key]?.sort(by: MonarchAgendaListTokens.order) }
        return map
    }

    /// How many events a day lists and how many hide behind "+N more". When
    /// some hide, the button takes the last place.
    static func visibleCount(_ count: Int, maxEvents: Int) -> (shown: Int, hidden: Int) {
        let limit = max(1, maxEvents)
        let shown = count > limit ? limit - 1 : count
        return (shown, count - shown)
    }

    /// Whether days show event chips.
    static func showsEvents(_ layout: MonarchCalendarMonthViewLayout, cellWidth: CGFloat, isAccessibilitySize: Bool)
        -> Bool
    {
        switch layout {
        case .dots: false
        case .events: true
        case .automatic: cellWidth >= eventsMinimumCellWidth && !isAccessibilitySize
        }
    }

    // MARK: Text

    /// The weekday header, starting at the calendar's first weekday: the
    /// very short and the full name.
    static func weekdays(calendar: Calendar) -> [(short: String, full: String)] {
        let short = calendar.shortStandaloneWeekdaySymbols.map { $0.uppercased(with: calendar.locale) }
        let full = calendar.standaloneWeekdaySymbols
        return (0..<7).map { offset in
            let index = (calendar.firstWeekday - 1 + offset) % 7
            return (short[index], full[index])
        }
    }

    /// The number of a day in the locale's numerals.
    static func dayNumber(_ day: Date, calendar: Calendar, locale: Locale) -> String {
        day.formatted(Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone).day())
    }

    /// The long date read for a day: "Wednesday, November 4, 2026".
    static func longDate(_ day: Date, calendar: Calendar, locale: Locale) -> String {
        day.formatted(
            Date.FormatStyle(
                date: .complete,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            )
        )
    }

    /// The month and year: "November 2026".
    static func monthTitle(_ month: Date, calendar: Calendar, locale: Locale) -> String {
        month.formatted(
            Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone).month(.wide).year()
        )
    }

    /// The start time of an event in a chip, or `nil` for an all-day event
    /// or one that began on an earlier day.
    static func chipTime(_ event: MonarchAgendaEvent, on day: Date, calendar: Calendar, locale: Locale) -> String? {
        guard !event.isAllDay, calendar.isDate(event.start, inSameDayAs: day) else { return nil }
        return event.start.formatted(
            Date.FormatStyle(
                date: .omitted,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            ).monarchTime()
        )
    }

    /// The overflow button's text.
    static func more(_ hidden: Int, locale: Locale, labels: MonarchCalendarMonthViewLabels) -> String {
        labels.more?(hidden) ?? MonarchCalendarMonthViewStrings.more(hidden.formatted(.number.locale(locale)))
    }

    /// What VoiceOver reads for a day: the long date and the count, then
    /// "today" and the badge when they apply.
    static func dayLabel(
        date: String,
        count: Int,
        isToday: Bool,
        badge: String?,
        locale: Locale,
        labels: MonarchCalendarMonthViewLabels
    ) -> String {
        let main: String
        if let custom = labels.day {
            main = custom(date, count)
        } else {
            let counted =
                count == 1
                ? MonarchCalendarMonthViewStrings.countOne
                : MonarchCalendarMonthViewStrings.countOther(count.formatted(.number.locale(locale)))
            main = "\(date), \(counted)"
        }
        var parts = [main]
        if isToday { parts.append(labels.today ?? MonarchCalendarMonthViewStrings.today) }
        if let badge, !badge.isEmpty { parts.append(badge) }
        return parts.joined(separator: ", ")
    }

    /// A way to move an event without dragging it.
    struct MoveAction: Equatable {
        /// The spoken name of the action.
        let name: String
        /// The number of days to move by: negative is earlier.
        let days: Int
    }

    /// The VoiceOver actions that do what dragging a chip to another day does:
    /// one day or one week earlier or later.
    static func moveActions(labels: MonarchCalendarMonthViewLabels) -> [MoveAction] {
        [
            MoveAction(name: labels.moveDayEarlier ?? MonarchCalendarMonthViewStrings.moveDayEarlier, days: -1),
            MoveAction(name: labels.moveDayLater ?? MonarchCalendarMonthViewStrings.moveDayLater, days: 1),
            MoveAction(name: labels.moveWeekEarlier ?? MonarchCalendarMonthViewStrings.moveWeekEarlier, days: -7),
            MoveAction(name: labels.moveWeekLater ?? MonarchCalendarMonthViewStrings.moveWeekLater, days: 7),
        ]
    }

    /// The day an event lands on after a move of `days` from `day`, or `nil`.
    static func movedDay(from day: Date, days: Int, calendar: Calendar) -> Date? {
        calendar.date(byAdding: .day, value: days, to: day)
    }
}
