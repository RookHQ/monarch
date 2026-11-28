import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let queryFields = [
    MonarchConditionField(
        id: "status",
        label: "Status",
        kind: .select([
            MonarchComboboxOption(id: "open", label: "Open"),
            MonarchComboboxOption(id: "closed", label: "Closed"),
        ])
    ),
    MonarchConditionField(id: "title", label: "Title", kind: .text),
    MonarchConditionField(id: "amount", label: "Amount", kind: .number, prompt: "Amount"),
    MonarchConditionField(id: "created", label: "Created", kind: .date),
    MonarchConditionField(id: "flagged", label: "Flagged", kind: .boolean),
]

/// 2026-03-04 at noon, UTC.
private let fixedDate = Date(timeIntervalSince1970: 1_772_625_600)

private let nestedQuery = MonarchQueryGroup(
    id: "root",
    rules: [MonarchCondition(id: "r1", field: "status", operator: .equals, value: .option("open"))],
    groups: [
        MonarchQueryGroup(
            id: "g1",
            combinator: .or,
            rules: [
                MonarchCondition(id: "r2", field: "amount", operator: .atLeast, value: .number(500)),
                MonarchCondition(id: "r3", field: "flagged", operator: .isTrue),
            ],
            groups: [
                MonarchQueryGroup(
                    id: "g2",
                    rules: [MonarchCondition(id: "r4", field: "created", operator: .before, value: .date(fixedDate))]
                )
            ]
        )
    ]
)

enum MonarchQueryBuilderFixtures: ComponentFixtures {
    static let id = "query-builder"
    static let fixtures = [
        Fixture("nested to the deepest level") {
            MonarchQueryBuilder(query: .constant(nestedQuery), fields: queryFields)
        },
        Fixture("every value input") {
            MonarchQueryBuilder(
                query: .constant(
                    MonarchQueryGroup(
                        id: "all",
                        combinator: .or,
                        rules: [
                            MonarchCondition(id: "a", field: "title", operator: .contains, value: .text("report")),
                            MonarchCondition(id: "b", field: "amount", operator: .greaterThan, value: .number(1_250.5)),
                            MonarchCondition(id: "c", field: "created", operator: .after, value: .date(fixedDate)),
                            MonarchCondition(id: "d", field: "created", operator: .equals, value: .date(nil)),
                            MonarchCondition(id: "e", field: "status", operator: .notEquals, value: .option(nil)),
                        ]
                    )
                ),
                fields: queryFields,
                maxDepth: 1
            )
        },
        Fixture("errors shown, removed field and option") {
            MonarchQueryBuilder(
                query: .constant(
                    MonarchQueryGroup(
                        id: "v",
                        rules: [
                            MonarchCondition(id: "x", field: nil),
                            MonarchCondition(id: "y", field: "gone", operator: .equals, value: .text("a")),
                            MonarchCondition(id: "z", field: "status", operator: .equals, value: .option("archived")),
                        ]
                    )
                ),
                fields: queryFields,
                showsErrors: true
            )
        },
        Fixture("empty, full, no fields, disabled") {
            VStack(alignment: .leading) {
                MonarchQueryBuilder(query: .constant(MonarchQueryGroup(id: "empty")), fields: queryFields)
                MonarchQueryBuilder(
                    query: .constant(
                        MonarchQueryGroup(id: "full", rules: [MonarchCondition(id: "f", field: queryFields[1])])
                    ),
                    fields: queryFields,
                    maxRulesPerGroup: 1
                )
                MonarchQueryBuilder(query: .constant(MonarchQueryGroup(id: "none")), fields: [])
                MonarchQueryBuilder(query: .constant(nestedQuery), fields: queryFields).disabled(true)
            }
        },
    ]
}

@Suite("MonarchQueryBuilder")
struct MonarchQueryBuilderTests {
    private let text = MonarchQueryBuilderText(labels: MonarchQueryBuilderLabels())
    private let english = Locale(identifier: "en_US")
    private var utc: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        return calendar
    }

    @Test("Operators follow the field kind, or the field's own list")
    func operators() {
        #expect(queryFields[4].availableOperators == [.isTrue, .isFalse])
        #expect(queryFields[3].availableOperators == [.equals, .before, .after, .isEmpty, .isNotEmpty])
        #expect(queryFields[0].availableOperators == [.equals, .notEquals, .anyOf, .noneOf, .isEmpty, .isNotEmpty])
        #expect(queryFields[2].availableOperators.contains(.atLeast))
        let narrowed = MonarchConditionField(id: "t", label: "T", kind: .text, operators: [.contains])
        #expect(narrowed.availableOperators == [.contains])
        #expect(!MonarchConditionOperator.isEmpty.takesValue)
        #expect(MonarchConditionOperator.contains.takesValue)
    }

    @Test("A new rule takes the field's first operator and an empty value of its kind")
    func newRule() {
        #expect(MonarchCondition(id: "1", field: queryFields[2]).value == .number(nil))
        #expect(MonarchCondition(id: "1", field: queryFields[2]).operator == .equals)
        #expect(MonarchCondition(id: "1", field: queryFields[4]).value == .none)
        #expect(MonarchCondition(id: "1", field: queryFields[0]).value == .option(nil))
        #expect(MonarchCondition(id: "1", field: nil).field.isEmpty)
    }

    @Test("Changing the field resets the operator and value; changing the operator keeps a fitting value")
    func editing() {
        let rule = MonarchCondition(id: "r", field: "title", operator: .equals, value: .text("abc"))
        let moved = MonarchQueryBuilderTokens.changingField(rule, to: queryFields[2])
        #expect(moved.id == "r")
        #expect(moved.field == "amount")
        #expect(moved.value == .number(nil))
        let contains = MonarchQueryBuilderTokens.changingOperator(rule, to: .contains, field: queryFields[1])
        #expect(contains.value == .text("abc"))
        let empty = MonarchQueryBuilderTokens.changingOperator(rule, to: .isEmpty, field: queryFields[1])
        #expect(empty.value == .none)
        let back = MonarchQueryBuilderTokens.changingOperator(empty, to: .equals, field: queryFields[1])
        #expect(back.value == .text(""))
    }

    @Test("A stored operator the field does not offer stays in the menu")
    func keptOperator() {
        let rule = MonarchCondition(id: "r", field: "flagged", operator: .equals, value: .none)
        #expect(MonarchQueryBuilderTokens.operators(for: rule, field: queryFields[4]) == [.equals, .isTrue, .isFalse])
    }

    @Test("Problems: field, value and a removed option")
    func problems() {
        let group = MonarchQueryGroup(
            id: "g",
            rules: [
                MonarchCondition(id: "a", field: nil),
                MonarchCondition(id: "b", field: "title", operator: .equals, value: .text("")),
                MonarchCondition(id: "c", field: "status", operator: .equals, value: .option("archived")),
                MonarchCondition(id: "d", field: "title", operator: .isEmpty),
            ],
            groups: [MonarchQueryGroup(id: "n", rules: [MonarchCondition(id: "e", field: "amount", operator: .atMost)])]
        )
        #expect(group.problems(fields: queryFields) == ["a": .field, "b": .value, "c": .option, "e": .value])
        #expect(nestedQuery.problems(fields: queryFields).isEmpty)
        #expect(nestedQuery.allRules.map(\.id) == ["r1", "r2", "r3", "r4"])
    }

    @Test("Problems are shown only when asked")
    func shownProblem() {
        let rule = MonarchCondition(id: "a", field: nil)
        #expect(MonarchQueryBuilderTokens.shownProblem(rule, fields: queryFields, showsErrors: false) == nil)
        #expect(MonarchQueryBuilderTokens.shownProblem(rule, fields: queryFields, showsErrors: true) == .field)
    }

    @Test("Matching follows AND and OR through nested groups")
    func matching() {
        let calendar = utc
        let record: [String: MonarchConditionValue] = [
            "status": .text("OPEN"), "amount": .number(120), "flagged": .boolean(true),
            "created": .date(fixedDate.addingTimeInterval(86_400)),
        ]
        #expect(nestedQuery.matches(record, calendar: calendar))
        var notFlagged = record
        notFlagged["flagged"] = .boolean(false)
        #expect(!nestedQuery.matches(notFlagged, calendar: calendar))
        var early = notFlagged
        early["created"] = .date(fixedDate.addingTimeInterval(-86_400))
        #expect(nestedQuery.matches(early, calendar: calendar))
        #expect(MonarchQueryGroup(id: "empty").matches([:]))
    }

    @Test("Rule operators compare by kind; text ignores case; dates by day")
    func ruleMatching() {
        let calendar = utc
        func rule(_ op: MonarchConditionOperator, _ value: MonarchConditionValue) -> MonarchCondition {
            MonarchCondition(id: "r", field: "f", operator: op, value: value)
        }
        #expect(rule(.contains, .text("Port")).matches(.text("Quarterly report"), calendar: calendar))
        #expect(rule(.notContains, .text("x")).matches(.text("abc"), calendar: calendar))
        #expect(rule(.equals, .text("abc")).matches(.text("ABC"), calendar: calendar))
        #expect(rule(.notEquals, .text("abc")).matches(.none, calendar: calendar))
        #expect(rule(.greaterThan, .number(5)).matches(.number(6), calendar: calendar))
        #expect(!rule(.lessThan, .number(5)).matches(.number(5), calendar: calendar))
        #expect(rule(.atMost, .number(5)).matches(.number(5), calendar: calendar))
        #expect(!rule(.atLeast, .number(5)).matches(.none, calendar: calendar))
        #expect(rule(.equals, .date(fixedDate)).matches(.date(fixedDate.addingTimeInterval(3_600)), calendar: calendar))
        #expect(!rule(.before, .date(fixedDate)).matches(.date(fixedDate), calendar: calendar))
        #expect(rule(.after, .date(fixedDate)).matches(.date(fixedDate.addingTimeInterval(86_400)), calendar: calendar))
        #expect(rule(.isEmpty, .none).matches(.text(""), calendar: calendar))
        #expect(rule(.isNotEmpty, .none).matches(.option("a"), calendar: calendar))
        #expect(rule(.isFalse, .none).matches(.boolean(false), calendar: calendar))
        #expect(!rule(.isTrue, .none).matches(.none, calendar: calendar))
    }

    @Test("Numbers are edited and parsed in the locale")
    func numbers() {
        #expect(MonarchQueryBuilderTokens.numberText(1_250.5, locale: english) == "1250.5")
        #expect(MonarchQueryBuilderTokens.numberText(nil, locale: english).isEmpty)
        #expect(MonarchQueryBuilderTokens.parseNumber("1250.5", locale: english) == 1_250.5)
        #expect(MonarchQueryBuilderTokens.parseNumber("12,5", locale: Locale(identifier: "de_DE")) == 12.5)
        #expect(MonarchQueryBuilderTokens.parseNumber("  ", locale: english) == nil)
        #expect(MonarchQueryBuilderTokens.parseNumber("abc", locale: english) == nil)
    }

    @Test("Depth: groups nest below the limit; the note shows at it")
    func depth() {
        #expect(MonarchQueryBuilderTokens.canAddGroup(depth: 2, maxDepth: 3))
        #expect(!MonarchQueryBuilderTokens.canAddGroup(depth: 3, maxDepth: 3))
        #expect(MonarchQueryBuilderTokens.showsDepthNote(depth: 3, maxDepth: 3))
        #expect(!MonarchQueryBuilderTokens.showsDepthNote(depth: 1, maxDepth: 1))
    }

    @Test("Colours: nested groups alternate fills; problems use the danger border")
    func colors() {
        #expect(MonarchQueryBuilderTokens.groupFill(depth: 1) == nil)
        #expect(MonarchQueryBuilderTokens.groupFill(depth: 2) == \.bgSubtle)
        #expect(MonarchQueryBuilderTokens.groupFill(depth: 3) == \.bgSurface)
        #expect(MonarchQueryBuilderTokens.border(hasProblem: true) == \.dangerStrong)
        #expect(MonarchQueryBuilderTokens.border(hasProblem: false) == \.borderSubtle)
    }

    @Test("Spoken names carry the group and position")
    func accessibility() {
        #expect(text.group([1]) == "Top group")
        #expect(text.group([1, 2]) == "Group 2")
        #expect(text.group([1, 2, 1]) == "Group 2.1")
        #expect(text.rule("Top group", 1) == "Top group, rule 1")
        #expect(text.field("Group 2", 3) == "Group 2, rule 3 field")
        #expect(text.operatorLabel("Group 2", 3) == "Group 2, rule 3 operator")
        #expect(text.value("Group 2", 3) == "Group 2, rule 3 value")
        #expect(text.removeRule("Top group", 1) == "Remove top group, rule 1")
        #expect(text.removeGroup("Group 2.1") == "Remove group 2.1")
        #expect(text.combinatorLabel("Top group") == "Top group combinator")
    }

    @Test("Built-in strings come from the table")
    func builtIn() {
        #expect(text.label == "Query")
        #expect(text.and == "AND")
        #expect(text.or == "OR")
        #expect(text.ruleTitle(2) == "Rule 2")
        #expect(text.maxRulesReached(20) == "A group can have up to 20 rules.")
        #expect(text.removedField("amount") == "amount (removed)")
        #expect(text.operatorName(.atLeast) == "is at least")
        #expect(text.problem(.option) == "The chosen value no longer exists. Pick another.")
        let operators = MonarchConditionOperator.allCases.map(text.operatorName)
        let problems = MonarchConditionProblem.allCases.map(text.problem)
        let all =
            operators + problems + [
                text.combinatorBefore, text.addRule, text.addGroup, text.emptyGroup, text.maxDepthReached, text.choose,
                text.chooseDate,
            ]
        #expect(all.allSatisfy { !$0.hasPrefix("monarch.") })
    }

    @Test("Every string can be replaced")
    func replaced() {
        let custom = MonarchQueryBuilderText(
            labels: MonarchQueryBuilderLabels(
                group: { "G\($0.count)" },
                and: "ALL",
                removeRule: { "Delete \($0) \($1)" },
                operatorName: { $0 == .equals ? "=" : "?" },
                problem: { _ in "Fix" }
            )
        )
        #expect(custom.group([1, 2]) == "G2")
        #expect(custom.and == "ALL")
        #expect(custom.removeRule("Top group", 1) == "Delete Top group 1")
        #expect(custom.operatorName(.equals) == "=")
        #expect(custom.problem(.value) == "Fix")
    }
}
