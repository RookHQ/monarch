/// Replacements for the built-in strings of ``MonarchSecurityAlertCard``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchSecurityAlertCardLabels: Sendable {
    /// The card title. Built in: "Security alerts".
    public var title: String?
    /// The line under the title. Built in: "Recent security notifications and account activity."
    public var description: String?
    /// The spoken name of the list. Built in: "Security alerts".
    public var listLabel: String?
    /// The name of a severity, shown in the badge. Built in: "Info", "Low", "Medium", "High", "Critical".
    public var severity: (@Sendable (MonarchSecurityAlertSeverity) -> String)?
    /// Read for an unread alert. Built in: "Unread".
    public var unread: String?
    /// The unread count in the header. Built in: "3 unread".
    public var unreadCount: (@Sendable (Int) -> String)?
    /// The row action. Built in: "Mark as read".
    public var markRead: String?
    /// The header action. Built in: "Mark all as read".
    public var markAllRead: String?
    /// The spoken name of a dismiss button, from the alert title. Built in: "Dismiss New sign-in".
    public var dismiss: (@Sendable (String) -> String)?
    /// Announced after an alert is dismissed, from its title. Built in: "Dismissed New sign-in".
    public var dismissed: (@Sendable (String) -> String)?
    /// Shown while loading. Built in: "Loading alerts".
    public var loading: String?
    /// The empty state title. Built in: "No security alerts".
    public var emptyTitle: String?
    /// The empty state message. Built in: "Alerts about sign-ins and account changes appear here."
    public var emptyDescription: String?
    /// The error state title. Built in: "Couldn’t load security alerts".
    public var errorTitle: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        description: String? = nil,
        listLabel: String? = nil,
        severity: (@Sendable (MonarchSecurityAlertSeverity) -> String)? = nil,
        unread: String? = nil,
        unreadCount: (@Sendable (Int) -> String)? = nil,
        markRead: String? = nil,
        markAllRead: String? = nil,
        dismiss: (@Sendable (String) -> String)? = nil,
        dismissed: (@Sendable (String) -> String)? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        emptyDescription: String? = nil,
        errorTitle: String? = nil
    ) {
        self.title = title
        self.description = description
        self.listLabel = listLabel
        self.severity = severity
        self.unread = unread
        self.unreadCount = unreadCount
        self.markRead = markRead
        self.markAllRead = markAllRead
        self.dismiss = dismiss
        self.dismissed = dismissed
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.emptyDescription = emptyDescription
        self.errorTitle = errorTitle
    }
}
