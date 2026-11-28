import MonarchTokens
import SwiftUI

/// The independent appearance inputs: scheme, contrast, accent and an
/// optional AI colour.
///
/// Resolution is a pure function of these inputs and the two system settings,
/// so the rule can be tested without a device.
public struct MonarchAppearance: Sendable, Hashable {
    /// Light, dark or the device setting.
    public var scheme: MonarchSchemePreference
    /// Standard, high contrast or the device setting.
    public var contrast: MonarchContrastPreference
    /// The brand accent.
    public var accent: MonarchAccent
    /// A custom AI colour, such as `MonarchBrandTheme.aiAccent`, or `nil`
    /// for the theme's own AI roles.
    public var ai: MonarchAccent?

    /// An appearance. The defaults follow the device and use the default
    /// accent and AI colours.
    public init(
        scheme: MonarchSchemePreference = .system,
        contrast: MonarchContrastPreference = .system,
        accent: MonarchAccent = MonarchTokens.Accents.rook,
        ai: MonarchAccent? = nil
    ) {
        self.scheme = scheme
        self.contrast = contrast
        self.accent = accent
        self.ai = ai
    }

    /// The theme tokens for this appearance.
    ///
    /// - Parameters:
    ///   - systemIsDark: Whether the device is in Dark Mode.
    ///   - systemIncreasesContrast: Whether Increase Contrast is on.
    /// - Returns: One of the four themes, with the accent and then the AI colour applied.
    public func resolve(systemIsDark: Bool, systemIncreasesContrast: Bool) -> MonarchThemeTokens {
        let isDark =
            switch scheme {
            case .system: systemIsDark
            case .light: false
            case .dark: true
            }
        let isHighContrast =
            switch contrast {
            case .system: systemIncreasesContrast
            case .standard: false
            case .more: true
            }
        let base =
            switch (isDark, isHighContrast) {
            case (false, false): MonarchTokens.light
            case (true, false): MonarchTokens.dark
            case (false, true): MonarchTokens.hcLight
            case (true, true): MonarchTokens.hcDark
            }
        let accented = base.withAccent(accent)
        guard let ai else { return accented }
        return accented.withAI(ai)
    }

    /// The theme tokens for this appearance and the SwiftUI environment values.
    public func resolve(colorScheme: ColorScheme, colorSchemeContrast: ColorSchemeContrast) -> MonarchThemeTokens {
        resolve(
            systemIsDark: colorScheme == .dark,
            systemIncreasesContrast: colorSchemeContrast == .increased
        )
    }
}
