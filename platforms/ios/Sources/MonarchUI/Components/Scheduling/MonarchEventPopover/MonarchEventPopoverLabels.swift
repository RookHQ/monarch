/// Replacements for the built-in strings of ``MonarchEventPopover``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchEventPopoverLabels: Sendable, Hashable {
    /// The spoken name of the time row. Built in: "Time".
    public var time: String?
    /// Shown instead of a time for an all-day event. Built in: "All day".
    public var allDay: String?
    /// Replaces the status badge's text. Built in: the event chip's status
    /// names, "Confirmed", "Tentative", "Cancelled" and "Declined".
    public var status: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(time: String? = nil, allDay: String? = nil, status: String? = nil) {
        self.time = time
        self.allDay = allDay
        self.status = status
    }
}
