package com.rookhq.monarch.components.search.querybuilder

import android.content.res.Resources
import com.rookhq.monarch.R
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionEditorLabels

/** The strings of a MonarchQueryBuilder: each override from [labels], or the built-in resource. */
internal class MonarchQueryBuilderText(
    private val labels: MonarchQueryBuilderLabels,
    private val resources: Resources,
) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val label: String get() = string(labels.label, R.string.monarch_querybuilder_label)
    val combinatorBefore: String
        get() = string(labels.combinatorBefore, R.string.monarch_querybuilder_combinator_before)
    val and: String get() = string(labels.and, R.string.monarch_querybuilder_and)
    val or: String get() = string(labels.or, R.string.monarch_querybuilder_or)
    val addRule: String get() = string(labels.addRule, R.string.monarch_querybuilder_add_rule)
    val addGroup: String get() = string(labels.addGroup, R.string.monarch_querybuilder_add_group)
    val remove: String get() = string(labels.remove, R.string.monarch_querybuilder_remove)
    val emptyGroup: String get() = string(labels.emptyGroup, R.string.monarch_querybuilder_empty_group)
    val maxDepthReached: String get() = string(labels.maxDepthReached, R.string.monarch_querybuilder_max_depth)

    fun group(path: List<Int>): String = labels.group?.invoke(path) ?: if (path.size <= 1) {
        resources.getString(R.string.monarch_querybuilder_top_group)
    } else {
        resources.getString(R.string.monarch_querybuilder_group, path.drop(1).joinToString("."))
    }

    fun combinatorLabel(group: String): String = labels.combinatorLabel?.invoke(group)
        ?: resources.getString(R.string.monarch_querybuilder_combinator_label, group)

    fun rule(position: Int): String =
        labels.rule?.invoke(position) ?: resources.getString(R.string.monarch_querybuilder_rule, position)

    fun removeRule(group: String, position: Int): String = labels.removeRule?.invoke(group, position)
        ?: resources.getString(R.string.monarch_querybuilder_remove_rule, group, position)

    fun removeGroup(group: String): String =
        labels.removeGroup?.invoke(group) ?: resources.getString(R.string.monarch_querybuilder_remove_group, group)

    /** The labels of one group's rule editor: [base] with the rule, remove, add and list names of this group. */
    fun ruleLabels(base: MonarchConditionEditorLabels, groupName: String): MonarchConditionEditorLabels =
        MonarchConditionEditorLabels(
            label = groupName,
            condition = ::rule,
            field = base.field,
            operator = base.operator,
            value = base.value,
            remove = base.remove ?: remove,
            removeItem = { position -> removeRule(groupName, position) },
            add = addRule,
            matchBefore = base.matchBefore,
            matchAfter = base.matchAfter,
            matchAll = base.matchAll,
            matchAny = base.matchAny,
            empty = emptyGroup,
            choose = base.choose,
            operators = base.operators,
            unknownField = base.unknownField,
            removedOption = base.removedOption,
            problems = base.problems,
            maxReached = base.maxReached,
            chooseDate = base.chooseDate,
            confirmDate = base.confirmDate,
            cancel = base.cancel,
        )
}
