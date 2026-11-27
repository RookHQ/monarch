import Foundation

/// One signed-in session, a browser or app on a device, shown by
/// ``MonarchSessionList``.
@_spi(Experimental) public struct MonarchSession: Identifiable, Sendable, Hashable {
    /// The id passed to the sign-out handlers.
    public let id: String
    /// What the person recognises, such as "Safari on iPhone".
    public let label: String
    /// The kind of device. It picks the icon.
    public let kind: MonarchDeviceKind
    /// A quieter line, such as an approximate location or an IP address.
    public let detail: String?
    /// When the session signed in.
    public let signedIn: Date?
    /// When the session was last used. Not shown for the current session, which is in use now.
    public let lastActive: Date?
    /// Whether this is the session the person is using now.
    public let isCurrent: Bool

    /// A session.
    ///
    /// - Parameters:
    ///   - id: The id passed to the sign-out handlers.
    ///   - label: What the person recognises, already localised, such as "Safari on iPhone".
    ///   - kind: The kind of device. It picks the icon.
    ///   - detail: A quieter line, such as an approximate location.
    ///   - signedIn: When the session signed in.
    ///   - lastActive: When the session was last used.
    ///   - isCurrent: Whether this is the session in use now.
    public init(
        id: String,
        label: String,
        kind: MonarchDeviceKind = .desktop,
        detail: String? = nil,
        signedIn: Date? = nil,
        lastActive: Date? = nil,
        isCurrent: Bool = false
    ) {
        self.id = id
        self.label = label
        self.kind = kind
        self.detail = detail
        self.signedIn = signedIn
        self.lastActive = lastActive
        self.isCurrent = isCurrent
    }
}
