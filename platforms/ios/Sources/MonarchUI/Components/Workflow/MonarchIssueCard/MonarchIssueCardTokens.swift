import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchIssueCard``.
enum MonarchIssueCardTokens {
    /// The gap between the header, the title and the meta row.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between items of the header and the meta row.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a symbol and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The width of the border of a selected card.
    static let selectedBorderWidth: CGFloat = MonarchTokens.Space.px * 2
    /// The dash of the empty assignee slot.
    static let unassignedDash: [CGFloat] = [MonarchTokens.Space.space1, MonarchTokens.Space.space0_5]
    /// The side of the assignee slot before Dynamic Type: the small avatar.
    static let assigneeSide: CGFloat = MonarchTokens.Space.space6
    /// The SF Symbol of the due date.
    static let dueSymbol = "calendar"
    /// The SF Symbol of an overdue date.
    static let overdueSymbol = "exclamationmark.circle.fill"
    /// The SF Symbol of a selected card.
    static let selectedSymbol = "checkmark.circle.fill"
    /// The separator between parts of what VoiceOver reads.
    static let separator = ", "

    /// The surface of the card: tinted with the accent when selected.
    struct Surface: Equatable {
        /// The fill.
        let fill: MonarchColorRole
        /// The border.
        let border: MonarchColorRole
        /// The border width.
        let borderWidth: CGFloat
        /// The shadow.
        let elevation: MonarchElevationLevel
    }

    /// The surface for a state: selected cards are tinted, dragged cards are lifted.
    static func surface(isSelected: Bool, isDragging: Bool) -> Surface {
        Surface(
            fill: isSelected ? \.accentSubtle : MonarchCardTokens.background,
            border: isSelected ? \.accentBorder : \.borderDefault,
            borderWidth: isSelected ? selectedBorderWidth : MonarchCardTokens.borderWidth,
            elevation: isDragging ? .lg : .xs
        )
    }

    /// The SF Symbol of a priority, with its own shape per level.
    static func prioritySymbol(_ priority: MonarchIssueCardPriority) -> String {
        switch priority {
        case .urgent: "exclamationmark.square.fill"
        case .high: "chevron.up"
        case .medium: "equal"
        case .low: "chevron.down"
        case .none: "minus"
        }
    }

    /// The colour of a priority's symbol.
    static func priorityRole(_ priority: MonarchIssueCardPriority) -> MonarchColorRole {
        switch priority {
        case .urgent: \.dangerStrong
        case .high: \.warningStrong
        case .medium, .low: \.textDefault
        case .none: \.textMuted
        }
    }

    /// The SF Symbol of a status category: dashed ring, ring, half-filled ring, check or cross.
    static func statusSymbol(_ category: MonarchIssueCardStatusCategory) -> String {
        switch category {
        case .backlog: "circle.dashed"
        case .unstarted: "circle"
        case .started: "circle.lefthalf.filled"
        case .completed: "checkmark.circle.fill"
        case .cancelled: "xmark.circle.fill"
        }
    }

    /// The colour of a status category's symbol.
    static func statusRole(_ category: MonarchIssueCardStatusCategory) -> MonarchColorRole {
        switch category {
        case .backlog, .cancelled: \.textSubtle
        case .unstarted: \.textMuted
        case .started: \.warningStrong
        case .completed: \.successStrong
        }
    }

    /// Whether the issue is closed, so its due date can no longer be missed.
    static func isClosed(_ status: MonarchIssueCardStatus?) -> Bool {
        status?.category == .completed || status?.category == .cancelled
    }

    /// Whether the due day is before today's day, in the calendar's time zone, for an open issue.
    static func isOverdue(due: Date, today: Date, status: MonarchIssueCardStatus?, calendar: Calendar) -> Bool {
        guard !isClosed(status) else { return false }
        return calendar.startOfDay(for: due) < calendar.startOfDay(for: today)
    }

    /// The short due date, such as "Mar 12", in the calendar's time zone.
    static func dueDateText(_ due: Date, calendar: Calendar, locale: Locale) -> String {
        due.formatted(
            Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone).month(.abbreviated).day()
        )
    }

    /// Only http and https addresses open from the card.
    static func safeURL(_ url: URL?) -> URL? {
        guard let url, let scheme = url.scheme?.lowercased(), scheme == "http" || scheme == "https" else {
            return nil
        }
        return url
    }

    /// What VoiceOver reads for the due date: "Due Mar 12", and "Overdue" when it has passed.
    static func dueAccessibilityLabel(dateText: String, isOverdue: Bool, labels: MonarchIssueCardLabels) -> String {
        var parts = [labels.due(dateText)]
        if isOverdue { parts.append(labels.overdue) }
        return parts.joined(separator: separator)
    }

    /// What VoiceOver reads for the title: the key, then the title.
    static func titleAccessibilityLabel(key: String, title: String) -> String {
        [key, title].filter { !$0.isEmpty }.joined(separator: separator)
    }

    /// What VoiceOver reads for the assignee slot.
    static func assigneeAccessibilityLabel(_ assignee: MonarchIssueCardAssignee, labels: MonarchIssueCardLabels)
        -> String
    {
        switch assignee {
        case .person(let name, _): labels.assignee(name)
        case .unassigned: labels.unassigned
        }
    }
}
