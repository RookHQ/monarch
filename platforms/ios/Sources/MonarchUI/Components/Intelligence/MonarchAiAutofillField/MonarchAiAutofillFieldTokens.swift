import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiAutofillField``.
enum MonarchAiAutofillFieldTokens {
    /// The gap between the control and the status line.
    static let spacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the parts of the status line.
    static let statusSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the control and the marker beside it.
    static let markerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width of the marker beside a suggested control.
    static let markerWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The SF Symbol of the accept button.
    static let acceptSymbol = "checkmark"
    /// The SF Symbol of the clear button.
    static let clearSymbol = "xmark"

    /// Whether the status line is shown: until the person clears the value.
    static func showsStatus(_ state: MonarchAiAutofillState) -> Bool {
        state != .cleared
    }

    /// Whether Accept and Clear are offered: while nobody has checked the value.
    static func showsActions(_ state: MonarchAiAutofillState) -> Bool {
        state == .suggested
    }

    /// Whether the confidence is shown: while suggested, when there is one.
    static func showsConfidence(_ state: MonarchAiAutofillState, confidence: Double?) -> Bool {
        state == .suggested && confidence != nil
    }

    /// Whether the source is shown: when there is one, until the person edits the value.
    static func showsSource(_ state: MonarchAiAutofillState, source: String?) -> Bool {
        guard let source, !source.isEmpty else { return false }
        return state != .edited && state != .cleared
    }

    /// Whether the original AI value is shown: once edited.
    static func showsOriginal(_ state: MonarchAiAutofillState) -> Bool {
        state == .edited
    }

    /// The marker beside the control: the AI border colour while suggested, else none.
    static func markerRole(_ state: MonarchAiAutofillState) -> MonarchColorRole? {
        state == .suggested ? \.borderStrong : nil
    }

    /// The kind of the provenance badge.
    static func badgeKind(_ state: MonarchAiAutofillState) -> MonarchAiGeneratedBadgeKind {
        state == .edited ? .edited : .generated
    }

    /// The badge text of a state.
    static func badgeText(_ state: MonarchAiAutofillState, labels: MonarchAiAutofillFieldLabels) -> String {
        labels.badge?(state) ?? MonarchAiAutofillFieldStrings.badge(state)
    }

    /// The source line.
    static func sourceText(_ source: String, labels: MonarchAiAutofillFieldLabels) -> String {
        labels.source?(source) ?? MonarchAiAutofillFieldStrings.source(source)
    }

    /// The line shown once edited.
    static func originalText(_ value: String, labels: MonarchAiAutofillFieldLabels) -> String {
        labels.original?(value) ?? MonarchAiAutofillFieldStrings.original(value)
    }

    /// The spoken name of the accept button: with the field's name when there is one.
    static func acceptLabel(fieldName: String?, labels: MonarchAiAutofillFieldLabels) -> String {
        guard let fieldName, !fieldName.isEmpty else { return labels.accept ?? MonarchAiAutofillFieldStrings.accept }
        return labels.acceptField?(fieldName) ?? MonarchAiAutofillFieldStrings.acceptField(fieldName)
    }

    /// The spoken name of the clear button: with the field's name when there is one.
    static func clearLabel(fieldName: String?, labels: MonarchAiAutofillFieldLabels) -> String {
        guard let fieldName, !fieldName.isEmpty else { return labels.clear ?? MonarchAiAutofillFieldStrings.clear }
        return labels.clearField?(fieldName) ?? MonarchAiAutofillFieldStrings.clearField(fieldName)
    }

    /// Announced after Accept.
    static func acceptedAnnouncement(_ labels: MonarchAiAutofillFieldLabels) -> String {
        labels.accepted ?? MonarchAiAutofillFieldStrings.accepted
    }

    /// Announced after Clear.
    static func clearedAnnouncement(_ labels: MonarchAiAutofillFieldLabels) -> String {
        labels.cleared ?? MonarchAiAutofillFieldStrings.cleared
    }
}
