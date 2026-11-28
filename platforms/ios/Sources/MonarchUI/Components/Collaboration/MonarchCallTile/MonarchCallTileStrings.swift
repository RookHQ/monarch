/// Built-in strings of the call tile, from the `MonarchCallTile` strings table.
enum MonarchCallTileStrings {
    private static let table = "MonarchCallTile"

    /// The name of your own tile.
    static func you(_ name: String) -> String { MonarchStrings.string("monarch.callTile.you", table: table, name) }
    /// The muted microphone.
    static var muted: String { MonarchStrings.string("monarch.callTile.muted", table: table) }
    /// A shared screen.
    static var sharingScreen: String { MonarchStrings.string("monarch.callTile.sharingScreen", table: table) }
    /// The visible hand badge.
    static var handRaised: String { MonarchStrings.string("monarch.callTile.handRaised", table: table) }
    /// The speaking state.
    static var speaking: String { MonarchStrings.string("monarch.callTile.speaking", table: table) }
    /// The pin button.
    static func pin(_ name: String) -> String { MonarchStrings.string("monarch.callTile.pin", table: table, name) }
    /// The unpin button.
    static func unpin(_ name: String) -> String { MonarchStrings.string("monarch.callTile.unpin", table: table, name) }
    /// The full-screen button.
    static func fullScreen(_ name: String) -> String {
        MonarchStrings.string("monarch.callTile.fullScreen", table: table, name)
    }
    /// The announcement of a reaction.
    static func reacted(_ name: String, _ emoji: String) -> String {
        MonarchStrings.string("monarch.callTile.reacted", table: table, name, emoji)
    }
    /// The connection quality.
    static func quality(_ quality: MonarchCallQuality) -> String {
        switch quality {
        case .good: MonarchStrings.string("monarch.callTile.qualityGood", table: table)
        case .fair: MonarchStrings.string("monarch.callTile.qualityFair", table: table)
        case .poor: MonarchStrings.string("monarch.callTile.qualityPoor", table: table)
        }
    }
}
