import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchQueryBuilder``: the app's replacements, else the built-in ones.
struct MonarchQueryBuilderText {
    let labels: MonarchQueryBuilderLabels

    private func pick(_ value: String?, _ key: String) -> String { value ?? MonarchQueryBuilderStrings.string(key) }

    var label: String { pick(labels.label, "label") }
    var combinatorBefore: String { pick(labels.combinatorBefore, "combinatorBefore") }
    var and: String { pick(labels.and, "and") }
    var or: String { pick(labels.or, "or") }
    var addRule: String { pick(labels.addRule, "addRule") }
    var addGroup: String { pick(labels.addGroup, "addGroup") }
    var emptyGroup: String { pick(labels.emptyGroup, "emptyGroup") }
    var maxDepthReached: String { pick(labels.maxDepthReached, "maxDepthReached") }
    var choose: String { pick(labels.choose, "choose") }
    var chooseDate: String { pick(labels.chooseDate, "chooseDate") }

    func group(_ path: [Int]) -> String {
        if let custom = labels.group { return custom(path) }
        if path.count <= 1 { return MonarchQueryBuilderStrings.string("topGroup") }
        return MonarchQueryBuilderStrings.string("group", path.dropFirst().map(String.init).joined(separator: "."))
    }

    func combinator(_ combinator: MonarchQueryCombinator) -> String {
        combinator == .and ? and : or
    }

    func combinatorLabel(_ group: String) -> String {
        labels.combinatorLabel?(group) ?? MonarchQueryBuilderStrings.string("combinatorLabel", group)
    }

    func ruleTitle(_ position: Int) -> String {
        labels.ruleTitle?(position) ?? MonarchQueryBuilderStrings.string("ruleTitle", position)
    }

    func rule(_ group: String, _ position: Int) -> String {
        labels.rule?(group, position) ?? MonarchQueryBuilderStrings.string("rule", group, position)
    }

    func field(_ group: String, _ position: Int) -> String {
        labels.field?(group, position) ?? MonarchQueryBuilderStrings.string("field", group, position)
    }

    func operatorLabel(_ group: String, _ position: Int) -> String {
        labels.operatorLabel?(group, position) ?? MonarchQueryBuilderStrings.string("operatorLabel", group, position)
    }

    func value(_ group: String, _ position: Int) -> String {
        labels.value?(group, position) ?? MonarchQueryBuilderStrings.string("value", group, position)
    }

    func removeRule(_ group: String, _ position: Int) -> String {
        labels.removeRule?(group, position)
            ?? MonarchQueryBuilderStrings.string("removeRule", group.localizedLowercase, position)
    }

    func removeGroup(_ group: String) -> String {
        labels.removeGroup?(group) ?? MonarchQueryBuilderStrings.string("removeGroup", group.localizedLowercase)
    }

    func maxRulesReached(_ max: Int) -> String {
        labels.maxRulesReached?(max) ?? MonarchQueryBuilderStrings.string("maxRulesReached", max)
    }

    func removedField(_ key: String) -> String {
        labels.removedField?(key) ?? MonarchQueryBuilderStrings.string("removedField", key)
    }

    func operatorName(_ queryOperator: MonarchConditionOperator) -> String {
        labels.operatorName?(queryOperator) ?? MonarchQueryBuilderStrings.string("operator.\(queryOperator.rawValue)")
    }

    func problem(_ problem: MonarchConditionProblem) -> String {
        labels.problem?(problem) ?? MonarchQueryBuilderStrings.string("problem.\(problem.rawValue)")
    }
}

/// Component tokens and editing logic for ``MonarchQueryBuilder``.
enum MonarchQueryBuilderTokens {
    /// The gap between the parts of a group.
    static let groupSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of a rule.
    static let ruleSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding inside a nested group and a rule.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The gap between an error icon and its text.
    static let messageIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The symbol of the add buttons.
    static let addSymbol = "plus"
    /// The symbol of the remove buttons.
    static let removeSymbol = "xmark"
    /// The symbol before a problem.
    static let problemSymbol = "exclamationmark.circle.fill"

    /// The fill of a group at a depth (1 is the top group): none at the top, then alternating
    /// subtle and surface so nesting stays visible.
    static func groupFill(depth: Int) -> MonarchColorRole? {
        guard depth > 1 else { return nil }
        return depth.isMultiple(of: 2) ? \.bgSubtle : \.bgSurface
    }

    /// The border of a nested group or a rule: the danger role when it has a problem shown.
    static func border(hasProblem: Bool) -> MonarchColorRole {
        hasProblem ? \.dangerStrong : \.borderSubtle
    }

    /// The fill of a rule card.
    static var ruleFill: MonarchColorRole { \.bgSurface }

    /// Whether a group at a depth may hold nested groups.
    static func canAddGroup(depth: Int, maxDepth: Int) -> Bool {
        depth < maxDepth
    }

    /// Whether the "cannot be nested deeper" note shows at a depth.
    static func showsDepthNote(depth: Int, maxDepth: Int) -> Bool {
        depth >= maxDepth && maxDepth > 1
    }

    /// The rule after choosing another field: the field's first operator and an empty value.
    static func changingField(_ rule: MonarchCondition, to field: MonarchConditionField?) -> MonarchCondition {
        MonarchCondition(id: rule.id, field: field)
    }

    /// The rule after choosing another operator. The value is kept while it still fits, and reset
    /// when the operator takes none or the value was cleared.
    static func changingOperator(
        _ rule: MonarchCondition,
        to queryOperator: MonarchConditionOperator,
        field: MonarchConditionField?
    ) -> MonarchCondition {
        var next = rule
        next.operator = queryOperator
        let blank = field?.blankValue(for: queryOperator) ?? .none
        if !queryOperator.takesValue || !sameKind(rule.value, blank) { next.value = blank }
        return next
    }

    private static func sameKind(_ a: MonarchConditionValue, _ b: MonarchConditionValue) -> Bool {
        switch (a, b) {
        case (.none, .none), (.text, .text), (.number, .number), (.date, .date), (.boolean, .boolean),
            (.option, .option), (.options, .options):
            true
        default: false
        }
    }

    /// The operators a rule's menu offers: the field's, plus the rule's own if the field lacks it.
    static func operators(for rule: MonarchCondition, field: MonarchConditionField?) -> [MonarchConditionOperator] {
        let offered = field?.availableOperators ?? [.equals]
        return offered.contains(rule.operator) ? offered : [rule.operator] + offered
    }

    /// A number as it is edited, in the locale and without grouping.
    static func numberText(_ number: Double?, locale: Locale) -> String {
        guard let number else { return "" }
        return number.formatted(.number.locale(locale).grouping(.never))
    }

    /// The number typed, or `nil` when the text is empty or not a number in the locale.
    static func parseNumber(_ text: String, locale: Locale) -> Double? {
        let trimmed = text.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return nil }
        return try? Double(trimmed, format: .number.locale(locale))
    }

    /// The problem shown under a rule: only when errors are shown.
    static func shownProblem(
        _ rule: MonarchCondition,
        fields: [MonarchConditionField],
        showsErrors: Bool
    ) -> MonarchConditionProblem? {
        showsErrors ? rule.problem(fields: fields) : nil
    }
}
