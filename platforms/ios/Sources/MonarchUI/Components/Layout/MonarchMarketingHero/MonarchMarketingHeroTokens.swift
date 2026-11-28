import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchMarketingHero``.
enum MonarchMarketingHeroTokens {
    /// The gap between the eyebrow, the title, the lead, the actions and the note.
    static let spacing: CGFloat = MonarchTokens.Space.space5
    /// The gap between the eyebrow and the title.
    static let eyebrowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding around the text, inside the backdrop.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// The padding above and below the text.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space12
    /// The corner radius of the backdrop.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.xl
    /// The type role of the title.
    static let titleStyle: MonarchTextStyle = .display
    /// The type role of the lead.
    static let leadStyle: MonarchTextStyle = .subheading
    /// The weight of the lead.
    static let leadWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.regular)
    /// The type role of the note.
    static let noteStyle: MonarchTextStyle = .bodySmall
    /// The colour of the eyebrow.
    static var eyebrowColor: MonarchColorRole { \.accentStrong }
    /// The colour of the title.
    static var titleColor: MonarchColorRole { \.textDefault }
    /// The colour of the lead and the note.
    static var supportingColor: MonarchColorRole { \.textMuted }
    /// The colour at the top of the backdrop; it fades to clear.
    static var backdropColor: MonarchColorRole { \.accentSubtle }
}
