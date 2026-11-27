/// Built-in strings of the appearance panel, from the `MonarchAppearancePanel` strings table.
enum MonarchAppearancePanelStrings {
    private static let table = "MonarchAppearancePanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.appearancePanel.\(key)", table: table)
    }

    /// The theme row.
    static var theme: String { string("theme") }
    /// The theme row's description.
    static var themeDescription: String { string("themeDescription") }
    /// The contrast row.
    static var contrast: String { string("contrast") }
    /// The contrast row's description.
    static var contrastDescription: String { string("contrastDescription") }
    /// Standard contrast.
    static var contrastStandard: String { string("contrastStandard") }
    /// More contrast.
    static var contrastMore: String { string("contrastMore") }
    /// Contrast that follows the device.
    static var contrastSystem: String { string("contrastSystem") }
    /// The accent row.
    static var accent: String { string("accent") }
    /// The accent row's description.
    static var accentDescription: String { string("accentDescription") }
    /// The motion row.
    static var motion: String { string("motion") }
    /// The motion row's description.
    static var motionDescription: String { string("motionDescription") }
    /// Motion that follows the device.
    static var motionSystem: String { string("motionSystem") }
    /// Reduced motion.
    static var motionReduce: String { string("motionReduce") }
    /// The density row.
    static var density: String { string("density") }
    /// The density row's description.
    static var densityDescription: String { string("densityDescription") }
    /// Comfortable density.
    static var densityComfortable: String { string("densityComfortable") }
    /// Compact density.
    static var densityCompact: String { string("densityCompact") }
}
