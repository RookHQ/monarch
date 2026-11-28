import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSessionList``.
enum MonarchSessionListTokens {
    /// The gap between rows, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the icon, the text and the action of a row.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the label and the badge.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the empty state.
    static let emptySymbol = "person.badge.key"

    /// What the card shows.
    enum Phase: Equatable {
        /// The sessions.
        case list
        /// The loading rows.
        case loading
        /// The empty state.
        case empty
        /// The error state.
        case failed
    }

    /// The phase for a state and a list: a loaded empty list is the empty state.
    static func phase(_ state: MonarchSessionListState, sessions: [MonarchSession]) -> Phase {
        switch state {
        case .loading: .loading
        case .failed: .failed
        case .ready: sessions.isEmpty ? .empty : .list
        }
    }

    /// The section card state of a phase.
    static func cardState(_ phase: Phase) -> MonarchSectionCardState {
        switch phase {
        case .list: .ready
        case .loading: .loading
        case .empty: .empty
        case .failed: .error
        }
    }

    /// The ids of every session except the current one, in order.
    static func otherIDs(_ sessions: [MonarchSession]) -> [String] {
        sessions.filter { !$0.isCurrent }.map(\.id)
    }

    /// Whether the header shows "Sign out other devices": there is a handler,
    /// the list is shown and at least one session is not the current one.
    static func showsSignOutOthers(_ sessions: [MonarchSession], phase: Phase, hasHandler: Bool) -> Bool {
        hasHandler && phase == .list && !otherIDs(sessions).isEmpty
    }

    /// The SF Symbol of a kind of device, shared with ``MonarchDeviceList``.
    static func symbol(_ kind: MonarchDeviceKind) -> String {
        // The web draws a laptop for a desktop session.
        kind == .desktop ? "laptopcomputer" : MonarchDeviceListTokens.symbol(kind)
    }

    /// The signed-in and last-active lines. A current session has no
    /// last-active line: it is in use now.
    static func times(
        _ session: MonarchSession,
        now: Date,
        locale: Locale,
        signedIn: String,
        lastActive: String
    ) -> [String] {
        var lines: [String] = []
        if let date = session.signedIn {
            lines.append(
                "\(signedIn) \(MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true))"
            )
        }
        if !session.isCurrent, let date = session.lastActive {
            lines.append(
                "\(lastActive) \(MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true))"
            )
        }
        return lines
    }

    /// What VoiceOver reads after the label of a session: the current badge,
    /// the detail and the times, leaving out what is missing.
    static func accessibilityValue(current: String?, detail: String?, times: [String]) -> String {
        ([current, detail].compactMap { $0 } + times)
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }
}
