/// Replacements for the built-in strings of ``MonarchSectionCard``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchSectionCardLabels: Sendable {
    /// Read while the section loads. Built in: "Loading".
    public var loading: String?
    /// The title of the empty state. Built in: "Nothing here yet".
    public var emptyTitle: String?
    /// The title of the error state. Built in: "This section didn’t load".
    public var errorTitle: String?
    /// The spoken value of a collapsible title that is open. Built in: "Expanded".
    public var expanded: String?
    /// The spoken value of a collapsible title that is closed. Built in: "Collapsed".
    public var collapsed: String?
    /// The spoken name of the tone icon. Built in: the tone's name, such as "Error".
    public var tone: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        loading: String? = nil,
        emptyTitle: String? = nil,
        errorTitle: String? = nil,
        expanded: String? = nil,
        collapsed: String? = nil,
        tone: String? = nil
    ) {
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.errorTitle = errorTitle
        self.expanded = expanded
        self.collapsed = collapsed
        self.tone = tone
    }
}
