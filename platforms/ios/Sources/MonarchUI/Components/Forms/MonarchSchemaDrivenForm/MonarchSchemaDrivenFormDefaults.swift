import Foundation

/// The rules of a ``MonarchSchemaDrivenForm``, for apps that validate or submit the same answers elsewhere,
/// such as from their own navigation bar button or after the server replies.
///
/// A ``MonarchFormFieldType/section`` question starts a section that lasts until the next one: when the
/// section's conditions do not hold, its questions are hidden with it. Hidden questions are neither
/// validated nor submitted; their answers stay in the app's values.
///
/// ```swift
/// let problems = MonarchSchemaDrivenFormDefaults.problems(form, values: answers)
/// if problems.isEmpty {
///     send(MonarchSchemaDrivenFormDefaults.submittedValues(form, values: answers))
/// }
/// ```
public enum MonarchSchemaDrivenFormDefaults {
    /// The questions and section headings shown for these answers, in schema order.
    ///
    /// - Parameters:
    ///   - fields: The questions and section headings, in order.
    ///   - values: Every answer, keyed by question id.
    ///   - calendar: The calendar that decides what a day is, for date conditions.
    /// - Returns: The shown questions and headings.
    public static func visibleFields(
        _ fields: [MonarchFormFieldDefinition],
        values: [String: MonarchFormFieldValue],
        calendar: Calendar = .autoupdatingCurrent
    ) -> [MonarchFormFieldDefinition] {
        var isSectionShown = true
        return fields.filter { field in
            if field.type == .section {
                isSectionShown = field.isVisible(in: values, calendar: calendar)
                return isSectionShown
            }
            return isSectionShown && field.isVisible(in: values, calendar: calendar)
        }
    }

    /// The problem of every shown question, keyed by question id, as
    /// ``MonarchFormFieldDefinition/problem(for:)`` finds it. Empty when the answers can be submitted.
    ///
    /// - Parameters:
    ///   - fields: The questions and section headings, in order.
    ///   - values: Every answer, keyed by question id.
    ///   - calendar: The calendar that decides what a day is, for date conditions.
    /// - Returns: The problems of the shown questions.
    public static func problems(
        _ fields: [MonarchFormFieldDefinition],
        values: [String: MonarchFormFieldValue],
        calendar: Calendar = .autoupdatingCurrent
    ) -> [String: MonarchFormFieldProblem] {
        var problems: [String: MonarchFormFieldProblem] = [:]
        for field in visibleFields(fields, values: values, calendar: calendar) where field.type != .section {
            if let problem = field.problem(for: values[field.id]) {
                problems[field.id] = problem
            }
        }
        return problems
    }

    /// The answers to the shown questions, keyed by question id. Unanswered and hidden questions have no key.
    ///
    /// - Parameters:
    ///   - fields: The questions and section headings, in order.
    ///   - values: Every answer, keyed by question id.
    ///   - calendar: The calendar that decides what a day is, for date conditions.
    /// - Returns: The answers to submit.
    public static func submittedValues(
        _ fields: [MonarchFormFieldDefinition],
        values: [String: MonarchFormFieldValue],
        calendar: Calendar = .autoupdatingCurrent
    ) -> [String: MonarchFormFieldValue] {
        var answers: [String: MonarchFormFieldValue] = [:]
        for field in visibleFields(fields, values: values, calendar: calendar) where field.type != .section {
            if let value = values[field.id] {
                answers[field.id] = value
            }
        }
        return answers
    }
}
