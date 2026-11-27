import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchMessageRow``.
enum MonarchMessageRowTokens {
    /// The size of the author's avatar.
    static let avatarSize: MonarchAvatarSize = .md
    /// The width of the avatar gutter, kept on grouped rows so bodies align.
    static let gutterWidth: CGFloat = MonarchAvatarTokens.side(avatarSize)
    /// The gap between the gutter and the message.
    static let gutterSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of a message.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the author, the tags and the time.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the row.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above the first message of a group.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above a grouped follow-up.
    static let groupedVerticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the leading bar of an internal note.
    static let barWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The gap between items of the failed line.
    static let statusSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol before the error of a failed message.
    static let failedSymbol = "exclamationmark.circle.fill"
    /// The SF Symbol of the retry button.
    static let retrySymbol = "arrow.clockwise"

    /// The fill of the row, or `nil` for none. A highlight wins over the
    /// internal tint, as on the web.
    static func background(variant: MonarchMessageRowVariant, isHighlighted: Bool) -> MonarchColorRole? {
        if isHighlighted { return \.accentSubtle }
        return variant == .internal ? \.warningSubtle : nil
    }

    /// The leading bar of the row, or `nil` for none.
    static func leadingBar(_ variant: MonarchMessageRowVariant) -> MonarchColorRole? {
        variant == .internal ? \.warningBorder : nil
    }

    /// The colour of the body text: muted while pending and when deleted.
    static func bodyForeground(status: MonarchMessageRowStatus, isDeleted: Bool) -> MonarchColorRole {
        isDeleted || status == .pending ? \.textMuted : \.textDefault
    }

    /// Whether the per-message actions are offered: only for a delivered,
    /// not deleted message.
    static func showsActions(status: MonarchMessageRowStatus, isDeleted: Bool, variant: MonarchMessageRowVariant)
        -> Bool
    {
        status == .sent && !isDeleted && variant != .system
    }

    /// Whether the reaction bar is shown.
    static func showsReactions(_ reactions: [MonarchReactionBarItem], isDeleted: Bool) -> Bool {
        !isDeleted && reactions.contains { $0.count > 0 }
    }

    /// The thread button text, or `nil` when there are no replies.
    static func repliesText(_ count: Int, labels: MonarchMessageRowLabels) -> String? {
        guard count > 0 else { return nil }
        return labels.replies?(count) ?? MonarchMessageRowStrings.replies(count)
    }

    /// The error shown under a failed message.
    static func errorText(_ message: String?, labels: MonarchMessageRowLabels) -> String {
        if let message, !message.isEmpty { return message }
        return labels.failed ?? MonarchMessageRowStrings.failed
    }

    /// The visible time: relative to `now`, or the clock time.
    static func timeText(
        _ date: Date,
        style: MonarchMessageRowTimeStyle,
        now: Date,
        locale: Locale,
        calendar: Calendar
    ) -> String {
        switch style {
        case .relative: relativeTime(date, now: now, locale: locale, calendar: calendar)
        case .clock: clockTime(date, locale: locale, calendar: calendar)
        }
    }

    /// "5 minutes ago", "yesterday". A time in the future reads as now.
    static func relativeTime(_ date: Date, now: Date, locale: Locale, calendar: Calendar) -> String {
        MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar)
    }

    /// The clock time, "14:05", in the locale's format.
    static func clockTime(_ date: Date, locale: Locale, calendar: Calendar) -> String {
        date.formatted(
            Date.FormatStyle(
                date: .omitted,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            ).monarchTime()
        )
    }

    /// The full date and time, read by VoiceOver instead of the visible time.
    static func absoluteTime(_ date: Date, locale: Locale, calendar: Calendar) -> String {
        date.formatted(
            Date.FormatStyle(
                date: .complete,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            ).monarchTime()
        )
    }

    /// What VoiceOver reads before the body of a grouped message, whose
    /// author and time are not shown.
    static func byline(author: String, time: String) -> String {
        MonarchMessageRowStrings.byline(author, time)
    }
}
