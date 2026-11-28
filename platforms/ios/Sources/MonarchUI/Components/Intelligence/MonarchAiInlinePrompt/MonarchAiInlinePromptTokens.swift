import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchAiInlinePrompt``,
/// ``MonarchAiInlinePromptArea`` and ``MonarchAiInlinePromptTextView``.
enum MonarchAiInlinePromptTokens {
    /// The most characters of the selection shown in the excerpt.
    static let excerptLimit = 140
    /// The space between the parts of the sheet.
    static let spacing: CGFloat = MonarchTokens.Space.space4
    /// The space inside the excerpt box and between its lines.
    static let excerptPadding: CGFloat = MonarchTokens.Space.space3
    /// The space between the excerpt's label and text.
    static let excerptSpacing: CGFloat = MonarchTokens.Space.space1
    /// The space between quick prompts.
    static let suggestionSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding around the sheet's content.
    static let sheetPadding: CGFloat = MonarchTokens.Space.space4
    /// The SF Symbol of the "Ask AI" menu item.
    static let askSymbol = "text.bubble"
    /// The text style of the selectable text view.
    static let textStyle: MonarchTextStyle = .body

    /// The excerpt box: the subtle AI fill and the AI border.
    static var excerptSurface: MonarchSurfaceAppearance {
        MonarchSurfaceAppearance(background: \.bgSubtle, foreground: \.textMuted, border: \.borderDefault)
    }

    /// The excerpt's label: the strong AI colour.
    static var excerptLabelRole: MonarchColorRole { \.aiStrong }
    /// The excerpt's text: muted.
    static var excerptTextRole: MonarchColorRole { \.textMuted }
    /// The text of the selectable text view.
    static func textRole(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The excerpt of the context: `nil` when blank, otherwise its first
    /// `limit` characters, with trailing spaces removed and an ellipsis
    /// added when it was cut.
    static func excerpt(_ context: String?, limit: Int = excerptLimit) -> String? {
        guard let context else { return nil }
        let trimmed = context.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        guard trimmed.count > limit else { return trimmed }
        let cut = String(trimmed.prefix(limit))
        let end = cut.lastIndex { !$0.isWhitespace }.map { cut.index(after: $0) } ?? cut.endIndex
        return String(cut[..<end]) + "…"
    }

    /// What is sent: the trimmed prompt, or `nil` for a blank one or while pending.
    static func submission(_ text: String, isPending: Bool) -> String? {
        guard !isPending else { return nil }
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    /// The excerpt as VoiceOver reads it: "Selection" then the text.
    static func excerptAccessibilityLabel(_ excerpt: String, labels: MonarchAiInlinePromptLabels) -> String {
        "\(labels.context ?? MonarchAiInlinePromptStrings.context), \(excerpt)"
    }

    /// The selected text of a text view, or `nil` when only a caret is placed
    /// or the range is outside the text. `location` and `length` are UTF-16.
    static func selection(in text: String, location: Int, length: Int) -> String? {
        let utf16 = text.utf16
        guard length > 0, location >= 0, location + length <= utf16.count else { return nil }
        let start = utf16.index(utf16.startIndex, offsetBy: location)
        let end = utf16.index(start, offsetBy: length)
        guard let lower = start.samePosition(in: text), let upper = end.samePosition(in: text) else { return nil }
        let selected = String(text[lower..<upper])
        return selected.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : selected
    }

    /// Whether the "Ask AI" menu item is added: enabled, with text selected.
    static func showsMenuItem(isEnabled: Bool, selection: String?) -> Bool {
        isEnabled && selection != nil
    }

    /// What the VoiceOver "Ask AI" action passes: the selection, or the whole
    /// text when nothing is selected; `nil` when the text is blank.
    static func accessibilityActionText(_ text: String, selection: String?) -> String? {
        if let selection { return selection }
        return text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : text
    }
}
