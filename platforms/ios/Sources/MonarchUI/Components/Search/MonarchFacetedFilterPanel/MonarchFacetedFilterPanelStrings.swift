/// Built-in strings of ``MonarchFacetedFilterPanel``, from the `MonarchFacetedFilterPanel` strings table.
enum MonarchFacetedFilterPanelStrings {
    private static let table = "MonarchFacetedFilterPanel"

    /// The string for a key without placeholders, such as `title`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.facetedFilterPanel.\(key)", table: table)
    }

    /// "Clear Status filter".
    static func clearGroupLabel(_ group: String) -> String {
        MonarchStrings.string("monarch.facetedFilterPanel.clearGroupLabel", table: table, group)
    }

    /// "2 selected".
    static func activeCount(_ count: Int) -> String {
        MonarchStrings.string("monarch.facetedFilterPanel.activeCount", table: table, count)
    }

    /// "Show 4 more".
    static func showMore(_ hidden: Int) -> String {
        MonarchStrings.string("monarch.facetedFilterPanel.showMore", table: table, hidden)
    }
}
