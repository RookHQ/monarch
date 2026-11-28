/// A named period of ``MonarchPeriodSelector``. Rolling periods, such as
/// ``last7Days``, end today and count today; "this" periods run from their
/// first day to today.
///
/// The raw values are the web's preset names, so a server can share them.
public enum MonarchPeriodPreset: String, Sendable, Hashable, CaseIterable, Identifiable {
    /// Today.
    case today
    /// Yesterday.
    case yesterday
    /// The 7 days ending today.
    case last7Days = "7d"
    /// The 14 days ending today.
    case last14Days = "14d"
    /// The 30 days ending today.
    case last30Days = "30d"
    /// The 90 days ending today.
    case last90Days = "90d"
    /// The 180 days ending today.
    case last180Days = "180d"
    /// The 365 days ending today, named "Last 12 months".
    case last365Days = "365d"
    /// From the first day of this week, in the calendar's first weekday, to today.
    case thisWeek = "this-week"
    /// From the first day of this month to today.
    case thisMonth = "this-month"
    /// The whole previous month.
    case lastMonth = "last-month"
    /// From the first day of this quarter to today.
    case thisQuarter = "this-quarter"
    /// The whole previous quarter.
    case lastQuarter = "last-quarter"
    /// From the first day of this year to today.
    case thisYear = "this-year"
    /// Dates the person chooses.
    case custom

    /// The identity of the preset: its raw value.
    public var id: String { rawValue }

    /// The number of days of a rolling preset, or `nil`.
    var rollingDays: Int? {
        switch self {
        case .last7Days: 7
        case .last14Days: 14
        case .last30Days: 30
        case .last90Days: 90
        case .last180Days: 180
        case .last365Days: 365
        default: nil
        }
    }
}
