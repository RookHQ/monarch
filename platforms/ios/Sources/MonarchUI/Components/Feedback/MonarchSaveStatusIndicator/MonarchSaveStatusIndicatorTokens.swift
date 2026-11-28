import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchSaveStatusIndicator``.
enum MonarchSaveStatusIndicatorTokens {
    /// The gap between the parts of the line.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The role of the separator and the time.
    static var muted: MonarchColorRole { \.textMuted }
    /// The visible separator before the time.
    static let separator = "·"

    /// The SF Symbol of a status, or `nil` for none (idle), a dot (unsaved) or a spinner (saving).
    static func symbol(_ status: MonarchSaveStatus) -> String? {
        switch status {
        case .saved: "checkmark"
        case .error: "exclamationmark.triangle"
        case .unsaved, .idle, .saving: nil
        }
    }

    /// The diameter of the dot that marks unsaved changes: small, as on the web, not a full icon.
    static let dotDiameter: CGFloat = MonarchTokens.Space.space1_5

    /// The size of the icon.
    static let iconSize: MonarchIconSize = .sm

    /// The colour role of the icon.
    static func icon(_ status: MonarchSaveStatus) -> MonarchColorRole {
        switch status {
        case .unsaved: \.warningStrong
        case .saved: \.successStrong
        case .error: \.dangerStrong
        case .idle, .saving: \.textMuted
        }
    }

    /// The colour role of the text: danger for a failure, muted otherwise.
    static func text(_ status: MonarchSaveStatus) -> MonarchColorRole {
        status == .error ? \.dangerStrong : \.textMuted
    }

    /// The words of a status, or `nil` for idle.
    static func message(_ status: MonarchSaveStatus, errorMessage: String?, labels: MonarchSaveStatusIndicatorLabels)
        -> String?
    {
        switch status {
        case .idle: nil
        case .unsaved: labels.unsaved ?? MonarchSaveStatusIndicatorStrings.unsaved
        case .saving: labels.saving ?? MonarchSaveStatusIndicatorStrings.saving
        case .saved: labels.saved ?? MonarchSaveStatusIndicatorStrings.saved
        case .error:
            errorMessage.flatMap { $0.isEmpty ? nil : $0 }
                ?? labels.error ?? MonarchSaveStatusIndicatorStrings.error
        }
    }

    /// Whether the time of the last save is shown: only when saved.
    static func showsTime(_ status: MonarchSaveStatus, savedAt: Date?) -> Bool {
        status == .saved && savedAt != nil
    }

    /// Whether the retry button is shown: only for a failure the app can retry.
    static func showsRetry(_ status: MonarchSaveStatus, hasHandler: Bool) -> Bool {
        status == .error && hasHandler
    }

    /// What is announced when the status changes: the outcome (saved or the
    /// failure), and "Saving…" only when asked, so autosaves stay quiet.
    static func announcement(
        from old: MonarchSaveStatus,
        to new: MonarchSaveStatus,
        announcesSaving: Bool,
        errorMessage: String?,
        labels: MonarchSaveStatusIndicatorLabels
    ) -> String? {
        guard old != new else { return nil }
        switch new {
        case .saved, .error: return message(new, errorMessage: errorMessage, labels: labels)
        case .saving: return announcesSaving ? message(new, errorMessage: errorMessage, labels: labels) : nil
        case .idle, .unsaved: return nil
        }
    }

    /// A date relative to `now` in words, such as "2 minutes ago".
    static func relativeText(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// What VoiceOver reads: the words, then the time after a pause.
    static func accessibilityLabel(message: String, time: String?) -> String {
        guard let time else { return message }
        return message + ", " + time
    }
}
