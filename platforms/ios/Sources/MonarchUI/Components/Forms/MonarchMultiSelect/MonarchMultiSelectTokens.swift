import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and selection logic of ``MonarchMultiSelect``.
enum MonarchMultiSelectTokens {
    /// The gap between the chosen tags.
    static let tagSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The padding inside the field above and below the tags.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The size of the chosen tags.
    static let tagSize: MonarchTagSize = .sm
    /// The SF Symbol at the end of the field: the web's single chevron.
    static let chevronSymbol = "chevron.down"
    /// The SF Symbol of a chosen row, also shown by the system pattern.
    static let checkSymbol = "checkmark"

    /// One chosen value with the label to show.
    struct Pick: Equatable, Identifiable {
        let id: String
        let label: String
    }

    /// Whether the limit is reached.
    static func isFull(count: Int, maxSelections: Int?) -> Bool {
        guard let maxSelections else { return false }
        return count >= max(0, maxSelections)
    }

    /// The selection after a row is activated: a chosen value is removed, a
    /// new one is added at the end unless the limit is reached.
    static func toggled(_ selection: [String], id: String, maxSelections: Int?) -> [String] {
        if selection.contains(id) { return selection.filter { $0 != id } }
        guard !isFull(count: selection.count, maxSelections: maxSelections) else { return selection }
        return selection + [id]
    }

    /// Whether a row cannot be activated: the option is disabled, or the
    /// limit is reached and it is not chosen (a chosen one can always be removed).
    static func isRowDisabled(_ option: MonarchComboboxOption, selection: [String], maxSelections: Int?) -> Bool {
        if option.isDisabled { return true }
        return !selection.contains(option.id) && isFull(count: selection.count, maxSelections: maxSelections)
    }

    /// The chosen values in pick order with their labels: the option's label,
    /// a remembered label for a value no longer in `options`, or the raw value.
    static func picks(_ selection: [String], options: [MonarchComboboxOption], remembered: [String: String]) -> [Pick] {
        selection.map { id in
            Pick(id: id, label: options.first { $0.id == id }?.label ?? remembered[id] ?? id)
        }
    }

    /// The title of the field's button.
    static func buttonTitle(hasSelection: Bool, prompt: String?, labels: MonarchMultiSelectLabels) -> String {
        if hasSelection { return labels.change ?? MonarchMultiSelectStrings.change }
        return prompt ?? labels.select ?? MonarchMultiSelectStrings.select
    }

    /// The count under the field, or `nil` without a limit.
    static func countText(count: Int, maxSelections: Int?, labels: MonarchMultiSelectLabels) -> String? {
        guard let maxSelections else { return nil }
        return labels.count?(count, maxSelections) ?? MonarchMultiSelectStrings.count(count, maxSelections)
    }

    /// What VoiceOver reads as the field's value: the chosen labels, or "None".
    static func accessibilityValue(_ picks: [Pick], labels: MonarchMultiSelectLabels) -> String {
        guard !picks.isEmpty else { return labels.list.noSelection ?? MonarchComboboxStrings.noSelection }
        return picks.map(\.label).joined(separator: ", ")
    }

    /// The field's hint: the error, the help text, "Required" and the count.
    static func accessibilityHint(
        helper: String?,
        error: String?,
        isRequired: Bool,
        count: String?,
        labels: MonarchMultiSelectLabels
    ) -> String {
        let field = MonarchFormFieldTokens.accessibilityHint(
            helper: helper,
            error: error,
            isRequired: isRequired,
            labels: labels.list.field
        )
        return [field, count ?? ""].filter { !$0.isEmpty }.joined(separator: ", ")
    }

    /// The colour role of the field's button text: the prompt, or the edit action.
    static func buttonColor(hasSelection: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return hasSelection ? \.textMuted : \.textSubtle
    }
}
