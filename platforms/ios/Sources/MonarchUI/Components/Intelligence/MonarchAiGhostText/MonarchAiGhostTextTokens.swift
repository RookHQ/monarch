import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiGhostText``. The field takes its box
/// from the text field, so the two look alike.
enum MonarchAiGhostTextTokens {
    /// The type role of the typed text and of the suggestion.
    static let textStyle: MonarchTextStyle = .body
    /// The gap between the label, the field and the supporting row.
    static let stackSpacing: CGFloat = MonarchTextFieldTokens.stackSpacing
    /// The gap between the helper text and the accept button.
    static let supportingSpacing: CGFloat = MonarchTokens.Space.space2
    /// How far around the suggestion a tap still accepts it.
    static let tapSlop: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the accept button.
    static let acceptSymbol = "checkmark"

    /// The colour role of the typed text.
    static func textRole(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour role of the suggestion: muted, so it is not mistaken for typed text.
    static var suggestionRole: MonarchColorRole { \.textMuted }

    /// The suggestion to draw: only a ready one, in an enabled field.
    static func shownSuggestion(status: MonarchAiCompletionStatus, suggestion: String, isEnabled: Bool) -> String {
        status == .ready && isEnabled ? suggestion : ""
    }

    /// What an edit or a caret move means for the completion.
    enum Evaluation: Equatable {
        /// Drop the suggestion.
        case dismiss
        /// Tell the completion the text and this cursor.
        case update(cursor: Int)
    }

    /// Decides what the field's state means. A suggestion belongs only at
    /// the end of the text, with a caret and no selection, in an editable
    /// field, and never while an input method is composing.
    ///
    /// - Parameters:
    ///   - text: The whole text.
    ///   - caret: The caret as a UTF-16 offset, or `nil` when text is selected.
    ///   - isComposing: Whether an input method has marked text.
    ///   - isEditable: Whether the field can be edited.
    /// - Returns: Whether to drop the suggestion or to tell the completion the cursor.
    static func evaluate(text: String, caret: Int?, isComposing: Bool, isEditable: Bool) -> Evaluation {
        guard !isComposing, isEditable, let caret else { return .dismiss }
        let whole = text as NSString
        let cursor = min(max(0, caret), whole.length)
        let rest = whole.substring(from: cursor).trimmingCharacters(in: .whitespacesAndNewlines)
        // In the middle of the text a suggestion would cover what follows.
        return rest.isEmpty ? .update(cursor: cursor) : .dismiss
    }

    /// A key of a hardware keyboard that the field handles while a suggestion shows.
    enum Key: Equatable {
        /// Tab.
        case tab
        /// Escape.
        case escape
        /// The left arrow.
        case leftArrow
        /// The right arrow.
        case rightArrow
    }

    /// The modifier keys held with a key.
    struct Modifiers: OptionSet, Hashable {
        let rawValue: Int
        /// Control.
        static let control = Modifiers(rawValue: 1 << 0)
        /// Option.
        static let option = Modifiers(rawValue: 1 << 1)
        /// Command.
        static let command = Modifiers(rawValue: 1 << 2)
        /// Shift.
        static let shift = Modifiers(rawValue: 1 << 3)
    }

    /// What a key does while a suggestion shows.
    enum KeyAction: Equatable {
        /// Accept all of the suggestion, or its next word.
        case accept(MonarchAiGhostTextAcceptKind)
        /// Dismiss the suggestion.
        case dismiss
    }

    /// Tab, or the forward arrow, accepts the suggestion; Control or Option
    /// with the forward arrow accepts one word; Escape dismisses it. The
    /// forward arrow is the left one in a right-to-left layout. Anything
    /// else is left to the system.
    static func keyAction(_ key: Key, modifiers: Modifiers, isRightToLeft: Bool) -> KeyAction? {
        let forward: Key = isRightToLeft ? .leftArrow : .rightArrow
        switch key {
        case .tab: return modifiers.isEmpty ? .accept(.all) : nil
        case .escape: return modifiers.isEmpty ? .dismiss : nil
        case forward:
            if modifiers.isEmpty { return .accept(.all) }
            if modifiers == .control || modifiers == .option || modifiers == [.control, .option] {
                return .accept(.word)
            }
            return nil
        default: return nil
        }
    }

    /// The key and modifier combinations the field takes from the system while a suggestion shows.
    static func handledKeys(isRightToLeft: Bool) -> [(key: Key, modifiers: Modifiers)] {
        let forward: Key = isRightToLeft ? .leftArrow : .rightArrow
        return [(.tab, []), (.escape, []), (forward, []), (forward, .control), (forward, .option)]
    }

    /// Whether a suggestion is news. The rest of one being typed through,
    /// or taken word by word, is not announced again.
    static func shouldAnnounce(suggestion: String, lastAnnounced: String) -> Bool {
        !suggestion.isEmpty && !lastAnnounced.hasSuffix(suggestion)
    }

    /// What VoiceOver reads after the label and value: how to accept the
    /// suggestion while one shows, otherwise the helper text.
    static func accessibilityHint(helper: String?, instruction: String, isShowing: Bool) -> String {
        isShowing ? instruction : (helper ?? "")
    }

    /// Where the caret goes after the text was set from outside the field:
    /// to the completion's cursor when the text is the completion's, as
    /// after accepting, otherwise to the end.
    static func caret(for text: String, completionText: String, completionCursor: Int) -> Int {
        let length = (text as NSString).length
        return text == completionText ? min(max(0, completionCursor), length) : length
    }
}

/// What the field reports after an edit or a caret move.
struct MonarchAiGhostTextInput: Equatable {
    /// The whole text.
    let text: String
    /// The caret as a UTF-16 offset, or `nil` when text is selected.
    let caret: Int?
    /// Whether an input method has marked text.
    let isComposing: Bool
}

/// How the platform field is drawn and described.
struct MonarchAiGhostTextFieldStyle {
    /// The point size of the text, already scaled for Dynamic Type.
    let fontSize: CGFloat
    /// The custom font family of the theme, or `nil` for the system font.
    let fontFamily: String?
    /// Extra space between lines, already scaled.
    let lineSpacing: CGFloat
    /// The colour of the typed text.
    let textColor: Color
    /// The colour of the suggestion.
    let suggestionColor: Color
    /// Whether the layout is right to left.
    let isRightToLeft: Bool
    /// Whether the field can be edited.
    let isEnabled: Bool
    /// The accessibility label.
    let label: String
    /// The accessibility hint.
    let hint: String
    /// The name of the accessibility action that accepts the suggestion.
    let acceptActionName: String
    /// The name of the accessibility action that dismisses the suggestion.
    let dismissActionName: String
    /// The key that accepts the suggestion, drawn as a small key cap after it (`nil` for none).
    var keyHint: String?
    /// Where the caret goes when the text is set from outside the field.
    let caretAfterExternalChange: Int
}

/// What the platform field tells the component.
struct MonarchAiGhostTextFieldEvents {
    /// The text or the caret changed.
    let onInput: @MainActor (MonarchAiGhostTextInput) -> Void
    /// The field gained or lost focus.
    let onFocusChange: @MainActor (Bool) -> Void
    /// The person accepted the suggestion: by a tap on it, a key or a VoiceOver action.
    let onAccept: @MainActor (MonarchAiGhostTextAcceptKind) -> Void
    /// The person dismissed the suggestion: by Escape or a VoiceOver action.
    let onDismiss: @MainActor () -> Void
}
