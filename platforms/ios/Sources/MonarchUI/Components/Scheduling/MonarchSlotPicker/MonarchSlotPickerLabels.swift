import Foundation

/// Replacements for the built-in strings of ``MonarchSlotPicker``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchSlotPickerLabels: Sendable {
    /// Shown before a day is chosen. Built in: "Choose a day to see available times."
    public var chooseDay: String?
    /// Shown when the day has no times. Built in: "No times are available on this day. Try another day."
    public var empty: String?
    /// Announced while times load. Built in: "Loading available times".
    public var loading: String?
    /// Announced when times arrive, from the number of open times and the day.
    /// Built in: "3 times on Wednesday, November 4".
    public var loaded: (@Sendable (Int, String) -> String)?
    /// The name of the group of times, from the day. Built in: "Available times on Wednesday, November 4".
    public var times: (@Sendable (String) -> String)?
    /// Under a time with limited places. Built in: "2 left".
    public var seatsLeft: (@Sendable (Int) -> String)?
    /// The label of the time zone picker. Built in: "Time zone".
    public var timeZone: String?
    /// Shown when the zone cannot be changed, from its name. Built in: "Times shown in Central European Time".
    public var shownIn: (@Sendable (String) -> String)?
    /// Shown when the business works in another zone, from its name. Built in: "Times are converted from Pacific Time."
    public var convertedFrom: (@Sendable (String) -> String)?
    /// The warning when the selection is not an open time of the day, from the time.
    /// Built in: "9:30 AM is not an open time on this day."
    public var notOpen: (@Sendable (String) -> String)?
    /// The button that asks for a time outside the open ones. Built in: "Choose another time".
    public var chooseOutside: String?
    /// The retry button of the error. Built in: "Try again".
    public var retry: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - chooseDay: Shown before a day is chosen.
    ///   - empty: Shown when the day has no times.
    ///   - loading: Announced while times load.
    ///   - loaded: Announced when times arrive.
    ///   - times: The name of the group of times.
    ///   - seatsLeft: Under a time with limited places.
    ///   - timeZone: The label of the time zone picker.
    ///   - shownIn: Shown when the zone cannot be changed.
    ///   - convertedFrom: Shown when the business works in another zone.
    ///   - notOpen: The warning when the selection is not an open time.
    ///   - chooseOutside: The button that asks for another time.
    ///   - retry: The retry button of the error.
    public init(
        chooseDay: String? = nil,
        empty: String? = nil,
        loading: String? = nil,
        loaded: (@Sendable (Int, String) -> String)? = nil,
        times: (@Sendable (String) -> String)? = nil,
        seatsLeft: (@Sendable (Int) -> String)? = nil,
        timeZone: String? = nil,
        shownIn: (@Sendable (String) -> String)? = nil,
        convertedFrom: (@Sendable (String) -> String)? = nil,
        notOpen: (@Sendable (String) -> String)? = nil,
        chooseOutside: String? = nil,
        retry: String? = nil
    ) {
        self.chooseDay = chooseDay
        self.empty = empty
        self.loading = loading
        self.loaded = loaded
        self.times = times
        self.seatsLeft = seatsLeft
        self.timeZone = timeZone
        self.shownIn = shownIn
        self.convertedFrom = convertedFrom
        self.notOpen = notOpen
        self.chooseOutside = chooseOutside
        self.retry = retry
    }
}
