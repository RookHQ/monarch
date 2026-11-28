/// Built-in strings of the section card, from the `MonarchSectionCard` strings table.
enum MonarchSectionCardStrings {
    private static let table = "MonarchSectionCard"

    /// Read while the section loads.
    static var loading: String { MonarchStrings.string("monarch.sectionCard.loading", table: table) }
    /// The title of the empty state.
    static var emptyTitle: String { MonarchStrings.string("monarch.sectionCard.emptyTitle", table: table) }
    /// The title of the error state.
    static var errorTitle: String { MonarchStrings.string("monarch.sectionCard.errorTitle", table: table) }
    /// The value of an open collapsible title.
    static var expanded: String { MonarchStrings.string("monarch.sectionCard.expanded", table: table) }
    /// The value of a closed collapsible title.
    static var collapsed: String { MonarchStrings.string("monarch.sectionCard.collapsed", table: table) }
}
