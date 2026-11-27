/// Replacements for the built-in strings of ``MonarchBusinessHoursEditor``.
/// A property that is `nil` keeps the built-in English string. `position` counts dates from 1.
/// The weekly hours take their own ``MonarchWeeklyHoursEditorLabels``.
public struct MonarchBusinessHoursEditorLabels: Sendable {
    /// The heading of the name field. Built in: "Name".
    public var name: String?
    /// The placeholder of the name field. Built in: "Office hours".
    public var namePlaceholder: String?
    /// The message when a required name is empty. Built in: "Enter a name.".
    public var nameRequired: String?
    /// The label of the time zone picker. Built in: "Time zone".
    public var timeZone: String?
    /// The note under the time zone. Built in: "Hours below are in this zone, including daylight-saving changes.".
    public var timeZoneDescription: String?
    /// The message when an open interval is required and there is none. Built in: "Open at least one interval.".
    public var noOpenHours: String?
    /// The label of the first effective day. Built in: "Effective from".
    public var effectiveFrom: String?
    /// The label of the last effective day. Built in: "Effective until".
    public var effectiveTo: String?
    /// The message when the last effective day is before the first. Built in: "The end date must be on or after the start date.".
    public var effectiveOrder: String?
    /// The heading of the holidays. Built in: "Holidays and exceptions".
    public var exceptions: String?
    /// The note under the holidays heading. Built in: "Dates that replace the weekly hours: closed all day, or open at other times.".
    public var exceptionsDescription: String?
    /// Shown when there is no date. Built in: "No holidays or exceptions.".
    public var noExceptions: String?
    /// The title of the add button. Built in: "Add date".
    public var addException: String?
    /// The label of a row's date, from its position. Built in: "Date 1".
    public var exceptionDate: (@Sendable (_ position: Int) -> String)?
    /// The spoken name of a row's name field, from its position. Built in: "Name of date 1".
    public var exceptionName: (@Sendable (_ position: Int) -> String)?
    /// The placeholder of a row's name field. Built in: "Public holiday".
    public var exceptionNamePlaceholder: String?
    /// The label of a row's closed or open menu, from its position. Built in: "Hours on date 1".
    public var exceptionKind: (@Sendable (_ position: Int) -> String)?
    /// The menu option for a date closed all day. Built in: "Closed all day".
    public var closed: String?
    /// The menu option for a date open at other times. Built in: "Open at other times".
    public var specialHours: String?
    /// The label of a row's opening time, from its position. Built in: "Date 1 opens".
    public var exceptionStart: (@Sendable (_ position: Int) -> String)?
    /// The label of a row's closing time, from its position. Built in: "Date 1 closes".
    public var exceptionEnd: (@Sendable (_ position: Int) -> String)?
    /// The visible title of a row's remove button and swipe action. Built in: "Remove".
    public var remove: String?
    /// The spoken name of a row's remove button, from its position and trimmed name (empty when unnamed). Built in: "Remove Christmas Day", or "Remove date 1" without a name.
    public var removeException: (@Sendable (_ position: Int, _ name: String) -> String)?
    /// The message under a row with a problem. Built in: "Choose a date.", "Give the date a name.", "This date is listed twice." and "The closing time must be after the opening time.".
    public var exceptionError: (@Sendable (MonarchBusinessHoursExceptionError) -> String)?
    /// The title of the default switch. Built in: "Default".
    public var defaultTitle: String?
    /// The line under the default switch. Built in: "Used wherever no other hours are chosen.".
    public var defaultDescription: String?
    /// The closing time at the end of the day on a 12-hour clock. Built in: "End of day". A 24-hour clock shows "24:00".
    public var endOfDay: String?
    /// The button of a date that is not chosen. Built in: "Choose a date".
    public var chooseDate: String?
    /// The title of an effective day's clear button, shown as an icon. Built in: "Clear".
    public var clearDate: String?
    /// The spoken name of the clear button, from the date's label. Built in: "Clear Effective from".
    public var clearDateItem: (@Sendable (_ label: String) -> String)?
    /// A date that is not chosen, in the read-only view. Built in: "Not set".
    public var noDate: String?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        name: String? = nil,
        namePlaceholder: String? = nil,
        nameRequired: String? = nil,
        timeZone: String? = nil,
        timeZoneDescription: String? = nil,
        noOpenHours: String? = nil,
        effectiveFrom: String? = nil,
        effectiveTo: String? = nil,
        effectiveOrder: String? = nil,
        exceptions: String? = nil,
        exceptionsDescription: String? = nil,
        noExceptions: String? = nil,
        addException: String? = nil,
        exceptionDate: (@Sendable (_ position: Int) -> String)? = nil,
        exceptionName: (@Sendable (_ position: Int) -> String)? = nil,
        exceptionNamePlaceholder: String? = nil,
        exceptionKind: (@Sendable (_ position: Int) -> String)? = nil,
        closed: String? = nil,
        specialHours: String? = nil,
        exceptionStart: (@Sendable (_ position: Int) -> String)? = nil,
        exceptionEnd: (@Sendable (_ position: Int) -> String)? = nil,
        remove: String? = nil,
        removeException: (@Sendable (_ position: Int, _ name: String) -> String)? = nil,
        exceptionError: (@Sendable (MonarchBusinessHoursExceptionError) -> String)? = nil,
        defaultTitle: String? = nil,
        defaultDescription: String? = nil,
        endOfDay: String? = nil,
        chooseDate: String? = nil,
        clearDate: String? = nil,
        clearDateItem: (@Sendable (_ label: String) -> String)? = nil,
        noDate: String? = nil
    ) {
        self.name = name
        self.namePlaceholder = namePlaceholder
        self.nameRequired = nameRequired
        self.timeZone = timeZone
        self.timeZoneDescription = timeZoneDescription
        self.noOpenHours = noOpenHours
        self.effectiveFrom = effectiveFrom
        self.effectiveTo = effectiveTo
        self.effectiveOrder = effectiveOrder
        self.exceptions = exceptions
        self.exceptionsDescription = exceptionsDescription
        self.noExceptions = noExceptions
        self.addException = addException
        self.exceptionDate = exceptionDate
        self.exceptionName = exceptionName
        self.exceptionNamePlaceholder = exceptionNamePlaceholder
        self.exceptionKind = exceptionKind
        self.closed = closed
        self.specialHours = specialHours
        self.exceptionStart = exceptionStart
        self.exceptionEnd = exceptionEnd
        self.remove = remove
        self.removeException = removeException
        self.exceptionError = exceptionError
        self.defaultTitle = defaultTitle
        self.defaultDescription = defaultDescription
        self.endOfDay = endOfDay
        self.chooseDate = chooseDate
        self.clearDate = clearDate
        self.clearDateItem = clearDateItem
        self.noDate = noDate
    }
}
