/// Replacements for the built-in strings of ``MonarchNotificationCenter``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchNotificationCenterLabels: Sendable {
    /// The navigation title. Built in: "Notifications".
    public var title: String?
    /// The All tab. Built in: "All".
    public var all: String?
    /// The Unread tab. Built in: "Unread".
    public var unread: String?
    /// The name of the tabs. Built in: "Show".
    public var filter: String?
    /// The mark-all button. Built in: "Mark all as read".
    public var markAllRead: String?
    /// The swipe action on an unread item. Built in: "Mark as read".
    public var markRead: String?
    /// Read by VoiceOver after the title of an unread item. Built in: "Unread".
    public var unreadItem: String?
    /// The heading of today's items. Built in: "Today".
    public var today: String?
    /// The heading of yesterday's items. Built in: "Yesterday".
    public var yesterday: String?
    /// The empty state. Built in: "You're all caught up".
    public var emptyTitle: String?
    /// The empty state's text. Built in: "New activity will appear here."
    public var emptyMessage: String?
    /// The empty state of the Unread tab. Built in: "No unread notifications".
    public var emptyUnreadTitle: String?
    /// Read by VoiceOver for the loading placeholder. Built in: "Loading notifications".
    public var loading: String?
    /// The button that closes the panel. Built in: "Done".
    public var done: String?
    /// Replacements for the strings of the load-more row.
    public var loadMore: MonarchLoadMoreLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        all: String? = nil,
        unread: String? = nil,
        filter: String? = nil,
        markAllRead: String? = nil,
        markRead: String? = nil,
        unreadItem: String? = nil,
        today: String? = nil,
        yesterday: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        emptyUnreadTitle: String? = nil,
        loading: String? = nil,
        done: String? = nil,
        loadMore: MonarchLoadMoreLabels = MonarchLoadMoreLabels()
    ) {
        self.title = title
        self.all = all
        self.unread = unread
        self.filter = filter
        self.markAllRead = markAllRead
        self.markRead = markRead
        self.unreadItem = unreadItem
        self.today = today
        self.yesterday = yesterday
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.emptyUnreadTitle = emptyUnreadTitle
        self.loading = loading
        self.done = done
        self.loadMore = loadMore
    }
}
