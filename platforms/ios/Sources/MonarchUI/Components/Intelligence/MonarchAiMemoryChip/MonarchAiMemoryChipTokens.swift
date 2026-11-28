import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiMemoryChip``.
enum MonarchAiMemoryChipTokens {
    /// Padding before and after the content of the chip.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the content of the chip.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the glyph, the text and the menu.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the text and the scope.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the chip's border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The SF Symbol of the options menu.
    static let menuSymbol = "ellipsis.circle"
    /// The SF Symbol of the edit command.
    static let editSymbol = "pencil"
    /// The SF Symbol of the forget command.
    static let forgetSymbol = "trash"
    /// The longest copy of the text used in a spoken name.
    static let shortTextLength = 48

    /// The colour roles of the chip.
    struct Appearance: Equatable {
        let background: MonarchColorRole?
        let border: MonarchColorRole?
        let glyph: MonarchColorRole
        let prefix: MonarchColorRole
        let text: MonarchColorRole
    }

    /// The roles for a variant and a memory action. The chip uses the AI
    /// roles; a forgotten memory's text is muted.
    static func appearance(variant: MonarchAiMemoryChipVariant, action: MonarchAiMemoryAction) -> Appearance {
        let text: MonarchColorRole = action == .forget ? \.textMuted : \.textDefault
        switch variant {
        case .chip:
            return Appearance(
                background: \.aiSubtle,
                border: \.aiBorder,
                glyph: \.aiStrong,
                prefix: \.aiStrong,
                text: text
            )
        case .row:
            return Appearance(background: nil, border: nil, glyph: \.aiStrong, prefix: \.textMuted, text: text)
        }
    }

    /// Whether the scope is shown: by default on the chip and not on the row.
    static func showsScope(_ explicit: Bool?, variant: MonarchAiMemoryChipVariant, scope: String?) -> Bool {
        guard let scope, !scope.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
        return explicit ?? (variant == .chip)
    }

    /// Whether the edit and forget commands are offered: never for a memory
    /// that is already forgotten.
    static func offersCommands(action: MonarchAiMemoryAction, hasEdit: Bool, hasForget: Bool) -> Bool {
        action != .forget && (hasEdit || hasForget)
    }

    /// The text with runs of white space collapsed, shortened with an
    /// ellipsis when it is longer than `max` characters, for spoken names.
    static func shortText(_ text: String, max: Int = shortTextLength) -> String {
        let clean = text.split(whereSeparator: \.isWhitespace).joined(separator: " ")
        guard clean.count > max, max > 1 else { return clean }
        let head = clean.prefix(max - 1)
        return String(head).trimmingCharacters(in: .whitespaces) + "…"
    }

    /// The text to save from the edit dialog, trimmed, or `nil` when it is
    /// empty or unchanged, so `onEdit` never receives either.
    static func edited(_ draft: String, original: String) -> String? {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed != original else { return nil }
        return trimmed
    }

    /// What VoiceOver reads for the memory: the prefix (chip only), the text
    /// and the scope when it is shown.
    static func accessibilityLabel(prefix: String?, text: String, scope: String?) -> String {
        var label = text
        if let prefix { label = prefix + " " + label }
        if let scope { label += ", " + scope }
        return label
    }
}
