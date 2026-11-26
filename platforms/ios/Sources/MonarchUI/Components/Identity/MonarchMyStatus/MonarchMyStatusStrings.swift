/// Built-in strings of the status row, from the `MonarchMyStatus` strings table.
enum MonarchMyStatusStrings {
    private static let table = "MonarchMyStatus"

    /// What VoiceOver says the row does.
    static var hint: String { MonarchStrings.string("monarch.myStatus.hint", table: table) }

    /// The invitation shown without a custom status.
    static var setStatus: String { MonarchStrings.string("monarch.myStatus.setStatus", table: table) }

    /// The spoken value: the presence, then the line under the name.
    static func value(presence: String, line: String) -> String {
        MonarchStrings.string("monarch.myStatus.value", table: table, presence, line)
    }
}
