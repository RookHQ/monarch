import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchMessageComposer``.
enum MonarchMessageComposerTokens {
    /// The gap between the parts of the composer.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the buttons of the action row.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding inside the box.
    static let padding: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the text inside the box.
    static let textPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the context icon and its text.
    static let contextSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the context bar's leading rule.
    static let contextRuleWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The most lines the text grows to before it scrolls.
    static let maxLines = 8
    /// The SF Symbols of the composer.
    static let sendSymbol = "paperplane.fill"
    static let attachSymbol = "paperclip"
    static let emojiSymbol = "face.smiling"
    static let replySymbol = "arrowshape.turn.up.left"
    static let editSymbol = "pencil"
    static let dismissSymbol = "xmark"

    /// The height of the rule between two groups of formatting tools.
    static let formatDividerHeight: CGFloat = MonarchTokens.Space.space5

    /// The groups of the formatting row, as on the web: inline marks, lists, then link, quote and code block.
    static let formatGroups: [[MonarchMessageComposerFormat]] = [
        [.bold, .italic, .strikethrough, .code],
        [.bulletList, .numberedList],
        [.link, .quote, .codeBlock],
    ]

    /// The name of a formatting tool.
    static func formatLabel(_ tool: MonarchMessageComposerFormat, labels: MonarchMessageComposerLabels) -> String {
        MonarchMessageComposerStrings.format(tool)
    }

    /// The text after a formatting tool is applied. The message is plain text, so a tool writes its Markdown
    /// marker at the end: an inline mark opens at the first tap and closes at the next, a block starts a line.
    static func applying(_ tool: MonarchMessageComposerFormat, to text: String) -> String {
        switch tool {
        case .bold, .italic, .strikethrough, .code:
            return text + tool.marker
        case .link:
            return text + "[](https://)"
        case .bulletList, .numberedList, .quote:
            let lead = text.isEmpty || text.hasSuffix("\n") ? "" : "\n"
            return text + lead + tool.marker
        case .codeBlock:
            let lead = text.isEmpty || text.hasSuffix("\n") ? "" : "\n"
            return text + lead + tool.marker + "\n"
        }
    }

    /// The fill of the box: the warning tint for a note, the subtle fill when disabled.
    static func background(isNote: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.bgSubtle }
        return isNote ? \.warningSubtle : \.bgSurface
    }

    /// The border of the box.
    static func border(isNote: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.borderSubtle }
        return isNote ? \.warningBorder : \.borderInput
    }

    /// The text colour.
    static func foreground(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// Whether the text has more characters than the limit.
    static func isOverLimit(_ text: String, maxLength: Int?) -> Bool {
        guard let maxLength else { return false }
        return text.count > maxLength
    }

    /// Whether the message can be sent: enabled, not sending, with text or
    /// attachments, and within the limit.
    static func canSend(
        text: String,
        hasAttachments: Bool,
        maxLength: Int?,
        isEnabled: Bool,
        isSending: Bool
    ) -> Bool {
        guard isEnabled, !isSending, !isOverLimit(text, maxLength: maxLength) else { return false }
        return !trimmed(text).isEmpty || hasAttachments
    }

    /// The text without leading or trailing white space and line breaks.
    static func trimmed(_ text: String) -> String {
        text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// The send button's title: "Sending…", the mode's title, "Save" while editing, or "Send".
    static func sendTitle(
        isSending: Bool,
        isEditing: Bool,
        mode: MonarchMessageComposerMode?,
        labels: MonarchMessageComposerLabels
    ) -> String {
        if isSending { return labels.sending ?? MonarchMessageComposerStrings.sending }
        if let title = mode?.sendLabel { return title }
        return isEditing
            ? labels.save ?? MonarchMessageComposerStrings.save
            : labels.send ?? MonarchMessageComposerStrings.send
    }

    /// The title of the context bar, or `nil` when there is none. Editing wins over a reply.
    static func contextTitle(
        isEditing: Bool,
        replyTo: MonarchMessageComposerReply?,
        labels: MonarchMessageComposerLabels
    ) -> String? {
        if isEditing { return labels.editing ?? MonarchMessageComposerStrings.editing }
        guard let replyTo else { return nil }
        return labels.replyingTo?(replyTo.author) ?? MonarchMessageComposerStrings.replyingTo(replyTo.author)
    }

    /// The visible count, "120/500", or `nil` without a limit.
    static func countText(_ text: String, maxLength: Int?, locale: Locale) -> String? {
        guard let maxLength else { return nil }
        return "\(text.count.formatted(.number.locale(locale)))/\(maxLength.formatted(.number.locale(locale)))"
    }

    /// The spoken count: characters left, or over the limit.
    static func countLabel(_ text: String, maxLength: Int, labels: MonarchMessageComposerLabels) -> String {
        let left = maxLength - text.count
        return labels.remaining?(left) ?? MonarchMessageComposerStrings.remaining(left)
    }

    /// The colour of the count: danger over the limit.
    static func countForeground(_ text: String, maxLength: Int?) -> MonarchColorRole {
        isOverLimit(text, maxLength: maxLength) ? \.dangerStrong : \.textMuted
    }

    /// The selected mode: the one with `value`, or the first.
    static func mode(_ value: String?, in modes: [MonarchMessageComposerMode]) -> MonarchMessageComposerMode? {
        modes.first { $0.value == value } ?? modes.first
    }

    /// Whether to report typing now: the first change, or `interval` after the last report.
    static func shouldSignalTyping(now: Date, lastSignal: Date?, interval: TimeInterval) -> Bool {
        guard let lastSignal else { return true }
        return now.timeIntervalSince(lastSignal) >= interval
    }

    /// The picked mentions still written in the text as trigger and label,
    /// each once, in the order they were picked.
    static func mentions(_ picked: [MonarchMessageComposerPicked], in text: String) -> [MonarchMentionItem] {
        var seen = Set<String>()
        return picked.compactMap { entry in
            guard !seen.contains(entry.item.id), text.contains(mentionText(entry.item, trigger: entry.trigger)) else {
                return nil
            }
            seen.insert(entry.item.id)
            return entry.item
        }
    }

    /// What a picked mention writes into the text: "@Ana Lopez".
    static func mentionText(_ item: MonarchMentionItem, trigger: Character) -> String {
        "\(trigger)\(item.label)"
    }

    /// The text after picking an emoji: added at the end, after a space
    /// when the text does not end in white space. iOS 17 gives a
    /// `TextField` no caret position to insert at.
    static func inserting(_ emoji: String, into text: String) -> String {
        guard let last = text.last, !last.isWhitespace else { return text + emoji }
        return text + " " + emoji
    }

    /// The message sent for the text.
    static func message(
        text: String,
        picked: [MonarchMessageComposerPicked],
        mode: MonarchMessageComposerMode?
    ) -> MonarchMessageComposerMessage {
        let clean = trimmed(text)
        return MonarchMessageComposerMessage(text: clean, mentions: mentions(picked, in: clean), mode: mode?.value)
    }
}

/// A mention picked in a ``MonarchMessageComposer``, with the trigger it was typed with.
struct MonarchMessageComposerPicked: Hashable {
    /// The trigger, such as `@`.
    let trigger: Character
    /// The suggestion.
    let item: MonarchMentionItem
}
