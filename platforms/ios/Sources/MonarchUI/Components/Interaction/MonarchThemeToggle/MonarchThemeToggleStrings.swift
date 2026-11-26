/// Built-in strings of the theme toggle, from the `MonarchThemeToggle` strings table.
enum MonarchThemeToggleStrings {
    private static let table = "MonarchThemeToggle"

    /// The name of the control.
    static var title: String { MonarchStrings.string("monarch.themeToggle.title", table: table) }
    /// The name of the light choice.
    static var light: String { MonarchStrings.string("monarch.themeToggle.light", table: table) }
    /// The name of the dark choice.
    static var dark: String { MonarchStrings.string("monarch.themeToggle.dark", table: table) }
    /// The name of the choice that follows the device.
    static var system: String { MonarchStrings.string("monarch.themeToggle.system", table: table) }
}
