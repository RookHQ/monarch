/// Replacements for the built-in strings of ``MonarchThreadPanel``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchThreadPanelLabels: Sendable {
    /// The navigation title. Built in: "Thread".
    public var title: String?
    /// The close button. Built in: "Close".
    public var close: String?
    /// The follow button while not following. Built in: "Follow".
    public var follow: String?
    /// The follow button while following. Built in: "Following".
    public var following: String?
    /// Announced after following. Built in: "You'll be notified about replies".
    public var followedNotice: String?
    /// Announced after unfollowing. Built in: "You won't be notified about replies".
    public var unfollowedNotice: String?
    /// Announced when following or unfollowing fails. Built in: "Thread notifications didn't change. Try again."
    public var followFailed: String?
    /// The divider between the message and its replies. Receives the count. Built in: "3 replies".
    public var replyCount: (@Sendable (Int) -> String)?
    /// The name of the list of replies. Built in: "Replies".
    public var replies: String?
    /// Spoken while the thread loads. Built in: "Loading thread".
    public var loading: String?
    /// Shown when the thread could not be loaded. Built in: "This thread couldn't be loaded."
    public var error: String?
    /// Shown for a deleted thread. Built in: "This thread was deleted."
    public var deleted: String?
    /// The strings of the list of replies, a ``MonarchVirtualMessageList``. Its `list` defaults to `replies`.
    public var list: MonarchVirtualMessageListLabels?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        close: String? = nil,
        follow: String? = nil,
        following: String? = nil,
        followedNotice: String? = nil,
        unfollowedNotice: String? = nil,
        followFailed: String? = nil,
        replyCount: (@Sendable (Int) -> String)? = nil,
        replies: String? = nil,
        loading: String? = nil,
        error: String? = nil,
        deleted: String? = nil,
        list: MonarchVirtualMessageListLabels? = nil
    ) {
        self.title = title
        self.close = close
        self.follow = follow
        self.following = following
        self.followedNotice = followedNotice
        self.unfollowedNotice = unfollowedNotice
        self.followFailed = followFailed
        self.replyCount = replyCount
        self.replies = replies
        self.loading = loading
        self.error = error
        self.deleted = deleted
        self.list = list
    }
}
