import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchMarketingSection`` and the blocks that go in it.
enum MonarchMarketingSectionTokens {
    /// The gap between the header and the content of a section.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space8
    /// The gap between the eyebrow, the title and the description.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between cards of a feature grid, stats of a strip and questions of a list.
    static let gridSpacing: CGFloat = MonarchTokens.Space.space4
    /// The padding inside a feature card, a CTA band and a stat.
    static let cardPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the title and the description inside a card.
    static let cardTextSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the actions of a CTA band.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The height of the browser bar of a product frame.
    static let chromeHeight: CGFloat = MonarchTokens.Space.space8
    /// The number of window dots in the browser bar.
    static let chromeDotCount = 3
    /// The padding at the ends of the browser bar.
    static let chromePadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between a stat's value and its label.
    static let statTextSpacing: CGFloat = MonarchTokens.Space.space1
    /// The diameter of a window dot in the browser bar.
    static let chromeDot: CGFloat = MonarchTokens.Space.space2_5
    /// The gap between the window dots.
    static let chromeDotSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The type role of the eyebrow.
    static let eyebrowStyle: MonarchTextStyle = .label
    /// The weight of the eyebrow.
    static let eyebrowWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The type role of a section title.
    static let titleStyle: MonarchTextStyle = .title
    /// The type role of a section description.
    static let descriptionStyle: MonarchTextStyle = .body
    /// The type role of a stat value.
    static let statValueStyle: MonarchTextStyle = .title
    /// The colour of the eyebrow.
    static var eyebrowColor: MonarchColorRole { \.accentStrong }
    /// The colour of titles.
    static var titleColor: MonarchColorRole { \.textDefault }
    /// The colour of descriptions and stat labels.
    static var supportingColor: MonarchColorRole { \.textMuted }
    /// The fill of a CTA band.
    static var bandFill: MonarchColorRole { \.bgSubtle }
    /// The border of cards, frames and bands.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The fill of the browser bar of a product frame.
    static var chromeFill: MonarchColorRole { \.bgSubtle }
    /// The fill of the window dots.
    static var chromeDotFill: MonarchColorRole { \.borderDefault }

    /// The horizontal alignment of stacked text for an alignment.
    static func horizontal(_ alignment: MonarchMarketingAlignment) -> HorizontalAlignment {
        switch alignment {
        case .leading: .leading
        case .center: .center
        }
    }

    /// The multiline text alignment for an alignment.
    static func text(_ alignment: MonarchMarketingAlignment) -> TextAlignment {
        switch alignment {
        case .leading: .leading
        case .center: .center
        }
    }

    /// The frame alignment for an alignment.
    static func frame(_ alignment: MonarchMarketingAlignment) -> Alignment {
        switch alignment {
        case .leading: .leading
        case .center: .center
        }
    }

    /// The number of columns of a feature grid: the requested number, one at the accessibility text sizes.
    static func columns(requested: Int, dynamicTypeSize: DynamicTypeSize) -> Int {
        dynamicTypeSize.isAccessibilitySize ? 1 : max(1, requested)
    }
}
