import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchSchemaDrivenForm``.
enum MonarchSchemaDrivenFormTokens {
    /// The space between a section heading and its help.
    static let headerGap: CGFloat = MonarchTokens.Space.space1
    /// The space between the failure banner and the submit button.
    static let actionGap: CGFloat = MonarchTokens.Space.space3
    /// The gap between the sections of the form, which a line separates.
    static let sectionGap: CGFloat = MonarchTokens.Space.space6
    /// The gap between a section's heading and its questions, and between the questions.
    static let questionGap: CGFloat = MonarchTokens.Space.space4
    /// The padding at the sides of the form.
    static let pagePadding: CGFloat = MonarchTokens.Space.space4
    /// The visible value of an unanswered question in the read-only view; VoiceOver reads the empty label.
    static let emptyValue = "\u{2014}"

    /// The background behind the sections.
    static var canvas: MonarchColorRole { \.bgCanvas }
    /// The background of a row.
    static var row: MonarchColorRole { \.bgSurface }
    /// Section headings and values.
    static var text: MonarchColorRole { \.textDefault }
    /// Section help and empty values.
    static var muted: MonarchColorRole { \.textMuted }

    /// One `Section` of the form: an optional heading question and the questions under it.
    struct Group: Equatable, Identifiable {
        /// The id of the heading question, or a fixed id for the questions before the first heading.
        let id: String
        /// The section heading, or `nil` for the questions before the first heading.
        let heading: MonarchFormFieldDefinition?
        /// The questions of the section, in order.
        let questions: [MonarchFormFieldDefinition]
    }

    /// The id of the group of questions that come before any heading.
    static let leadingGroupID = "\u{0}leading"

    /// The shown questions split into sections at each section heading. Questions before the first heading
    /// form a group without one; a heading without questions still forms a group.
    static func groups(_ shown: [MonarchFormFieldDefinition]) -> [Group] {
        var groups: [Group] = []
        var heading: MonarchFormFieldDefinition?
        var questions: [MonarchFormFieldDefinition] = []
        func close() {
            if heading != nil || !questions.isEmpty {
                groups.append(Group(id: heading?.id ?? leadingGroupID, heading: heading, questions: questions))
            }
        }
        for field in shown {
            if field.type == .section {
                close()
                heading = field
                questions = []
            } else {
                questions.append(field)
            }
        }
        close()
        return groups
    }

    /// The questions typed into, whose errors first show when the person leaves them.
    static func isTyped(_ type: MonarchFormFieldType) -> Bool {
        switch type {
        case .shortText, .longText, .email, .phone, .url, .number: true
        default: false
        }
    }

    /// The visited questions after a question changed: a choice counts as visited when it is answered; a
    /// typed question when the person moves on, which is when another question changes.
    static func visited(
        _ visited: Set<String>,
        afterEditing field: MonarchFormFieldDefinition,
        previous: String?
    ) -> Set<String> {
        var result = visited
        if !isTyped(field.type) { result.insert(field.id) }
        if let previous, previous != field.id { result.insert(previous) }
        return result
    }

    /// The visited questions after keyboard focus moved: the question that lost focus counts as visited.
    static func visited(_ visited: Set<String>, focusFrom old: String?, to new: String?) -> Set<String> {
        guard let old, old != new else { return visited }
        return visited.union([old])
    }

    /// Whether a question's own problem is shown: always after a submit attempt, and with
    /// ``MonarchSchemaDrivenFormValidateOn/blur`` once the question was visited.
    static func showsProblem(
        validateOn: MonarchSchemaDrivenFormValidateOn,
        isSubmitted: Bool,
        isVisited: Bool
    ) -> Bool {
        isSubmitted || (validateOn == .blur && isVisited)
    }

    /// The first shown question with a problem or an app error, which a failed submit scrolls to.
    static func firstInvalid(
        _ shown: [MonarchFormFieldDefinition],
        problems: [String: MonarchFormFieldProblem],
        errors: [String: String] = [:]
    ) -> String? {
        shown.first { problems[$0.id] != nil || errors[$0.id] != nil }?.id
    }

    /// Whether the navigation bar Save button can be used.
    static func canSave(isDirty: Bool, isEnabled: Bool, isPending: Bool) -> Bool {
        isDirty && isEnabled && !isPending
    }

    /// The message of a failed submit: the error's description, or the fallback.
    static func failureText(_ error: any Error, fallback: String) -> String {
        if let localized = error as? any LocalizedError, let text = localized.errorDescription,
            !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        {
            return text
        }
        return fallback
    }

    /// An answer as text for the read-only view: option labels for choices, yes or no for flags, a medium
    /// date, a locale number, file names, and `nil` for no answer.
    static func display(
        _ field: MonarchFormFieldDefinition,
        _ value: MonarchFormFieldValue?,
        locale: Locale,
        calendar: Calendar,
        timeZone: TimeZone,
        yes: String,
        no: String
    ) -> String? {
        guard let value, !value.isEmpty else { return nil }
        func label(_ option: String) -> String {
            field.options.first { $0.value == option }?.label ?? option
        }
        switch value {
        case .text(let text):
            return label(text)
        case .number(let number):
            return number.formatted(.number.locale(locale))
        case .date(let date):
            let style = Date.FormatStyle(
                date: .abbreviated,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: timeZone
            )
            return date.formatted(style)
        case .flag(let flag):
            return flag ? yes : no
        case .values(let values):
            return list(values.map(label), locale: locale)
        case .files(let files):
            return list(files.map(\.name), locale: locale)
        }
    }

    /// Items joined as a list in the locale's way, such as "Remote and Office".
    static func list(_ items: [String], locale: Locale) -> String {
        let formatter = ListFormatter()
        formatter.locale = locale
        return formatter.string(from: items) ?? items.joined(separator: ", ")
    }
}
