import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchMethodBadge``.
enum MonarchMethodBadgeTokens {
    /// Padding before and after the method.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// Padding above and below the method.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The smallest width before Dynamic Type, so the common methods line up
    /// in a column.
    static let minimumWidth: CGFloat = MonarchTokens.Space.space16
    /// The type role of the method: the code typeface.
    static let textStyle: MonarchTextStyle = .code
    /// The weight of the method.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)

    /// The shape of the badge: a 6-point continuous corner.
    static var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.sm, style: .continuous)
    }

    /// The method as it is shown: without surrounding white space and in
    /// upper case. HTTP methods are ASCII, so the conversion does not depend
    /// on the locale.
    static func text(_ method: String) -> String {
        method.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
    }

    /// The tone of a method, matched in any case. Reads are success, creates
    /// info, updates warning, deletes danger, everything else neutral.
    static func tone(for method: String) -> MonarchTone {
        switch text(method) {
        case "GET": .success
        case "POST": .info
        case "PUT", "PATCH": .warning
        case "DELETE": .danger
        default: .neutral
        }
    }

    /// The colour roles for a tone: the tinted fill and strong text of the
    /// tone, with the tone's border. The neutral tone has no border.
    static func appearance(tone: MonarchTone) -> MonarchSurfaceAppearance {
        let palette = MonarchTonePalette.palette(for: tone)
        return MonarchSurfaceAppearance(
            background: palette.subtle,
            foreground: palette.strong,
            border: tone == .neutral ? nil : palette.border
        )
    }
}
