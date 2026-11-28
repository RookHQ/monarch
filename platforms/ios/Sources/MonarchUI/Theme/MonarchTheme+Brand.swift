import MonarchTokens
import SwiftUI

extension View {
    /// Installs a Monarch theme whose accent is derived from one brand colour.
    ///
    /// The colour runs through `MonarchBrandTheme` (in MonarchTokens), the same engine the web's
    /// `deriveTheme` uses, so the accent meets Monarch's contrast guarantees in
    /// the light, dark and both high-contrast themes. An invalid colour, such as
    /// an empty string or a named colour, silently gives the default accent.
    ///
    /// ```swift
    /// ContentView()
    ///     .monarchTheme(brand: workspace.brandColor)
    /// ```
    ///
    /// - Parameters:
    ///   - brand: The brand colour, `#rgb` or `#rrggbb`.
    ///   - scheme: Light, dark or the device setting.
    ///   - contrast: Standard, high contrast or the device setting.
    ///   - ai: A custom AI colour, applied after the brand accent. `nil` keeps the theme's AI roles.
    ///   - typography: The font families. The default is the system typeface.
    /// - Returns: The themed view.
    public func monarchTheme(
        brand: String,
        scheme: MonarchSchemePreference = .system,
        contrast: MonarchContrastPreference = .system,
        ai: MonarchAccent? = nil,
        typography: MonarchTypography = .system
    ) -> some View {
        monarchTheme(
            scheme: scheme,
            contrast: contrast,
            accent: MonarchAccent(brand: brand),
            ai: ai,
            typography: typography
        )
    }
}

extension MonarchTheme {
    /// A theme with the accent derived from a brand colour, for a pinned theme
    /// such as a preview or a screenshot. Invalid input gives the default accent.
    ///
    /// - Parameters:
    ///   - tokens: One of the four themes, for example `MonarchTokens.dark`.
    ///   - brand: The brand colour, `#rgb` or `#rrggbb`.
    ///   - typography: The font families.
    public init(tokens: MonarchThemeTokens, brand: String, typography: MonarchTypography = .system) {
        self.init(tokens: tokens.withAccent(MonarchAccent(brand: brand)), typography: typography)
    }
}
