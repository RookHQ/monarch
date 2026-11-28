import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchPendingInvitesList``.
enum MonarchPendingInvitesListTokens {
    /// The gap between rows, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The separator between the parts of the detail line.
    static let separator = " · "
    /// The SF Symbol of the copy link button.
    static let copySymbol = "doc.on.doc"

    /// What the card shows: a loaded empty list is the empty state.
    static func phase(_ state: MonarchSectionCardState, invites: [MonarchPendingInvite]) -> MonarchSectionCardState {
        state == .ready && invites.isEmpty ? .empty : state
    }

    /// Whether an invitation has expired: its own flag, otherwise whether
    /// `expiresAt` is at or before `now`.
    static func isExpired(_ invite: MonarchPendingInvite, now: Date) -> Bool {
        if let isExpired = invite.isExpired { return isExpired }
        guard let expiresAt = invite.expiresAt else { return false }
        return expiresAt <= now
    }

    /// A time relative to `now`, such as "2 days ago" or "in 5 days".
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The detail line: sender, sent time and, unless expired, the expiry,
    /// such as "Invited by Sam · Sent 2 days ago · Expires in 5 days".
    static func detail(
        _ invite: MonarchPendingInvite,
        isExpired: Bool,
        now: Date,
        locale: Locale,
        labels: MonarchPendingInvitesListLabels
    ) -> String? {
        var parts: [String] = []
        if let sender = invite.invitedBy?.trimmingCharacters(in: .whitespacesAndNewlines), !sender.isEmpty {
            parts.append("\(labels.invitedBy ?? MonarchPendingInvitesListStrings.invitedBy) \(sender)")
        }
        if let sentAt = invite.sentAt {
            let prefix = labels.sent ?? MonarchPendingInvitesListStrings.sent
            parts.append("\(prefix) \(relativeTime(sentAt, now: now, locale: locale))")
        }
        if let expiresAt = invite.expiresAt, !isExpired {
            let prefix = labels.expires ?? MonarchPendingInvitesListStrings.expires
            parts.append("\(prefix) \(relativeTime(expiresAt, now: now, locale: locale))")
        }
        return parts.isEmpty ? nil : parts.joined(separator: separator)
    }

    /// The link to copy: present, not blank, and the invitation has not expired.
    static func copyableLink(_ invite: MonarchPendingInvite, isExpired: Bool) -> String? {
        guard !isExpired, let link = invite.link?.trimmingCharacters(in: .whitespacesAndNewlines), !link.isEmpty
        else { return nil }
        return link
    }

    /// The strings of a row. An expired invitation's status badge reads
    /// "Expired" instead of "Invited", so expiry is said in words.
    static func rowLabels(isExpired: Bool, labels: MonarchPendingInvitesListLabels) -> MonarchMemberRowLabels {
        var row = labels.row
        if isExpired {
            row.invited = labels.expired ?? MonarchPendingInvitesListStrings.expired
        }
        return row
    }

    /// The spoken words added to the copy button's name.
    static func copyTarget(_ email: String, labels: MonarchPendingInvitesListLabels) -> String {
        labels.copyTarget?(email) ?? MonarchPendingInvitesListStrings.copyTarget(email)
    }

    /// Announced after an invitation is revoked.
    static func revokedAnnouncement(_ email: String, labels: MonarchPendingInvitesListLabels) -> String {
        labels.revoked?(email) ?? MonarchPendingInvitesListStrings.revoked(email)
    }
}
