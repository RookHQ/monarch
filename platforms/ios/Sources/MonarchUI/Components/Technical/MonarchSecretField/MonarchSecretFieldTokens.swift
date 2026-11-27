import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchSecretField``. The sizes are
/// those of the text field, so the two line up in a form.
enum MonarchSecretFieldTokens {
    /// The least height of the field, which is also its touch target.
    static let minHeight: CGFloat = MonarchTextFieldTokens.minHeight
    /// Padding before the text.
    static let leadingPadding: CGFloat = MonarchTextFieldTokens.horizontalPadding
    /// Padding above and below the text.
    static let verticalPadding: CGFloat = MonarchTextFieldTokens.verticalPadding
    /// The gap between the label, the field and the supporting text.
    static let stackSpacing: CGFloat = MonarchTextFieldTokens.stackSpacing
    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTextFieldTokens.messageIconSpacing
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchTextFieldTokens.cornerRadius
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTextFieldTokens.borderWidth
    /// The SF Symbol shown before an error message.
    static let errorSymbol = MonarchTextFieldTokens.errorSymbol
    /// The SF Symbol of the copy button before a copy.
    static let copySymbol = "doc.on.doc"
    /// The SF Symbol of the copy button after a copy.
    static let copiedSymbol = "checkmark"
    /// The character that stands for each character of a masked read-only secret.
    static let maskCharacter: Character = "\u{2022}"
    /// How long the copy button shows that it copied.
    static let copiedDuration: Duration = .seconds(2)

    /// The SF Symbol of the reveal toggle: what activating it will do.
    static func revealSymbol(isRevealed: Bool) -> String {
        isRevealed ? "eye.slash" : "eye"
    }

    /// The SF Symbol of the copy button for a state.
    static func copySymbol(isCopied: Bool) -> String {
        isCopied ? copiedSymbol : copySymbol
    }

    /// Whether the reveal and copy buttons can act: only when there is text.
    /// A saved value that is not shown is not on the device, so an empty
    /// field has nothing to reveal or copy.
    static func hasPlainText(_ text: String) -> Bool {
        !text.isEmpty
    }

    /// The placeholder: the "saved" prompt while a saved value is not
    /// shown and nothing new is typed, otherwise the app's prompt.
    static func prompt(_ prompt: String?, isSet: Bool, text: String, setPrompt: String) -> String? {
        isSet && text.isEmpty ? setPrompt : prompt
    }

    /// The text shown for a read-only secret.
    static func readOnlyText(_ text: String, isRevealed: Bool) -> String {
        isRevealed ? text : String(repeating: maskCharacter, count: text.count)
    }

    /// The spoken value of a read-only secret: the secret only while it is
    /// revealed.
    static func readOnlyAccessibilityValue(_ text: String, isRevealed: Bool, hidden: String) -> String {
        if text.isEmpty { return "" }
        return isRevealed ? text : hidden
    }

    /// What VoiceOver reads after the label and value: the error if there is
    /// one, otherwise the helper text, after the "saved" description while a
    /// saved value is not shown.
    static func accessibilityHint(
        helper: String?,
        error: String?,
        isSet: Bool,
        text: String,
        labels: MonarchSecretFieldLabels
    ) -> String {
        var parts: [String] = []
        if let error, !error.isEmpty {
            parts.append(labels.errorText(error))
        }
        if isSet, text.isEmpty {
            parts.append(labels.resolvedSetDescription)
        }
        if error?.isEmpty ?? true, let helper, !helper.isEmpty {
            parts.append(helper)
        }
        return parts.joined(separator: " ")
    }

    /// The border role for a state.
    static func border(hasError: Bool, isEnabled: Bool) -> MonarchColorRole {
        MonarchTextFieldTokens.border(hasError: hasError, isEnabled: isEnabled)
    }

    /// The fill role for a state. A read-only secret has the subtle fill,
    /// like a copy field.
    static func background(isEnabled: Bool, isReadOnly: Bool) -> MonarchColorRole {
        isReadOnly ? \.bgSubtle : MonarchTextFieldTokens.background(isEnabled: isEnabled)
    }

    /// The role of the text.
    static func foreground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The role of a button symbol: muted when it can act, disabled otherwise.
    static func actionForeground(isEnabled: Bool, canAct: Bool) -> MonarchColorRole {
        isEnabled && canAct ? \.textMuted : \.textDisabled
    }

    /// Whether the copied state ends by itself: only after a copy.
    static func resets(copyCount: Int) -> Bool {
        copyCount > 0
    }
}
