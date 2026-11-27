import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchConditionEditor``: the app's replacements, else the built-in
/// ones. Operator names and problems fall back to the shared ``MonarchQueryBuilder`` table.
struct MonarchConditionEditorText {
    let labels: MonarchConditionEditorLabels

    private func pick(_ value: String?, _ key: String) -> String {
        value ?? MonarchConditionEditorStrings.string(key)
    }

    var label: String { pick(labels.label, "label") }
    var add: String { pick(labels.add, "add") }
    var matchBefore: String { pick(labels.matchBefore, "matchBefore") }
    var matchAfter: String { pick(labels.matchAfter, "matchAfter") }
    var matchLabel: String { pick(labels.matchLabel, "matchLabel") }
    var empty: String { pick(labels.empty, "empty") }
    var choose: String { labels.choose ?? MonarchQueryBuilderStrings.string("choose") }
    var chooseDate: String { labels.chooseDate ?? MonarchQueryBuilderStrings.string("chooseDate") }

    func match(_ combinator: MonarchConditionMatch) -> String {
        combinator == .all ? pick(labels.matchAll, "matchAll") : pick(labels.matchAny, "matchAny")
    }

    func condition(_ position: Int) -> String {
        labels.condition?(position) ?? MonarchConditionEditorStrings.string("condition", position)
    }

    func maxReached(_ max: Int) -> String {
        labels.maxReached?(max) ?? MonarchConditionEditorStrings.string("maxReached", max)
    }

    func removed(_ position: Int) -> String {
        labels.removed?(position) ?? MonarchConditionEditorStrings.string("removed", position)
    }

    /// Every string one row needs, for the condition at a position (1-based).
    func row(_ position: Int) -> MonarchConditionRowText {
        let labels = labels
        return MonarchConditionRowText(
            title: condition(position),
            name: condition(position).isEmpty
                ? MonarchConditionEditorStrings.string("condition", position) : condition(position),
            field: labels.field?(position) ?? MonarchConditionEditorStrings.string("field", position),
            operatorLabel: labels.operatorLabel?(position)
                ?? MonarchConditionEditorStrings.string("operatorLabel", position),
            value: labels.value?(position) ?? MonarchConditionEditorStrings.string("value", position),
            remove: labels.remove?(position) ?? MonarchConditionEditorStrings.string("remove", position),
            choose: choose,
            chooseDate: chooseDate,
            removedField: { key in
                labels.removedField?(key) ?? MonarchQueryBuilderStrings.string("removedField", key)
            },
            operatorName: { op in
                labels.operatorName?(op) ?? MonarchQueryBuilderStrings.string("operator.\(op.rawValue)")
            },
            problem: { problem in
                labels.problem?(problem) ?? MonarchQueryBuilderStrings.string("problem.\(problem.rawValue)")
            }
        )
    }
}

/// Component tokens and editing logic for ``MonarchConditionEditor``.
enum MonarchConditionEditorTokens {
    /// The gap between the switch, the rows and the add button.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the words and the switch of the match line.
    static let matchSpacing: CGFloat = MonarchTokens.Space.space2
    /// The symbol of the add button.
    static let addSymbol = "plus"
    /// The role of the empty text and the notes.
    static var muted: MonarchColorRole { \.textMuted }

    /// The field a new condition starts on: `defaultField` when it exists, else the first field.
    static func startField(_ fields: [MonarchConditionField], defaultField: String?) -> MonarchConditionField? {
        if let defaultField, let field = fields.first(where: { $0.id == defaultField }) { return field }
        return fields.first
    }

    /// Whether the all or any switch shows: only when the app passes a binding and there are
    /// at least two conditions to combine.
    static func showsMatch(hasBinding: Bool, count: Int) -> Bool {
        hasBinding && count > 1
    }

    /// Whether the add button shows. At the limit a note replaces it.
    static func canAdd(count: Int, maxConditions: Int) -> Bool {
        count < maxConditions
    }

    /// The list with a new condition appended on the start field.
    static func adding(
        to conditions: [MonarchCondition],
        id: String,
        fields: [MonarchConditionField],
        defaultField: String?
    ) -> [MonarchCondition] {
        conditions + [MonarchCondition(id: id, field: startField(fields, defaultField: defaultField))]
    }

    /// The problem of every incomplete condition, by condition id. Empty when all are complete.
    static func problems(
        _ conditions: [MonarchCondition],
        fields: [MonarchConditionField]
    ) -> [String: MonarchConditionProblem] {
        var out: [String: MonarchConditionProblem] = [:]
        for condition in conditions {
            if let problem = condition.problem(fields: fields) { out[condition.id] = problem }
        }
        return out
    }
}
