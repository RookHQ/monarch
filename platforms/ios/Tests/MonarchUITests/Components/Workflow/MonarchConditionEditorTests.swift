import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private enum ConditionSamples {
    static let fields = [
        MonarchConditionField(
            id: "priority",
            label: "Priority",
            kind: .select([
                MonarchComboboxOption(id: "high", label: "High"),
                MonarchComboboxOption(id: "low", label: "Low"),
            ])
        ),
        MonarchConditionField(id: "subject", label: "Subject", kind: .text, prompt: "Text"),
        MonarchConditionField(id: "hours", label: "Hours since created", kind: .number, prompt: "Hours"),
        MonarchConditionField(id: "due", label: "Due date", kind: .date),
        MonarchConditionField(id: "vip", label: "VIP customer", kind: .boolean),
    ]
    static let day = Date(timeIntervalSince1970: 1_767_225_600)  // 2026-01-01 UTC
}

enum MonarchConditionEditorFixtures: ComponentFixtures {
    static let id = "condition-editor"
    static let fixtures = [
        Fixture("every value kind with the all or any switch") {
            MonarchConditionEditor(
                conditions: .constant([
                    MonarchCondition(id: "c1", field: "priority", operator: .equals, value: .option("high")),
                    MonarchCondition(id: "c2", field: "subject", operator: .contains, value: .text("refund")),
                    MonarchCondition(id: "c3", field: "hours", operator: .atLeast, value: .number(4)),
                    MonarchCondition(id: "c4", field: "due", operator: .before, value: .date(ConditionSamples.day)),
                    MonarchCondition(id: "c5", field: "vip", operator: .isTrue),
                ]),
                fields: ConditionSamples.fields,
                match: .constant(.any)
            )
        },
        Fixture("errors shown: no field, no value, removed field, no date") {
            MonarchConditionEditor(
                conditions: .constant([
                    MonarchCondition(id: "e1", field: nil),
                    MonarchCondition(id: "e2", field: "subject", operator: .equals, value: .text("")),
                    MonarchCondition(id: "e3", field: "region", operator: .equals, value: .text("EU")),
                    MonarchCondition(id: "e4", field: "due", operator: .after, value: .date(nil)),
                ]),
                fields: ConditionSamples.fields,
                showsErrors: true
            )
        },
        Fixture("empty") {
            MonarchConditionEditor(conditions: .constant([]), fields: ConditionSamples.fields, match: .constant(.all))
        },
        Fixture("full, disabled, long labels") {
            MonarchConditionEditor(
                conditions: .constant([
                    MonarchCondition(id: "f1", field: "subject", operator: .isNotEmpty)
                ]),
                fields: ConditionSamples.fields,
                maxConditions: 1,
                labels: MonarchConditionEditorLabels(
                    condition: { "Requirement number \($0) for this automation rule to run" }
                )
            )
            .disabled(true)
        },
        Fixture("no fields") {
            MonarchConditionEditor(conditions: .constant([]), fields: [])
        },
    ]
}

@Suite("MonarchConditionEditor")
@MainActor
struct MonarchConditionEditorTests {
    private let text = MonarchConditionEditorText(labels: MonarchConditionEditorLabels())

    @Test("New conditions start on the default field, else the first")
    func startField() {
        let fields = ConditionSamples.fields
        #expect(MonarchConditionEditorTokens.startField(fields, defaultField: "hours")?.id == "hours")
        #expect(MonarchConditionEditorTokens.startField(fields, defaultField: "missing")?.id == "priority")
        #expect(MonarchConditionEditorTokens.startField(fields, defaultField: nil)?.id == "priority")
        #expect(MonarchConditionEditorTokens.startField([], defaultField: nil) == nil)
    }

    @Test("Adding appends a condition with the field's first operator and an empty value")
    func adding() {
        let added = MonarchConditionEditorTokens.adding(
            to: [],
            id: "n1",
            fields: ConditionSamples.fields,
            defaultField: "hours"
        )
        #expect(added == [MonarchCondition(id: "n1", field: "hours", operator: .equals, value: .number(nil))])
        let blank = MonarchConditionEditorTokens.adding(to: added, id: "n2", fields: [], defaultField: nil)
        #expect(blank.count == 2)
        #expect(blank[1].field.isEmpty)
        #expect(blank[1].problem(fields: ConditionSamples.fields) == .field)
    }

    @Test("The switch shows with a binding and two or more conditions; the add button until the limit")
    func visibility() {
        #expect(!MonarchConditionEditorTokens.showsMatch(hasBinding: true, count: 1))
        #expect(MonarchConditionEditorTokens.showsMatch(hasBinding: true, count: 2))
        #expect(!MonarchConditionEditorTokens.showsMatch(hasBinding: false, count: 5))
        #expect(MonarchConditionEditorTokens.canAdd(count: 19, maxConditions: 20))
        #expect(!MonarchConditionEditorTokens.canAdd(count: 20, maxConditions: 20))
        #expect(!MonarchConditionEditorTokens.canAdd(count: 0, maxConditions: 0))
    }

    @Test("Problems are keyed by condition id")
    func problems() {
        let conditions = [
            MonarchCondition(id: "a", field: nil),
            MonarchCondition(id: "b", field: "subject", operator: .contains, value: .text("")),
            MonarchCondition(id: "c", field: "priority", operator: .equals, value: .option("gone")),
            MonarchCondition(id: "d", field: "vip", operator: .isFalse),
        ]
        let problems = MonarchConditionEditorTokens.problems(conditions, fields: ConditionSamples.fields)
        #expect(problems == ["a": .field, "b": .value, "c": .option])
    }

    @Test("Changing the operator keeps a value of the same kind and clears it for no-value operators")
    func operatorChange() throws {
        let field = try #require(ConditionSamples.fields.first { $0.id == "hours" })
        let rule = MonarchCondition(id: "r", field: "hours", operator: .atLeast, value: .number(4))
        let kept = MonarchQueryBuilderTokens.changingOperator(rule, to: .lessThan, field: field)
        #expect(kept.value == .number(4))
        let cleared = MonarchQueryBuilderTokens.changingOperator(rule, to: .isEmpty, field: field)
        #expect(cleared.value == .none)
    }

    @Test("All and any combine the list the same way as a query group")
    func evaluation() {
        let conditions = [
            MonarchCondition(id: "a", field: "priority", operator: .equals, value: .option("high")),
            MonarchCondition(id: "b", field: "hours", operator: .atLeast, value: .number(4)),
        ]
        let record: [String: MonarchConditionValue] = ["priority": .option("high"), "hours": .number(2)]
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        #expect(!MonarchQueryGroup(id: "all", combinator: .and, rules: conditions).matches(record, calendar: calendar))
        #expect(MonarchQueryGroup(id: "any", combinator: .or, rules: conditions).matches(record, calendar: calendar))
    }

    @Test("Spoken names carry the position")
    func rowText() {
        let row = text.row(2)
        #expect(row.title == "Condition 2")
        #expect(row.name == "Condition 2")
        #expect(row.field == "Condition 2 field")
        #expect(row.operatorLabel == "Condition 2 operator")
        #expect(row.value == "Condition 2 value")
        #expect(row.remove == "Remove condition 2")
        #expect(row.operatorName(.atLeast) == "is at least")
        #expect(row.problem(.value) == "Enter a value.")
        #expect(row.removedField("region") == "region (removed)")
        #expect(text.removed(3) == "Condition 3 removed.")
        #expect(text.maxReached(20) == "You can add up to 20 conditions.")
    }

    @Test("Replacements win over the built-in strings")
    func overrides() {
        let custom = MonarchConditionEditorText(
            labels: MonarchConditionEditorLabels(
                condition: { "Rule \($0)" },
                field: { "Field of rule \($0)" },
                add: "New rule",
                matchAll: "every",
                operatorName: { $0 == .equals ? "equals" : $0.rawValue },
                problem: { _ in "Fix this." }
            )
        )
        let row = custom.row(1)
        #expect(row.title == "Rule 1")
        #expect(row.field == "Field of rule 1")
        #expect(row.operatorLabel == "Condition 1 operator")
        #expect(row.operatorName(.equals) == "equals")
        #expect(row.problem(.field) == "Fix this.")
        #expect(custom.add == "New rule")
        #expect(custom.match(.all) == "every")
        #expect(custom.match(.any) == "any")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(text.label == "Conditions")
        #expect(text.add == "Add condition")
        #expect(text.matchBefore == "Match")
        #expect(text.matchAfter == "of these conditions")
        #expect(text.matchLabel == "Conditions that must hold")
        #expect(text.match(.all) == "all")
        #expect(text.empty == "No conditions. This applies every time.")
        #expect(text.choose == "Choose…")
        #expect(text.chooseDate == "Choose a date")
    }
}
