/// Built-in strings of the section header, from the `MonarchSectionHeader` strings table.
enum MonarchSectionHeaderStrings {
    /// Spoken as the value of the title while the section's content is loading.
    static var busy: String { MonarchStrings.string("monarch.sectionHeader.busy", table: "MonarchSectionHeader") }
}
