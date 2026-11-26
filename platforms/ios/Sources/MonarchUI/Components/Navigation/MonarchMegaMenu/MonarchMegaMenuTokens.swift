import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchMegaMenu``.
enum MonarchMegaMenuTokens {
    /// The gap between top-level entries.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space1
    /// The horizontal padding of a top-level entry.
    static let itemPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between a trigger's title and its chevron.
    static let triggerSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between a link's title, its badge and its arrow.
    static let badgeSpacing: CGFloat = MonarchTokens.Space.space2
    /// The turn of a trigger's chevron while its panel is open.
    static let openRotation: Angle = .degrees(180)
    /// The padding inside the panel.
    static let panelPadding: CGFloat = MonarchTokens.Space.space5
    /// The widest the panel gets.
    static let panelWidth: CGFloat = MonarchTokens.Space.space24 * 3 + MonarchTokens.Space.space16
    /// The gap between groups of the panel.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space5
    /// The gap between links of a group.
    static let linkSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the tile and the text of a link.
    static let linkIconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The vertical padding of a link row.
    static let linkPadding: CGFloat = MonarchTokens.Space.space2
    /// The side of a link's symbol tile.
    static let tileSize: CGFloat = MonarchTokens.Space.space8
    /// The padding inside the featured card.
    static let featuredPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the lines of the featured card.
    static let featuredSpacing: CGFloat = MonarchTokens.Space.space2
    /// The symbol of a trigger.
    static let chevronSymbol = "chevron.down"
    /// The symbol of the featured card's action.
    static let exploreSymbol = "chevron.right"
    /// The symbol after an external link.
    static let externalSymbol = "arrow.up.forward"
    /// The weight of group headings.
    static let headingWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The fill of an open trigger.
    static var openFill: MonarchColorRole { \.bgMuted }
    /// The fill of a link's symbol tile.
    static var tileFill: MonarchColorRole { \.bgMuted }
    /// The colour of a link's symbol.
    static var tileForeground: MonarchColorRole { \.accentStrong }
    /// The fill of the featured card.
    static var featuredFill: MonarchColorRole { \.bgMuted }
    /// The colour of group headings and descriptions.
    static var supportingColor: MonarchColorRole { \.textMuted }
    /// The fill of the panel.
    static var panelFill: MonarchColorRole { \.bgRaised }

    /// The colour of a top-level entry's title.
    static func triggerColor(isOpen: Bool) -> MonarchColorRole {
        isOpen ? \.textDefault : \.textMuted
    }

    /// What VoiceOver reads for a link: its title, its badge and its description.
    static func accessibilityLabel(_ link: MonarchMegaMenuLink) -> String {
        [link.label, link.badge, link.description].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
