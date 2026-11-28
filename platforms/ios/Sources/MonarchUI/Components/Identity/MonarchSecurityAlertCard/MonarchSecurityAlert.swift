import Foundation

/// One security alert, such as a sign-in from a new device or a changed password.
public struct MonarchSecurityAlert: Identifiable, Sendable, Hashable {
    /// A stable id, passed to the callbacks.
    public var id: String
    /// What happened, such as "New sign-in from Firefox on Linux".
    public var title: String
    /// More detail: where, which device, what to do.
    public var description: String?
    /// How serious it is.
    public var severity: MonarchSecurityAlertSeverity
    /// When it happened.
    public var date: Date?
    /// Whether the person has seen it. Unread alerts are marked and counted.
    public var isRead: Bool
    /// The title of a follow-up action, such as "Review sessions".
    public var actionTitle: String?
    /// Where the follow-up action goes. Only `http`, `https` and `mailto`
    /// links are opened; any other link is ignored, because alerts are often
    /// data from a server.
    public var actionURL: URL?

    /// A security alert.
    ///
    /// - Parameters:
    ///   - id: A stable id, passed to the callbacks.
    ///   - title: What happened, already localised.
    ///   - description: More detail, already localised.
    ///   - severity: How serious it is.
    ///   - date: When it happened.
    ///   - isRead: Whether the person has seen it.
    ///   - actionTitle: The title of a follow-up action.
    ///   - actionURL: Where the follow-up action goes: an `http`, `https` or `mailto` link.
    public init(
        id: String,
        title: String,
        description: String? = nil,
        severity: MonarchSecurityAlertSeverity = .info,
        date: Date? = nil,
        isRead: Bool = false,
        actionTitle: String? = nil,
        actionURL: URL? = nil
    ) {
        self.id = id
        self.title = title
        self.description = description
        self.severity = severity
        self.date = date
        self.isRead = isRead
        self.actionTitle = actionTitle
        self.actionURL = actionURL
    }
}
