/// Replacements for the built-in strings of ``MonarchThemeToggle``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchThemeToggleLabels: Sendable, Hashable {
    /// The name of the control. Built in: "Theme".
    public var title: String?
    /// The name of the light choice. Built in: "Light".
    public var light: String?
    /// The name of the dark choice. Built in: "Dark".
    public var dark: String?
    /// The name of the choice that follows the device. Built in: "System".
    public var system: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(title: String? = nil, light: String? = nil, dark: String? = nil, system: String? = nil) {
        self.title = title
        self.light = light
        self.dark = dark
        self.system = system
    }
}
