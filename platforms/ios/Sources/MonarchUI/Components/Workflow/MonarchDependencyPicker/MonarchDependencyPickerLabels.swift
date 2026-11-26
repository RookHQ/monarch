/// Replacements for the built-in strings of ``MonarchDependencyPicker``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchDependencyPickerLabels: Sendable {
    /// The names of the relations, used as headings and menu items. Built in: "Blocked by", "Blocks",
    /// "Related to", "Duplicates", "Duplicated by".
    public var relation: (@Sendable (MonarchDependencyRelation) -> String)?
    /// The label of the relation menu. Built in: "Relation".
    public var relationLabel: String?
    /// The label of the search field. Built in: "Find an item to link".
    public var searchLabel: String?
    /// The placeholder of the search field. Built in: "Search by key or title".
    public var searchPrompt: String?
    /// Shown when the search finds nothing. Built in: "No matching items."
    public var noResults: String?
    /// Shown when there are no links. Built in: "No linked items."
    public var empty: String?
    /// The spoken name of a remove button, from the relation and the item. Built in:
    /// "Remove “Blocked by” link to STR-12 Fix login".
    public var remove: (@Sendable (String, String) -> String)?
    /// Announced after adding, from the relation and the item. Built in: "Blocked by STR-12 Fix login added."
    public var added: (@Sendable (String, String) -> String)?
    /// Announced after removing, from the relation and the item. Built in: "Blocked by STR-12 Fix login removed."
    public var removed: (@Sendable (String, String) -> String)?
    /// The spoken status of a linked item, from its name. Built in: "Status: In progress".
    public var status: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        relation: (@Sendable (MonarchDependencyRelation) -> String)? = nil,
        relationLabel: String? = nil,
        searchLabel: String? = nil,
        searchPrompt: String? = nil,
        noResults: String? = nil,
        empty: String? = nil,
        remove: (@Sendable (String, String) -> String)? = nil,
        added: (@Sendable (String, String) -> String)? = nil,
        removed: (@Sendable (String, String) -> String)? = nil,
        status: (@Sendable (String) -> String)? = nil
    ) {
        self.relation = relation
        self.relationLabel = relationLabel
        self.searchLabel = searchLabel
        self.searchPrompt = searchPrompt
        self.noResults = noResults
        self.empty = empty
        self.remove = remove
        self.added = added
        self.removed = removed
        self.status = status
    }
}
