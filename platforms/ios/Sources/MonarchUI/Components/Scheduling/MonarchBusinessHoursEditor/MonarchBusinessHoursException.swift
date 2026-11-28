import Foundation

/// A date that replaces the weekly hours of a ``MonarchBusinessHoursValue``: a holiday, closed
/// all day, or a date with special hours.
public struct MonarchBusinessHoursException: Hashable, Sendable {
    /// The date, as the start of that day in UTC; `nil` while the person has not chosen one.
    ///
    /// The editor writes midnight UTC, the same instant as Android's UTC-midnight milliseconds,
    /// so the day does not move when the calendar's time zone changes. Any time on a day is
    /// read as that day in UTC.
    public var date: Date?
    /// What the date is, such as "Christmas Day".
    public var name: String
    /// The open intervals that day. Empty means closed all day. The editor edits the first one
    /// and keeps the rest.
    public var intervals: [MonarchBusinessHoursInterval]

    /// An exception.
    ///
    /// - Parameters:
    ///   - date: The day, as the start of the day in UTC, or `nil` while it is not chosen.
    ///   - name: What the date is, such as "Christmas Day".
    ///   - intervals: The open intervals that day; empty (the default) means closed all day.
    public init(date: Date?, name: String, intervals: [MonarchBusinessHoursInterval] = []) {
        self.date = date
        self.name = name
        self.intervals = intervals
    }
}
