import Foundation

/// The period chosen in ``MonarchPeriodSelector``: a preset, and the days of
/// a custom period.
///
/// ```swift
/// @State private var period = MonarchPeriod(.last30Days)
/// let days = period.resolved(today: .now, calendar: .current) // for the query
/// ```
public struct MonarchPeriod: Sendable, Hashable {
    /// The preset.
    public var preset: MonarchPeriodPreset
    /// The first and last day of a custom period, both included. Ignored for other presets.
    public var range: ClosedRange<Date>?

    /// A period.
    ///
    /// - Parameters:
    ///   - preset: The preset.
    ///   - range: The first and last day of a custom period, both included.
    public init(_ preset: MonarchPeriodPreset, range: ClosedRange<Date>? = nil) {
        self.preset = preset
        self.range = range
    }

    /// The days the period covers, both included, as the first moment of the
    /// first and last day; `nil` for a custom period with no range yet.
    ///
    /// Run the query with this on the device, or compute the same on the
    /// server from the preset's raw value.
    ///
    /// - Parameters:
    ///   - today: Today. Pass the workspace's day to match the server.
    ///   - calendar: The calendar, its time zone and first weekday.
    /// - Returns: The first and last day.
    public func resolved(today: Date, calendar: Calendar) -> ClosedRange<Date>? {
        let day = calendar.startOfDay(for: today)
        if let count = preset.rollingDays {
            return Self.adding(-(count - 1), .day, to: day, calendar: calendar)...day
        }
        switch preset {
        case .today:
            return day...day
        case .yesterday:
            let yesterday = Self.adding(-1, .day, to: day, calendar: calendar)
            return yesterday...yesterday
        case .thisWeek:
            let start = calendar.dateInterval(of: .weekOfYear, for: day)?.start ?? day
            return start...day
        case .thisMonth:
            return Self.startOfMonth(day, calendar: calendar)...day
        case .lastMonth:
            let first = Self.adding(-1, .month, to: Self.startOfMonth(day, calendar: calendar), calendar: calendar)
            return first...Self.lastDay(ofMonthsFrom: first, count: 1, calendar: calendar)
        case .thisQuarter:
            return Self.startOfQuarter(day, calendar: calendar)...day
        case .lastQuarter:
            let first = Self.adding(-3, .month, to: Self.startOfQuarter(day, calendar: calendar), calendar: calendar)
            return first...Self.lastDay(ofMonthsFrom: first, count: 3, calendar: calendar)
        case .thisYear:
            let start = calendar.dateInterval(of: .year, for: day)?.start ?? day
            return start...day
        default:
            return range
        }
    }

    private static func adding(_ value: Int, _ component: Calendar.Component, to date: Date, calendar: Calendar) -> Date
    {
        calendar.date(byAdding: component, value: value, to: date) ?? date
    }

    private static func startOfMonth(_ day: Date, calendar: Calendar) -> Date {
        calendar.dateInterval(of: .month, for: day)?.start ?? day
    }

    private static func startOfQuarter(_ day: Date, calendar: Calendar) -> Date {
        let month = calendar.component(.month, from: day)
        let back = (month - 1) % 3
        return adding(-back, .month, to: startOfMonth(day, calendar: calendar), calendar: calendar)
    }

    /// The last day of `count` months starting at the first day `first`.
    private static func lastDay(ofMonthsFrom first: Date, count: Int, calendar: Calendar) -> Date {
        adding(-1, .day, to: adding(count, .month, to: first, calendar: calendar), calendar: calendar)
    }
}
