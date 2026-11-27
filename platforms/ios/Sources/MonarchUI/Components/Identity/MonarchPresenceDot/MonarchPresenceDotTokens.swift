import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchPresenceDot``.
enum MonarchPresenceDotTokens {
    /// The diameter before Dynamic Type is applied.
    static func diameter(_ size: MonarchPresenceDotSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space2
        case .md: MonarchTokens.Space.space2_5
        case .lg: MonarchTokens.Space.space3
        }
    }

    /// The width of the ring of the offline dot, and of the ring around an overlaid dot.
    static let ringWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The corner radius of the square drawn for busy when colour cannot be relied on.
    static let squareRadius: CGFloat = MonarchTokens.Space.px
    /// The width of the bar of do not disturb, as a part of the diameter.
    static let barWidthRatio: CGFloat = 0.6
    /// The height of the bar of do not disturb, as a part of the diameter.
    static let barHeightRatio: CGFloat = 0.2
    /// The filled part of the away dot when colour cannot be relied on.
    static let halfRatio: CGFloat = 0.5

    /// How a dot is drawn.
    enum Form: Equatable {
        /// A filled circle.
        case filled
        /// A circle filled on its leading half, with a ring.
        case half
        /// A filled square.
        case square
        /// A filled circle with a bar across it.
        case barred
        /// A ring.
        case ring
    }

    /// The form of a presence. By default online, away and busy are filled
    /// circles that differ by colour. With Differentiate Without Colour each
    /// presence has its own form.
    static func form(_ status: MonarchPresenceStatus, differentiateWithoutColor: Bool) -> Form {
        switch status {
        case .online: .filled
        case .away: differentiateWithoutColor ? .half : .filled
        case .busy: differentiateWithoutColor ? .square : .filled
        case .doNotDisturb: .barred
        case .offline: .ring
        }
    }

    /// The role of the fill, or of the ring of the offline dot.
    static func fill(_ status: MonarchPresenceStatus) -> MonarchColorRole {
        switch status {
        case .online: \.successSolid
        case .away: \.warningSolid
        case .busy, .doNotDisturb: \.dangerSolid
        case .offline: \.textSubtle
        }
    }

    /// The role of the bar of do not disturb.
    static var bar: MonarchColorRole { \.dangerOnSolid }
    /// The role of the inside of a ring, and of the ring around an overlaid dot.
    static var surface: MonarchColorRole { \.bgSurface }

    /// The text of a presence: its name, joined with the person's status
    /// message when there is one, such as "Away · In a meeting".
    static func label(_ status: MonarchPresenceStatus, customStatus: String?, statusLabel: String?) -> String {
        let presence = statusLabel ?? MonarchPresenceDotStrings.name(status)
        let custom = customStatus?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return custom.isEmpty ? presence : MonarchPresenceDotStrings.withStatus(presence, custom)
    }
}
