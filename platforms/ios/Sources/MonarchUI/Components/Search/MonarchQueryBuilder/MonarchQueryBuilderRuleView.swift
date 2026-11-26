import SwiftUI

/// One rule of a ``MonarchQueryBuilder``: a ``MonarchConditionRow`` with the wording of its group.
struct MonarchQueryBuilderRuleView: View {
    @Binding var rule: MonarchCondition
    let groupName: String
    let position: Int
    let context: MonarchQueryBuilderContext
    let onRemove: () -> Void

    private var text: MonarchQueryBuilderText { context.text }

    var body: some View {
        MonarchConditionRow(
            rule: $rule,
            fields: context.fields,
            text: MonarchConditionRowText(
                title: "",
                name: text.rule(groupName, position),
                field: text.field(groupName, position),
                operatorLabel: text.operatorLabel(groupName, position),
                value: text.value(groupName, position),
                remove: text.removeRule(groupName, position),
                choose: text.choose,
                chooseDate: text.chooseDate,
                removedField: text.removedField,
                operatorName: text.operatorName,
                problem: text.problem
            ),
            problem: MonarchQueryBuilderTokens.shownProblem(
                rule,
                fields: context.fields,
                showsErrors: context.showsErrors
            ),
            defaultDate: { Date() },
            onRemove: onRemove
        )
    }
}
