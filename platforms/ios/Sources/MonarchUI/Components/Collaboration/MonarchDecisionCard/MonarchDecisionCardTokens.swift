import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchDecisionCard``.
enum MonarchDecisionCardTokens {
    /// The gap between the sections of the card.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between two choices.
    static let choiceSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the selection glyph and the text of a choice.
    static let glyphSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the label and the description of a choice.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// Padding before and after the content of a choice.
    static let choiceHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the content of a choice.
    static let choiceVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between the badge, the reason and the time in the read-only view.
    static let readOnlySpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the selected choice.
    static let selectedSymbol = "checkmark.circle.fill"
    /// The SF Symbol of a choice that is not selected.
    static let unselectedSymbol = "circle"
    /// The SF Symbol shown before an error message.
    static let errorSymbol = "exclamationmark.circle"
    /// The number of lines the reason field shows: at least and at most.
    static let reasonLines = 2...6
    /// The longest reason accepted by default.
    static let defaultReasonLimit = 500
    /// The number of characters left from which the count is shown.
    static let charactersLeftThreshold = 50

    /// What separates the parts of a spoken label. VoiceOver pauses at it.
    static let spokenSeparator = ", "

    /// The colour roles of one choice.
    struct ChoiceAppearance: Equatable {
        let background: MonarchColorRole
        let border: MonarchColorRole
        let glyph: MonarchColorRole
        let label: MonarchColorRole
        let description: MonarchColorRole
    }

    /// The roles of a choice for a state. Selection is also a glyph.
    static func choiceAppearance(isSelected: Bool, isEnabled: Bool) -> ChoiceAppearance {
        switch (isSelected, isEnabled) {
        case (true, true):
            ChoiceAppearance(
                background: \.accentSubtle,
                border: \.accentStrong,
                glyph: \.accentStrong,
                label: \.textDefault,
                description: \.textMuted
            )
        case (false, true):
            ChoiceAppearance(
                background: \.bgSurface,
                border: \.borderDefault,
                glyph: \.borderInput,
                label: \.textDefault,
                description: \.textMuted
            )
        case (true, false):
            ChoiceAppearance(
                background: \.bgMuted,
                border: \.borderSubtle,
                glyph: \.textDisabled,
                label: \.textDisabled,
                description: \.textDisabled
            )
        case (false, false):
            ChoiceAppearance(
                background: \.bgSubtle,
                border: \.borderSubtle,
                glyph: \.textDisabled,
                label: \.textDisabled,
                description: \.textDisabled
            )
        }
    }

    /// The SF Symbol of a choice.
    static func choiceSymbol(isSelected: Bool) -> String {
        isSelected ? selectedSymbol : unselectedSymbol
    }

    /// What VoiceOver reads for a choice: the label, then the description.
    static func choiceAccessibilityLabel(label: String, description: String?) -> String {
        guard let description, !description.isEmpty else { return label }
        return [label, description]
            .joined(separator: spokenSeparator)
    }

    /// A string without surrounding white space.
    static func trimmed(_ text: String) -> String {
        text.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// The option with a value, if there is one.
    static func option(_ value: String?, in options: [MonarchDecisionOption]) -> MonarchDecisionOption? {
        guard let value else { return nil }
        return options.first { $0.value == value }
    }

    /// Whether the current choice needs a reason. "No decision" never does;
    /// an option's own setting wins over the card's.
    static func needsReason(choice: String?, options: [MonarchDecisionOption], isReasonRequired: Bool) -> Bool {
        guard choice != nil else { return false }
        return option(choice, in: options)?.isReasonRequired ?? isReasonRequired
    }

    /// Whether a required reason is missing.
    static func isReasonMissing(
        choice: String?,
        text: String,
        options: [MonarchDecisionOption],
        isReasonRequired: Bool
    ) -> Bool {
        needsReason(choice: choice, options: options, isReasonRequired: isReasonRequired) && trimmed(text).isEmpty
    }

    /// Whether the form differs from the saved decision.
    static func isDirty(choice: String?, text: String, value: String?, reason: String?) -> Bool {
        choice != value || trimmed(text) != trimmed(reason ?? "")
    }

    /// The decision the form holds.
    static func decision(choice: String?, text: String) -> MonarchDecision {
        let reason = trimmed(text)
        return MonarchDecision(value: choice, reason: reason.isEmpty ? nil : reason)
    }

    /// The text cut to the limit.
    static func limited(_ text: String, to limit: Int) -> String {
        String(text.prefix(max(0, limit)))
    }

    /// The number of characters left, or `nil` while far from the limit.
    static func charactersLeft(text: String, limit: Int) -> Int? {
        let left = max(0, limit - text.count)
        return left <= charactersLeftThreshold ? left : nil
    }

    /// The text for the number of characters left.
    static func charactersLeftText(_ count: Int, labels: MonarchDecisionCardLabels) -> String {
        labels.charactersLeft?(count) ?? MonarchDecisionCardStrings.charactersLeft(count)
    }

    /// The message for a failed save: the description of an error that has
    /// one, or else the built-in text.
    static func failureMessage(for error: any Error, labels: MonarchDecisionCardLabels) -> String {
        MonarchFailureMessage.message(for: error, override: labels.failed, fallback: MonarchDecisionCardStrings.failed)
    }

    /// The line that says when and by whom the decision was made, with the
    /// time relative to `now`, or `nil` when neither is known.
    static func decidedText(
        decidedAt: Date?,
        decidedBy: String?,
        now: Date,
        locale: Locale = .current,
        labels: MonarchDecisionCardLabels
    ) -> String? {
        let name = decidedBy.flatMap { $0.isEmpty ? nil : $0 }
        guard decidedAt != nil || name != nil else { return nil }
        if let decided = labels.decided { return decided }
        let time = decidedAt.map { date in
            MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
        }
        return composed(time: time, name: name)
    }

    /// The same line with the absolute date and time, for VoiceOver.
    static func decidedAccessibilityLabel(
        decidedAt: Date?,
        decidedBy: String?,
        locale: Locale = .current,
        timeZone: TimeZone = .current,
        labels: MonarchDecisionCardLabels
    ) -> String? {
        let name = decidedBy.flatMap { $0.isEmpty ? nil : $0 }
        guard decidedAt != nil || name != nil else { return nil }
        if let decided = labels.decided { return decided }
        let time = decidedAt.map { date -> String in
            var style = Date.FormatStyle(date: .abbreviated, time: .omitted, locale: locale).monarchTime()
            style.timeZone = timeZone
            return date.formatted(style)
        }
        return composed(time: time, name: name)
    }

    private static func composed(time: String?, name: String?) -> String? {
        switch (time, name) {
        case (let time?, let name?): MonarchDecisionCardStrings.decidedAtBy(time, name)
        case (let time?, nil): MonarchDecisionCardStrings.decidedAt(time)
        case (nil, let name?): MonarchDecisionCardStrings.decidedBy(name)
        case (nil, nil): nil
        }
    }
}
