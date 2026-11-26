import Foundation

/// The value of a ``MonarchBusinessHoursEditor``: a named calendar of weekly hours in a time
/// zone, with holidays and special-hours dates.
///
/// Plain data that goes to a server unchanged, the same shape as on the web and Android:
/// `{ timeZone, hours, exceptions, name, isDefault, effectiveFrom, effectiveTo }`.
/// Dates are the start of the day in UTC (see ``MonarchBusinessHoursException/date``).
public struct MonarchBusinessHoursValue: Hashable, Sendable {
    /// The IANA id of the zone the hours are in, such as "Europe/London".
    public var timeZone: String
    /// The weekly hours: ISO weekdays and minutes from midnight, as ``MonarchWeeklyHoursEditor`` takes them.
    public var hours: [MonarchWeeklyHoursInterval]
    /// Holidays and special-hours dates, in the order arranged.
    public var exceptions: [MonarchBusinessHoursException]
    /// The calendar's name, such as "Support hours".
    public var name: String
    /// Whether this is the workspace's default calendar.
    public var isDefault: Bool
    /// The first day these hours apply, as the start of the day in UTC, or `nil` for no limit.
    public var effectiveFrom: Date?
    /// The last day these hours apply, as the start of the day in UTC, or `nil` for no limit.
    public var effectiveTo: Date?

    /// A business-hours value.
    ///
    /// - Parameters:
    ///   - timeZone: The IANA id of the zone the hours are in.
    ///   - hours: The weekly hours.
    ///   - exceptions: Holidays and special-hours dates.
    ///   - name: The calendar's name.
    ///   - isDefault: Whether this is the workspace's default calendar.
    ///   - effectiveFrom: The first day these hours apply, or `nil`.
    ///   - effectiveTo: The last day these hours apply, or `nil`.
    public init(
        timeZone: String,
        hours: [MonarchWeeklyHoursInterval] = [],
        exceptions: [MonarchBusinessHoursException] = [],
        name: String = "",
        isDefault: Bool = false,
        effectiveFrom: Date? = nil,
        effectiveTo: Date? = nil
    ) {
        self.timeZone = timeZone
        self.hours = hours
        self.exceptions = exceptions
        self.name = name
        self.isDefault = isDefault
        self.effectiveFrom = effectiveFrom
        self.effectiveTo = effectiveTo
    }

    /// Checks the value: weekly hours (range, order, overlap), each date (chosen, named, not
    /// listed twice, special hours that open before they close and fall within the day), the
    /// effective days in order, and optionally a name and at least one open interval.
    ///
    /// Days are compared in UTC. The editor shows these problems; the app calls this when
    /// saving and decides whether to block it.
    ///
    /// - Parameters:
    ///   - isNameRequired: A name that is empty or only spaces is a problem.
    ///   - isOpenRequired: A week without an open interval is a problem.
    /// - Returns: The problems; ``MonarchBusinessHoursErrors/isValid`` is `true` when there is none.
    public func validate(isNameRequired: Bool = false, isOpenRequired: Bool = false) -> MonarchBusinessHoursErrors {
        let calendar = MonarchBusinessHoursEditorTokens.dayCalendar
        let days = exceptions.map { $0.date.map { calendar.startOfDay(for: $0) } }
        var exceptionErrors: [Int: MonarchBusinessHoursExceptionError] = [:]
        for (index, exception) in exceptions.enumerated() {
            if days[index] == nil {
                exceptionErrors[index] = .date
            } else if exception.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                exceptionErrors[index] = .name
            } else if days.firstIndex(of: days[index]) != index {
                exceptionErrors[index] = .duplicate
            } else if exception.intervals.contains(where: { $0.end <= $0.start || $0.start < 0 || $0.end > 1440 }) {
                exceptionErrors[index] = .hours
            }
        }
        var isEffectiveOrderInvalid = false
        if let from = effectiveFrom, let to = effectiveTo {
            isEffectiveOrderInvalid = calendar.startOfDay(for: to) < calendar.startOfDay(for: from)
        }
        return MonarchBusinessHoursErrors(
            isNameMissing: isNameRequired && name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            hours: MonarchWeeklyHoursInterval.validate(hours),
            isOpenIntervalMissing: isOpenRequired && !hours.contains { $0.end > $0.start },
            exceptions: exceptionErrors,
            isEffectiveOrderInvalid: isEffectiveOrderInvalid
        )
    }
}
