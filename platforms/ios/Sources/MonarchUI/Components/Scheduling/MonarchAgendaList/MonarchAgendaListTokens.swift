import Foundation
import MonarchTokens
import SwiftUI

/// One day of an agenda: its events in order and its notes.
struct MonarchAgendaDay: Equatable, Identifiable {
    /// The start of the day, in the list's calendar.
    let day: Date
    /// The events that touch the day: all-day first, then by start time.
    let events: [MonarchAgendaEvent]
    /// Informational rows shown before the events.
    let notes: [String]

    var id: Date { day }
}

/// The time of an event on one day: what is shown and what is read.
struct MonarchAgendaTime: Equatable {
    /// The visible time, "09:30 – 10:15", "– 11:00" or "All day".
    let text: String
    /// The spoken time, "09:30 to 10:15", "until 11:00" or "All day".
    let spoken: String
}

/// Component tokens and logic for ``MonarchAgendaList``.
enum MonarchAgendaListTokens {
    /// The type role of a day's date.
    static let headingStyle: MonarchTextStyle = .label
    /// The type role of a day's count and of a note.
    static let captionStyle: MonarchTextStyle = .caption
    /// The gap between the parts of a day's heading.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding above and below a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space1
    /// The weight of a day's date.
    static let headingWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The SF Symbol of the empty state.
    static let emptySymbol = "calendar"
    /// The SF Symbol before a day note.
    static let noteSymbol = "info.circle"
    /// The tone of the Today marker: the accent, as on the web.
    static let todayTone: MonarchTone = .accent
    /// The colour of a day's date.
    static var headingColor: MonarchColorRole { \.textDefault }
    /// The colour of a day's count and of a note.
    static var secondaryColor: MonarchColorRole { \.textMuted }

    /// The first and last day (inclusive) an event touches. A timed event that ends exactly at
    /// midnight does not touch the day it ends on.
    static func span(of event: MonarchAgendaEvent, calendar: Calendar) -> (first: Date, last: Date) {
        let first = calendar.startOfDay(for: event.start)
        guard let end = event.end, end > event.start else { return (first, first) }
        if event.isAllDay { return (first, calendar.startOfDay(for: end)) }
        let lastDay = calendar.startOfDay(for: end)
        let last = lastDay == end ? (calendar.date(byAdding: .day, value: -1, to: lastDay) ?? lastDay) : lastDay
        return (first, max(first, last))
    }

    /// The days of the agenda.
    ///
    /// The range starts at `from` (or the first day with an event) and covers `days` days
    /// (or runs through the last event). A day is listed when it has events or notes, or when
    /// `showsEmptyDays` is on. Events that started earlier and are still running appear on
    /// each day they touch.
    static func days(
        events: [MonarchAgendaEvent],
        from: Date?,
        days count: Int?,
        showsEmptyDays: Bool,
        calendar: Calendar,
        notes: (Date) -> [String] = { _ in [] }
    ) -> [MonarchAgendaDay] {
        let spans = events.map { (event: $0, span: span(of: $0, calendar: calendar)) }
        guard let start = from.map(calendar.startOfDay(for:)) ?? spans.map(\.span.first).min() else { return [] }
        let end: Date
        if let count {
            guard count > 0 else { return [] }
            end = calendar.date(byAdding: .day, value: count - 1, to: start) ?? start
        } else {
            guard let last = spans.map(\.span.last).max(), last >= start else {
                return showsEmptyDays || !notes(start).isEmpty
                    ? [MonarchAgendaDay(day: start, events: [], notes: notes(start))] : []
            }
            end = last
        }
        var result: [MonarchAgendaDay] = []
        var day = start
        while day <= end {
            let dayEvents =
                spans
                .filter { $0.span.first <= day && day <= $0.span.last }
                .map(\.event)
                .sorted(by: order)
            let dayNotes = notes(day)
            if showsEmptyDays || !dayEvents.isEmpty || !dayNotes.isEmpty {
                result.append(MonarchAgendaDay(day: day, events: dayEvents, notes: dayNotes))
            }
            guard let next = calendar.date(byAdding: .day, value: 1, to: day) else { break }
            day = next
        }
        return result
    }

    /// The notes of a day: every note whose key falls on that day, earliest key first.
    static func notes(on day: Date, from notes: [Date: [String]], calendar: Calendar) -> [String] {
        notes.filter { calendar.isDate($0.key, inSameDayAs: day) }
            .sorted { $0.key < $1.key }
            .flatMap(\.value)
    }

    /// All-day events first, then by start time, then by title.
    static func order(_ lhs: MonarchAgendaEvent, _ rhs: MonarchAgendaEvent) -> Bool {
        if lhs.isAllDay != rhs.isAllDay { return lhs.isAllDay }
        if lhs.start != rhs.start { return lhs.start < rhs.start }
        return lhs.title < rhs.title
    }

    /// The time of an event on a day. A timed event continuing from an earlier day shows its
    /// end only, and "All day" on a day it covers whole, as on the web.
    static func time(
        of event: MonarchAgendaEvent,
        on day: Date,
        calendar: Calendar,
        labels: MonarchAgendaListLabels,
        format: (Date) -> String
    ) -> MonarchAgendaTime {
        let allDay = labels.allDay ?? MonarchAgendaListStrings.allDay
        if event.isAllDay { return MonarchAgendaTime(text: allDay, spoken: allDay) }
        let start = format(event.start)
        guard let endDate = event.end else { return MonarchAgendaTime(text: start, spoken: start) }
        let end = format(endDate)
        let span = span(of: event, calendar: calendar)
        if span.first < day {
            if day < span.last { return MonarchAgendaTime(text: allDay, spoken: allDay) }
            let spoken = labels.until?(end) ?? MonarchAgendaListStrings.until(end)
            return MonarchAgendaTime(text: "– \(end)", spoken: spoken)
        }
        let spoken = labels.range?(start, end) ?? MonarchAgendaListStrings.range(start, end)
        return MonarchAgendaTime(text: "\(start) – \(end)", spoken: spoken)
    }

    /// The count in a day's heading.
    static func count(_ count: Int, labels: MonarchAgendaListLabels, locale: Locale = .autoupdatingCurrent) -> String {
        if let custom = labels.count { return custom(count) }
        return count == 1
            ? MonarchAgendaListStrings.countOne
            : MonarchAgendaListStrings.countOther(count.formatted(.number.locale(locale)))
    }

    /// What VoiceOver reads for a day's heading: the date, "Today" when it is today, and the count.
    static func headingLabel(date: String, isToday: Bool, count: String, labels: MonarchAgendaListLabels) -> String {
        let today = labels.today ?? MonarchAgendaListStrings.today
        return ([date] + (isToday ? [today] : []) + [count]).joined(separator: ", ")
    }

    /// The title of the empty state.
    static func emptyTitle(_ labels: MonarchAgendaListLabels) -> String {
        labels.emptyTitle ?? MonarchAgendaListStrings.emptyTitle
    }
}
