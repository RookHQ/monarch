/// Replacements for the built-in strings of ``MonarchSpaceSwitcher``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchSpaceSwitcherLabels: Sendable {
    /// The spoken name of the button and the sheet title. Built in: "Space".
    public var label: String?
    /// The all-spaces choice. Built in: "All spaces".
    public var all: String?
    /// The button text when no Space is chosen and "All spaces" is not offered. Built in: "Choose a Space".
    public var placeholder: String?
    /// The search field prompt. Built in: "Search spaces".
    public var search: String?
    /// Shown when no Space matches the search. Built in: "No spaces match."
    public var empty: String?
    /// Shown while the Spaces load. Built in: "Loading spaces".
    public var loading: String?
    /// The create action. Built in: "New Space".
    public var create: String?
    /// The manage link. Built in: "Manage Spaces".
    public var manage: String?
    /// The button that closes the sheet. Built in: "Cancel".
    public var cancel: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        all: String? = nil,
        placeholder: String? = nil,
        search: String? = nil,
        empty: String? = nil,
        loading: String? = nil,
        create: String? = nil,
        manage: String? = nil,
        cancel: String? = nil
    ) {
        self.label = label
        self.all = all
        self.placeholder = placeholder
        self.search = search
        self.empty = empty
        self.loading = loading
        self.create = create
        self.manage = manage
        self.cancel = cancel
    }
}
