/// Built-in strings of the presence dot, from the `MonarchPresenceDot` strings table.
enum MonarchPresenceDotStrings {
    private static let table = "MonarchPresenceDot"

    /// The name of a presence.
    static func name(_ status: MonarchPresenceStatus) -> String {
        switch status {
        case .online: MonarchStrings.string("monarch.presenceDot.online", table: table)
        case .away: MonarchStrings.string("monarch.presenceDot.away", table: table)
        case .busy: MonarchStrings.string("monarch.presenceDot.busy", table: table)
        case .doNotDisturb: MonarchStrings.string("monarch.presenceDot.doNotDisturb", table: table)
        case .offline: MonarchStrings.string("monarch.presenceDot.offline", table: table)
        }
    }

    /// A presence joined with the person's own status message.
    static func withStatus(_ presence: String, _ status: String) -> String {
        MonarchStrings.string("monarch.presenceDot.withStatus", table: table, presence, status)
    }
}
