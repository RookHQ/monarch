import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiConversationList``.
enum MonarchAiConversationListTokens {
    /// The gap between the title and the preview.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the active marker and the text.
    static let markerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the active marker.
    static let markerWidth: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the new conversation button.
    static let newSymbol = "plus"
    /// The star before a pinned conversation, as the web draws it.
    static let pinnedMarkSymbol = "star"
    /// The glyph of the row menu.
    static let moreSymbol = "ellipsis"
    /// The SF Symbol of the rename command.
    static let renameSymbol = "pencil"
    /// The SF Symbol of the pin command.
    static let pinSymbol = "pin"
    /// The SF Symbol of the unpin command.
    static let unpinSymbol = "pin.slash"
    /// The SF Symbol of the delete command.
    static let deleteSymbol = "trash"
    /// The height of each list in the preview, which sits in a scroll view.
    static let previewHeight: CGFloat = MonarchTokens.Space.space24 * 5
    /// The length of a day, for building preview dates.
    static let day: TimeInterval = 86_400
    /// The number of days before today that still count as the previous 7 days.
    static let recentDays = 7

    /// The conversations of one group.
    struct Section: Equatable, Identifiable {
        let group: MonarchAiConversationGroup
        let items: [MonarchAiConversationSummary]
        var id: MonarchAiConversationGroup { group }
    }

    /// The text of the last message that has text, on one line.
    static func preview(_ messages: [MonarchAiConversationMessage]) -> String? {
        for message in messages.reversed() {
            let text = message.text.split(whereSeparator: \.isNewline).joined(separator: " ")
                .trimmingCharacters(in: .whitespaces)
            if !text.isEmpty { return text }
        }
        return nil
    }

    /// The group of a conversation, by calendar days before `now`.
    static func group(
        of conversation: MonarchAiConversationSummary,
        now: Date,
        calendar: Calendar
    ) -> MonarchAiConversationGroup {
        if conversation.isPinned { return .pinned }
        let days =
            calendar.dateComponents(
                [.day],
                from: calendar.startOfDay(for: conversation.updatedAt),
                to: calendar.startOfDay(for: now)
            ).day ?? 0
        if days <= 0 { return .today }
        if days <= recentDays { return .previous7Days }
        return .older
    }

    /// The conversations split into Pinned, Today, Previous 7 days and
    /// Older, newest first within each group. Empty groups are left out.
    static func sections(
        _ conversations: [MonarchAiConversationSummary],
        now: Date,
        calendar: Calendar
    ) -> [Section] {
        let grouped = Dictionary(grouping: conversations) { group(of: $0, now: now, calendar: calendar) }
        return MonarchAiConversationGroup.allCases.compactMap { group in
            guard let items = grouped[group], !items.isEmpty else { return nil }
            return Section(group: group, items: items.sorted { $0.updatedAt > $1.updatedAt })
        }
    }

    /// Whether a conversation matches a search: the title or the preview
    /// contains the trimmed query, ignoring case and diacritics. An empty
    /// query matches everything.
    static func matches(_ conversation: MonarchAiConversationSummary, query: String) -> Bool {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return true }
        let options: String.CompareOptions = [.caseInsensitive, .diacriticInsensitive]
        return conversation.title.range(of: query, options: options) != nil
            || (conversation.preview?.range(of: query, options: options) != nil)
    }

    /// The first `limit` conversations across the sections, in display order.
    static func capped(_ sections: [Section], limit: Int) -> [Section] {
        var room = max(0, limit)
        var result: [Section] = []
        for section in sections where room > 0 {
            let items = Array(section.items.prefix(room))
            result.append(Section(group: section.group, items: items))
            room -= items.count
        }
        return result
    }

    /// The step of the "Show more" button: at least one.
    static func step(_ visibleCount: Int) -> Int { max(1, visibleCount) }

    /// The title shown for a conversation: its own, or "New conversation".
    static func title(_ conversation: MonarchAiConversationSummary, untitled: String) -> String {
        let trimmed = conversation.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? untitled : trimmed
    }

    /// The title to save from the rename dialog, trimmed, or `nil` when it
    /// is empty or unchanged.
    static func renamed(_ draft: String, original: String) -> String? {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, trimmed != original else { return nil }
        return trimmed
    }

    /// What VoiceOver reads for a row: the title, then the preview.
    static func accessibilityLabel(title: String, preview: String?) -> String {
        guard let preview, !preview.isEmpty else { return title }
        return title + ", " + preview
    }

    /// The announcement after the search changes: the number of matches,
    /// or nothing when the search is empty or the app searches itself.
    static func announcement(
        query: String,
        count: Int,
        searchesLocally: Bool,
        labels: MonarchAiConversationListLabels
    ) -> String? {
        guard searchesLocally, !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        return labels.results(count)
    }
}
