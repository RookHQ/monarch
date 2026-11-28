package dev.monarchui.components.search.querybuilder

import androidx.compose.runtime.Immutable
import dev.monarchui.components.workflow.conditioneditor.MonarchCondition
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionField
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionProblem

/**
 * A group of rules and nested groups joined by one combinator. The top group is the whole query.
 *
 * @property id a stable, unique id, unique across the query together with every rule id.
 * @property combinator how the rules and sub-groups combine.
 * @property rules the rules of this group, shown first. Each is a `MonarchCondition`.
 * @property groups the nested groups, shown after the rules.
 */
@Immutable
public data class MonarchQueryGroup(
    public val id: String,
    public val combinator: MonarchQueryCombinator = MonarchQueryCombinator.And,
    public val rules: List<MonarchCondition> = emptyList(),
    public val groups: List<MonarchQueryGroup> = emptyList(),
) {
    /** Every rule in the query, depth first. */
    public fun allRules(): List<MonarchCondition> = rules + groups.flatMap { it.allRules() }

    /** The problems of every incomplete rule, keyed by rule id. Empty when the query is complete. */
    public fun problems(fields: List<MonarchConditionField>): Map<String, MonarchConditionProblem> =
        MonarchCondition.problems(allRules(), fields)

    /**
     * Tests the query against [values] keyed by field, with `MonarchCondition.matches` for each rule. An empty
     * group matches. A useful default for previews; the server stays the authority.
     */
    public fun matches(values: Map<String, Any?>): Boolean {
        val tests = rules.map { { it.matches(values[it.field]) } } + groups.map { { it.matches(values) } }
        if (tests.isEmpty()) return true
        return if (combinator == MonarchQueryCombinator.And) tests.all { it() } else tests.any { it() }
    }

    /** Every group and rule id in the query. */
    internal fun ids(): List<String> = listOf(id) + rules.map { it.id } + groups.flatMap { it.ids() }
}
