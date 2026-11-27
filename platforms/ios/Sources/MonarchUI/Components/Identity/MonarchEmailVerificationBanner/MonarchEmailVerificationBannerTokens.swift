import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchEmailVerificationBanner``. The surface,
/// padding and icon follow ``MonarchCalloutBanner`` so the two match.
enum MonarchEmailVerificationBannerTokens {
    /// The gap between the text, the status and the buttons.
    static let blockSpacing: CGFloat = MonarchCalloutBannerTokens.actionSpacing
    /// The gap between buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a status icon and its text.
    static let statusIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the sent status.
    static let sentSymbol = "checkmark.circle.fill"
    /// The SF Symbol of the error status.
    static let errorSymbol = "exclamationmark.circle.fill"

    /// The title of the resend button.
    static func resendTitle(remaining: Int, labels: MonarchEmailVerificationBannerLabels) -> String {
        if remaining > 0 {
            return labels.resendIn?(remaining) ?? MonarchEmailVerificationBannerStrings.resendIn(remaining)
        }
        return labels.resend ?? MonarchEmailVerificationBannerStrings.resend
    }

    /// The message with the address.
    static func message(email: String, labels: MonarchEmailVerificationBannerLabels) -> String {
        labels.message?(email) ?? MonarchEmailVerificationBannerStrings.message(email)
    }

    /// The text of a failed send: the error's description, or the fallback.
    static func errorMessage(_ error: any Error, labels: MonarchEmailVerificationBannerLabels) -> String {
        MonarchFailureMessage.message(for: error, fallback: labels.error ?? MonarchEmailVerificationBannerStrings.error)
    }

    /// The tone that is drawn: info or warning. Other tones fall back to warning.
    static func effectiveTone(_ tone: MonarchTone) -> MonarchTone {
        tone == .info ? .info : .warning
    }
}
