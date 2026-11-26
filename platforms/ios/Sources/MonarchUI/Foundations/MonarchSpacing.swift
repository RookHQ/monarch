import CoreGraphics
import MonarchTokens

/// The spacing scale in points, on a 4-point grid.
///
/// Read it from the theme: `theme.spacing.space4`. Names match the generated
/// tokens, including the half steps such as `space0_5`.
public struct MonarchSpacing: Sendable, Hashable {
    /// 0 points.
    public let space0: CGFloat = MonarchTokens.Space.space0
    /// 1 point, for hairlines and borders.
    public let px: CGFloat = MonarchTokens.Space.px
    // swift-format-ignore: AlwaysUseLowerCamelCase
    /// 2 points.
    public let space0_5: CGFloat = MonarchTokens.Space.space0_5
    /// 4 points.
    public let space1: CGFloat = MonarchTokens.Space.space1
    // swift-format-ignore: AlwaysUseLowerCamelCase
    /// 6 points.
    public let space1_5: CGFloat = MonarchTokens.Space.space1_5
    /// 8 points.
    public let space2: CGFloat = MonarchTokens.Space.space2
    // swift-format-ignore: AlwaysUseLowerCamelCase
    /// 10 points.
    public let space2_5: CGFloat = MonarchTokens.Space.space2_5
    /// 12 points.
    public let space3: CGFloat = MonarchTokens.Space.space3
    /// 16 points.
    public let space4: CGFloat = MonarchTokens.Space.space4
    /// 20 points.
    public let space5: CGFloat = MonarchTokens.Space.space5
    /// 24 points.
    public let space6: CGFloat = MonarchTokens.Space.space6
    /// 32 points.
    public let space8: CGFloat = MonarchTokens.Space.space8
    /// 40 points.
    public let space10: CGFloat = MonarchTokens.Space.space10
    /// 48 points.
    public let space12: CGFloat = MonarchTokens.Space.space12
    /// 64 points.
    public let space16: CGFloat = MonarchTokens.Space.space16
    /// 80 points.
    public let space20: CGFloat = MonarchTokens.Space.space20
    /// 96 points.
    public let space24: CGFloat = MonarchTokens.Space.space24

    /// The scale from the generated tokens.
    public init() {}
}
