import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchErrorState``. Sizes and type come from
/// `MonarchEmptyStateTokens`, because the two share one layout.
enum MonarchErrorStateTokens {
    /// The gap between the label and the value of the request id.
    static let requestIDSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the retry button.
    static let retrySymbol = "arrow.clockwise"

    /// The SF Symbol: the app's, or the one of the kind.
    static func symbol(kind: MonarchErrorStateKind, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        return switch kind {
        case .error: "exclamationmark.triangle"
        case .forbidden: "lock"
        case .notFound: "magnifyingglass"
        case .offline: "wifi.slash"
        case .server: "xmark.octagon"
        case .maintenance: "wrench.and.screwdriver"
        }
    }

    /// The colour role of the icon: danger for a failure, muted for a state
    /// that is not a fault (no access, not found, maintenance).
    static func iconRole(kind: MonarchErrorStateKind) -> MonarchColorRole {
        switch kind {
        case .error, .offline, .server: \.dangerStrong
        case .forbidden, .notFound, .maintenance: \.textMuted
        }
    }

    /// The colour role of the tile behind the icon: a tint of danger for a failure, as on the web.
    static func tileRole(kind: MonarchErrorStateKind) -> MonarchColorRole {
        switch kind {
        case .error, .offline, .server: \.dangerSubtle
        case .forbidden, .notFound, .maintenance: \.bgMuted
        }
    }

    /// Whether a retry can help. Retrying does not grant access or create
    /// what does not exist.
    static func canRetry(kind: MonarchErrorStateKind) -> Bool {
        switch kind {
        case .error, .offline, .server, .maintenance: true
        case .forbidden, .notFound: false
        }
    }

    /// Whether the retry button is shown.
    static func showsRetry(kind: MonarchErrorStateKind, hasHandler: Bool) -> Bool {
        hasHandler && canRetry(kind: kind)
    }

    /// What VoiceOver reads for the request id, or `nil` when there is none.
    static func requestIDLabel(_ requestID: String?, label: String?) -> String? {
        guard let requestID, !requestID.isEmpty else { return nil }
        return "\(label ?? MonarchErrorStateStrings.requestID): \(requestID)"
    }
}
