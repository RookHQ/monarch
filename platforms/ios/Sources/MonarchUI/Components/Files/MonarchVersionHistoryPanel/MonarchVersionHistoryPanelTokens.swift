import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchVersionHistoryPanel``.
enum MonarchVersionHistoryPanelTokens {
    /// The gap between the header, the save row and the versions.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding around the content.
    static let panelPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of text in a row.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the blocks of the detail screen.
    static let detailSpacing: CGFloat = MonarchTokens.Space.space4
    /// The padding of the detail screen.
    static let detailPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between paragraphs of the full text.
    static let paragraphSpacing: CGFloat = MonarchTokens.Space.space3
    /// The avatar size in the rows.
    static let rowAvatarSize: MonarchAvatarSize = .sm
    /// The longest version name, as on the web.
    static let maximumNoteLength = 200
    /// The placeholder rows shown while versions load.
    static let placeholderRows = 3
    /// The placeholder lines shown while a version's text loads.
    static let placeholderLines = ["Placeholder line of text that loads", "A shorter line", "A line of medium length"]
    /// The SF Symbol of the restore button.
    static let restoreSymbol = "clock.arrow.circlepath"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "clock"
    /// The colour role of secondary text.
    static var secondary: MonarchColorRole { \.textMuted }

    /// The id of the version that matches the content now: the given one, or the newest.
    static func currentID(_ currentID: String?, versions: [MonarchVersionHistoryPanelVersion]) -> String? {
        currentID ?? versions.first?.id
    }

    /// When the version was saved, relative to `now`: "2 hours ago".
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// When the version was saved, as a date and time.
    static func absoluteTime(_ date: Date, locale: Locale, timeZone: TimeZone = .autoupdatingCurrent) -> String {
        var style = Date.FormatStyle(date: .abbreviated, time: .omitted).locale(locale).monarchTime()
        style.timeZone = timeZone
        return date.formatted(style)
    }

    /// The second line of a row: the author, then the version's name or "Saved automatically".
    static func byline(_ version: MonarchVersionHistoryPanelVersion, labels: MonarchVersionHistoryPanelLabels) -> String
    {
        let note = version.note?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let note, !note.isEmpty { return "\(version.authorName) · \(note)" }
        if version.isAutomatic {
            return "\(version.authorName) · \(labels.automatic ?? MonarchVersionHistoryPanelStrings.automatic)"
        }
        return version.authorName
    }

    /// What VoiceOver reads for a row: when, who and the version's name.
    static func accessibilityLabel(
        _ version: MonarchVersionHistoryPanelVersion,
        relativeTime: String,
        labels: MonarchVersionHistoryPanelLabels
    ) -> String {
        var parts = [relativeTime, version.authorName]
        let note = version.note?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !note.isEmpty {
            parts.append(note)
        } else if version.isAutomatic {
            parts.append(labels.automatic ?? MonarchVersionHistoryPanelStrings.automatic)
        }
        return parts.joined(separator: ", ")
    }

    /// The value VoiceOver reads for a row: "Current" for the current version.
    static func accessibilityValue(isCurrent: Bool, labels: MonarchVersionHistoryPanelLabels) -> String {
        isCurrent ? (labels.current ?? MonarchVersionHistoryPanelStrings.current) : ""
    }

    /// Whether a version's text equals the content now.
    static func isIdentical(_ lines: [String], to current: [String]) -> Bool {
        let stats = MonarchTextDiff.stats(MonarchTextDiff.lines(lines, current))
        return stats.added + stats.removed == 0
    }

    /// Whether Restore is offered: there is a handler and the version is not the current one.
    static func canRestore(versionID: String, currentID: String?, hasHandler: Bool) -> Bool {
        hasHandler && versionID != currentID
    }

    /// The version name sent on save: trimmed and cut to the longest allowed.
    static func note(_ text: String) -> String {
        String(text.trimmingCharacters(in: .whitespacesAndNewlines).prefix(maximumNoteLength))
    }

    /// The tab's title.
    static func title(_ tab: MonarchVersionHistoryPanelTab, labels: MonarchVersionHistoryPanelLabels) -> String {
        switch tab {
        case .changes: labels.changes ?? MonarchVersionHistoryPanelStrings.changes
        case .fullText: labels.fullText ?? MonarchVersionHistoryPanelStrings.fullText
        }
    }
}
