import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchInfoTooltip``.
enum MonarchInfoTooltipTokens {
    /// The type role of the explanation.
    static let textStyle: MonarchTextStyle = .body
    /// The padding inside the bubble.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The width the explanation wraps at: three times the largest spacing step, about 288 points.
    static let idealWidth: CGFloat = MonarchTokens.Space.space24 * 3
    /// The colour of the explanation.
    static var textColor: MonarchColorRole { \.textDefault }
    /// The background of the bubble, the raised surface.
    static var bubbleColor: MonarchColorRole { \.bgRaised }

    /// The SF Symbol of an icon.
    static func symbol(_ icon: MonarchInfoTooltipIcon) -> String {
        switch icon {
        case .info: "info.circle"
        case .help: "questionmark.circle"
        }
    }

    /// The colour of the glyph: muted at rest, the default text colour while pressed or open.
    static func iconColor(isPressed: Bool, isOpen: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return isPressed || isOpen ? \.textDefault : \.textMuted
    }

    /// The spoken name of the button.
    static func label(_ label: String?) -> String {
        label ?? MonarchInfoTooltipStrings.label
    }
}
