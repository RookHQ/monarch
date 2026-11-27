import Foundation

/// A non-working day for everyone in a ``MonarchTeamCalendar``, such as a
/// public holiday. Its column is shaded, its name is read with the day and a
/// tap on the day shows the name.
///
/// ```swift
/// MonarchTeamCalendarHoliday("Thanksgiving", date: thanksgiving)
/// ```
@_spi(Experimental) public struct MonarchTeamCalendarHoliday: Hashable, Sendable {
    /// The holiday's name, already localised.
    public var name: String
    /// Any time on the day, read in the calendar's time zone.
    public var date: Date

    /// A holiday.
    ///
    /// - Parameters:
    ///   - name: The holiday's name, already localised.
    ///   - date: Any time on the day.
    public init(_ name: String, date: Date) {
        self.name = name
        self.date = date
    }
}
