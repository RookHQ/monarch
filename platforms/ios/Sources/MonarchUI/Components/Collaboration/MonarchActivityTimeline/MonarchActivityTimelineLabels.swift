/// Replacements for the built-in strings of ``MonarchActivityTimeline``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchActivityTimelineLabels: Sendable, Hashable {
    /// The spoken name of the feed when it is not grouped by day. Built in: "Activity".
    public var list: String?
    /// The heading of today's events. Built in: "Today".
    public var today: String?
    /// The heading of yesterday's events. Built in: "Yesterday".
    public var yesterday: String?
    /// The tag of automation and system actors. Built in: "Automated".
    public var automated: String?
    /// The disclosure of an event's details. Built in: "Details".
    public var details: String?
    /// The empty state. Built in: "No activity yet".
    public var empty: String?
    /// Spoken while the first page loads. Built in: "Loading activity".
    public var loading: String?
    /// The error message. Built in: "Couldn’t load activity."
    public var error: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        list: String? = nil,
        today: String? = nil,
        yesterday: String? = nil,
        automated: String? = nil,
        details: String? = nil,
        empty: String? = nil,
        loading: String? = nil,
        error: String? = nil
    ) {
        self.list = list
        self.today = today
        self.yesterday = yesterday
        self.automated = automated
        self.details = details
        self.empty = empty
        self.loading = loading
        self.error = error
    }
}
