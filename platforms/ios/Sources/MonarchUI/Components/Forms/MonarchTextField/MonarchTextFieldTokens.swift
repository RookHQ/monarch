import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchTextField``. The field box, label row and
/// supporting text are the shared field chrome (`Internal/MonarchFieldChrome`).
enum MonarchTextFieldTokens {
    /// The least height of the field, which is also its touch target.
    static let minHeight: CGFloat = max(MonarchTokens.Control.lg, MonarchAccessibility.minimumHitTarget)
    /// Padding before and after the text.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the text.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the label, the field and the supporting text.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between an accessory and the text.
    static let accessorySpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The SF Symbol shown before an error message.
    static let errorSymbol = "exclamationmark.circle"

    /// The border role for a state.
    static func border(hasError: Bool, isEnabled: Bool) -> MonarchColorRole {
        MonarchFieldChrome.border(hasError: hasError, isEnabled: isEnabled, isReadOnly: false)
    }

    /// The fill role for a state.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        MonarchFieldChrome.background(isEnabled: isEnabled, isReadOnly: false)
    }

    /// The colour of the text.
    static func textColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour of a read-only value, or of its prompt when it is empty.
    static func readOnlyColor(isEmpty: Bool) -> MonarchColorRole {
        isEmpty ? \.textSubtle : \.textDefault
    }

    /// The colour of the accessories.
    static func accessoryColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// Whether the visible label is drawn: unless it is hidden for a `Form` row.
    static func showsLabel(_ visibility: Visibility) -> Bool {
        visibility != .hidden
    }

    /// The line limit: one line across; for a multiline field the given
    /// range with at least one line, or `nil` to grow without a limit.
    static func lineLimit(axis: Axis, range: ClosedRange<Int>?) -> ClosedRange<Int>? {
        guard axis == .vertical else { return 1...1 }
        guard let range else { return nil }
        return max(1, range.lowerBound)...max(1, range.upperBound)
    }

    /// What VoiceOver reads after the label and value: the error if there is
    /// one, otherwise the helper text.
    static func accessibilityHint(helper: String?, error: String?) -> String {
        if let error, !error.isEmpty { return MonarchTextFieldStrings.error(error) }
        return helper ?? ""
    }
}
