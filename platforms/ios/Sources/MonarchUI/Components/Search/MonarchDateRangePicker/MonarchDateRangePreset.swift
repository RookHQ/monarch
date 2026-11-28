import Foundation

/// A quick pick for ``MonarchDateRangePicker``, such as "Last 7 days".
///
/// ```swift
/// [
///     .lastDays(7, label: "Last 7 days"),
///     .month(0, label: "This month"),
///     .month(-1, label: "Last month"),
/// ]
/// ```
///
/// Relative presets are resolved against the picker's `today`, in the
/// environment's calendar.
public struct MonarchDateRangePreset: Identifiable, Hashable, Sendable {
    /// How the preset finds its range.
    enum Kind: Hashable, Sendable {
        case range(ClosedRange<Date>)
        case lastDays(Int)
        case nextDays(Int)
        case month(Int)
    }

    /// The text of the preset, already localised.
    public let label: String
    let kind: Kind

    /// The identity of the preset: its label.
    public var id: String { label }

    /// A fixed range.
    ///
    /// - Parameters:
    ///   - range: The first and last day, inclusive.
    ///   - label: The text of the preset.
    /// - Returns: The preset.
    public static func range(_ range: ClosedRange<Date>, label: String) -> MonarchDateRangePreset {
        MonarchDateRangePreset(label: label, kind: .range(range))
    }

    /// The days ending today: `lastDays(7, …)` is today and the six days before.
    ///
    /// - Parameters:
    ///   - count: The number of days, at least one.
    ///   - label: The text of the preset.
    /// - Returns: The preset.
    public static func lastDays(_ count: Int, label: String) -> MonarchDateRangePreset {
        MonarchDateRangePreset(label: label, kind: .lastDays(count))
    }

    /// The days starting today: `nextDays(7, …)` is today and the six days after.
    ///
    /// - Parameters:
    ///   - count: The number of days, at least one.
    ///   - label: The text of the preset.
    /// - Returns: The preset.
    public static func nextDays(_ count: Int, label: String) -> MonarchDateRangePreset {
        MonarchDateRangePreset(label: label, kind: .nextDays(count))
    }

    /// A whole calendar month: `0` is this month, `-1` the previous one.
    ///
    /// - Parameters:
    ///   - offset: The month counted from this month.
    ///   - label: The text of the preset.
    /// - Returns: The preset.
    public static func month(_ offset: Int, label: String) -> MonarchDateRangePreset {
        MonarchDateRangePreset(label: label, kind: .month(offset))
    }
}
