import SwiftUI

/// A stretch of wall-clock minutes inside one day: `top` inclusive, `bottom`
/// exclusive.
struct MonarchTimeGridSpan: Hashable {
    var top: Int
    var bottom: Int
}

/// A timed item placed in a day column: its span and its place in a cluster
/// of overlapping items.
struct MonarchTimeGridPacked<Item> {
    let item: Item
    let span: MonarchTimeGridSpan
    /// Zero-based column inside its overlap cluster.
    var column: Int
    /// The number of columns in the cluster.
    var columns: Int
}

/// Everything one day column draws.
struct MonarchTimeGridColumnModel {
    let day: Date
    let timed: [MonarchTimeGridPacked<MonarchAgendaEvent>]
    let allDay: [MonarchAgendaEvent]
    let unavailable: [MonarchTimeGridPacked<MonarchTimeGridUnavailable>]
    let shade: [MonarchTimeGridSpan]
}

/// An event being dragged: where its ghost is.
struct MonarchTimeGridDrag: Equatable {
    let eventID: String
    /// Minutes between the event's top and the finger, kept while dragging.
    let grab: Int
    /// The event's length in minutes.
    let duration: Int
    var dayIndex: Int
    var startMinutes: Int
}

/// A VoiceOver action that moves an event, and the move it makes.
struct MonarchTimeGridMoveAction {
    let name: String
    let move: MonarchTimeGridMove
}

/// The pure date and layout logic of ``MonarchTimeGrid``: how events map onto
/// days, how overlaps are laid out, and what a tap or a drag means. Every
/// function takes the calendar, so time zones and daylight-saving days are
/// decided by it and tested with fixed ones.
enum MonarchTimeGridModel {
    static let minutesPerDay = 24 * 60

    /// The visible range in minutes, from the first hour (0–23) to the end hour
    /// (exclusive, 1–24). At least one hour is shown.
    static func range(startHour: Int, endHour: Int) -> MonarchTimeGridSpan {
        let start = min(max(startHour, 0), 23) * 60
        let end = max(start + 60, min(max(endHour, 0), 24) * 60)
        return MonarchTimeGridSpan(top: start, bottom: end)
    }

    /// The hours drawn, in order.
    static func hours(in range: MonarchTimeGridSpan) -> [Int] {
        Array(stride(from: range.top / 60, to: range.bottom / 60, by: 1))
    }

    /// The day and wall-clock minutes of a moment in a calendar.
    static func wallClock(_ date: Date, calendar: Calendar) -> (day: Date, minutes: Int) {
        let parts = calendar.dateComponents([.hour, .minute], from: date)
        return (calendar.startOfDay(for: date), (parts.hour ?? 0) * 60 + (parts.minute ?? 0))
    }

    /// The moment a wall-clock time falls on a day. A time skipped by a
    /// daylight-saving change moves forward, as the calendar decides.
    static func date(day: Date, minutes: Int, calendar: Calendar) -> Date {
        var parts = calendar.dateComponents([.era, .year, .month, .day], from: day)
        parts.hour = minutes / 60
        parts.minute = minutes % 60
        return calendar.date(from: parts) ?? day.addingTimeInterval(TimeInterval(minutes * 60))
    }

    /// The ISO weekday of a day: 1 is Monday, 7 is Sunday.
    static func isoWeekday(_ day: Date, calendar: Calendar) -> Int {
        let weekday = calendar.component(.weekday, from: day)
        return weekday == 1 ? 7 : weekday - 1
    }

    /// The part of `start..<end` that falls on a day, in wall-clock minutes,
    /// or `nil` when it does not touch the day. A span that ends exactly at
    /// midnight does not touch the day it ends on. Shorter spans grow to
    /// `minimumLength`. An end before the start counts as no end.
    static func span(
        start: Date,
        end: Date?,
        on day: Date,
        calendar: Calendar,
        minimumLength: Int
    ) -> MonarchTimeGridSpan? {
        let day = calendar.startOfDay(for: day)
        let end = end.map { max($0, start) } ?? start
        let first = wallClock(start, calendar: calendar)
        let last = wallClock(end, calendar: calendar)
        if last.day < day || first.day > day { return nil }
        if last.day == day, last.minutes == 0, first.day != day { return nil }
        let top = first.day < day ? 0 : first.minutes
        var bottom = last.day > day ? minutesPerDay : last.minutes
        if bottom - top < minimumLength { bottom = min(minutesPerDay, top + minimumLength) }
        return MonarchTimeGridSpan(top: top, bottom: bottom)
    }

    /// Whether an all-day event covers a day. Its end is any time on its last day.
    static func covers(_ event: MonarchAgendaEvent, day: Date, calendar: Calendar) -> Bool {
        let first = calendar.startOfDay(for: event.start)
        let last = calendar.startOfDay(for: max(event.end ?? event.start, event.start))
        let day = calendar.startOfDay(for: day)
        return first <= day && day <= last
    }

    /// Side-by-side layout of overlapping spans: clusters of spans that
    /// overlap one another, each span in the first free column, and every
    /// span in a cluster sharing the cluster's column count.
    static func pack<Item>(_ items: [(item: Item, span: MonarchTimeGridSpan)]) -> [MonarchTimeGridPacked<Item>] {
        let sorted = items.enumerated().sorted { lhs, rhs in
            if lhs.element.span.top != rhs.element.span.top { return lhs.element.span.top < rhs.element.span.top }
            if lhs.element.span.bottom != rhs.element.span.bottom {
                return lhs.element.span.bottom > rhs.element.span.bottom
            }
            return lhs.offset < rhs.offset
        }
        var result: [MonarchTimeGridPacked<Item>] = []
        var cluster: [MonarchTimeGridPacked<Item>] = []
        var columnEnds: [Int] = []
        var clusterEnd = Int.min
        func flush() {
            for index in cluster.indices { cluster[index].columns = columnEnds.count }
            result += cluster
            cluster = []
            columnEnds = []
        }
        for (_, entry) in sorted {
            if !cluster.isEmpty, entry.span.top >= clusterEnd { flush() }
            let column: Int
            if let free = columnEnds.firstIndex(where: { $0 <= entry.span.top }) {
                column = free
                columnEnds[free] = entry.span.bottom
            } else {
                column = columnEnds.count
                columnEnds.append(entry.span.bottom)
            }
            cluster.append(MonarchTimeGridPacked(item: entry.item, span: entry.span, column: column, columns: 1))
            clusterEnd = max(clusterEnd, entry.span.bottom)
        }
        if !cluster.isEmpty { flush() }
        return result
    }

    /// The shaded spans of a day: the visible range outside its working hours.
    /// `nil` working hours shade nothing.
    static func shade(
        weekday: Int,
        workingHours: [MonarchTimeGridWorkingHours]?,
        range: MonarchTimeGridSpan
    ) -> [MonarchTimeGridSpan] {
        guard let workingHours else { return [] }
        let open = workingHours.filter { $0.weekday == weekday }.sorted { $0.start < $1.start }
        var result: [MonarchTimeGridSpan] = []
        var cursor = range.top
        for interval in open {
            if interval.start > cursor {
                result.append(MonarchTimeGridSpan(top: cursor, bottom: min(interval.start, range.bottom)))
            }
            cursor = max(cursor, interval.end)
        }
        if cursor < range.bottom { result.append(MonarchTimeGridSpan(top: cursor, bottom: range.bottom)) }
        return result.filter { $0.bottom > $0.top }
    }

    /// A span cut to the visible range, or `nil` when nothing of it is visible.
    static func clip(_ span: MonarchTimeGridSpan, to range: MonarchTimeGridSpan) -> MonarchTimeGridSpan? {
        let top = max(span.top, range.top)
        let bottom = min(span.bottom, range.bottom)
        return bottom > top ? MonarchTimeGridSpan(top: top, bottom: bottom) : nil
    }

    /// The columns of the grid, one per day.
    static func columns(
        days: [Date],
        events: [MonarchAgendaEvent],
        unavailable: [MonarchTimeGridUnavailable],
        workingHours: [MonarchTimeGridWorkingHours]?,
        range: MonarchTimeGridSpan,
        snapMinutes: Int,
        calendar: Calendar
    ) -> [MonarchTimeGridColumnModel] {
        let minimum = max(1, snapMinutes)
        return days.map { date in
            let day = calendar.startOfDay(for: date)
            let timed = events.filter { !$0.isAllDay }.compactMap {
                event -> (item: MonarchAgendaEvent, span: MonarchTimeGridSpan)? in
                guard
                    let span = span(
                        start: event.start,
                        end: event.end,
                        on: day,
                        calendar: calendar,
                        minimumLength: minimum
                    ),
                    let visible = clip(span, to: range)
                else { return nil }
                return (event, visible)
            }
            let blocks = unavailable.compactMap { block -> MonarchTimeGridPacked<MonarchTimeGridUnavailable>? in
                guard
                    let span = span(start: block.start, end: block.end, on: day, calendar: calendar, minimumLength: 1),
                    let visible = clip(span, to: range)
                else { return nil }
                return MonarchTimeGridPacked(item: block, span: visible, column: 0, columns: 1)
            }
            return MonarchTimeGridColumnModel(
                day: day,
                timed: pack(timed),
                allDay: events.filter { $0.isAllDay && covers($0, day: day, calendar: calendar) },
                unavailable: blocks,
                shade: shade(weekday: isoWeekday(day, calendar: calendar), workingHours: workingHours, range: range)
            )
        }
    }

    /// The minutes the grid scrolls to: the given value, else an hour before
    /// the earliest visible event, else 08:00; inside the visible range.
    static func scrollTarget(requested: Int?, columns: [MonarchTimeGridColumnModel], range: MonarchTimeGridSpan) -> Int
    {
        let earliest = columns.flatMap { $0.timed.map(\.span.top) }.min()
        let target = requested ?? earliest.map { max(0, $0 - 60) } ?? 8 * 60
        return min(max(target, range.top), range.bottom - 1)
    }

    /// The slot a tap at `offset` points below the top of a column falls in:
    /// snapped down to `snapMinutes`, and inside the visible range.
    static func slotMinutes(offset: CGFloat, minuteHeight: CGFloat, range: MonarchTimeGridSpan, snapMinutes: Int) -> Int
    {
        let snap = max(1, snapMinutes)
        guard minuteHeight > 0, offset.isFinite else { return range.top }
        let raw = Double(range.top) + Double(offset / minuteHeight)
        let snapped = Int((raw / Double(snap)).rounded(.down)) * snap
        return min(max(snapped, range.top), max(range.top, range.bottom - snap))
    }

    /// Where a moved event may start: inside the visible range, so the whole
    /// event stays on screen (an event longer than the range starts at its top).
    static func clampStart(_ start: Int, duration: Int, range: MonarchTimeGridSpan, snapMinutes: Int) -> Int {
        let latest = max(range.top, range.bottom - max(duration, snapMinutes))
        return max(range.top, min(latest, start))
    }

    /// The length of an event in minutes, at least one snap interval.
    static func duration(of event: MonarchAgendaEvent, snapMinutes: Int) -> Int {
        let end = max(event.end ?? event.start, event.start)
        return max(max(1, snapMinutes), Int((end.timeIntervalSince(event.start) / 60).rounded()))
    }

    /// The drag state for a finger at `minutes` in column `dayIndex`: the
    /// event's new start snapped and kept inside the range.
    static func drag(
        _ drag: MonarchTimeGridDrag,
        to dayIndex: Int,
        minutes: Double,
        range: MonarchTimeGridSpan,
        snapMinutes: Int
    ) -> MonarchTimeGridDrag {
        let snap = Double(max(1, snapMinutes))
        let snapped = Int(((minutes - Double(drag.grab)) / snap).rounded()) * Int(snap)
        var next = drag
        next.dayIndex = dayIndex
        next.startMinutes = clampStart(snapped, duration: drag.duration, range: range, snapMinutes: snapMinutes)
        return next
    }

    /// The move an event makes to `startMinutes` on `day`, keeping its length,
    /// or `nil` when it would start where it already does.
    static func move(
        _ event: MonarchAgendaEvent,
        to day: Date,
        startMinutes: Int,
        snapMinutes: Int,
        calendar: Calendar
    ) -> MonarchTimeGridMove? {
        let start = date(day: day, minutes: startMinutes, calendar: calendar)
        guard start != event.start else { return nil }
        let length = duration(of: event, snapMinutes: snapMinutes)
        return MonarchTimeGridMove(event: event, start: start, end: start.addingTimeInterval(TimeInterval(length * 60)))
    }

    /// The move one VoiceOver action makes: `minutes` earlier or later, or
    /// `days` columns over, inside the range and the columns. `nil` when the
    /// event cannot go further.
    static func nudge(
        _ event: MonarchAgendaEvent,
        minutes delta: Int = 0,
        days dayDelta: Int = 0,
        columns days: [Date],
        range: MonarchTimeGridSpan,
        snapMinutes: Int,
        calendar: Calendar
    ) -> MonarchTimeGridMove? {
        let now = wallClock(event.start, calendar: calendar)
        guard let index = days.firstIndex(where: { calendar.isDate($0, inSameDayAs: now.day) }) else { return nil }
        let target = index + dayDelta
        guard days.indices.contains(target) else { return nil }
        let length = duration(of: event, snapMinutes: snapMinutes)
        let start = clampStart(now.minutes + delta, duration: length, range: range, snapMinutes: snapMinutes)
        return move(
            event,
            to: calendar.startOfDay(for: days[target]),
            startMinutes: start,
            snapMinutes: snapMinutes,
            calendar: calendar
        )
    }

    /// The index of the column whose frame is nearest to `x` along the
    /// screen, from the columns' frames in one coordinate space. It does not
    /// depend on the layout direction.
    static func columnIndex(at x: CGFloat, frames: [Int: CGRect]) -> Int? {
        if let inside = frames.first(where: { $0.value.minX <= x && x < $0.value.maxX }) { return inside.key }
        return frames.min { abs($0.value.midX - x) < abs($1.value.midX - x) }?.key
    }
}
