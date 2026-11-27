import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiConnectorPicker``.
enum MonarchAiConnectorPickerTokens {
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between an icon and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a name and the line under it.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the no-results state.
    static let searchSymbol = "magnifyingglass"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "puzzlepiece.extension"
    /// The SF Symbol before the destructive lock reason.
    static let lockSymbol = "lock.fill"

    /// The counts behind the summary line.
    struct Counts: Equatable {
        /// Connectors that are connected and turned on.
        let on: Int
        /// All connectors, whatever the search.
        let total: Int
        /// Tools set to Ask first in the connectors that are on.
        let asking: Int
    }

    /// Whether the assistant can use a connector: connected and turned on.
    static func isActive(_ connector: MonarchAiConnector) -> Bool {
        connector.isEnabled && connector.status == .connected
    }

    /// The counts of the summary line.
    static func counts(_ connectors: [MonarchAiConnector]) -> Counts {
        let active = connectors.filter(isActive)
        let asking = active.flatMap(\.tools).filter { $0.permission == .ask }.count
        return Counts(on: active.count, total: connectors.count, asking: asking)
    }

    /// The summary line.
    static func summary(_ counts: Counts, labels: MonarchAiConnectorPickerLabels) -> String {
        labels.summary?(counts.on, counts.total, counts.asking)
            ?? MonarchAiConnectorPickerStrings.summary(on: counts.on, total: counts.total, asking: counts.asking)
    }

    /// The connectors whose name contains the query, ignoring case, accents and outer spaces.
    static func filtered(_ connectors: [MonarchAiConnector], query: String, locale: Locale) -> [MonarchAiConnector] {
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !needle.isEmpty else { return connectors }
        return connectors.filter {
            $0.name.range(of: needle, options: [.caseInsensitive, .diacriticInsensitive], locale: locale) != nil
        }
    }

    /// The text of a status, with the account after Connected.
    static func statusText(_ connector: MonarchAiConnector, labels: MonarchAiConnectorPickerLabels) -> String {
        let status = labels.status?(connector.status) ?? MonarchAiConnectorPickerStrings.status(connector.status)
        guard connector.status == .connected, let account = connector.account, !account.isEmpty else { return status }
        return "\(status) \(labels.account?(account) ?? MonarchAiConnectorPickerStrings.account(account))"
    }

    /// Why the switch is off, or `nil` for a connected connector.
    static func disabledReason(_ status: MonarchAiConnectorStatus, labels: MonarchAiConnectorPickerLabels) -> String? {
        guard status != .connected else { return nil }
        return labels.disabledReason?(status) ?? MonarchAiConnectorPickerStrings.disabledReason(status)
    }

    /// The SF Symbol of a status.
    static func statusSymbol(_ status: MonarchAiConnectorStatus) -> String {
        switch status {
        case .connected: "checkmark.circle"
        case .needsSignIn: "lock.fill"
        case .error: "xmark.circle"
        case .unavailable: "wifi.slash"
        }
    }

    /// The colour role of a status symbol. The text says the same.
    static func statusRole(_ status: MonarchAiConnectorStatus) -> MonarchColorRole {
        switch status {
        case .connected: \.successStrong
        case .needsSignIn: \.warningStrong
        case .error: \.dangerStrong
        case .unavailable: \.textMuted
        }
    }

    /// The button that fixes the connection: Sign in, Reconnect, or none.
    static func connectTitle(_ status: MonarchAiConnectorStatus, labels: MonarchAiConnectorPickerLabels) -> String? {
        switch status {
        case .needsSignIn: labels.signIn ?? MonarchAiConnectorPickerStrings.signIn
        case .error: labels.reconnect ?? MonarchAiConnectorPickerStrings.reconnect
        case .connected, .unavailable: nil
        }
    }

    /// The tone of a risk badge.
    static func tone(_ risk: MonarchAiConnectorToolRisk) -> MonarchTone {
        switch risk {
        case .read: .neutral
        case .write: .warning
        case .destructive: .danger
        }
    }

    /// The SF Symbol of a risk badge.
    static func riskSymbol(_ risk: MonarchAiConnectorToolRisk) -> String {
        switch risk {
        case .read: "eye"
        case .write: "pencil"
        case .destructive: "exclamationmark.triangle"
        }
    }

    /// Whether Allow is withheld from a tool.
    static func isLocked(_ tool: MonarchAiConnectorTool, allowsDestructiveAutoApprove: Bool) -> Bool {
        tool.risk == .destructive && !allowsDestructiveAutoApprove
    }

    /// The permissions offered for a tool. A locked tool is not offered Allow,
    /// unless it is already set to Allow, so its value is never hidden.
    static func options(_ tool: MonarchAiConnectorTool, allowsDestructiveAutoApprove: Bool)
        -> [MonarchAiConnectorToolPermission]
    {
        guard isLocked(tool, allowsDestructiveAutoApprove: allowsDestructiveAutoApprove), tool.permission != .allow
        else {
            return MonarchAiConnectorToolPermission.allCases
        }
        return [.ask, .never]
    }

    /// The connectors with one connector's switch changed.
    static func setting(_ connectors: [MonarchAiConnector], id: String, isEnabled: Bool) -> [MonarchAiConnector] {
        connectors.map { connector in
            guard connector.id == id, connector.status == .connected || !isEnabled else { return connector }
            var changed = connector
            changed.isEnabled = isEnabled
            return changed
        }
    }

    /// The connectors with one tool's permission changed. Allow is refused for a locked tool.
    static func setting(
        _ connectors: [MonarchAiConnector],
        id: String,
        tool: String,
        to permission: MonarchAiConnectorToolPermission,
        allowsDestructiveAutoApprove: Bool
    ) -> [MonarchAiConnector] {
        connectors.map { connector in
            guard connector.id == id else { return connector }
            var changed = connector
            changed.tools = connector.tools.map { item in
                guard item.id == tool else { return item }
                if permission == .allow, isLocked(item, allowsDestructiveAutoApprove: allowsDestructiveAutoApprove) {
                    return item
                }
                var next = item
                next.permission = permission
                return next
            }
            return changed
        }
    }
}
