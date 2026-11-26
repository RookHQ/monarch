import CoreGraphics
import MonarchTokens

/// Corner radii in points.
///
/// Read it from the theme: `theme.radius.md`.
public struct MonarchRadius: Sendable, Hashable {
    /// Square corners.
    public let none: CGFloat = MonarchTokens.Radius.none
    /// 4 points.
    public let xs: CGFloat = MonarchTokens.Radius.xs
    /// 6 points.
    public let sm: CGFloat = MonarchTokens.Radius.sm
    /// 8 points. Controls.
    public let md: CGFloat = MonarchTokens.Radius.md
    /// 12 points. Cards and banners.
    public let lg: CGFloat = MonarchTokens.Radius.lg
    /// 16 points.
    public let xl: CGFloat = MonarchTokens.Radius.xl
    /// 20 points.
    public let xl2: CGFloat = MonarchTokens.Radius.xl2
    /// A radius large enough to make any shape a capsule.
    public let full: CGFloat = MonarchTokens.Radius.full

    /// The radii from the generated tokens.
    public init() {}
}
