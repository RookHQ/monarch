/// Replacements for the built-in strings of ``MonarchAppearancePanel``.
///
/// Every property is `nil` by default, which keeps the built-in string. The
/// names of the light, dark and system themes default to those of
/// ``MonarchThemeToggle``.
public struct MonarchAppearancePanelLabels: Sendable, Hashable {
    /// The theme row. Built in: "Theme".
    public var theme: String?
    /// Built in: "Light, dark, or match your device."
    public var themeDescription: String?
    /// The light theme. Built in: "Light".
    public var light: String?
    /// The dark theme. Built in: "Dark".
    public var dark: String?
    /// The theme that follows the device. Built in: "System".
    public var system: String?
    /// The contrast row. Built in: "Contrast".
    public var contrast: String?
    /// Built in: "Stronger text and border colours."
    public var contrastDescription: String?
    /// Built in: "Standard".
    public var contrastStandard: String?
    /// Built in: "More".
    public var contrastMore: String?
    /// Built in: "System".
    public var contrastSystem: String?
    /// The accent row. Built in: "Accent".
    public var accent: String?
    /// Built in: "The colour of buttons, links and selection."
    public var accentDescription: String?
    /// The motion row. Built in: "Motion".
    public var motion: String?
    /// Built in: "Reduce animation and movement."
    public var motionDescription: String?
    /// Built in: "System".
    public var motionSystem: String?
    /// Built in: "Reduce".
    public var motionReduce: String?
    /// The density row. Built in: "Density".
    public var density: String?
    /// Built in: "Tighter spacing fits more on screen."
    public var densityDescription: String?
    /// Built in: "Comfortable".
    public var densityComfortable: String?
    /// Built in: "Compact".
    public var densityCompact: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        theme: String? = nil,
        themeDescription: String? = nil,
        light: String? = nil,
        dark: String? = nil,
        system: String? = nil,
        contrast: String? = nil,
        contrastDescription: String? = nil,
        contrastStandard: String? = nil,
        contrastMore: String? = nil,
        contrastSystem: String? = nil,
        accent: String? = nil,
        accentDescription: String? = nil,
        motion: String? = nil,
        motionDescription: String? = nil,
        motionSystem: String? = nil,
        motionReduce: String? = nil,
        density: String? = nil,
        densityDescription: String? = nil,
        densityComfortable: String? = nil,
        densityCompact: String? = nil
    ) {
        self.theme = theme
        self.themeDescription = themeDescription
        self.light = light
        self.dark = dark
        self.system = system
        self.contrast = contrast
        self.contrastDescription = contrastDescription
        self.contrastStandard = contrastStandard
        self.contrastMore = contrastMore
        self.contrastSystem = contrastSystem
        self.accent = accent
        self.accentDescription = accentDescription
        self.motion = motion
        self.motionDescription = motionDescription
        self.motionSystem = motionSystem
        self.motionReduce = motionReduce
        self.density = density
        self.densityDescription = densityDescription
        self.densityComfortable = densityComfortable
        self.densityCompact = densityCompact
    }
}
