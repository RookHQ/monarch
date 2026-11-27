import MonarchTokens
import SwiftUI

/// Everything a component needs to draw itself: colours, typography, spacing,
/// radii, elevation and motion for one resolved theme.
///
/// Components read it from the environment and never read `MonarchTokens`
/// directly:
///
/// ```swift
/// @Environment(\.monarchTheme) private var theme
/// ```
///
/// Install it with `monarchTheme(scheme:contrast:accent:typography:)`.
public struct MonarchTheme: Sendable, Hashable {
    /// The generated tokens this theme was built from.
    public let tokens: MonarchThemeTokens
    /// Semantic colours.
    public let colors: MonarchColors
    /// Font families used by ``MonarchTextStyle``.
    public let typography: MonarchTypography
    /// The spacing scale.
    public let spacing: MonarchSpacing
    /// Corner radii.
    public let radius: MonarchRadius
    /// Shadow layers for each elevation level.
    public let elevation: MonarchElevation
    /// Durations and curves.
    public let motion: MonarchMotion

    /// Builds a theme from generated tokens.
    public init(tokens: MonarchThemeTokens, typography: MonarchTypography = .system) {
        self.tokens = tokens
        self.colors = MonarchColors(tokens.colors)
        self.typography = typography
        self.spacing = MonarchSpacing()
        self.radius = MonarchRadius()
        self.elevation = MonarchElevation(tokens.shadows)
        self.motion = MonarchMotion()
    }

    /// The theme id: `light`, `dark`, `hc-light` or `hc-dark`.
    public var id: String { tokens.id }
    /// Whether the theme has a dark canvas.
    public var isDark: Bool { tokens.isDark }
    /// Whether the theme is one of the two high-contrast themes.
    public var isHighContrast: Bool { tokens.isHighContrast }
    /// The width of the focus ring in points.
    public var focusRingWidth: CGFloat { tokens.focusRingWidth }

    /// The light theme with the default accent.
    public static let light = MonarchTheme(tokens: MonarchTokens.light)
    /// The dark theme with the default accent.
    public static let dark = MonarchTheme(tokens: MonarchTokens.dark)
    /// The high-contrast light theme with the default accent.
    public static let highContrastLight = MonarchTheme(tokens: MonarchTokens.hcLight)
    /// The high-contrast dark theme with the default accent.
    public static let highContrastDark = MonarchTheme(tokens: MonarchTokens.hcDark)
}
