import Foundation

/// One day of a ``MonarchCalendarHeatmap``. Days in the range without an
/// entry are "no data".
public struct MonarchCalendarHeatmapDay: Sendable, Hashable {
    /// The day. Any moment of it: the heatmap reads it as a calendar day in
    /// the environment's calendar and time zone.
    public let date: Date
    /// The value; `nil` for no data.
    public let value: Double?
    /// Shown instead of the formatted value, such as "7h 30m", already localised.
    public let valueLabel: String?
    /// A short note after the value, such as "Holiday", already localised.
    public let note: String?
    /// A non-working or out-of-scope day, such as a weekend: drawn dashed
    /// unless it has a value above zero.
    public let isInactive: Bool
    /// Draws a small dot in the cell to flag the day. Say why in `note`.
    public let hasMarker: Bool

    /// A day.
    ///
    /// - Parameters:
    ///   - date: The day, read in the environment's calendar.
    ///   - value: The value; `nil` for no data.
    ///   - valueLabel: Shown instead of the formatted value, already localised.
    ///   - note: A short note after the value, already localised.
    ///   - isInactive: A non-working or out-of-scope day.
    ///   - hasMarker: Draws a dot to flag the day.
    public init(
        _ date: Date,
        value: Double?,
        valueLabel: String? = nil,
        note: String? = nil,
        isInactive: Bool = false,
        hasMarker: Bool = false
    ) {
        self.date = date
        self.value = value
        self.valueLabel = valueLabel
        self.note = note
        self.isInactive = isInactive
        self.hasMarker = hasMarker
    }
}
