import MonarchTokens
import SwiftUI

#if canImport(UIKit)
    import UIKit
#endif

/// Component tokens and logic for ``MonarchPasswordInput``. The sizes are
/// those of the text field, so the two line up in a form.
enum MonarchPasswordInputTokens {
    /// The least height of the field, which is also its touch target.
    static let minHeight: CGFloat = max(MonarchTokens.Control.lg, MonarchAccessibility.minimumHitTarget)
    /// Padding before the text.
    static let leadingPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the text.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the label, the field and the supporting text.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The SF Symbol shown before an error message.
    static let errorSymbol = "exclamationmark.circle"

    /// The SF Symbol of the reveal toggle: what activating it will do.
    static func revealSymbol(isRevealed: Bool) -> String {
        isRevealed ? "eye.slash" : "eye"
    }

    /// The border role for a state.
    static func border(hasError: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.borderSubtle }
        return hasError ? \.dangerStrong : \.borderInput
    }

    /// The fill role for a state.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.bgSurface : \.bgSubtle
    }

    /// The role of the typed text.
    static func foreground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The role of the reveal symbol.
    static func revealForeground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// What VoiceOver reads after the label and value: the error if there is
    /// one, otherwise the helper text.
    static func accessibilityHint(helper: String?, error: String?) -> String {
        if let error, !error.isEmpty { return MonarchPasswordInputStrings.error(error) }
        return helper ?? ""
    }

    #if canImport(UIKit)
        /// The autofill content type for a purpose.
        static func contentType(_ purpose: MonarchPasswordInputPurpose) -> UITextContentType {
            switch purpose {
            case .current: .password
            case .new: .newPassword
            }
        }
    #endif
}
