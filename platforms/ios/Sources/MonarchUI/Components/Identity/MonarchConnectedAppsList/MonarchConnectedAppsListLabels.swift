/// Replacements for the built-in strings of ``MonarchConnectedAppsList``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchConnectedAppsListLabels: Sendable {
    /// The badge of a reviewed app. Built in: "Verified".
    public var verified: String?
    /// The badge of a suspended app. Built in: "Suspended".
    public var suspended: String?
    /// The connected time, from the relative time. Built in: "Connected 3 days ago".
    public var connected: (@Sendable (String) -> String)?
    /// The last-used time, from the relative time. Built in: "Last used yesterday".
    public var lastUsed: (@Sendable (String) -> String)?
    /// The workspace, from its name. Built in: "Workspace: Acme".
    public var workspace: (@Sendable (String) -> String)?
    /// The spoken name of an app's scope list, from the app name. Built in: "What Notes can access".
    public var scopes: (@Sendable (String) -> String)?
    /// The remove button. Built in: "Remove access".
    public var revoke: String?
    /// The spoken name of the remove button, from the app name. Built in: "Remove access for Notes".
    public var revokeLabel: (@Sendable (String) -> String)?
    /// The confirmation title, from the app name. Built in: "Remove access for Notes?"
    public var confirmTitle: (@Sendable (String) -> String)?
    /// The confirmation message.
    public var confirmMessage: String?
    /// The cancel button of the confirmation. Built in: "Cancel".
    public var cancel: String?
    /// Announced after access is removed, from the app name. Built in: "Removed access for Notes".
    public var revoked: (@Sendable (String) -> String)?
    /// Read while loading. Built in: "Loading connected apps".
    public var loading: String?
    /// The empty state title. Built in: "No connected apps".
    public var emptyTitle: String?
    /// The empty state message.
    public var emptyMessage: String?
    /// The error state title. Built in: "Couldn’t load your connected apps".
    public var errorTitle: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        verified: String? = nil,
        suspended: String? = nil,
        connected: (@Sendable (String) -> String)? = nil,
        lastUsed: (@Sendable (String) -> String)? = nil,
        workspace: (@Sendable (String) -> String)? = nil,
        scopes: (@Sendable (String) -> String)? = nil,
        revoke: String? = nil,
        revokeLabel: (@Sendable (String) -> String)? = nil,
        confirmTitle: (@Sendable (String) -> String)? = nil,
        confirmMessage: String? = nil,
        cancel: String? = nil,
        revoked: (@Sendable (String) -> String)? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        errorTitle: String? = nil
    ) {
        self.verified = verified
        self.suspended = suspended
        self.connected = connected
        self.lastUsed = lastUsed
        self.workspace = workspace
        self.scopes = scopes
        self.revoke = revoke
        self.revokeLabel = revokeLabel
        self.confirmTitle = confirmTitle
        self.confirmMessage = confirmMessage
        self.cancel = cancel
        self.revoked = revoked
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.errorTitle = errorTitle
    }
}
