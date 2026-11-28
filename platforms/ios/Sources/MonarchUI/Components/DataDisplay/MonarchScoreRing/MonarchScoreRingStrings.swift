/// Built-in strings of the score ring, from the `MonarchScoreRing` strings table.
enum MonarchScoreRingStrings {
    private static let table = "MonarchScoreRing"

    /// The spoken name when the ring has no label.
    static var name: String { MonarchStrings.string("monarch.scoreRing.name", table: table) }
    /// "78 out of 100".
    static func valueText(_ value: String, _ max: String) -> String {
        MonarchStrings.string("monarch.scoreRing.valueText", table: table, value, max)
    }
    /// "/ 100".
    static func ofMax(_ max: String) -> String {
        MonarchStrings.string("monarch.scoreRing.ofMax", table: table, max)
    }
    /// Spoken when there is no score.
    static var noValue: String { MonarchStrings.string("monarch.scoreRing.noValue", table: table) }
}
