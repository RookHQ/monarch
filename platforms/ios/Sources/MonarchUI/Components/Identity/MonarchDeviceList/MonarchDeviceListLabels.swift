/// Replacements for the built-in strings of ``MonarchDeviceList``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchDeviceListLabels: Sendable {
    /// The card title. Built in: "Devices".
    public var title: String?
    /// The line under the title. Built in: "Devices that have accessed your account."
    public var description: String?
    /// The spoken name of the list. Built in: "Devices".
    public var listLabel: String?
    /// The name of a device without one. Built in: "Unknown device".
    public var unnamed: String?
    /// The badge of the current device. Built in: "This device".
    public var current: String?
    /// The badge of a blocked device. Built in: "Blocked".
    public var blocked: String?
    /// Before the last-used time. Built in: "Last used".
    public var lastUsed: String?
    /// Before the first-seen time. Built in: "First seen".
    public var firstSeen: String?
    /// The row button and the confirm button. Built in: "Remove".
    public var remove: String?
    /// The spoken name of a row's remove button. Built in: "Remove <name>".
    public var removeLabel: (@Sendable (String) -> String)?
    /// The confirmation title. Built in: "Remove <name>?".
    public var confirmTitle: (@Sendable (String) -> String)?
    /// The confirmation message. Built in: "It is signed out and has to sign in again to get access."
    public var confirmMessage: String?
    /// The cancel button of the confirmation. Built in: "Cancel".
    public var cancel: String?
    /// Announced after a device is removed. Built in: "Removed <name>".
    public var removed: (@Sendable (String) -> String)?
    /// Shown when removing fails. Built in: "Couldn’t remove <name>. Try again."
    public var removeFailed: (@Sendable (String) -> String)?
    /// Read while loading. Built in: "Loading devices".
    public var loading: String?
    /// The empty state title. Built in: "No devices yet".
    public var emptyTitle: String?
    /// The empty state message. Built in: "Devices appear here when they access your account."
    public var emptyMessage: String?
    /// The error state title. Built in: "Couldn’t load your devices".
    public var errorTitle: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        description: String? = nil,
        listLabel: String? = nil,
        unnamed: String? = nil,
        current: String? = nil,
        blocked: String? = nil,
        lastUsed: String? = nil,
        firstSeen: String? = nil,
        remove: String? = nil,
        removeLabel: (@Sendable (String) -> String)? = nil,
        confirmTitle: (@Sendable (String) -> String)? = nil,
        confirmMessage: String? = nil,
        cancel: String? = nil,
        removed: (@Sendable (String) -> String)? = nil,
        removeFailed: (@Sendable (String) -> String)? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        errorTitle: String? = nil
    ) {
        self.title = title
        self.description = description
        self.listLabel = listLabel
        self.unnamed = unnamed
        self.current = current
        self.blocked = blocked
        self.lastUsed = lastUsed
        self.firstSeen = firstSeen
        self.remove = remove
        self.removeLabel = removeLabel
        self.confirmTitle = confirmTitle
        self.confirmMessage = confirmMessage
        self.cancel = cancel
        self.removed = removed
        self.removeFailed = removeFailed
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.errorTitle = errorTitle
    }
}
