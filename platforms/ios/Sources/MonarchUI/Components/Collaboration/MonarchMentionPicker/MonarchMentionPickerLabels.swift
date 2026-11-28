/// Replacements for the built-in strings of ``MonarchMentionPicker``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchMentionPickerLabels: Sendable {
    /// The spoken name of the list. Built in: "Suggestions".
    public var label: String?
    /// The heading of people. Built in: "People".
    public var people: String?
    /// The heading of channels. Built in: "Channels".
    public var channels: String?
    /// Read while searching. Built in: "Searching…".
    public var loading: String?
    /// Shown when nothing matches. Built in: "No matches".
    public var empty: String?
    /// Shown when the search failed. Built in: "Couldn’t load suggestions."
    public var error: String?
    /// The retry button. Built in: the inline error's "Try again".
    public var retry: String?
    /// Announced when results arrive. Receives the count. Built in: "1
    /// suggestion", "4 suggestions".
    public var count: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        people: String? = nil,
        channels: String? = nil,
        loading: String? = nil,
        empty: String? = nil,
        error: String? = nil,
        retry: String? = nil,
        count: (@Sendable (Int) -> String)? = nil
    ) {
        self.label = label
        self.people = people
        self.channels = channels
        self.loading = loading
        self.empty = empty
        self.error = error
        self.retry = retry
        self.count = count
    }
}
