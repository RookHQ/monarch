import Foundation
import MonarchTokens
import SwiftUI

/// The rules a ``MonarchEditableCell`` checks a new value against.
struct MonarchEditableCellRules {
    var kind: MonarchEditableCellKind
    var isRequired = false
    var minimum: Double?
    var maximum: Double?
    var minimumDate: Date?
    var maximumDate: Date?
    var calendar: Calendar
    var locale: Locale
}

/// What checking a new value decided.
enum MonarchEditableCellCheck: Equatable {
    /// The value equals the saved one: close without saving.
    case unchanged
    /// The value is refused with this message.
    case invalid(String)
    /// The value may be saved.
    case valid(MonarchEditableCellValue)
}

/// Component tokens and pure rules for ``MonarchEditableCell``.
enum MonarchEditableCellTokens {
    /// Padding before and after the text of the value and of the field.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below the text of the field.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the field and its trailing buttons.
    static let accessorySpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the editor and the error.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1
    /// The least height of the cell: the touch target.
    static let minHeight: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The SF Symbol of the button that saves the editor.
    static let saveSymbol = "checkmark.circle.fill"
    /// The SF Symbol of the button that leaves the editor.
    static let cancelSymbol = "xmark.circle.fill"

    /// The fill of the field.
    static var fieldBackground: MonarchColorRole { \.bgSurface }
    /// The colour of the cancel glyph.
    static var cancelGlyph: MonarchColorRole { \.textMuted }
    /// The colour of the save glyph.
    static var saveGlyph: MonarchColorRole { \.accentStrong }

    /// The colour of the value in view mode: the value, the placeholder of an
    /// empty cell, or either while disabled.
    static func viewForeground(isEmpty: Bool, isEnabled: Bool) -> MonarchColorRole {
        if !isEnabled { return \.textDisabled }
        return isEmpty ? \.textSubtle : \.textDefault
    }

    /// The border of the field.
    static func border(hasError: Bool) -> MonarchColorRole {
        hasError ? \.dangerStrong : \.borderInput
    }

    /// Whether the cell is right-aligned with tabular figures: the override, or number cells.
    static func isNumeric(_ override: Bool?, kind: MonarchEditableCellKind) -> Bool {
        override ?? (kind == .number)
    }

    // MARK: Text

    /// The draft the editor starts from: the value as text, a number without
    /// grouping so it parses back.
    static func draft(from value: MonarchEditableCellValue, locale: Locale) -> String {
        switch value {
        case .empty, .date: ""
        case .text(let text): text
        case .number(let number): number.formatted(.number.grouping(.never).locale(locale))
        }
    }

    /// Turns a typed draft into a value of the cell's kind, or the message of an
    /// unreadable number. Leading and trailing spaces are dropped; an empty draft is `.empty`.
    static func parse(
        _ draft: String,
        kind: MonarchEditableCellKind,
        locale: Locale,
        invalidNumber: String
    ) -> Result<MonarchEditableCellValue, MonarchEditableCellParseError> {
        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return .success(.empty) }
        guard kind == .number else { return .success(.text(text)) }
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .decimal
        formatter.isLenient = false
        let compact = text.filter { !$0.isWhitespace }
        if let number = formatter.number(from: compact)?.doubleValue, number.isFinite {
            return .success(.number(number))
        }
        return .failure(MonarchEditableCellParseError(message: invalidNumber))
    }

    // MARK: Checking

    /// Checks a new value, in the web's order: required, unchanged, minimum,
    /// maximum, then the app's own rule.
    @MainActor
    static func check(
        _ next: MonarchEditableCellValue,
        saved: MonarchEditableCellValue,
        rules: MonarchEditableCellRules,
        labels: MonarchEditableCellLabels,
        validate: (@MainActor (MonarchEditableCellValue) -> String?)?
    ) -> MonarchEditableCellCheck {
        if rules.isRequired, next.isEmpty {
            return .invalid(labels.required ?? MonarchEditableCellStrings.required)
        }
        if isSame(next, saved, calendar: rules.calendar) { return .unchanged }
        switch next {
        case .number(let number):
            if let minimum = rules.minimum, number < minimum {
                return .invalid(belowMinimum(formatNumber(minimum, locale: rules.locale), labels: labels))
            }
            if let maximum = rules.maximum, number > maximum {
                return .invalid(aboveMaximum(formatNumber(maximum, locale: rules.locale), labels: labels))
            }
        case .date(let date):
            let day = rules.calendar.startOfDay(for: date)
            if let minimum = rules.minimumDate, day < rules.calendar.startOfDay(for: minimum) {
                let text = formatDate(minimum, calendar: rules.calendar, locale: rules.locale)
                return .invalid(belowMinimum(text, labels: labels))
            }
            if let maximum = rules.maximumDate, day > rules.calendar.startOfDay(for: maximum) {
                let text = formatDate(maximum, calendar: rules.calendar, locale: rules.locale)
                return .invalid(aboveMaximum(text, labels: labels))
            }
        case .empty, .text:
            break
        }
        if let refusal = validate?(next), !refusal.isEmpty { return .invalid(refusal) }
        return .valid(next)
    }

    /// Whether two values are the same: both empty, or equal (dates by day).
    static func isSame(_ lhs: MonarchEditableCellValue, _ rhs: MonarchEditableCellValue, calendar: Calendar) -> Bool {
        if lhs.isEmpty, rhs.isEmpty { return true }
        if case .date(let a) = lhs, case .date(let b) = rhs { return calendar.isDate(a, inSameDayAs: b) }
        return lhs == rhs
    }

    private static func belowMinimum(_ text: String, labels: MonarchEditableCellLabels) -> String {
        labels.belowMinimum?(text) ?? MonarchEditableCellStrings.belowMinimum(text)
    }

    private static func aboveMaximum(_ text: String, labels: MonarchEditableCellLabels) -> String {
        labels.aboveMaximum?(text) ?? MonarchEditableCellStrings.aboveMaximum(text)
    }

    // MARK: Display

    /// A number in the locale.
    static func formatNumber(_ number: Double, locale: Locale) -> String {
        number.formatted(.number.locale(locale))
    }

    /// A day in the locale's abbreviated date format.
    static func formatDate(_ date: Date, calendar: Calendar, locale: Locale) -> String {
        date.formatted(
            Date.FormatStyle(
                date: .abbreviated,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: calendar.timeZone
            )
        )
    }

    /// The text of the value in view mode: the app's format, the option label,
    /// the number or day in the locale, or the text. `nil` when empty.
    @MainActor
    static func display(
        _ value: MonarchEditableCellValue,
        options: [MonarchEditableCellOption],
        calendar: Calendar,
        locale: Locale,
        format: (@MainActor (MonarchEditableCellValue) -> String)?
    ) -> String? {
        if let format {
            let text = format(value)
            return text.isEmpty ? nil : text
        }
        switch value {
        case .empty: return nil
        case .text(let text):
            if text.isEmpty { return nil }
            return options.first { $0.value == text }?.label ?? text
        case .number(let number): return formatNumber(number, locale: locale)
        case .date(let date): return formatDate(date, calendar: calendar, locale: locale)
        }
    }

    /// The text drawn in view mode: the value, or the placeholder of an empty cell.
    static func shownText(display: String?, placeholder: String?) -> String {
        display ?? placeholder ?? ""
    }

    /// What VoiceOver reads as the value: "Saving" while a save runs, otherwise the shown text.
    static func accessibilityValue(shown: String, isSaving: Bool, labels: MonarchEditableCellLabels) -> String {
        isSaving ? (labels.saving ?? MonarchEditableCellStrings.saving) : shown
    }

    /// What VoiceOver reads as the hint of the cell in view mode.
    static func editHint(_ labels: MonarchEditableCellLabels) -> String {
        labels.edit ?? MonarchEditableCellStrings.edit
    }

    /// What VoiceOver reads as the hint of the editor: the error, spoken as "Error: …".
    static func errorHint(_ error: String?, labels: MonarchEditableCellLabels) -> String {
        guard let error, !error.isEmpty else { return "" }
        return labels.error?(error) ?? MonarchEditableCellStrings.error(error)
    }

    /// The message of a failed save: the error's own description, the
    /// override, or the built-in sentence.
    static func failureMessage(for error: any Error, labels: MonarchEditableCellLabels) -> String {
        MonarchFailureMessage.message(
            for: error,
            override: labels.saveFailed,
            fallback: MonarchEditableCellStrings.saveFailed
        )
    }

    /// The choices of a select cell: "None" first unless the cell is required
    /// (or is required but still empty, so the picker can show its value).
    static func showsNoValueChoice(isRequired: Bool, value: MonarchEditableCellValue) -> Bool {
        !isRequired || value.isEmpty
    }

    /// The day a date editor starts on for an empty cell: today, moved into the allowed range.
    static func initialDay(today: Date, minimumDate: Date?, maximumDate: Date?, calendar: Calendar) -> Date {
        var day = calendar.startOfDay(for: today)
        if let minimumDate { day = max(day, calendar.startOfDay(for: minimumDate)) }
        if let maximumDate { day = min(day, calendar.startOfDay(for: maximumDate)) }
        return day
    }
}

/// A draft that could not be read as a value of the cell's kind.
struct MonarchEditableCellParseError: Error, Equatable {
    /// The message to show.
    let message: String
}
