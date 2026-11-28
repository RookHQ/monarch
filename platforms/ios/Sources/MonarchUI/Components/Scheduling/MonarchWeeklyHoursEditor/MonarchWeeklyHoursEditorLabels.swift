/// Replacements for the built-in strings of ``MonarchWeeklyHoursEditor``.
/// A property that is `nil` keeps the built-in English string. `day` is the localised name of
/// the weekday, such as "Monday".
public struct MonarchWeeklyHoursEditorLabels: Sendable {
    /// The heading of the editor. Built in: "Weekly hours".
    public var title: String?
    /// Shown under a day that is switched off. Built in: "Unavailable".
    public var unavailable: String?
    /// The spoken name of an interval's start menu. Built in: "Start".
    public var start: String?
    /// The spoken name of an interval's end menu. Built in: "End".
    public var end: String?
    /// The spoken name of one interval, from the day and its position. Built in: "Monday interval 1".
    public var interval: (@Sendable (_ day: String, _ position: Int) -> String)?
    /// The visible title of a day's add button. Built in: "Add interval".
    public var add: String?
    /// The spoken name of the add button, from the day. Built in: "Add interval to Monday".
    public var addItem: (@Sendable (_ day: String) -> String)?
    /// The title of an interval's remove button, shown as an icon. Built in: "Remove".
    public var remove: String?
    /// The spoken name of the remove button, from the day and the interval's position.
    /// Built in: "Remove Monday interval 1".
    public var removeItem: (@Sendable (_ day: String, _ position: Int) -> String)?
    /// The visible title of a day's copy button. Built in: "Copy to all".
    public var copyToAll: String?
    /// The spoken name of the copy button, from the day. Built in: "Copy to all days from Monday".
    public var copyToAllItem: (@Sendable (_ day: String) -> String)?
    /// Announced after a copy, from the day. Built in: "Monday hours copied to every day."
    public var copied: (@Sendable (_ day: String) -> String)?
    /// One interval as text in the read-only view, from its start and end. Built in: "9:00 AM – 5:00 PM".
    public var range: (@Sendable (_ start: String, _ end: String) -> String)?
    /// The end of the day on a 12-hour clock. Built in: "Midnight". A 24-hour clock shows "24:00".
    public var midnight: String?
    /// The message under a day with a time outside the day. Built in: "Times must fall within the day".
    public var rangeError: String?
    /// The message under a day with an interval that ends before it starts.
    /// Built in: "Each interval must end after it starts".
    public var orderError: String?
    /// The message under a day with overlapping intervals. Built in: "Intervals must not overlap".
    public var overlapError: String?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        title: String? = nil,
        unavailable: String? = nil,
        start: String? = nil,
        end: String? = nil,
        interval: (@Sendable (_ day: String, _ position: Int) -> String)? = nil,
        add: String? = nil,
        addItem: (@Sendable (_ day: String) -> String)? = nil,
        remove: String? = nil,
        removeItem: (@Sendable (_ day: String, _ position: Int) -> String)? = nil,
        copyToAll: String? = nil,
        copyToAllItem: (@Sendable (_ day: String) -> String)? = nil,
        copied: (@Sendable (_ day: String) -> String)? = nil,
        range: (@Sendable (_ start: String, _ end: String) -> String)? = nil,
        midnight: String? = nil,
        rangeError: String? = nil,
        orderError: String? = nil,
        overlapError: String? = nil
    ) {
        self.title = title
        self.unavailable = unavailable
        self.start = start
        self.end = end
        self.interval = interval
        self.add = add
        self.addItem = addItem
        self.remove = remove
        self.removeItem = removeItem
        self.copyToAll = copyToAll
        self.copyToAllItem = copyToAllItem
        self.copied = copied
        self.range = range
        self.midnight = midnight
        self.rangeError = rangeError
        self.orderError = orderError
        self.overlapError = overlapError
    }
}
