/// The problems in a ``MonarchBusinessHoursValue``, from
/// ``MonarchBusinessHoursValue/validate(isNameRequired:isOpenRequired:)``.
public struct MonarchBusinessHoursErrors: Hashable, Sendable {
    /// A name is required and the name is empty.
    public var isNameMissing: Bool
    /// The weekly hours problems by ISO weekday, from ``MonarchWeeklyHoursInterval/validate(_:)``.
    public var hours: [Int: MonarchWeeklyHoursError]
    /// An open interval is required and the week has none.
    public var isOpenIntervalMissing: Bool
    /// The problem of each holiday or special-hours date, by its index in `exceptions`.
    public var exceptions: [Int: MonarchBusinessHoursExceptionError]
    /// The last effective day is before the first.
    public var isEffectiveOrderInvalid: Bool

    /// Whether there is no problem at all.
    public var isValid: Bool {
        !isNameMissing && hours.isEmpty && !isOpenIntervalMissing && exceptions.isEmpty && !isEffectiveOrderInvalid
    }

    /// A set of problems. With no arguments, a valid result.
    ///
    /// - Parameters:
    ///   - isNameMissing: A name is required and the name is empty.
    ///   - hours: The weekly hours problems by ISO weekday.
    ///   - isOpenIntervalMissing: An open interval is required and the week has none.
    ///   - exceptions: The problem of each date, by its index.
    ///   - isEffectiveOrderInvalid: The last effective day is before the first.
    public init(
        isNameMissing: Bool = false,
        hours: [Int: MonarchWeeklyHoursError] = [:],
        isOpenIntervalMissing: Bool = false,
        exceptions: [Int: MonarchBusinessHoursExceptionError] = [:],
        isEffectiveOrderInvalid: Bool = false
    ) {
        self.isNameMissing = isNameMissing
        self.hours = hours
        self.isOpenIntervalMissing = isOpenIntervalMissing
        self.exceptions = exceptions
        self.isEffectiveOrderInvalid = isEffectiveOrderInvalid
    }
}
