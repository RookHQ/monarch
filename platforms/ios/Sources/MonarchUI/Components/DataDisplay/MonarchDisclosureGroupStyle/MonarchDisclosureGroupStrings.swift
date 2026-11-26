/// Built-in strings of the disclosure group style, from the
/// `MonarchDisclosureGroupStyle` strings table.
enum MonarchDisclosureGroupStrings {
    /// Spoken as the value of the header while the content is shown.
    static var expanded: String {
        MonarchStrings.string("monarch.disclosureGroupStyle.expanded", table: "MonarchDisclosureGroupStyle")
    }

    /// Spoken as the value of the header while the content is hidden.
    static var collapsed: String {
        MonarchStrings.string("monarch.disclosureGroupStyle.collapsed", table: "MonarchDisclosureGroupStyle")
    }
}
