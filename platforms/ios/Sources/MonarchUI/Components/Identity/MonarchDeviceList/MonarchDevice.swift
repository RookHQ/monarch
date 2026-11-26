import Foundation

/// One device that has accessed an account, shown by ``MonarchDeviceList``.
public struct MonarchDevice: Identifiable, Sendable, Hashable {
    /// The id passed to the remove handler and the row actions.
    public let id: String
    /// The device's name, such as "Work laptop". An empty name reads "Unknown device".
    public let name: String
    /// The kind of device. It picks the icon.
    public let kind: MonarchDeviceKind
    /// The operating system, such as "macOS 15".
    public let os: String?
    /// The browser or app, such as "Safari".
    public let browser: String?
    /// An IP address or an approximate location.
    public let location: String?
    /// When the device was last used.
    public let lastUsed: Date?
    /// When the device was first seen.
    public let firstSeen: Date?
    /// Whether the device is blocked from signing in.
    public let isBlocked: Bool
    /// Whether this is the device the person is using now. It cannot be removed from the list.
    public let isCurrent: Bool

    /// A device.
    ///
    /// - Parameters:
    ///   - id: The id passed to the remove handler and the row actions.
    ///   - name: The device's name, already localised where it is not the person's own.
    ///   - kind: The kind of device. It picks the icon.
    ///   - os: The operating system.
    ///   - browser: The browser or app.
    ///   - location: An IP address or an approximate location.
    ///   - lastUsed: When the device was last used.
    ///   - firstSeen: When the device was first seen.
    ///   - isBlocked: Whether the device is blocked from signing in.
    ///   - isCurrent: Whether this is the device in use now.
    public init(
        id: String,
        name: String,
        kind: MonarchDeviceKind = .desktop,
        os: String? = nil,
        browser: String? = nil,
        location: String? = nil,
        lastUsed: Date? = nil,
        firstSeen: Date? = nil,
        isBlocked: Bool = false,
        isCurrent: Bool = false
    ) {
        self.id = id
        self.name = name
        self.kind = kind
        self.os = os
        self.browser = browser
        self.location = location
        self.lastUsed = lastUsed
        self.firstSeen = firstSeen
        self.isBlocked = isBlocked
        self.isCurrent = isCurrent
    }
}
