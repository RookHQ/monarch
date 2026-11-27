import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchApiKeyRow``.
enum MonarchApiKeyRowTokens {
    /// Space between the row's lines.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space2
    /// Space between the name and the status, and between scope badges.
    static let inlineSpacing: CGFloat = MonarchTokens.Space.space2
    /// Vertical padding of the row.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The mask after the prefix. It stands for the hidden rest of the key.
    static let mask = "••••••••"
    /// The separator between the meta parts.
    static let metaSeparator = " · "

    /// An action that needs confirmation.
    enum Operation: Equatable {
        /// Issue a new key and retire this one.
        case rotate
        /// Revoke this key.
        case revoke
    }

    /// Where a confirmed action stands.
    struct OperationState: Equatable {
        /// The action in progress.
        var running: Operation?
        /// The action that failed last.
        var failed: Operation?
        /// Why it failed.
        var message: String?

        /// Starts an action, unless one is already running.
        mutating func begin(_ operation: Operation) -> Bool {
            guard running == nil else { return false }
            running = operation
            failed = nil
            message = nil
            return true
        }

        /// Ends the running action successfully.
        mutating func succeed() {
            running = nil
        }

        /// Ends the running action with a failure.
        mutating func fail(message: String) {
            failed = running
            self.message = message
            running = nil
        }
    }

    /// The badge tone of a status, as on the web.
    static func tone(_ status: MonarchApiKeyStatus) -> MonarchTone {
        switch status {
        case .active: .success
        case .expiring: .warning
        case .expired: .neutral
        case .revoked: .danger
        }
    }

    /// Whether the key can still be rotated or revoked.
    static func isLive(_ status: MonarchApiKeyStatus) -> Bool {
        status == .active || status == .expiring
    }

    /// Whether the actions menu is shown: a live key with at least one action.
    static func showsMenu(status: MonarchApiKeyStatus, canRotate: Bool, canRevoke: Bool, hasActions: Bool) -> Bool {
        isLive(status) && (canRotate || canRevoke || hasActions)
    }

    /// The badge for a status.
    static func statusDefinition(_ status: MonarchApiKeyStatus, labels: MonarchApiKeyRowLabels)
        -> MonarchStatusDefinition
    {
        MonarchStatusDefinition(labels.status ?? MonarchApiKeyRowStrings.status(status), tone: tone(status))
    }

    /// The scopes shown as badges and the ones folded into "+N".
    static func scopes(_ scopes: [String], max: Int) -> (shown: [String], hidden: [String]) {
        let limit = Swift.max(0, max)
        return (Array(scopes.prefix(limit)), Array(scopes.dropFirst(limit)))
    }

    /// The scopes as VoiceOver reads them: "Scopes", then every scope, the
    /// hidden ones too, or "No scopes".
    static func scopesAccessibility(_ scopes: [String], labels: MonarchApiKeyRowLabels) -> (
        label: String, value: String
    ) {
        let label = text(labels.scopes, "scopes")
        guard !scopes.isEmpty else { return (label, text(labels.noScopes, "noScopes")) }
        return (label, scopes.joined(separator: ", "))
    }

    /// The prefix's spoken name: "Key starting with sk_live_4f2a".
    static func prefixAccessibility(_ prefix: String, labels: MonarchApiKeyRowLabels) -> String {
        format(labels.prefix, "prefix", prefix)
    }

    /// A time relative to `now`, such as "3 months ago" or "in 5 days".
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The meta parts: created, last used (or never used) and, if set, expires.
    static func meta(
        createdAt: Date,
        lastUsedAt: Date?,
        expiresAt: Date?,
        now: Date,
        locale: Locale,
        labels: MonarchApiKeyRowLabels
    ) -> [String] {
        var parts = [format(labels.created, "created", relativeTime(createdAt, now: now, locale: locale))]
        if let lastUsedAt {
            parts.append(format(labels.lastUsed, "lastUsed", relativeTime(lastUsedAt, now: now, locale: locale)))
        } else {
            parts.append(text(labels.neverUsed, "neverUsed"))
        }
        if let expiresAt {
            parts.append(format(labels.expires, "expires", relativeTime(expiresAt, now: now, locale: locale)))
        }
        return parts
    }

    /// The menu's spoken name: "Actions for Reporting job".
    static func actionsLabel(name: String, labels: MonarchApiKeyRowLabels) -> String {
        format(labels.actions, "actions", name)
    }

    /// The confirmation title of an action: "Rotate Reporting job?".
    static func confirmTitle(_ operation: Operation, name: String, labels: MonarchApiKeyRowLabels) -> String {
        switch operation {
        case .rotate: format(labels.rotateTitle, "rotateTitle", name)
        case .revoke: format(labels.revokeTitle, "revokeTitle", name)
        }
    }

    /// The confirmation message of an action.
    static func confirmMessage(_ operation: Operation, labels: MonarchApiKeyRowLabels) -> String {
        switch operation {
        case .rotate: text(labels.rotateMessage, "rotateMessage")
        case .revoke: text(labels.revokeMessage, "revokeMessage")
        }
    }

    /// The confirming button of an action.
    static func confirmButton(_ operation: Operation, labels: MonarchApiKeyRowLabels) -> String {
        switch operation {
        case .rotate: text(labels.rotateConfirm, "rotateConfirm")
        case .revoke: text(labels.revokeConfirm, "revokeConfirm")
        }
    }

    /// The menu item of an action.
    static func menuTitle(_ operation: Operation, labels: MonarchApiKeyRowLabels) -> String {
        switch operation {
        case .rotate: text(labels.rotate, "rotate")
        case .revoke: text(labels.revoke, "revoke")
        }
    }

    /// The progress text while an action runs.
    static func progress(_ operation: Operation, labels: MonarchApiKeyRowLabels) -> String {
        switch operation {
        case .rotate: text(labels.rotating, "rotating")
        case .revoke: text(labels.revoking, "revoking")
        }
    }

    /// The message of a failure: the error's own description, or the built-in sentence.
    static func failureMessage(_ operation: Operation, error: any Error, labels: MonarchApiKeyRowLabels) -> String {
        let fallback =
            switch operation {
            case .rotate: text(labels.rotateFailed, "rotateFailed")
            case .revoke: text(labels.revokeFailed, "revokeFailed")
            }
        return MonarchFailureMessage.message(for: error, fallback: fallback)
    }

    /// "Cancel", with the override applied.
    static func cancel(labels: MonarchApiKeyRowLabels) -> String {
        text(labels.cancel, "cancel")
    }

    /// The prompt of the confirmation phrase field: "Type Reporting job to confirm".
    static func phrasePrompt(_ phrase: String, labels: MonarchApiKeyRowLabels) -> String {
        format(labels.revokePhrasePrompt, "revokePhrasePrompt", phrase)
    }

    /// Whether a revocation may go ahead: no phrase is asked for, or the typed text matches it.
    static func canConfirmRevoke(typed: String, phrase: String?) -> Bool {
        guard let phrase else { return true }
        return MonarchTypeToConfirmInput.matches(typed, phrase: phrase)
    }

    private static func text(_ override: String?, _ key: String) -> String {
        override ?? MonarchApiKeyRowStrings.string(key)
    }

    private static func format(_ override: String?, _ key: String, _ argument: String) -> String {
        String(format: text(override, key), argument)
    }
}
