/// Replacements for the built-in strings of ``MonarchSessionList``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchSessionListLabels: Sendable {
    /// The card title. Built in: "Where you’re signed in".
    public var title: String?
    /// The line under the title. Built in: "Devices and browsers signed in to your account. …".
    public var description: String?
    /// The spoken name of the list. Built in: "Signed-in sessions".
    public var listLabel: String?
    /// The badge of the current session. Built in: "This device".
    public var current: String?
    /// Before the sign-in time. Built in: "Signed in".
    public var signedIn: String?
    /// Before the last-active time. Built in: "Last active".
    public var lastActive: String?
    /// The row button. Built in: "Sign out".
    public var signOut: String?
    /// The spoken name of a row button. Built in: "Sign out <label>".
    public var signOutLabel: (@Sendable (String) -> String)?
    /// The header button. Built in: "Sign out other devices".
    public var signOutOthers: String?
    /// The confirmation title for one session, given its label and whether it is the current one.
    /// Built in: "Sign out <label>?" or "Sign out of this device?".
    public var confirmTitle: (@Sendable (String, Bool) -> String)?
    /// The confirmation message for one session, given whether it is the current one.
    public var confirmMessage: (@Sendable (Bool) -> String)?
    /// The confirmation title for every other session. Built in: "Sign out other devices?".
    public var othersTitle: String?
    /// The confirmation message for every other session, given their number.
    public var othersMessage: (@Sendable (Int) -> String)?
    /// The cancel button. Built in: "Cancel".
    public var cancel: String?
    /// Announced after one session is signed out. Built in: "Signed out <label>".
    public var signedOut: (@Sendable (String) -> String)?
    /// Announced after every other session is signed out. Built in: "Signed out <n> other devices".
    public var signedOutOthers: (@Sendable (Int) -> String)?
    /// Shown when signing out one session fails. Built in: "Couldn’t sign out <label>. Try again.".
    public var signOutFailed: (@Sendable (String) -> String)?
    /// Shown when signing out the other sessions fails.
    public var signOutOthersFailed: String?
    /// Read while loading. Built in: "Loading sessions".
    public var loading: String?
    /// The empty state title. Built in: "No active sessions".
    public var emptyTitle: String?
    /// The error state title. Built in: "Couldn’t load your sessions".
    public var errorTitle: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        description: String? = nil,
        listLabel: String? = nil,
        current: String? = nil,
        signedIn: String? = nil,
        lastActive: String? = nil,
        signOut: String? = nil,
        signOutLabel: (@Sendable (String) -> String)? = nil,
        signOutOthers: String? = nil,
        confirmTitle: (@Sendable (String, Bool) -> String)? = nil,
        confirmMessage: (@Sendable (Bool) -> String)? = nil,
        othersTitle: String? = nil,
        othersMessage: (@Sendable (Int) -> String)? = nil,
        cancel: String? = nil,
        signedOut: (@Sendable (String) -> String)? = nil,
        signedOutOthers: (@Sendable (Int) -> String)? = nil,
        signOutFailed: (@Sendable (String) -> String)? = nil,
        signOutOthersFailed: String? = nil,
        loading: String? = nil,
        emptyTitle: String? = nil,
        errorTitle: String? = nil
    ) {
        self.title = title
        self.description = description
        self.listLabel = listLabel
        self.current = current
        self.signedIn = signedIn
        self.lastActive = lastActive
        self.signOut = signOut
        self.signOutLabel = signOutLabel
        self.signOutOthers = signOutOthers
        self.confirmTitle = confirmTitle
        self.confirmMessage = confirmMessage
        self.othersTitle = othersTitle
        self.othersMessage = othersMessage
        self.cancel = cancel
        self.signedOut = signedOut
        self.signedOutOthers = signedOutOthers
        self.signOutFailed = signOutFailed
        self.signOutOthersFailed = signOutOthersFailed
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.errorTitle = errorTitle
    }
}
