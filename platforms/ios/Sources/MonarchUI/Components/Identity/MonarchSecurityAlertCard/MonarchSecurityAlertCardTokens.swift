import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchSecurityAlertCard``.
enum MonarchSecurityAlertCardTokens {
    /// The gap between the header, the actions and the list.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the header icon and the title.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the title and the line under it.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between alerts, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the severity icon and the text of an alert.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2_5
    /// The gap between the parts of an alert.
    static let rowContentSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the badge and the time.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the actions of an alert.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The diameter of the unread mark, before Dynamic Type.
    static let unreadDotSize: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol in the header.
    static let headerSymbol = "bell"
    /// The SF Symbol of the dismiss button.
    static let dismissSymbol = "xmark"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "checkmark.shield"
    /// The colour role of the unread mark.
    static var unreadRole: MonarchColorRole { \.accentSolid }

    /// The tone of a severity: low is neutral, medium a warning, high and critical danger.
    static func tone(_ severity: MonarchSecurityAlertSeverity) -> MonarchTone {
        switch severity {
        case .info: .info
        case .low: .neutral
        case .medium: .warning
        case .high, .critical: .danger
        }
    }

    /// The badge variant: critical is solid so it stands out from high, which shares its tone.
    static func badgeVariant(_ severity: MonarchSecurityAlertSeverity) -> MonarchBadgeVariant {
        severity == .critical ? .solid : .subtle
    }

    /// The SF Symbol of a severity.
    static func symbol(_ severity: MonarchSecurityAlertSeverity) -> String {
        switch severity {
        case .info, .low: "info.circle"
        case .medium: "exclamationmark.triangle"
        case .high, .critical: "xmark.octagon"
        }
    }

    /// The colour role of the severity icon.
    static func iconRole(_ severity: MonarchSecurityAlertSeverity) -> MonarchColorRole {
        severity == .low ? \.textMuted : MonarchTonePalette.palette(for: tone(severity)).strong
    }

    /// The fill behind the severity icon: the tone's tint, muted for low.
    static func iconFill(_ severity: MonarchSecurityAlertSeverity) -> MonarchColorRole {
        severity == .low ? \.bgMuted : MonarchTonePalette.palette(for: tone(severity)).subtle
    }

    /// What the card shows.
    enum Phase: Equatable {
        /// The alerts.
        case list
        /// The loading indicator.
        case loading
        /// The empty state.
        case empty
        /// The error state.
        case failed
    }

    /// The phase for a state and a list: a loaded empty list is the empty state.
    static func phase(_ state: MonarchSecurityAlertCardState, alerts: [MonarchSecurityAlert]) -> Phase {
        switch state {
        case .loading: .loading
        case .failed: .failed
        case .ready: alerts.isEmpty ? .empty : .list
        }
    }

    /// The number of unread alerts.
    static func unreadCount(_ alerts: [MonarchSecurityAlert]) -> Int {
        alerts.filter { !$0.isRead }.count
    }

    /// The action link, when it is safe to open: `http`, `https` or `mailto`
    /// with a destination (`Internal/MonarchSafeURL`). Anything else, such as `javascript:` or a custom
    /// scheme, is dropped.
    static func safeURL(_ url: URL?) -> URL? {
        url.flatMap(MonarchSafeURL.url)
    }

    /// How the follow-up of an alert is offered.
    enum FollowUp: Equatable {
        /// A link that opens the URL.
        case link(String, URL)
        /// A button that calls the handler.
        case button(String)
    }

    /// The follow-up of an alert: a safe link, else a button when the card has an action handler, else none.
    static func followUp(_ alert: MonarchSecurityAlert, hasHandler: Bool) -> FollowUp? {
        guard let title = alert.actionTitle, !title.isEmpty else { return nil }
        if let url = safeURL(alert.actionURL) { return .link(title, url) }
        return hasHandler ? .button(title) : nil
    }

    /// The time of an alert relative to `now`, such as "5 minutes ago" or "yesterday".
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// What VoiceOver reads after the title of an alert: unread, the severity,
    /// the time and the description, in that order, leaving out what is missing.
    static func accessibilityValue(
        unread: String?,
        severity: String,
        time: String?,
        description: String?
    ) -> String {
        [unread, severity, time, description]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }
}
