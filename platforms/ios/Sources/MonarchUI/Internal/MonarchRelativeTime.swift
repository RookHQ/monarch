import Foundation
import SwiftUI

/// The one relative-time rule of Monarch, on every platform: the rule of the
/// web `RelativeTime` component ("5 minutes ago", "yesterday", "in 2 hours").
///
/// 1. Take the signed distance in seconds, `date - now`.
/// 2. Walk the units in order. For each, round the distance in that unit half
///    up (JavaScript `Math.round`), and keep the first unit whose rounded
///    value stays below its rollover: second (< 60), minute (< 60), hour
///    (< 24), day (< 7), week (< 4.345), month (< 12), year (no limit).
///    So 59.6 minutes is "1 hour ago" and 6.6 days is "last week".
/// 3. Write it with the locale's words where it has them: 0 is "now", ±1 day
///    "yesterday" or "tomorrow", ±1 week, month or year "last week" or
///    "next month", ±2 days the locale's word where it has one (German
///    "vorgestern"; English has none). Everything else is a number.
/// 4. Never switch to a date by itself. A component that shows a date beyond
///    some age decides with ``showsAbsolute(_:now:absoluteAfter:)``.
/// 5. Redraw on the web's `live` interval: ``refreshInterval(for:now:)``.
///
/// Android implements the same rule in `internal/MonarchRelativeTime.kt`.
enum MonarchRelativeTime {
    /// A unit of the rule: its length in seconds and the rounded value at
    /// which the next unit takes over.
    enum Unit: CaseIterable, Sendable {
        case second, minute, hour, day, week, month, year

        /// The length of the unit, in seconds (a month is 30.4375 days, a
        /// year 365.25 days, as on the web).
        var seconds: Double {
            switch self {
            case .second: 1
            case .minute: 60
            case .hour: 3600
            case .day: 86400
            case .week: 604_800
            case .month: 2_629_800
            case .year: 31_557_600
            }
        }

        /// The rounded value at which the next unit takes over.
        var rollover: Double {
            switch self {
            case .second, .minute: 60
            case .hour: 24
            case .day: 7
            case .week: 4.345
            case .month: 12
            case .year: .infinity
            }
        }

        /// `value` of this unit as date components, for the formatter.
        func components(_ value: Int) -> DateComponents {
            switch self {
            case .second: DateComponents(second: value)
            case .minute: DateComponents(minute: value)
            case .hour: DateComponents(hour: value)
            case .day: DateComponents(day: value)
            case .week: DateComponents(weekOfMonth: value)
            case .month: DateComponents(month: value)
            case .year: DateComponents(year: value)
            }
        }
    }

    /// A signed value in a unit: negative in the past, positive in the future.
    struct Parts: Equatable, Sendable {
        /// The rounded value.
        let value: Int
        /// The unit.
        let unit: Unit
    }

    /// The unit and signed value of `date` relative to `now`, by the rule.
    static func parts(_ date: Date, now: Date) -> Parts {
        let seconds = date.timeIntervalSince(now)
        for unit in Unit.allCases {
            let value = jsRound(seconds / unit.seconds)
            if abs(value) < unit.rollover { return Parts(value: Int(value), unit: unit) }
        }
        return Parts(value: Int(jsRound(seconds / Unit.year.seconds)), unit: .year)
    }

    /// "5 minutes ago", "yesterday", or "in 2 hours", in `locale`.
    ///
    /// - Parameters:
    ///   - date: The moment to describe.
    ///   - now: The moment it is described from. Passed in, so tests are deterministic.
    ///   - locale: The locale of the words.
    ///   - calendar: The calendar of the formatter.
    ///   - allowsFuture: Whether a later `date` reads as "in …". When `false`,
    ///     a time in the future (a clock that runs ahead) reads as now.
    ///   - unitsStyle: Full words ("5 minutes ago") or short ("5 min. ago").
    ///   - isNumeric: Always a number ("1 day ago"), the web `numeric: 'always'`.
    /// - Returns: The relative time.
    static func string(
        _ date: Date,
        now: Date,
        locale: Locale,
        calendar: Calendar = .autoupdatingCurrent,
        allowsFuture: Bool = false,
        unitsStyle: RelativeDateTimeFormatter.UnitsStyle = .full,
        isNumeric: Bool = false
    ) -> String {
        let parts = parts(allowsFuture ? date : min(date, now), now: now)
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = locale
        var calendar = calendar
        calendar.locale = locale
        formatter.calendar = calendar
        formatter.dateTimeStyle = isNumeric ? .numeric : .named
        formatter.unitsStyle = unitsStyle
        return formatter.localizedString(from: parts.unit.components(parts.value))
    }

    /// Whether a component shows the absolute date instead: `date` is more
    /// than `absoluteAfter` seconds from `now`. `nil` never switches, which
    /// is the default of the rule.
    static func showsAbsolute(_ date: Date, now: Date, absoluteAfter: TimeInterval?) -> Bool {
        guard let absoluteAfter else { return false }
        return abs(now.timeIntervalSince(date)) > absoluteAfter
    }

    /// How often a relative time of `date` is redrawn at `now`, the web
    /// `live` interval: 5 seconds in the first minute, 30 seconds up to an
    /// hour, 5 minutes up to a day, then hourly.
    static func refreshInterval(for date: Date, now: Date) -> TimeInterval {
        let age = abs(now.timeIntervalSince(date))
        if age < 60 { return 5 }
        if age < 3600 { return 30 }
        if age < 86400 { return 300 }
        return 3600
    }

    /// The interval for a screen that shows several times: the one the
    /// freshest of them needs. Hourly when there is none.
    static func refreshInterval(for dates: [Date?], now: Date) -> TimeInterval {
        dates.compactMap { $0 }.map { refreshInterval(for: $0, now: now) }.min() ?? 3600
    }

    /// JavaScript `Math.round`: halves go up, towards positive infinity (-2.5 is -2).
    private static func jsRound(_ value: Double) -> Double {
        (value + 0.5).rounded(.down)
    }
}

/// The redraw times of relative times: each next time is the current one
/// plus the ``MonarchRelativeTime/refreshInterval(for:now:)`` the freshest
/// time needs then, so a screen redraws often while a time is new and
/// rarely once it is old.
struct MonarchRelativeTimeSchedule: TimelineSchedule {
    /// The times shown.
    let dates: [Date]

    func entries(from startDate: Date, mode: TimelineScheduleMode) -> Entries {
        Entries(cursor: startDate, dates: dates, isLowFrequency: mode == .lowFrequency)
    }

    /// The redraw times, one after another.
    struct Entries: Sequence, IteratorProtocol {
        var cursor: Date
        let dates: [Date]
        let isLowFrequency: Bool

        mutating func next() -> Date? {
            let current = cursor
            var interval = MonarchRelativeTime.refreshInterval(for: dates, now: current)
            // The always-on display redraws at most once a minute.
            if isLowFrequency { interval = Swift.max(interval, 60) }
            cursor = current.addingTimeInterval(interval)
            return current
        }
    }
}

/// Redraws its content with the current time on the relative-time schedule,
/// so relative times ("5 minutes ago") stay true while a screen is open.
///
/// It is SwiftUI's `TimelineView` on ``MonarchRelativeTimeSchedule``: one
/// clock, no timers in components, and nothing runs while the view is off
/// screen. Pass `now` to freeze the time, for tests and previews.
struct MonarchRelativeTimeClock<Content: View>: View {
    private let dates: [Date]
    private let fixedNow: Date?
    private let content: (Date) -> Content

    @Environment(\.monarchNow) private var clockNow

    /// A clock.
    ///
    /// - Parameters:
    ///   - dates: The times the content shows; the freshest sets the pace.
    ///     With none, the clock redraws hourly.
    ///   - now: A fixed time to show instead of the live clock.
    ///   - content: The content for a time.
    init(dates: [Date?] = [], now: Date? = nil, @ViewBuilder content: @escaping (Date) -> Content) {
        self.dates = dates.compactMap { $0 }
        self.fixedNow = now
        self.content = content
    }

    var body: some View {
        if let fixedNow = fixedNow ?? clockNow {
            content(fixedNow)
        } else {
            TimelineView(MonarchRelativeTimeSchedule(dates: dates)) { context in
                content(context.date)
            }
        }
    }
}
