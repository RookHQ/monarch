import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchBadge``.
enum MonarchBadgeTokens {
    /// Padding before and after the text.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the text of a small badge.
    static let smallHorizontalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The diameter of the status dot.
    static let dotSize: CGFloat = 6
    /// Padding above and below the text.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the icon and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The weight of the text.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)

    /// The colour roles for a tone and variant, from the shared tone palette.
    static func appearance(tone: MonarchTone, variant: MonarchBadgeVariant) -> MonarchSurfaceAppearance {
        let emphasis: MonarchToneEmphasis =
            switch variant {
            case .subtle: .subtle
            case .solid: .solid
            case .outline: .outline
            }
        return MonarchTonePalette.surface(tone: tone, emphasis: emphasis)
    }
}
