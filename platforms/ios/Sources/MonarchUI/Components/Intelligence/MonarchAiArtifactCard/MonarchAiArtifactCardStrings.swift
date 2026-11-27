/// Built-in strings of the artifact card, from the `MonarchAiArtifactCard` strings table.
enum MonarchAiArtifactCardStrings {
    private static let table = "MonarchAiArtifactCard"

    /// The visible name of a kind.
    static func kind(_ kind: MonarchAiArtifactKind) -> String {
        switch kind {
        case .document: MonarchStrings.string("monarch.aiArtifactCard.kind.document", table: table)
        case .code: MonarchStrings.string("monarch.aiArtifactCard.kind.code", table: table)
        case .table: MonarchStrings.string("monarch.aiArtifactCard.kind.table", table: table)
        case .chart: MonarchStrings.string("monarch.aiArtifactCard.kind.chart", table: table)
        case .image: MonarchStrings.string("monarch.aiArtifactCard.kind.image", table: table)
        case .html: MonarchStrings.string("monarch.aiArtifactCard.kind.html", table: table)
        }
    }

    /// The visible version, such as "v3".
    static func version(_ version: Int) -> String {
        MonarchStrings.string("monarch.aiArtifactCard.version", table: table, String(version))
    }

    /// The version as spoken, such as "version 3".
    static func versionLong(_ version: Int) -> String {
        MonarchStrings.string("monarch.aiArtifactCard.versionLong", table: table, String(version))
    }

    /// The call to action.
    static var open: String { MonarchStrings.string("monarch.aiArtifactCard.open", table: table) }
    /// Shown in place of the call to action while the artifact is the one on show.
    static var opened: String { MonarchStrings.string("monarch.aiArtifactCard.opened", table: table) }
}
