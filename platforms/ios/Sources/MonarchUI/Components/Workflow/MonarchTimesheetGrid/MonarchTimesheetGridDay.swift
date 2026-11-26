import Foundation

/// One day of a ``MonarchTimesheetGrid``: a column of the grid at regular
/// width, a page at compact width.
@_spi(Experimental) public struct MonarchTimesheetGridDay: Sendable, Hashable, Identifiable {
    /// The day. Also the key of each row's `cells`, so pass the same value
    /// there (usually the start of the day in the app's calendar).
    public let date: Date
    /// The heading. `nil` writes the weekday and day of the month in the
    /// environment's locale, such as "Mon 12".
    public let label: String?
    /// Whether the day is today. The heading is marked and read as today.
    public let isToday: Bool
    /// A weekend or holiday. The column is shaded; entry is still allowed
    /// unless the day is locked.
    public let isNonWorking: Bool
    /// Whether the day cannot be edited (approved, payroll closed, leave).
    /// True whenever `lockReason` is set.
    public let isLocked: Bool
    /// A short reason the day is locked, shown under the heading and read
    /// with each cell, such as "Approved".
    public let lockReason: String?
    /// A short note under the heading, such as a holiday or leave name.
    public let note: String?
    /// The minutes expected that day, shown as "of 8h" under the day total.
    public let expectedMinutes: Int?

    /// The identity of the day: its date.
    public var id: Date { date }

    /// A day of the grid.
    ///
    /// - Parameters:
    ///   - date: The day, and the key of each row's `cells`.
    ///   - label: The heading. By default the weekday and day of the month.
    ///   - isToday: Whether the day is today.
    ///   - isNonWorking: Whether the day is a weekend or holiday.
    ///   - isLocked: Whether the day cannot be edited.
    ///   - lockReason: A short reason the day is locked. Setting it locks the day.
    ///   - note: A short note under the heading, such as a holiday name.
    ///   - expectedMinutes: The minutes expected that day.
    public init(
        _ date: Date,
        label: String? = nil,
        isToday: Bool = false,
        isNonWorking: Bool = false,
        isLocked: Bool = false,
        lockReason: String? = nil,
        note: String? = nil,
        expectedMinutes: Int? = nil
    ) {
        self.date = date
        self.label = label
        self.isToday = isToday
        self.isNonWorking = isNonWorking
        self.isLocked = isLocked || lockReason != nil
        self.lockReason = lockReason
        self.note = note
        self.expectedMinutes = expectedMinutes
    }
}
