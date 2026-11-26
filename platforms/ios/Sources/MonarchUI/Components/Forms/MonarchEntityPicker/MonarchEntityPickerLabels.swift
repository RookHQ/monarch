/// Replacements for the built-in strings of ``MonarchEntityPicker``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchEntityPickerLabels: Sendable {
    /// The field's text while nothing is chosen and there is no prompt. Built in: "Select…".
    public var select: String?
    /// The search field's placeholder. Built in: "Search".
    public var search: String?
    /// The button that closes a single-choice list without choosing. Built in: "Cancel".
    public var cancel: String?
    /// The button that closes a multiple-choice list. Built in: "Done".
    public var done: String?
    /// Shown while results load. Built in: "Loading…".
    public var loading: String?
    /// Shown when nothing matches. Built in: "No matches".
    public var empty: String?
    /// Shown while the search is shorter than `minSearchLength`. Receives the length. Built in: "Type at least 2 characters to search."
    public var tooShort: (@Sendable (Int) -> String)?
    /// Shown when the search throws. Built in: "The results could not be loaded."
    public var failed: String?
    /// The create row, from the typed text. Built in: "Create “text”".
    public var create: (@Sendable (String) -> String)?
    /// The field's text with several chosen. Receives the first name and how many more. Built in: "Ana Silva +2".
    public var summary: (@Sendable (String, Int) -> String)?
    /// The spoken name of the button that clears the selection. Built in: "Clear".
    public var clear: String?
    /// The spoken value while nothing is chosen. Built in: "None".
    public var noSelection: String?
    /// Replacements for the spoken "Required" and the spoken form of an error.
    public var field: MonarchFormFieldLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        select: String? = nil,
        search: String? = nil,
        cancel: String? = nil,
        done: String? = nil,
        loading: String? = nil,
        empty: String? = nil,
        tooShort: (@Sendable (Int) -> String)? = nil,
        failed: String? = nil,
        create: (@Sendable (String) -> String)? = nil,
        summary: (@Sendable (String, Int) -> String)? = nil,
        clear: String? = nil,
        noSelection: String? = nil,
        field: MonarchFormFieldLabels = MonarchFormFieldLabels()
    ) {
        self.select = select
        self.search = search
        self.cancel = cancel
        self.done = done
        self.loading = loading
        self.empty = empty
        self.tooShort = tooShort
        self.failed = failed
        self.create = create
        self.summary = summary
        self.clear = clear
        self.noSelection = noSelection
        self.field = field
    }
}
