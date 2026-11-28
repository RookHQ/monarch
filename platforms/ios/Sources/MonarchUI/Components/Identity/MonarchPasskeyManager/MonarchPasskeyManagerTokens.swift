import AuthenticationServices
import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchPasskeyManager``.
enum MonarchPasskeyManagerTokens {
    /// The gap between the header, the notices and the list.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the line under it.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between rows, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon and the text of a row.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the name and the badge, and between actions.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of a passkey.
    static let symbol = "key"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "person.badge.key"
    /// The SF Symbol of the add button.
    /// The tile behind a passkey's key icon.
    static let iconTile: CGFloat = MonarchTokens.Space.space8
    static let addSymbol = "plus"

    /// What the card shows.
    enum Phase: Equatable {
        /// The passkeys.
        case list
        /// The loading indicator.
        case loading
        /// The empty state.
        case empty
        /// The error state.
        case failed
    }

    /// The phase for a state and a list: a loaded empty list is the empty state.
    static func phase(_ state: MonarchPasskeyManagerState, passkeys: [MonarchPasskey]) -> Phase {
        switch state {
        case .loading: .loading
        case .failed: .failed
        case .ready: passkeys.isEmpty ? .empty : .list
        }
    }

    /// A message in the card, with its tone.
    struct Notice: Equatable {
        /// The text.
        let text: String
        /// The tone: neutral for a cancellation, danger for a failure.
        let tone: MonarchTone
    }

    /// Whether an error means the person cancelled the system passkey sheet
    /// or the task was cancelled. Cancelling is not a failure.
    static func isCancellation(_ error: any Error) -> Bool {
        if error is CancellationError { return true }
        if let authorization = error as? ASAuthorizationError, authorization.code == .canceled { return true }
        let nsError = error as NSError
        return nsError.domain == ASAuthorizationError.errorDomain
            && nsError.code == ASAuthorizationError.Code.canceled.rawValue
    }

    /// The notice after adding failed: a neutral note for a cancellation, the
    /// error's own description when it has one, else the built-in failure.
    static func addFailure(_ error: any Error, labels: MonarchPasskeyManagerLabels) -> Notice {
        if isCancellation(error) {
            return Notice(text: labels.cancelled ?? MonarchPasskeyManagerStrings.cancelled, tone: .neutral)
        }
        if let description = (error as? any LocalizedError)?.errorDescription, !description.isEmpty {
            return Notice(text: description, tone: .danger)
        }
        return Notice(text: labels.addFailed ?? MonarchPasskeyManagerStrings.addFailed, tone: .danger)
    }

    /// A time relative to `now`, such as "5 minutes ago" or "yesterday".
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The added and last-used lines. A passkey never used reads "Never used".
    static func times(
        _ passkey: MonarchPasskey,
        now: Date,
        locale: Locale,
        labels: MonarchPasskeyManagerLabels
    ) -> [String] {
        var lines: [String] = []
        if let date = passkey.created {
            let prefix = labels.created ?? MonarchPasskeyManagerStrings.created
            lines.append("\(prefix) \(relativeTime(date, now: now, locale: locale))")
        }
        if let date = passkey.lastUsed {
            let prefix = labels.lastUsed ?? MonarchPasskeyManagerStrings.lastUsed
            lines.append("\(prefix) \(relativeTime(date, now: now, locale: locale))")
        } else {
            lines.append(labels.neverUsed ?? MonarchPasskeyManagerStrings.neverUsed)
        }
        return lines
    }

    /// What VoiceOver reads after the name of a passkey: synced, the detail
    /// and the times, leaving out what is missing.
    static func accessibilityValue(synced: String?, detail: String?, times: [String]) -> String {
        ([synced, detail].compactMap { $0 } + times)
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }

    /// Whether removing this passkey would leave the person without a way to sign in.
    static func isLastMethod(passkeyCount: Int, isLastSignInMethod: Bool) -> Bool {
        isLastSignInMethod && passkeyCount == 1
    }

    /// The removal confirmation message, with the warning when it is the last way to sign in.
    static func confirmMessage(isLastMethod: Bool, labels: MonarchPasskeyManagerLabels) -> String {
        let message = labels.confirmMessage ?? MonarchPasskeyManagerStrings.confirmMessage
        guard isLastMethod else { return message }
        return message + "\n\n" + (labels.lastMethodWarning ?? MonarchPasskeyManagerStrings.lastMethodWarning)
    }

    /// The new name to save, or `nil` when it is blank or unchanged.
    static func renamed(_ draft: String, from name: String) -> String? {
        let trimmed = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty || trimmed == name ? nil : trimmed
    }

    /// Whether the Add button can be used.
    static func canAdd(isSupported: Bool, isAdding: Bool) -> Bool {
        isSupported && !isAdding
    }
}
