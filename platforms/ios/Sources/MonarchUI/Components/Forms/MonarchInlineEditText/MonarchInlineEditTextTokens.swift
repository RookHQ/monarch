import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchInlineEditText``.
enum MonarchInlineEditTextTokens {
    /// The gap between the text and the pencil.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the field, the error and the buttons.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between Save and Cancel.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The least height of the single-line field and of the text in view mode: the touch target.
    static let minHeight: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The height of the multi-line editor, before Dynamic Type. Longer text scrolls inside it.
    static let editorHeight: CGFloat = MonarchTokens.Space.space24
    /// The distance of the prompt from the leading edge of the multi-line editor, to sit on its text.
    static let editorPromptLeadingInset: CGFloat = MonarchTokens.Space.space1
    /// The distance of the prompt from the top of the multi-line editor, to sit on its first line.
    static let editorPromptTopInset: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol that marks editable text.
    static let editSymbol = "pencil"

    /// The colour of the pencil.
    static var editGlyph: MonarchColorRole { \.textMuted }

    /// The colour of the text in view mode: the value, the prompt of an
    /// empty value, or either while disabled.
    static func viewForeground(isEmpty: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.textDisabled }
        return isEmpty ? \.textSubtle : \.textDefault
    }

    /// The text drawn in view mode: the value, or the prompt when it is empty.
    static func viewText(value: String, prompt: String?) -> String {
        value.isEmpty ? (prompt ?? "") : value
    }

    /// The error to show while editing: the session's own error first, then
    /// the one the app supplies. Empty strings count as none.
    static func shownError(local: String?, external: String?) -> String? {
        if let local, !local.isEmpty { return local }
        if let external, !external.isEmpty { return external }
        return nil
    }

    /// The message of a failed save: the error's own description when it
    /// has one, otherwise the override, otherwise the built-in sentence.
    static func failureMessage(for error: any Error, override: String?) -> String {
        MonarchFailureMessage.message(for: error, override: override, fallback: MonarchInlineEditTextStrings.saveFailed)
    }

    /// What VoiceOver reads as the hint of the text in view mode.
    static func editHint(_ override: String?) -> String {
        if let override, !override.isEmpty { return override }
        return MonarchInlineEditTextStrings.edit
    }

    /// What VoiceOver reads as the hint of the field: the error, spoken as "Error: …".
    static func fieldHint(error: String?) -> String {
        guard let error, !error.isEmpty else { return "" }
        return MonarchInlineEditTextStrings.error(error)
    }
}
