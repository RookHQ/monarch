/// Replacements for the built-in strings of ``MonarchAiConnectorPicker``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiConnectorPickerLabels: Sendable {
    /// The navigation title. Built in: "Connectors".
    public var title: String?
    /// The prompt of the search field. Built in: "Search connectors".
    public var searchPrompt: String?
    /// The button that closes the sheet. Built in: "Done".
    public var done: String?
    /// The summary line, given the connectors on, all connectors and the tools that ask first. Built in: "2 of 5 connectors on · 3 tools ask first".
    public var summary: (@Sendable (Int, Int, Int) -> String)?
    /// Announced after the search changes, given the count. Built in: "3 connectors found".
    public var results: (@Sendable (Int) -> String)?
    /// Shown when the search matches nothing. Built in: "No connectors match “<query>”."
    public var noResults: (@Sendable (String) -> String)?
    /// The button that clears the search. Built in: "Clear search".
    public var clearSearch: String?
    /// Shown when there are no connectors. Built in: "No connectors are available."
    public var empty: String?
    /// The text of a status. Return `nil` to keep the built-in text: "Connected", "Sign-in needed", "Connection problem", "Unavailable".
    public var status: (@Sendable (MonarchAiConnectorStatus) -> String?)?
    /// After Connected, given the account. Built in: "as <account>".
    public var account: (@Sendable (String) -> String)?
    /// The spoken name of a connector's switch. Built in: "Use <name> in this conversation".
    public var enable: (@Sendable (String) -> String)?
    /// Why the switch is off for a status that is not connected. Return `nil` to keep the built-in reason.
    public var disabledReason: (@Sendable (MonarchAiConnectorStatus) -> String?)?
    /// The button for a connector that needs sign-in. Built in: "Sign in".
    public var signIn: String?
    /// The button for a connector with a connection problem. Built in: "Reconnect".
    public var reconnect: String?
    /// The button that manages a connector. Built in: "Manage".
    public var manage: String?
    /// The spoken name of a row button, given the action and the connector. Built in: "<action>: <name>".
    public var actionLabel: (@Sendable (String, String) -> String)?
    /// The title of a connector's tools, given the count. Built in: "4 tools".
    public var tools: (@Sendable (Int) -> String)?
    /// The name of a tool's permission picker, given the tool. Built in: "Permission for <tool>".
    public var permissionLabel: (@Sendable (String) -> String)?
    /// The name of a permission. Return `nil` to keep the built-in name: "Allow", "Ask first", "Never".
    public var permission: (@Sendable (MonarchAiConnectorToolPermission) -> String?)?
    /// The badge of a risk. Return `nil` to keep the built-in text: "Reads data", "Changes data", "Can delete data".
    public var risk: (@Sendable (MonarchAiConnectorToolRisk) -> String?)?
    /// Why Allow is not offered for a tool that can delete data. Built in: "Tools that can delete data cannot run without asking."
    public var destructiveLocked: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        searchPrompt: String? = nil,
        done: String? = nil,
        summary: (@Sendable (Int, Int, Int) -> String)? = nil,
        results: (@Sendable (Int) -> String)? = nil,
        noResults: (@Sendable (String) -> String)? = nil,
        clearSearch: String? = nil,
        empty: String? = nil,
        status: (@Sendable (MonarchAiConnectorStatus) -> String?)? = nil,
        account: (@Sendable (String) -> String)? = nil,
        enable: (@Sendable (String) -> String)? = nil,
        disabledReason: (@Sendable (MonarchAiConnectorStatus) -> String?)? = nil,
        signIn: String? = nil,
        reconnect: String? = nil,
        manage: String? = nil,
        actionLabel: (@Sendable (String, String) -> String)? = nil,
        tools: (@Sendable (Int) -> String)? = nil,
        permissionLabel: (@Sendable (String) -> String)? = nil,
        permission: (@Sendable (MonarchAiConnectorToolPermission) -> String?)? = nil,
        risk: (@Sendable (MonarchAiConnectorToolRisk) -> String?)? = nil,
        destructiveLocked: String? = nil
    ) {
        self.title = title
        self.searchPrompt = searchPrompt
        self.done = done
        self.summary = summary
        self.results = results
        self.noResults = noResults
        self.clearSearch = clearSearch
        self.empty = empty
        self.status = status
        self.account = account
        self.enable = enable
        self.disabledReason = disabledReason
        self.signIn = signIn
        self.reconnect = reconnect
        self.manage = manage
        self.actionLabel = actionLabel
        self.tools = tools
        self.permissionLabel = permissionLabel
        self.permission = permission
        self.risk = risk
        self.destructiveLocked = destructiveLocked
    }
}
