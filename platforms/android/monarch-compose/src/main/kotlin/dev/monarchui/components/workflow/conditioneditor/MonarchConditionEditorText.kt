package dev.monarchui.components.workflow.conditioneditor

import android.content.res.Resources
import dev.monarchui.R

/** The strings of a MonarchConditionEditor: each override from [labels], or the built-in resource. */
internal class MonarchConditionEditorText(
    private val labels: MonarchConditionEditorLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val label: String get() = string(labels.label, R.string.monarch_conditioneditor_label)
    val field: String get() = string(labels.field, R.string.monarch_conditioneditor_field)
    val operator: String get() = string(labels.operator, R.string.monarch_conditioneditor_operator)
    val value: String get() = string(labels.value, R.string.monarch_conditioneditor_value)
    val remove: String get() = string(labels.remove, R.string.monarch_conditioneditor_remove)
    val add: String get() = string(labels.add, R.string.monarch_conditioneditor_add)
    val matchBefore: String get() = string(labels.matchBefore, R.string.monarch_conditioneditor_match_before)
    val matchAfter: String get() = string(labels.matchAfter, R.string.monarch_conditioneditor_match_after)
    val matchAll: String get() = string(labels.matchAll, R.string.monarch_conditioneditor_match_all)
    val matchAny: String get() = string(labels.matchAny, R.string.monarch_conditioneditor_match_any)
    val empty: String get() = string(labels.empty, R.string.monarch_conditioneditor_empty)
    val choose: String get() = string(labels.choose, R.string.monarch_conditioneditor_choose)
    val chooseDate: String get() = string(labels.chooseDate, R.string.monarch_conditioneditor_choose_date)
    val confirmDate: String get() = string(labels.confirmDate, R.string.monarch_conditioneditor_confirm_date)
    val cancel: String get() = string(labels.cancel, R.string.monarch_conditioneditor_cancel)

    fun condition(position: Int): String =
        labels.condition?.invoke(position) ?: resources.getString(R.string.monarch_conditioneditor_condition, position)

    fun removeItem(position: Int): String = labels.removeItem?.invoke(position)
        ?: resources.getString(R.string.monarch_conditioneditor_remove_item, position)

    fun unknownField(key: String): String =
        labels.unknownField?.invoke(key) ?: resources.getString(R.string.monarch_conditioneditor_unknown_field, key)

    fun removedOption(value: String): String = labels.removedOption?.invoke(value)
        ?: resources.getString(R.string.monarch_conditioneditor_removed_option, value)

    fun maxReached(max: Int): String = labels.maxReached?.invoke(max)
        ?: resources.getQuantityString(R.plurals.monarch_conditioneditor_max_reached, max, max)

    fun problem(problem: MonarchConditionProblem): String = labels.problems[problem] ?: resources.getString(
        when (problem) {
            MonarchConditionProblem.Field -> R.string.monarch_conditioneditor_problem_field
            MonarchConditionProblem.Value -> R.string.monarch_conditioneditor_problem_value
            MonarchConditionProblem.Number -> R.string.monarch_conditioneditor_problem_number
            MonarchConditionProblem.Option -> R.string.monarch_conditioneditor_problem_option
        },
    )

    fun operator(operator: MonarchConditionOperator): String =
        labels.operators[operator] ?: resources.getString(operatorResource(operator))

    @Suppress("CyclomaticComplexMethod") // One resource per operator.
    private fun operatorResource(operator: MonarchConditionOperator): Int = when (operator) {
        MonarchConditionOperator.Eq -> R.string.monarch_conditioneditor_op_eq
        MonarchConditionOperator.Neq -> R.string.monarch_conditioneditor_op_neq
        MonarchConditionOperator.In -> R.string.monarch_conditioneditor_op_in
        MonarchConditionOperator.Nin -> R.string.monarch_conditioneditor_op_nin
        MonarchConditionOperator.Contains -> R.string.monarch_conditioneditor_op_contains
        MonarchConditionOperator.NotContains -> R.string.monarch_conditioneditor_op_not_contains
        MonarchConditionOperator.Gt -> R.string.monarch_conditioneditor_op_gt
        MonarchConditionOperator.Gte -> R.string.monarch_conditioneditor_op_gte
        MonarchConditionOperator.Lt -> R.string.monarch_conditioneditor_op_lt
        MonarchConditionOperator.Lte -> R.string.monarch_conditioneditor_op_lte
        MonarchConditionOperator.Before -> R.string.monarch_conditioneditor_op_before
        MonarchConditionOperator.After -> R.string.monarch_conditioneditor_op_after
        MonarchConditionOperator.IsTrue -> R.string.monarch_conditioneditor_op_is_true
        MonarchConditionOperator.IsFalse -> R.string.monarch_conditioneditor_op_is_false
        MonarchConditionOperator.IsEmpty -> R.string.monarch_conditioneditor_op_is_empty
        MonarchConditionOperator.IsNotEmpty -> R.string.monarch_conditioneditor_op_is_not_empty
    }
}
