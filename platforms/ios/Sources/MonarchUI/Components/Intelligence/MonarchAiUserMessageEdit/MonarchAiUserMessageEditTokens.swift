import Foundation
import MonarchTokens

/// Component tokens for ``MonarchAiUserMessageEdit``.
enum MonarchAiUserMessageEditTokens {
    /// The gap between the field, the note and the buttons.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the two buttons.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the note's icon and its text.
    static let hintIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after the text.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the text.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol before the note.
    static let hintSymbol = "info.circle"
    /// The colour role of the note.
    static var hintColor: MonarchColorRole { \.textMuted }
    /// The fewest lines the field shows.
    static let minLines = 2

    /// The lines the field shows: at least `minLines`, growing to `maxLines`.
    /// A `maxLines` below the minimum is raised to it.
    static func lineLimit(maxLines: Int) -> ClosedRange<Int> {
        minLines...max(minLines, maxLines)
    }

    /// The text to resend, or `nil` when Send must be disabled: the trimmed
    /// text is empty or equal to the trimmed original.
    static func submission(text: String, original: String) -> String? {
        let next = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !next.isEmpty, next != original.trimmingCharacters(in: .whitespacesAndNewlines) else { return nil }
        return next
    }

    /// The border role of the field.
    static func border(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.borderInput : \.borderSubtle
    }

    /// The fill role of the field.
    static func background(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.bgSurface : \.bgSubtle
    }

    /// The text role of the field.
    static func text(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }
}
