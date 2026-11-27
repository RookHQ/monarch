import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchUsernameAvailabilityInput``. The
/// sizes are those of the text field, so the two line up in a form.
enum MonarchUsernameAvailabilityInputTokens {
    /// The least height of the field, which is also its touch target.
    static let minHeight: CGFloat = max(MonarchTokens.Control.lg, MonarchAccessibility.minimumHitTarget)
    /// Padding before and after the content of the field.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the text.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the prefix, the text and the status icon.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the label, the field and the supporting text.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the message and the retry button.
    static let messageSpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// How long typing must pause before the check runs, as on the web.
    static let debounce: Duration = .milliseconds(MonarchTokens.Duration.slower)

    /// The status for an answer of the check. No answer means there was nothing to check.
    static func status(for result: MonarchUsernameAvailabilityResult?) -> MonarchUsernameAvailabilityStatus {
        guard let result else { return .idle }
        return result.isValid ? .valid : .invalid
    }

    /// The built-in texts with the app's replacements applied.
    struct Labels: Equatable {
        var checking: String?
        var valid: String?
        var invalid: String?
        var error: String?
    }

    /// The text under the field for a status: the check's own message for a
    /// settled answer, otherwise the text of the status. Empty when idle.
    static func text(status: MonarchUsernameAvailabilityStatus, message: String?, labels: Labels = Labels()) -> String {
        switch status {
        case .idle: ""
        case .checking: labels.checking ?? MonarchUsernameAvailabilityInputStrings.checking
        case .valid: message ?? labels.valid ?? MonarchUsernameAvailabilityInputStrings.valid
        case .invalid: message ?? labels.invalid ?? MonarchUsernameAvailabilityInputStrings.invalid
        case .error: message ?? labels.error ?? MonarchUsernameAvailabilityInputStrings.error
        }
    }

    /// What to announce when the status changes: settled answers only.
    /// "Checking" on every pause would be noise.
    static func announcement(
        status: MonarchUsernameAvailabilityStatus,
        message: String?,
        labels: Labels = Labels()
    ) -> String? {
        switch status {
        case .idle, .checking: nil
        case .valid, .invalid, .error: text(status: status, message: message, labels: labels)
        }
    }

    /// The SF Symbol at the end of the field, or `nil` for none. While
    /// checking, a progress indicator is shown instead.
    static func symbol(_ status: MonarchUsernameAvailabilityStatus) -> String? {
        switch status {
        case .idle, .checking: nil
        case .valid: "checkmark.circle.fill"
        case .invalid: "exclamationmark.triangle.fill"
        case .error: "info.circle"
        }
    }

    /// The role of the status icon.
    static func iconForeground(_ status: MonarchUsernameAvailabilityStatus) -> MonarchColorRole {
        switch status {
        case .idle, .checking: \.textMuted
        case .valid: \.successStrong
        case .invalid: \.dangerStrong
        case .error: \.warningStrong
        }
    }

    /// The role of the message under the field.
    static func messageForeground(_ status: MonarchUsernameAvailabilityStatus) -> MonarchColorRole {
        switch status {
        case .idle, .checking, .error: \.textMuted
        case .valid: \.successStrong
        case .invalid: \.dangerStrong
        }
    }

    /// The border role for a state.
    static func border(status: MonarchUsernameAvailabilityStatus, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.borderSubtle }
        switch status {
        case .invalid: return \.dangerStrong
        case .valid: return \.successBorder
        case .idle, .checking, .error: return \.borderInput
        }
    }

    /// The fill role for a state.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.bgSurface : \.bgSubtle
    }

    /// What VoiceOver reads after the label and value: the prefix, the helper
    /// text and the status text, in that order.
    static func accessibilityHint(prefix: String?, helper: String?, statusText: String) -> String {
        [prefix, helper, statusText].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
