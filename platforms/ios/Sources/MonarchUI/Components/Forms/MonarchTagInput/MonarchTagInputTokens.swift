import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchTagInput``. The box repeats the border,
/// fill and padding of ``MonarchTextField`` so the two look the same.
enum MonarchTagInputTokens {
    /// The gap between the label, the box and the supporting text.
    static let stackSpacing: CGFloat = MonarchTextFieldTokens.stackSpacing
    /// The gap between the tags and the text field in the box.
    static let boxSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between tags.
    static let tagSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the error icon and the message.
    static let messageIconSpacing: CGFloat = MonarchTextFieldTokens.messageIconSpacing
    /// The SF Symbol before an error.
    static let errorSymbol = MonarchTextFieldTokens.errorSymbol
    /// The SF Symbol of a suggestion button.
    static let suggestionSymbol = "plus"
    /// The most suggestions shown at once.
    static let suggestionLimit = 8

    /// The message under the box: a rejection, then the app's error.
    static func message(rejection: String?, error: String?) -> String? {
        if let rejection, !rejection.isEmpty { return rejection }
        if let error, !error.isEmpty { return error }
        return nil
    }

    /// The hint of the text field: the message as an error, otherwise the
    /// help text.
    static func accessibilityHint(helper: String?, message: String?) -> String {
        MonarchTextFieldTokens.accessibilityHint(helper: helper, error: message)
    }

    /// The spoken name of a suggestion button.
    static func suggestionLabel(_ suggestion: String, labels: MonarchTagInputLabels) -> String {
        labels.addSuggestion?(suggestion) ?? MonarchTagInputStrings.addSuggestion(suggestion)
    }

    /// The colour role of the label.
    static func labelColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }
}
