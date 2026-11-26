import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchApprovalQueue``.
enum MonarchApprovalQueueTokens {
    /// The gap between the checkbox, the avatar and the text.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a request.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the text and the buttons, and between buttons.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of the dialog.
    static let dialogSpacing: CGFloat = MonarchTokens.Space.space4
    /// Padding around the dialog.
    static let dialogPadding: CGFloat = MonarchTokens.Space.space6
    /// Padding above and below a request.
    static let rowVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of approve.
    static let approveSymbol = "checkmark"
    /// The SF Symbol of send back.
    static let rejectSymbol = "xmark"
    /// The SF Symbol of review.
    static let reviewSymbol = "eye"
    /// The SF Symbol of a failed decision.
    static let failedSymbol = "exclamationmark.triangle.fill"
    /// The swipe tint of approve.
    static var approveTint: MonarchColorRole { \.successSolid }
    /// The swipe tint of send back.
    static var rejectTint: MonarchColorRole { \.dangerSolid }
    /// The swipe tint of review.
    static var reviewTint: MonarchColorRole { \.accentSolid }
    /// The dot between the title and the time.
    static let metaDot = "·"
    /// The separator between parts of what VoiceOver reads.
    static let separator = ", "

    /// The text the search matches: the item's search text, or the requester's name and the title.
    static func searchText(_ item: MonarchApprovalQueueItem) -> String {
        item.searchText ?? "\(item.requester.name) \(item.title)"
    }

    /// The requests shown: those that match the trimmed query, ignoring case, at most `limit`.
    static func visible(_ items: [MonarchApprovalQueueItem], query: String, limit: Int?) -> [MonarchApprovalQueueItem] {
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let matched =
            needle.isEmpty
            ? items : items.filter { searchText($0).range(of: needle, options: .caseInsensitive) != nil }
        guard let limit else { return matched }
        return Array(matched.prefix(max(0, limit)))
    }

    /// The selected requests that still exist and can be decided, in list order.
    static func chosen(_ items: [MonarchApprovalQueueItem], selection: Set<String>) -> [MonarchApprovalQueueItem] {
        items.filter { $0.canDecide && selection.contains($0.id) }
    }

    /// The selection after Select all is turned on or off for the shown requests that can be decided.
    static func selectingAll(
        _ isOn: Bool,
        visible: [MonarchApprovalQueueItem],
        selection: Set<String>
    ) -> Set<String> {
        let ids = Set(visible.filter(\.canDecide).map(\.id))
        return isOn ? selection.union(ids) : selection.subtracting(ids)
    }

    /// The selection after one request is turned on or off.
    static func toggled(_ selection: Set<String>, id: String, isOn: Bool) -> Set<String> {
        var next = selection
        if isOn { next.insert(id) } else { next.remove(id) }
        return next
    }

    /// Whether the reason allows sending back: a required reason must have text.
    static func canConfirmReject(reason: String, requirement: MonarchApprovalQueueRejectReason) -> Bool {
        requirement != .required || !reason.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// The reason passed to the app: trimmed, or `nil` when empty or not asked for.
    static func submittedReason(_ reason: String, requirement: MonarchApprovalQueueRejectReason) -> String? {
        guard requirement != .none else { return nil }
        let trimmed = reason.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    /// The submission time relative to `now`, such as "2 hours ago".
    static func relativeTime(_ date: Date, now: Date, calendar: Calendar, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar, allowsFuture: true)
    }

    /// The submission date and time, which VoiceOver reads instead of the relative time.
    static func absoluteTime(_ date: Date, calendar: Calendar, locale: Locale) -> String {
        date.formatted(
            Date.FormatStyle(
                date: .abbreviated,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            ).monarchTime()
        )
    }

    /// What VoiceOver reads for a request: requester, detail, title, time,
    /// Past due, badges, summary and the failure.
    static func accessibilityLabel(
        _ item: MonarchApprovalQueueItem,
        time: String?,
        isFailed: Bool,
        labels: MonarchApprovalQueueLabels
    ) -> String {
        var parts = [item.requester.name]
        if let detail = item.requesterDetail, !detail.isEmpty { parts.append(detail) }
        parts.append(item.title)
        if let time { parts.append(time) }
        if item.isOverdue { parts.append(labels.overdue) }
        parts += item.badges.map(\.label)
        if let summary = item.summary, !summary.isEmpty { parts.append(summary) }
        if isFailed { parts.append(labels.failed) }
        return parts.joined(separator: separator)
    }
}
