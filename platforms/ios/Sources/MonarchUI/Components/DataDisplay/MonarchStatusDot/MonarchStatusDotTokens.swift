import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchStatusDot``.
enum MonarchStatusDotTokens {
    /// The diameter of the dot before Dynamic Type is applied.
    static func dotSize(_ size: MonarchStatusDotSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space2
        case .md: MonarchTokens.Space.space2_5
        }
    }

    /// The gap between the dot and the label.
    static func spacing(_ size: MonarchStatusDotSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1_5
        case .md: MonarchTokens.Space.space2
        }
    }

    /// The type role of the label.
    static func textStyle(_ size: MonarchStatusDotSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .label
        }
    }

    /// The weight of the label.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.regular)
    /// The label role.
    static var label: MonarchColorRole { \.textDefault }
    /// How far the pulse ring grows, as a multiple of the dot.
    static let pulseScale: CGFloat = 2.5
    /// How long one pulse takes, in seconds.
    static let pulseDuration: TimeInterval = TimeInterval(MonarchTokens.Duration.shimmer) / 1000
    /// A string with the line height of the label and no glyph, used to
    /// centre the dot on the first line of the label.
    static let lineBox = " "

    /// The role of the dot for a tone: the solid colour of the tone, or the
    /// subtle text colour for the neutral tone.
    static func dot(tone: MonarchTone) -> MonarchColorRole {
        tone == .neutral ? \.textSubtle : MonarchTonePalette.palette(for: tone).solid
    }
}
