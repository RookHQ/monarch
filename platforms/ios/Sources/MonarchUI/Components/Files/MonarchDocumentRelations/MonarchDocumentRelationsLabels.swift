/// Replacements for the built-in strings of ``MonarchDocumentRelations``.
/// A property that is `nil` keeps the built-in English string. The type
/// labels are replaced by passing your own types.
@_spi(Experimental) public struct MonarchDocumentRelationsLabels: Sendable {
    /// The spoken name of the list. Built in: "Relations".
    public var label: String?
    /// The add button. Built in: "Add relation".
    public var add: String?
    /// The title of the add sheet. Built in: "Add a relation".
    public var sheetTitle: String?
    /// The text under the type picker. Built in: "Choose how this document connects to another one."
    public var sheetDescription: String?
    /// The type picker. Built in: "Relation".
    public var type: String?
    /// The search prompt. Built in: "Search documents…".
    public var search: String?
    /// The heading of the results. Built in: "Matching documents".
    public var results: String?
    /// Shown when nothing matches. Built in: "No matching documents."
    public var noResults: String?
    /// Closes the sheet. Built in: "Cancel".
    public var cancel: String?
    /// The remove button. Receives the title. Built in: "Remove relation to Q3 plan".
    public var remove: (@Sendable (String) -> String)?
    /// Announced after adding. Receives the title. Built in: "Relation to Q3 plan added".
    public var added: (@Sendable (String) -> String)?
    /// Announced after removing. Receives the title. Built in: "Relation to Q3 plan removed".
    public var removed: (@Sendable (String) -> String)?
    /// Shown when adding or removing failed without a message. Built in: "That didn’t work. Try again."
    public var failed: String?
    /// Shown when there are no relations. Built in: "No relations yet."
    public var none: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        add: String? = nil,
        sheetTitle: String? = nil,
        sheetDescription: String? = nil,
        type: String? = nil,
        search: String? = nil,
        results: String? = nil,
        noResults: String? = nil,
        cancel: String? = nil,
        remove: (@Sendable (String) -> String)? = nil,
        added: (@Sendable (String) -> String)? = nil,
        removed: (@Sendable (String) -> String)? = nil,
        failed: String? = nil,
        none: String? = nil
    ) {
        self.label = label
        self.add = add
        self.sheetTitle = sheetTitle
        self.sheetDescription = sheetDescription
        self.type = type
        self.search = search
        self.results = results
        self.noResults = noResults
        self.cancel = cancel
        self.remove = remove
        self.added = added
        self.removed = removed
        self.failed = failed
        self.none = none
    }
}
