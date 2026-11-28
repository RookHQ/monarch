/// Built-in strings of the citation, from the `MonarchAiCitation` strings table.
enum MonarchAiCitationStrings {
    private static let table = "MonarchAiCitation"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiCitation.\(key)", table: table)
    }

    /// The spoken name of the marker.
    static func marker(_ index: Int, title: String) -> String {
        MonarchStrings.string("monarch.aiCitation.marker", table: table, index, title)
    }
    /// The heading of the card.
    static func card(_ index: Int) -> String {
        MonarchStrings.string("monarch.aiCitation.card", table: table, index)
    }
    /// The open button.
    static var open: String { string("open") }
    /// The visit link.
    static var visit: String { string("visit") }
    /// The name of a kind of source.
    static func kind(_ kind: MonarchAiSourceKind) -> String { string("kind.\(kind.rawValue)") }
}
