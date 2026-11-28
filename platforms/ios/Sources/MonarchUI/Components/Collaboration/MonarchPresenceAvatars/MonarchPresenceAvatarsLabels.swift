/// Replacements for the built-in strings of ``MonarchPresenceAvatars``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPresenceAvatarsLabels: Sendable {
    /// The spoken name of the stack. Receives the number of people. Built
    /// in: "1 person here", "5 people here".
    public var label: (@Sendable (Int) -> String)?
    /// The spoken hint of the stack when it opens the list. Built in: "Shows everyone here".
    public var showAll: String?
    /// The title of the list. Built in: "Everyone here".
    public var list: String?
    /// The button that closes the list. Built in: "Done".
    public var done: String?
    /// The spoken status of one person, read after the name. Receives the
    /// presence and the activity. Built in: "Online, Editing".
    public var status: (@Sendable (String, String?) -> String)?
    /// The second line of a person in the list. Receives the presence and
    /// the activity. Built in: "Online · Editing".
    public var detail: (@Sendable (String, String?) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: (@Sendable (Int) -> String)? = nil,
        showAll: String? = nil,
        list: String? = nil,
        done: String? = nil,
        status: (@Sendable (String, String?) -> String)? = nil,
        detail: (@Sendable (String, String?) -> String)? = nil
    ) {
        self.label = label
        self.showAll = showAll
        self.list = list
        self.done = done
        self.status = status
        self.detail = detail
    }
}
