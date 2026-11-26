import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchConnectedAppsList``.
enum MonarchConnectedAppsListTokens {
    /// The gap between the header and the list.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// Padding inside a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the logo and the text.
    static let logoSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the name and the badges.
    static let badgeSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between a scope's mark and its text.
    static let scopeSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The corner radius of the list.
    static let radius: CGFloat = MonarchTokens.Radius.lg
    /// The number of placeholder rows while loading.
    static let loadingRowCount = 2
    /// The SF Symbol in the empty state.
    static let emptySymbol = "link"
    /// The SF Symbol of the verified badge.
    static let verifiedSymbol = "checkmark"
    /// The SF Symbol before each scope.
    static let scopeSymbol = "checkmark"
    /// The SF Symbol before a failed removal.
    static let errorSymbol = "exclamationmark.circle"
    /// The separator between the parts of the meta line.
    static let metaSeparator = " · "

    /// A time relative to `now` in the locale, such as "3 days ago" or "yesterday".
    static func relative(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The parts of an app's meta line: when it was connected and last used, and its workspace.
    static func metaParts(
        _ app: MonarchConnectedApp,
        now: Date,
        locale: Locale,
        labels: MonarchConnectedAppsListLabels
    ) -> [String] {
        var parts: [String] = []
        if let connectedAt = app.connectedAt {
            let time = relative(connectedAt, now: now, locale: locale)
            parts.append(labels.connected?(time) ?? MonarchConnectedAppsListStrings.connected(time))
        }
        if let lastUsedAt = app.lastUsedAt {
            let time = relative(lastUsedAt, now: now, locale: locale)
            parts.append(labels.lastUsed?(time) ?? MonarchConnectedAppsListStrings.lastUsed(time))
        }
        if let workspace = app.workspace, !workspace.isEmpty {
            parts.append(labels.workspace?(workspace) ?? MonarchConnectedAppsListStrings.workspace(workspace))
        }
        return parts
    }

    /// The badges shown after an app's name, as text, in order.
    static func badges(_ app: MonarchConnectedApp, labels: MonarchConnectedAppsListLabels) -> [String] {
        var badges: [String] = []
        if app.isVerified { badges.append(labels.verified ?? MonarchConnectedAppsListStrings.verified) }
        if app.isSuspended { badges.append(labels.suspended ?? MonarchConnectedAppsListStrings.suspended) }
        return badges
    }

    /// What VoiceOver reads after an app's name: the badges, the domain and the meta parts.
    static func accessibilityValue(badges: [String], domain: String?, metaParts: [String]) -> String {
        (badges + [domain ?? ""] + metaParts).filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// What the list shows.
    enum Content: Equatable {
        /// The placeholder rows.
        case loading
        /// The error with a retry.
        case error
        /// The empty state.
        case empty
        /// The rows.
        case rows
    }

    /// What the list shows for a state and a number of apps.
    static func content(state: MonarchConnectedAppsListState, count: Int) -> Content {
        switch state {
        case .loading: .loading
        case .error: .error
        case .ready: count == 0 ? .empty : .rows
        }
    }

    /// The colour of an app's name: a suspended app is muted.
    static func nameRole(isSuspended: Bool) -> MonarchColorRole {
        isSuspended ? \.textMuted : \.textDefault
    }
}
