/// Built-in strings of the leaderboard, from the `MonarchLeaderboard` strings table.
enum MonarchLeaderboardStrings {
    private static let table = "MonarchLeaderboard"

    /// The tag after the viewer's own name.
    static var you: String { MonarchStrings.string("monarch.leaderboard.you", table: table) }
    /// Shown when there are no entries.
    static var empty: String { MonarchStrings.string("monarch.leaderboard.empty", table: table) }
    /// The spoken name of the loading placeholder.
    static var loading: String { MonarchStrings.string("monarch.leaderboard.loading", table: table) }

    /// "4 more".
    static func more(_ hidden: Int) -> String {
        MonarchStrings.string("monarch.leaderboard.more", table: table, hidden)
    }

    /// "Rank 1:".
    static func rank(_ rank: Int) -> String {
        MonarchStrings.string("monarch.leaderboard.rank", table: table, rank)
    }
}
