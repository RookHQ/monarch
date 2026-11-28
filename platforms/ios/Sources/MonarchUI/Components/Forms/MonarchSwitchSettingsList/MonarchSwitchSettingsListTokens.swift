import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchSwitchSettingsList`` and ``MonarchSwitchSettingsRow``.
enum MonarchSwitchSettingsListTokens {
    /// The gap between the heading and the rows.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the description.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after the content of a row in a list.
    static let rowHorizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content of a row in a list.
    static let rowVerticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the switch and the status line.
    static let statusSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the label and the description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between an icon and its text in the status line.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The width of the border and the lines between rows.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The SF Symbol of the saved state.
    static let savedSymbol = "checkmark"
    /// The SF Symbol of the error state.
    static let errorSymbol = "exclamationmark.circle.fill"
    /// The SF Symbol of the retry button.
    static let retrySymbol = "arrow.clockwise"
    /// How long "Saved" stays by default.
    static let defaultSavedDuration: Duration = .seconds(2)

    /// The colour role of a row's label.
    static func labelColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour role of a row's description.
    static func descriptionColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The colour role of the status line for a status.
    static func statusColor(_ status: MonarchSwitchSettingsStatus) -> MonarchColorRole {
        switch status {
        case .idle, .saving: \.textMuted
        case .saved: \.successStrong
        case .error: \.dangerStrong
        }
    }

    /// The text of the status line, or `nil` when there is none.
    static func statusText(
        _ status: MonarchSwitchSettingsStatus,
        message: String?,
        labels: MonarchSwitchSettingsListLabels
    ) -> String? {
        switch status {
        case .idle: return nil
        case .saving: return labels.saving ?? MonarchSwitchSettingsListStrings.saving
        case .saved: return labels.saved ?? MonarchSwitchSettingsListStrings.saved
        case .error:
            let lead = labels.error ?? MonarchSwitchSettingsListStrings.error
            guard let message, !message.isEmpty else { return lead }
            return "\(lead) \(message)"
        }
    }

    /// What is announced when a row's status changes: "Saved" or the error.
    /// Saving is not announced; the spinner is labelled instead.
    static func announcement(
        _ status: MonarchSwitchSettingsStatus,
        message: String?,
        labels: MonarchSwitchSettingsListLabels
    ) -> String? {
        switch status {
        case .saved, .error: statusText(status, message: message, labels: labels)
        case .idle, .saving: nil
        }
    }

    /// The hint of a row's switch: the error while a save has failed.
    static func switchHint(
        _ status: MonarchSwitchSettingsStatus,
        message: String?,
        labels: MonarchSwitchSettingsListLabels
    ) -> String {
        status == .error ? statusText(status, message: message, labels: labels) ?? "" : ""
    }

    /// The title of the retry button.
    static func retryTitle(labels: MonarchSwitchSettingsListLabels) -> String {
        labels.retry ?? MonarchSwitchSettingsListStrings.retry
    }

    /// The spoken name of the retry button: "Retry <label>".
    static func retryLabel(rowLabel: String, labels: MonarchSwitchSettingsListLabels) -> String {
        labels.retryLabel?(rowLabel) ?? MonarchSwitchSettingsListStrings.retryLabel(rowLabel)
    }

    /// The message of a failed save.
    static func errorMessage(_ error: any Error, labels: MonarchSwitchSettingsListLabels) -> String? {
        if let convert = labels.errorMessage { return convert(error) }
        return (error as? any LocalizedError)?.errorDescription
    }
}

/// What a list hands to its rows.
struct MonarchSwitchSettingsContext: Sendable {
    /// The list's labels.
    var labels = MonarchSwitchSettingsListLabels()
    /// How long "Saved" stays.
    var savedDuration: Duration = MonarchSwitchSettingsListTokens.defaultSavedDuration
    /// Whether the row sits in a list, which draws its padding and lines.
    var isInList = false
}

private struct MonarchSwitchSettingsContextKey: EnvironmentKey {
    static let defaultValue = MonarchSwitchSettingsContext()
}

extension EnvironmentValues {
    /// The context of the surrounding ``MonarchSwitchSettingsList``.
    var monarchSwitchSettingsContext: MonarchSwitchSettingsContext {
        get { self[MonarchSwitchSettingsContextKey.self] }
        set { self[MonarchSwitchSettingsContextKey.self] = newValue }
    }
}
