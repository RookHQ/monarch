import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchRookBar``.
enum MonarchRookBarTokens {
    /// The horizontal padding of the bar.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// The vertical padding of the bar.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of the bar.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the mark and the brand name.
    static let brandSpacing: CGFloat = MonarchTokens.Space.space2
    /// The side of the brand mark.
    static let markSize: CGFloat = MonarchTokens.Space.space6 + MonarchTokens.Space.space1
    /// The gap between the lines of a link row in the menu.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The symbol of the menu button.
    static let menuSymbol = "line.3.horizontal"
    /// The symbol of the close button.
    static let closeSymbol = "xmark"
    /// The symbol at the end of a link row.
    static let chevronSymbol = "chevron.right"
    /// The symbol after an external link.
    static let externalSymbol = "arrow.up.forward"
    /// The weight of the brand name.
    static let brandWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The fill of the bar.
    static var fill: MonarchColorRole { \.bgSurface }
    /// The hairline under the bar.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The fill of the letter mark.
    static var markFill: MonarchColorRole { \.bgMuted }
    /// The colour of descriptions.
    static var supportingColor: MonarchColorRole { \.textMuted }

    /// The first letter of the brand, for the letter mark.
    static func initial(_ brand: String) -> String {
        brand.first.map { String($0).uppercased() } ?? ""
    }

    /// What VoiceOver reads for a link row: its title, its description and its meta line.
    static func accessibilityLabel(_ link: MonarchRookBarLink) -> String {
        [link.label, link.description, link.meta].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
