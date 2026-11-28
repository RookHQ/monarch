/// Built-in strings of the provenance note, from the `MonarchProvenance` strings table.
enum MonarchProvenanceStrings {
    private static let table = "MonarchProvenance"

    /// "Source: Registry"; the argument is the source.
    static func source(_ source: String) -> String {
        MonarchStrings.string("monarch.provenance.source", table: table, source)
    }

    /// "checked 2 hours ago"; the argument is the relative time.
    static func checked(_ time: String) -> String {
        MonarchStrings.string("monarch.provenance.checked", table: table, time)
    }
}
