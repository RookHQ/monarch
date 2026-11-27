/// Replacements for the built-in strings of ``MonarchWorkspaceSwitcher``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchWorkspaceSwitcherLabels: Sendable {
    /// The spoken name of the button and the sheet title. Built in: "Workspace".
    public var label: String?
    /// The button text when no workspace is chosen. Built in: "Choose a workspace".
    public var placeholder: String?
    /// The search field prompt. Built in: "Search workspaces".
    public var search: String?
    /// Shown when no workspace matches the search. Built in: "No workspaces match."
    public var empty: String?
    /// Shown while the workspaces load. Built in: "Loading workspaces".
    public var loading: String?
    /// The create action. Built in: "Create workspace".
    public var create: String?
    /// The manage link. Built in: "All workspaces".
    public var manage: String?
    /// The button that closes the sheet. Built in: "Cancel".
    public var cancel: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        placeholder: String? = nil,
        search: String? = nil,
        empty: String? = nil,
        loading: String? = nil,
        create: String? = nil,
        manage: String? = nil,
        cancel: String? = nil
    ) {
        self.label = label
        self.placeholder = placeholder
        self.search = search
        self.empty = empty
        self.loading = loading
        self.create = create
        self.manage = manage
        self.cancel = cancel
    }
}
