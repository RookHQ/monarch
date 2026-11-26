import MonarchTokens
import SwiftUI

/// Resolves the appearance against the system settings and installs the theme.
struct MonarchThemeModifier: ViewModifier {
    let appearance: MonarchAppearance
    let typography: MonarchTypography

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    func body(content: Content) -> some View {
        let tokens = appearance.resolve(colorScheme: colorScheme, colorSchemeContrast: colorSchemeContrast)
        let theme = MonarchTheme(tokens: tokens, typography: typography)
        content
            .environment(\.monarchTheme, theme)
            .environment(\.colorScheme, theme.isDark ? .dark : .light)
            // accent-strong, the colour of a checked control on the web:
            // the on track of a switch and the filled part of a slider or
            // stepper take it.
            .tint(theme.colors.accentStrong)
    }
}

extension View {
    /// Installs a Monarch theme for this view and its descendants.
    ///
    /// The theme follows Dark Mode and Increase Contrast unless `scheme` or
    /// `contrast` fix them. The modifier also sets `tint` to the strong accent,
    /// so system controls such as `Toggle`, `Slider` and `Stepper` take the
    /// colour of a checked Monarch control,
    /// and sets the colour scheme of the subtree to match the resolved theme.
    ///
    /// ```swift
    /// ContentView()
    ///     .monarchTheme(accent: MonarchTokens.Accents.neutral)
    /// ```
    ///
    /// - Parameters:
    ///   - scheme: Light, dark or the device setting.
    ///   - contrast: Standard, high contrast or the device setting.
    ///   - accent: The brand accent.
    ///   - ai: A custom AI colour, applied after the accent. `nil` keeps the theme's AI roles.
    ///   - typography: The font families. The default is the system typeface.
    /// - Returns: The themed view.
    public func monarchTheme(
        scheme: MonarchSchemePreference = .system,
        contrast: MonarchContrastPreference = .system,
        accent: MonarchAccent = MonarchTokens.Accents.rook,
        ai: MonarchAccent? = nil,
        typography: MonarchTypography = .system
    ) -> some View {
        modifier(
            MonarchThemeModifier(
                appearance: MonarchAppearance(scheme: scheme, contrast: contrast, accent: accent, ai: ai),
                typography: typography
            )
        )
    }

    /// Installs a Monarch theme from an appearance value.
    public func monarchTheme(
        _ appearance: MonarchAppearance,
        typography: MonarchTypography = .system
    ) -> some View {
        modifier(MonarchThemeModifier(appearance: appearance, typography: typography))
    }
}
