import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiActionLog``.
enum MonarchAiActionLogTokens {
    /// The gap between entries, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the avatar, the text and the undo button.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of an entry.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the agent's name and its marker.
    static let nameSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the error icon and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol before a failed undo.
    static let errorSymbol = "exclamationmark.circle"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "cpu"
    /// The separator between the time and "Undone".
    static let separator = " · "

    /// Where an entry's undo stands.
    enum UndoState: Equatable {
        /// The undo handler is running.
        case pending
        /// The action was reversed.
        case done
        /// The undo handler threw.
        case failed
    }

    /// How many entries are shown at first and added by Show more: at least one.
    static func step(_ pageSize: Int) -> Int {
        max(1, pageSize)
    }

    /// How many entries are hidden behind Show more.
    static func hiddenCount(total: Int, shown: Int) -> Int {
        max(0, total - shown)
    }

    /// How many entries Show more adds: a step, or what is left.
    static func moreCount(total: Int, shown: Int, step: Int) -> Int {
        min(step, hiddenCount(total: total, shown: shown))
    }

    /// The entry in words: "<agent> <action> <target>", leaving out what is blank.
    static func describe(_ entry: MonarchAiActionLogEntry) -> String {
        [entry.agent.name, entry.action, entry.target ?? ""]
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
    }

    /// Whether an entry reads as undone: its own flag or a finished undo.
    static func isUndone(_ entry: MonarchAiActionLogEntry, state: UndoState?) -> Bool {
        entry.isUndone || state == .done
    }

    /// Whether an entry has an Undo button.
    static func canUndo(_ entry: MonarchAiActionLogEntry, state: UndoState?, hasHandler: Bool) -> Bool {
        entry.isUndoable && hasHandler && !isUndone(entry, state: state)
    }

    /// The link of a target, only for `http` and `https`; anything else shows as text.
    static func safeURL(_ url: URL?) -> URL? {
        guard let url, let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https" else {
            return nil
        }
        return url
    }

    /// A time relative to `now`, such as "4 minutes ago".
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The line under the entry: the time and, once undone, "Undone".
    static func meta(
        _ entry: MonarchAiActionLogEntry,
        isUndone: Bool,
        now: Date,
        locale: Locale,
        labels: MonarchAiActionLogLabels
    ) -> String {
        let time = relativeTime(entry.date, now: now, locale: locale)
        guard isUndone else { return time }
        return time + separator + (labels.undone ?? MonarchAiActionLogStrings.undone)
    }

    /// The marker after the agent's name, "AI agent".
    static func actorKind(_ labels: MonarchAiActionLogLabels) -> String {
        labels.actorKind ?? MonarchAiAgentAvatarStrings.kind
    }

    /// The spoken entry: "Research agent (AI agent) added a summary to Quarterly report".
    static func accessibilityLabel(_ entry: MonarchAiActionLogEntry, labels: MonarchAiActionLogLabels) -> String {
        let action = [entry.action, entry.target ?? ""]
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
        return "\(entry.agent.name) (\(actorKind(labels))) \(action)"
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// What is read after the entry: the meta line and a failure.
    static func accessibilityValue(meta: String, failure: String?) -> String {
        [meta, failure].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The colour role of the action text: muted once undone.
    static func textRole(isUndone: Bool) -> MonarchColorRole {
        isUndone ? \.textMuted : \.textDefault
    }

    /// The spoken name of an entry's undo button.
    static func undoLabel(_ entry: MonarchAiActionLogEntry, labels: MonarchAiActionLogLabels) -> String {
        let text = describe(entry)
        return labels.undoLabel?(text) ?? MonarchAiActionLogStrings.undoLabel(text)
    }

    /// Announced after an undo.
    static func undoneAnnouncement(_ entry: MonarchAiActionLogEntry, labels: MonarchAiActionLogLabels) -> String {
        let text = describe(entry)
        return labels.undoneAnnouncement?(text) ?? MonarchAiActionLogStrings.undoneAnnouncement(text)
    }

    /// Shown and announced when an undo fails.
    static func undoFailed(_ entry: MonarchAiActionLogEntry, labels: MonarchAiActionLogLabels) -> String {
        let text = describe(entry)
        return labels.undoFailed?(text) ?? MonarchAiActionLogStrings.undoFailed(text)
    }

    /// The Show more button.
    static func showMore(_ count: Int, labels: MonarchAiActionLogLabels) -> String {
        labels.showMore?(count) ?? MonarchAiActionLogStrings.showMore(count)
    }
}
