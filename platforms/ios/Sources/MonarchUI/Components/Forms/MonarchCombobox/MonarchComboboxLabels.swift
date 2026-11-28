/// Replacements for the built-in strings of ``MonarchCombobox``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchComboboxLabels: Sendable {
    /// The search field's placeholder. Built in: "Search".
    public var search: String?
    /// Shown while options load and there are none yet. Built in: "Loading…".
    public var loading: String?
    /// Shown when no option matches. Built in: "No matches".
    public var empty: String?
    /// The create option, from the typed text. Built in: "Create “text”".
    public var create: (@Sendable (String) -> String)?
    /// Shown under a list cut short by `limit`. Built in: "Showing 50 of 120. Type to narrow the list."
    public var more: (@Sendable (Int, Int) -> String)?
    /// The button that closes the list without choosing. Built in: "Cancel".
    public var cancel: String?
    /// The spoken name of the button that clears the selection. Built in: "Clear".
    public var clear: String?
    /// The spoken value while nothing is chosen. Built in: "None".
    public var noSelection: String?
    /// Shown when the options failed to load and `loadError` is empty.
    /// Built in: "Couldn't load the options."
    public var loadFailed: String?
    /// Replacements for the spoken "Required" and the spoken form of an error.
    public var field: MonarchFormFieldLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        search: String? = nil,
        loading: String? = nil,
        empty: String? = nil,
        create: (@Sendable (String) -> String)? = nil,
        more: (@Sendable (Int, Int) -> String)? = nil,
        cancel: String? = nil,
        clear: String? = nil,
        noSelection: String? = nil,
        loadFailed: String? = nil,
        field: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        self.search = search
        self.loading = loading
        self.empty = empty
        self.create = create
        self.more = more
        self.cancel = cancel
        self.clear = clear
        self.noSelection = noSelection
        self.loadFailed = loadFailed
        self.field = field
    }
}
