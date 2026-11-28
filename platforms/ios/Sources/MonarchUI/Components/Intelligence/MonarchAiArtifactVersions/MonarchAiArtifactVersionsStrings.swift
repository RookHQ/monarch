/// Built-in strings of the artifact versions stepper, from the `MonarchAiArtifactVersions` strings table.
enum MonarchAiArtifactVersionsStrings {
    private static let table = "MonarchAiArtifactVersions"

    private static func key(_ name: String) -> String { "monarch.aiArtifactVersions.\(name)" }

    /// The spoken name of the group.
    static var label: String { MonarchStrings.string(key("label"), table: table) }
    /// The position.
    static func position(_ version: Int, of total: Int) -> String {
        MonarchStrings.string(key("position"), table: table, version, total)
    }
    /// The previous button.
    static var previous: String { MonarchStrings.string(key("previous"), table: table) }
    /// The next button.
    static var next: String { MonarchStrings.string(key("next"), table: table) }
    /// The version menu.
    static var select: String { MonarchStrings.string(key("select"), table: table) }
    /// The short name of a version.
    static func versionTag(_ version: Int) -> String {
        MonarchStrings.string(key("versionTag"), table: table, version)
    }
    /// The name of a version above the diff.
    static func versionName(_ version: Int) -> String {
        MonarchStrings.string(key("versionName"), table: table, version)
    }
    /// The compare toggle.
    static var compare: String { MonarchStrings.string(key("compare"), table: table) }
    /// Announced when the version changes.
    static func announce(_ version: Int, of total: Int) -> String {
        MonarchStrings.string(key("announce"), table: table, version, total)
    }
}
