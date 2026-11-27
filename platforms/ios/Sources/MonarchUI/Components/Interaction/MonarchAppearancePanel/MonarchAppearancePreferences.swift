import MonarchTokens

/// Every appearance choice a ``MonarchAppearancePanel`` edits.
///
/// Scheme, contrast and accent are the inputs of the Monarch theme: pass
/// ``appearance`` to `.monarchTheme(_:)`. Motion and density are the app's to
/// apply, because SwiftUI does not let an app change the Reduce Motion
/// setting or the spacing of other views.
public struct MonarchAppearancePreferences: Sendable, Hashable {
    /// Light, dark or the device setting.
    public var scheme: MonarchSchemePreference
    /// Standard, more or the device setting.
    public var contrast: MonarchContrastPreference
    /// The brand accent.
    public var accent: MonarchAccent
    /// The device setting or always reduced.
    public var motion: MonarchAppearanceMotion
    /// Comfortable or compact spacing.
    public var density: MonarchAppearanceDensity

    /// Appearance choices. The defaults follow the device settings and use
    /// the default accent.
    public init(
        scheme: MonarchSchemePreference = .system,
        contrast: MonarchContrastPreference = .system,
        accent: MonarchAccent = MonarchTokens.Accents.rook,
        motion: MonarchAppearanceMotion = .system,
        density: MonarchAppearanceDensity = .comfortable
    ) {
        self.scheme = scheme
        self.contrast = contrast
        self.accent = accent
        self.motion = motion
        self.density = density
    }

    /// The theme inputs: scheme, contrast and accent, for `.monarchTheme(_:)`.
    public var appearance: MonarchAppearance {
        MonarchAppearance(scheme: scheme, contrast: contrast, accent: accent)
    }

    /// Whether the app should reduce motion: always with `reduce`, otherwise
    /// as the device setting says.
    ///
    /// - Parameter systemReducesMotion: The `accessibilityReduceMotion` environment value.
    /// - Returns: `true` when animation and movement should be reduced.
    public func reducesMotion(systemReducesMotion: Bool) -> Bool {
        switch motion {
        case .system: systemReducesMotion
        case .reduce: true
        }
    }
}
