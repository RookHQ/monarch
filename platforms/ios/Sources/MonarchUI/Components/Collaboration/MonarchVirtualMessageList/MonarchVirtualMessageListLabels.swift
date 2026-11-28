/// Replacements for the built-in strings of ``MonarchVirtualMessageList``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchVirtualMessageListLabels: Sendable {
    /// The name of the list. Built in: "Messages".
    public var list: String?
    /// The divider above the first unread message. Built in: "New messages".
    public var newMessages: String?
    /// The day separator for today. Built in: "Today".
    public var today: String?
    /// The day separator for yesterday. Built in: "Yesterday".
    public var yesterday: String?
    /// The jump button. Built in: "Jump to latest".
    public var jumpToLatest: String?
    /// The jump button while new messages arrived below. Receives the count. Built in: "3 new messages".
    public var unseen: (@Sendable (Int) -> String)?
    /// Announced when messages arrive. Receives the count. Built in: "3 new messages".
    public var arrived: (@Sendable (Int) -> String)?
    /// The button that loads older history. Built in: "Load older messages".
    public var loadOlder: String?
    /// The button while older history loads. Built in: "Loading older messages…".
    public var loadingOlder: String?
    /// Shown when older history fails to load. Built in: "Couldn't load older messages."
    public var olderFailed: String?
    /// Spoken while the first messages load. Built in: "Loading messages".
    public var loading: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        list: String? = nil,
        newMessages: String? = nil,
        today: String? = nil,
        yesterday: String? = nil,
        jumpToLatest: String? = nil,
        unseen: (@Sendable (Int) -> String)? = nil,
        arrived: (@Sendable (Int) -> String)? = nil,
        loadOlder: String? = nil,
        loadingOlder: String? = nil,
        olderFailed: String? = nil,
        loading: String? = nil
    ) {
        self.list = list
        self.newMessages = newMessages
        self.today = today
        self.yesterday = yesterday
        self.jumpToLatest = jumpToLatest
        self.unseen = unseen
        self.arrived = arrived
        self.loadOlder = loadOlder
        self.loadingOlder = loadingOlder
        self.olderFailed = olderFailed
        self.loading = loading
    }
}
