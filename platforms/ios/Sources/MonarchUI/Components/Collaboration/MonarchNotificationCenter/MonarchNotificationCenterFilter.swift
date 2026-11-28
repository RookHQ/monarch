/// The tab of a ``MonarchNotificationCenter``.
public enum MonarchNotificationCenterFilter: String, Sendable, Hashable, CaseIterable {
    /// Every loaded notification.
    case all
    /// Only unread notifications.
    case unread
}
