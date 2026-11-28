import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and list logic for ``MonarchCombobox``.
enum MonarchComboboxTokens {
    /// The least height of the field and of each row, which is also the touch target.
    static let minHeight: CGFloat = MonarchTextFieldTokens.minHeight
    /// Padding before and after the field's text.
    static let horizontalPadding: CGFloat = MonarchTextFieldTokens.horizontalPadding
    /// Padding above and below the field's text.
    static let verticalPadding: CGFloat = MonarchTextFieldTokens.verticalPadding
    /// The gap between the label, the field and the supporting text.
    static let stackSpacing: CGFloat = MonarchTextFieldTokens.stackSpacing
    /// The gap between the parts of the field and of a row.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between an option's label and its description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the error icon and the error text.
    static let messageIconSpacing: CGFloat = MonarchTextFieldTokens.messageIconSpacing
    /// The SF Symbol at the end of the field.
    static let chevronSymbol = "chevron.down"
    /// The SF Symbol of the clear button.
    static let clearSymbol = "xmark.circle.fill"
    /// The SF Symbol beside the chosen option.
    static let checkSymbol = "checkmark"
    /// The SF Symbol of the create option.
    static let createSymbol = "plus"
    /// The SF Symbol shown before an error message.
    static let errorSymbol = MonarchTextFieldTokens.errorSymbol

    /// A group of results under an optional heading.
    struct Section: Equatable {
        let title: String?
        let options: [MonarchComboboxOption]
    }

    /// The options to show for a query.
    struct Results: Equatable {
        let sections: [Section]
        /// The number of options shown.
        let shown: Int
        /// The number of options that match.
        let total: Int

        var isEmpty: Bool { shown == 0 }
        var isTruncated: Bool { shown < total }
    }

    /// The default filter: the label, hint or keywords contain the query,
    /// ignoring case and diacritics.
    static func matches(_ option: MonarchComboboxOption, query: String) -> Bool {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return true }
        return [option.label, option.hint, option.keywords].contains { text in
            text?.localizedStandardContains(query) ?? false
        }
    }

    /// How well an option matches: 0 when the label starts with the query,
    /// 1 when a word in it does, 2 otherwise.
    static func rank(_ option: MonarchComboboxOption, query: String) -> Int {
        let label = fold(option.label)
        let query = fold(query.trimmingCharacters(in: .whitespacesAndNewlines))
        if label.hasPrefix(query) { return 0 }
        if [" ", "(", "-"].contains(where: { label.contains($0 + query) }) { return 1 }
        return 2
    }

    /// The options that match, best first when `sortsByMatch`, cut to
    /// `limit`, in groups: options without a group first, then each group in
    /// the order it first appears.
    static func results(
        _ options: [MonarchComboboxOption],
        query: String,
        filter: (@Sendable (MonarchComboboxOption, String) -> Bool)?,
        sortsByMatch: Bool,
        limit: Int?
    ) -> Results {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        var matched = options.filter { filter?($0, trimmed) ?? matches($0, query: trimmed) }
        if sortsByMatch, !trimmed.isEmpty {
            matched = matched.enumerated()
                .sorted { lhs, rhs in
                    let left = rank(lhs.element, query: trimmed)
                    let right = rank(rhs.element, query: trimmed)
                    return left == right ? lhs.offset < rhs.offset : left < right
                }
                .map(\.element)
        }
        let total = matched.count
        if let limit { matched = Array(matched.prefix(max(0, limit))) }
        var order: [String?] = []
        var grouped: [String?: [MonarchComboboxOption]] = [:]
        for option in matched {
            let key = option.group.flatMap { $0.isEmpty ? nil : $0 }
            if grouped[key] == nil { order.append(key) }
            grouped[key, default: []].append(option)
        }
        if let index = order.firstIndex(where: { $0 == nil }), index > 0 {
            order.remove(at: index)
            order.insert(nil, at: 0)
        }
        let sections = order.map { Section(title: $0, options: grouped[$0] ?? []) }
        return Results(sections: sections, shown: matched.count, total: total)
    }

    /// Whether to offer creating the typed text: there is a create handler,
    /// the text is not blank and no option has that label.
    static func showsCreate(query: String, options: [MonarchComboboxOption], canCreate: Bool) -> Bool {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard canCreate, !trimmed.isEmpty else { return false }
        return !options.contains { fold($0.label) == fold(trimmed) }
    }

    /// The text shown for the selection: the option's label, a remembered
    /// label for a value not in `options`, or the raw value.
    static func selectedLabel(
        _ selection: String?,
        options: [MonarchComboboxOption],
        remembered: [String: String]
    ) -> String? {
        guard let selection else { return nil }
        return options.first { $0.id == selection }?.label ?? remembered[selection] ?? selection
    }

    /// What VoiceOver reads as the field's value.
    static func accessibilityValue(selectedLabel: String?, labels: MonarchComboboxLabels) -> String {
        selectedLabel ?? labels.noSelection ?? MonarchComboboxStrings.noSelection
    }

    /// What VoiceOver reads for an option row: the label, the description and the hint.
    static func accessibilityLabel(_ option: MonarchComboboxOption) -> String {
        [option.label, option.description, option.hint]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }

    /// The spoken label of an option, with the label part marked as written
    /// in `labelLanguage` so VoiceOver pronounces an endonym in its own
    /// language and the rest in the app's.
    static func spokenLabel(_ option: MonarchComboboxOption) -> AttributedString {
        var label = AttributedString(option.label)
        if let language = option.labelLanguage {
            label.languageIdentifier = language.maximalIdentifier
        }
        let rest = [option.description, option.hint].compactMap { $0 }.filter { !$0.isEmpty }
        guard !rest.isEmpty else { return label }
        return label + AttributedString(", " + rest.joined(separator: ", "))
    }

    /// The text shown while the options failed to load: the app's message
    /// or the built-in one.
    static func loadFailedMessage(_ message: String, labels: MonarchComboboxLabels) -> String {
        message.isEmpty ? (labels.loadFailed ?? MonarchComboboxStrings.loadFailed) : message
    }

    /// The colour role of the field's text: the chosen label, or the prompt.
    static func valueColor(hasSelection: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.textDisabled }
        return hasSelection ? \.textDefault : \.textSubtle
    }

    /// The colour role of an option's label.
    static func optionColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour role of an option's description and hint.
    static func secondaryColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    private static func fold(_ text: String) -> String {
        text.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: nil)
    }
}
