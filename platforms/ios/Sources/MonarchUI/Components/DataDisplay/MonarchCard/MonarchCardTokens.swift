import CoreGraphics
import MonarchTokens

/// Component tokens for ``MonarchCard``.
enum MonarchCardTokens {
    /// Padding around the content.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the footer.
    static let footerVerticalPadding: CGFloat = MonarchTokens.Space.space3

    /// The padding around the content for a padding option.
    static func contentPadding(_ option: MonarchCardPadding) -> CGFloat {
        switch option {
        case .standard: padding
        case .flush: 0
        }
    }
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The fill role.
    static var background: MonarchColorRole { \.bgSurface }
    /// The border role.
    static var border: MonarchColorRole { \.borderSubtle }
}
