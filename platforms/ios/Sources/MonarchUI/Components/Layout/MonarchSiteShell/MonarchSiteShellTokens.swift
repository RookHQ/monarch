import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchSiteShell`` and ``MonarchSiteFooter``.
enum MonarchSiteShellTokens {
    /// The gap between the content and the footer.
    static let footerSpacing: CGFloat = MonarchTokens.Space.space10
    /// The padding around the footer.
    static let footerPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between footer columns, and between the columns and the legal line.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space6
    /// The narrowest a footer column gets.
    static let columnMinimumWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space12
    /// The gap between a column title and its links.
    static let linkSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the items of the legal line.
    static let legalSpacing: CGFloat = MonarchTokens.Space.space5
    /// The symbol of the menu button.
    static let menuSymbol = "line.3.horizontal"
    /// The symbol beside a submenu's title.
    static let submenuSymbol = "chevron.down"
    /// The colour of footer links and the legal line.
    static var mutedColor: MonarchColorRole { \.textMuted }
    /// The colour of footer column titles.
    static var titleColor: MonarchColorRole { \.textDefault }
}
