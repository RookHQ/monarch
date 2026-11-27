import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and the pure logic of ``MonarchFieldRenderer``.
enum MonarchFieldRendererTokens {
    /// The gap between a question and the note under it.
    static let gap: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between options, between files and between a control and its buttons.
    static let controlGap: CGFloat = MonarchTokens.Space.space2
    /// The gap between an icon and its text.
    static let iconGap: CGFloat = MonarchTokens.Space.space1
    /// The padding before and after the text of a choice row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space3
    /// The least height of a choice row and of a file row: the touch target.
    static let rowMinHeight: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The default confidence under which the note asks for a check, as on the web.
    static let defaultConfidenceThreshold: Double = 0.6
    /// The default scale of a rating, as on the web.
    static let defaultRatingRange: ClosedRange<Int> = 1...5
    /// The placeholder of a web address question without one of its own, as on the web.
    static let urlPlaceholder = "https://"
    /// The SF Symbol of the automatic-fill note.
    static let aiSymbol = "cpu"
    /// The SF Symbol of the chosen option of a radio question.
    static let selectedSymbol = "checkmark"
    /// The SF Symbol of the button that sets a date.
    static let dateSymbol = "calendar"
    /// The SF Symbol of the button that sets a time.
    static let timeSymbol = "clock"
    /// The SF Symbol of a chosen file.
    static let fileSymbol = "doc"

    private static let minutesPerHour = 60
    private static let hoursPerDay = 24

    // MARK: Colours

    /// The colour role of the automatic-fill note: the AI text, or the warning text when unsure.
    static func aiNoteColor(isUnsure: Bool) -> MonarchColorRole {
        isUnsure ? \.warningStrong : \.aiStrong
    }

    /// The colour role of a section's description, help text and file sizes.
    static func noteColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The colour role of the text of a question's own parts: a section heading, a choice, a file name.
    static func textColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour role of the check mark of the chosen option.
    static func selectedColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.accentStrong : \.textDisabled
    }

    // MARK: State

    /// The note under an answer filled in automatically.
    enum AiNote: Equatable {
        /// Confident enough: the answer was filled in and should be checked.
        case filled
        /// Below the threshold: the answer needs a check.
        case check
    }

    /// The note to show for a confidence, or `nil` when the answer was not filled in automatically.
    static func aiNote(confidence: Double?, threshold: Double) -> AiNote? {
        guard let confidence else { return nil }
        return confidence < threshold ? .check : .filled
    }

    /// The error to show: a non-empty `error` wins over the message of `problem`.
    static func resolvedError(
        error: String?,
        problem: MonarchFormFieldProblem?,
        text: MonarchFieldRendererText
    ) -> String? {
        if let error, !error.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return error }
        return problem.map(text.message)
    }

    /// Whether a read-only question keeps a readable input instead of being disabled: text and numbers, as
    /// on the web.
    static func keepsReadOnlyInput(_ type: MonarchFormFieldType) -> Bool {
        switch type {
        case .shortText, .longText, .email, .phone, .url, .number: true
        default: false
        }
    }

    /// Whether the question's input is disabled: the renderer is disabled, or the question is read-only and
    /// has no readable read-only form.
    static func isDisabled(_ field: MonarchFormFieldDefinition, isEnabled: Bool) -> Bool {
        !isEnabled || (field.isReadOnly && !keepsReadOnlyInput(field.type))
    }

    /// The placeholder of a text question.
    static func prompt(_ field: MonarchFormFieldDefinition) -> String? {
        field.placeholder ?? (field.type == .url ? urlPlaceholder : nil)
    }

    /// Typed text, cut at the question's most characters.
    static func typed(_ text: String, maxLength: Int?) -> String {
        guard let maxLength, maxLength >= 0, text.count > maxLength else { return text }
        return String(text.prefix(maxLength))
    }

    /// The scale of a rating question: its `min` to `max`, or 1 to 5. A reversed scale falls back to 1 to 5.
    static func ratingRange(_ field: MonarchFormFieldDefinition) -> ClosedRange<Int> {
        let whole = { (limit: Double?) in limit.flatMap { $0.isFinite ? Int($0) : nil } }
        let lower = whole(field.min) ?? defaultRatingRange.lowerBound
        let upper = whole(field.max) ?? defaultRatingRange.upperBound
        return lower <= upper ? lower...upper : defaultRatingRange
    }

    /// Option values in the order of the options; values no option has are dropped.
    static func ordered(_ chosen: Set<String>, options: [MonarchFormFieldOption]) -> [String] {
        options.map(\.value).filter(chosen.contains)
    }

    /// The chosen option of a select or radio question, or `nil` when the answer is none or no option's value.
    static func chosenOption(_ value: MonarchFormFieldValue?, options: [MonarchFormFieldOption]) -> String? {
        guard case .text(let text)? = value, options.contains(where: { $0.value == text }) else { return nil }
        return text
    }

    // MARK: Dates and times

    /// The day a date question starts on when people tap "Choose a date": today, kept inside the limits.
    static func initialDate(today: Date, minDate: Date?, maxDate: Date?) -> Date {
        if let minDate, today < minDate { return minDate }
        if let maxDate, today > maxDate { return maxDate }
        return today
    }

    /// Minutes from midnight of an `HH:MM` answer, or `nil` when it is not one.
    static func minutes(of text: String?) -> Int? {
        guard let text else { return nil }
        let parts = text.trimmingCharacters(in: .whitespaces).split(separator: ":", omittingEmptySubsequences: false)
        guard parts.count == 2, (1...2).contains(parts[0].count), parts[1].count == 2,
            parts.allSatisfy({ $0.allSatisfy(\.isASCII) }),
            let hours = Int(parts[0]), let minutes = Int(parts[1]),
            (0..<hoursPerDay).contains(hours), (0..<minutesPerHour).contains(minutes)
        else { return nil }
        return hours * minutesPerHour + minutes
    }

    /// A time answer, `HH:MM` on a 24-hour clock.
    static func timeAnswer(hour: Int, minute: Int) -> String {
        let two = { (n: Int) in n < 10 ? "0\(n)" : "\(n)" }
        return "\(two(hour)):\(two(minute))"
    }

    /// The time answer of a date the time picker gives, read in `calendar`.
    static func timeAnswer(from date: Date, calendar: Calendar) -> String {
        let parts = calendar.dateComponents([.hour, .minute], from: date)
        return timeAnswer(hour: parts.hour ?? 0, minute: parts.minute ?? 0)
    }

    /// The date the time picker shows for an answer: that time on `day` in `calendar`, or `nil` when the
    /// answer is not `HH:MM`.
    static func timeDate(_ text: String?, on day: Date, calendar: Calendar) -> Date? {
        guard let minutes = minutes(of: text) else { return nil }
        return calendar.date(
            bySettingHour: minutes / minutesPerHour,
            minute: minutes % minutesPerHour,
            second: 0,
            of: day
        )
    }

    /// The width of a number input, as the web's short number field.
    static let numberWidth: CGFloat = 192

    /// The format of a date in a locale as a placeholder, such as "DD/MM/YYYY" in British English.
    static func datePlaceholder(locale: Locale) -> String {
        let pattern = DateFormatter.dateFormat(fromTemplate: "ddMMyyyy", options: 0, locale: locale) ?? "dd/MM/yyyy"
        var result = ""
        var run = ""
        func flush() {
            switch run.first {
            case "d": result += "DD"
            case "M": result += "MM"
            case "y": result += "YYYY"
            case .some: result += run
            case nil: break
            }
            run = ""
        }
        for character in pattern {
            if let last = run.last, last != character { flush() }
            if "dMy".contains(character) {
                run.append(character)
            } else {
                flush()
                result.append(character)
            }
        }
        flush()
        return result
    }

    // MARK: Validation

    /// The checks of ``MonarchFormFieldDefinition/problem(for:)``, in the web's order.
    static func problem(_ field: MonarchFormFieldDefinition, _ value: MonarchFormFieldValue?)
        -> MonarchFormFieldProblem?
    {
        switch field.type {
        case .section: return nil
        case .consent: return field.isRequired && value != .flag(true) ? .mustAgree : nil
        default: break
        }
        var unansweredYesNo = field.type == .yesNo
        if case .flag? = value { unansweredYesNo = false }
        guard let value, !value.isEmpty, !unansweredYesNo else {
            if case .number(let number)? = value, number.isNaN { return .notANumber }
            return field.isRequired ? .required : nil
        }
        switch value {
        case .text(let text): return textProblem(field, text.trimmingCharacters(in: .whitespacesAndNewlines))
        case .number(let number): return numberProblem(field, number)
        case .values(let values): return selectionProblem(field, count: values.count)
        default: return nil
        }
    }

    private static func textProblem(_ field: MonarchFormFieldDefinition, _ text: String) -> MonarchFormFieldProblem? {
        if field.type == .email && !isEmailAddress(text) { return .email }
        if field.type == .url && !isWebAddress(text) { return .url }
        if let min = field.minLength, text.count < min { return .minLength(min) }
        if let max = field.maxLength, text.count > max { return .maxLength(max) }
        return nil
    }

    private static func numberProblem(_ field: MonarchFormFieldDefinition, _ number: Double) -> MonarchFormFieldProblem?
    {
        guard field.type == .number else { return nil }
        if let min = field.min, number < min { return .min(min) }
        if let max = field.max, number > max { return .max(max) }
        return nil
    }

    private static func selectionProblem(_ field: MonarchFormFieldDefinition, count: Int) -> MonarchFormFieldProblem? {
        guard field.type == .checkboxes || field.type == .multiSelect else { return nil }
        if let min = field.minSelections, count < min { return .minSelections(min) }
        if let max = field.maxSelections, count > max { return .maxSelections(max) }
        return nil
    }

    /// Whether text is an email address by the web's pattern: something, `@`, something, a dot, something,
    /// with no spaces and one `@`.
    static func isEmailAddress(_ text: String) -> Bool {
        text.wholeMatch(of: #/[^\s@]+@[^\s@]+\.[^\s@]+/#) != nil
    }

    /// Whether text is an absolute `http` or `https` address with a host.
    static func isWebAddress(_ text: String) -> Bool {
        guard let components = URLComponents(string: text),
            let scheme = components.scheme?.lowercased(), scheme == "http" || scheme == "https",
            let host = components.host, !host.isEmpty
        else { return false }
        return true
    }

    // MARK: Conditions

    /// The checks of ``MonarchFormFieldDefinition/isVisible(in:calendar:)``.
    static func isVisible(
        _ field: MonarchFormFieldDefinition,
        values: [String: MonarchFormFieldValue],
        calendar: Calendar
    ) -> Bool {
        guard !field.conditions.isEmpty else { return true }
        let results = field.conditions.map { matches($0, answer: values[$0.field], calendar: calendar) }
        return field.conditionMatch == .all ? results.allSatisfy { $0 } : results.contains(true)
    }

    /// Whether an answer passes one condition. A list answer equals or contains a value when one of its
    /// items is that value, as on the web.
    static func matches(_ rule: MonarchCondition, answer: MonarchFormFieldValue?, calendar: Calendar) -> Bool {
        let list: [String]
        switch answer {
        case .values(let values)?: list = values
        case .files(let files)?: list = files.map(\.name)
        default: return rule.matches(queryValue(answer), calendar: calendar)
        }
        var equals = rule
        equals.operator = .equals
        let has = list.contains { equals.matches(.text($0), calendar: calendar) }
        switch rule.operator {
        case .isEmpty: return list.isEmpty
        case .isNotEmpty: return !list.isEmpty
        case .equals, .contains: return has
        case .notEquals, .notContains: return !has
        case .anyOf, .noneOf:
            guard case .options(let wanted) = rule.value else { return rule.operator == .noneOf }
            let found = list.contains { wanted.contains($0) }
            return rule.operator == .anyOf ? found : !found
        default: return rule.matches(.none, calendar: calendar)
        }
    }

    /// An answer as the value a ``MonarchCondition`` tests. Lists are tested by ``matches(_:answer:calendar:)``.
    static func queryValue(_ answer: MonarchFormFieldValue?) -> MonarchConditionValue {
        switch answer {
        case nil: .none
        case .text(let text)?: .text(text)
        case .number(let number)?: .number(number.isNaN ? nil : number)
        case .date(let date)?: .date(date)
        case .flag(let flag)?: .boolean(flag)
        case .values(let values)?: .text(values.joined(separator: ", "))
        case .files(let files)?: .text(files.map(\.name).joined(separator: ", "))
        }
    }
}
