import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchFormField``.
enum MonarchFormFieldTokens {
    /// The gap between the label, the control and the supporting text.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the text column and the control in the row layout.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the label and the help text in the row layout.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the label and the required mark or optional text.
    static let markSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol shown before an error message.
    static let errorSymbol = "exclamationmark.circle"
    /// The visible mark after the label of a required field. It is hidden
    /// from VoiceOver, which hears "Required" in the hint instead.
    static let requiredMark = "*"

    /// The error to show: `nil` for a missing or empty message.
    static func activeError(_ error: String?) -> String? {
        guard let error, !error.isEmpty else { return nil }
        return error
    }

    /// The colour role of the label.
    static func labelColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour role of the help text.
    static func helperColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The colour role of the required mark.
    static func requiredColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.dangerStrong : \.textDisabled
    }

    /// The colour role of the error message and its icon.
    static var errorColor: MonarchColorRole { \.dangerStrong }

    /// Whether the parts are drawn as a stack: always for the stack layout,
    /// and for the row layout at the accessibility text sizes.
    static func stacks(layout: MonarchFormFieldLayout, isAccessibilitySize: Bool) -> Bool {
        layout == .stack || isAccessibilitySize
    }

    /// The spoken error, from the override or the strings table.
    static func spokenError(_ message: String, labels: MonarchFormFieldLabels) -> String {
        labels.error?(message) ?? MonarchFormFieldStrings.error(message)
    }

    /// What VoiceOver reads after the control's label and value: the error,
    /// the help text, then "Required", each when present.
    static func accessibilityHint(
        helper: String?,
        error: String?,
        isRequired: Bool,
        labels: MonarchFormFieldLabels
    ) -> String {
        var parts: [String] = []
        if let error = activeError(error) { parts.append(spokenError(error, labels: labels)) }
        if let helper, !helper.isEmpty { parts.append(helper) }
        if isRequired { parts.append(labels.required ?? MonarchFormFieldStrings.required) }
        return parts.joined(separator: ", ")
    }
}
