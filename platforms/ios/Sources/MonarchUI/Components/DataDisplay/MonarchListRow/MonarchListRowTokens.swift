import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchListRow``.
enum MonarchListRowTokens {
    /// The least height of a row, which is also its touch target.
    static let minHeight: CGFloat = MonarchAccessibility.minimumHitTarget
    /// Padding before and after the content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the text, so wrapped text keeps a margin.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the leading view, the text and the trailing view.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The width of the leading icon.
    static let leadingWidth: CGFloat = MonarchTokens.Space.space6
    /// The gap between the title and the supporting text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The SF Symbol of the chevron that marks a row that opens something.
    static let chevronSymbol = "chevron.right"
    /// The width of the divider.
    static let dividerWidth: CGFloat = MonarchShapes.borderWidth

    /// Where the divider starts: under the text, past the leading icon, as
    /// the rows of an iOS list, so the icons stand in one clean column.
    static func dividerLeadingInset(hasIcon: Bool, leadingWidth: CGFloat = MonarchListRowTokens.leadingWidth) -> CGFloat
    {
        horizontalPadding + (hasIcon ? leadingWidth + spacing : 0)
    }

    /// The colour of the title.
    static func titleColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour of the supporting text and the chevron.
    static func supportingColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The colour of the leading icon.
    static func iconColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The colour of the divider.
    static var dividerColor: MonarchColorRole { \.borderSubtle }
}
