package com.rookhq.monarch.components.workflow.conditioneditor

import androidx.compose.runtime.Immutable

/**
 * One condition: a field, an operator and, when the operator takes one, a value.
 *
 * @property id a stable, unique id.
 * @property field the `key` of a [MonarchConditionField].
 * @property operator how the field's value is compared.
 * @property value the value to compare with; `null` for an operator that takes none.
 */
@Immutable
public data class MonarchCondition(
    public val id: String,
    public val field: String,
    public val operator: MonarchConditionOperator,
    public val value: MonarchConditionValue? = null,
) {
    /** What is missing from this condition, or `null` when it is complete. */
    public fun problem(fields: List<MonarchConditionField>): MonarchConditionProblem? {
        val definition = fields.firstOrNull { it.key == field } ?: return MonarchConditionProblem.Field
        if (!operator.takesValue) return null
        val v = value
        return when {
            v == null || v.isEmpty -> MonarchConditionProblem.Value
            definition.type == MonarchConditionFieldType.Number &&
                (v !is MonarchConditionValue.Number || v.parsed == null) -> MonarchConditionProblem.Number
            definition.options.isNotEmpty() && definition.type.choosesOptions() &&
                !definition.options.map { it.value }.containsAll(v.asList) -> MonarchConditionProblem.Option
            else -> null
        }
    }

    /**
     * Tests this condition against [actual], the record's value of the field: a `String`, a number, a `Boolean`,
     * a `Collection` of values, or a day as epoch milliseconds. Text comparisons ignore case; dates compare the
     * UTC day. A useful default for previews and for logic such as "show this question when"; the server stays
     * the authority.
     */
    public fun matches(actual: Any?): Boolean = MonarchConditionEvaluation.matches(this, actual)

    public companion object {
        /** A new condition on [field] with its first operator and an empty value. */
        public fun create(id: String, field: MonarchConditionField?): MonarchCondition {
            val operator = MonarchConditionOperator.forField(field).firstOrNull() ?: MonarchConditionOperator.Eq
            return MonarchCondition(id, field?.key.orEmpty(), operator, blankValue(field, operator))
        }

        /** The problems of every incomplete condition, keyed by condition id. Empty when all are complete. */
        public fun problems(
            conditions: List<MonarchCondition>,
            fields: List<MonarchConditionField>,
        ): Map<String, MonarchConditionProblem> = buildMap {
            conditions.forEach { c -> c.problem(fields)?.let { put(c.id, it) } }
        }

        /**
         * Tests [conditions] against [values] keyed by field: all of them, or at least one, as [match] says. No
         * conditions always match.
         */
        public fun evaluate(
            conditions: List<MonarchCondition>,
            values: Map<String, Any?>,
            match: MonarchConditionMatch = MonarchConditionMatch.All,
        ): Boolean {
            if (conditions.isEmpty()) return true
            val test = { c: MonarchCondition -> c.matches(values[c.field]) }
            return if (match == MonarchConditionMatch.All) conditions.all(test) else conditions.any(test)
        }

        /** The empty value a field and an operator start with, or `null` when the operator takes none. */
        internal fun blankValue(
            field: MonarchConditionField?,
            operator: MonarchConditionOperator,
        ): MonarchConditionValue? =
            when {
                !operator.takesValue -> null
                operator.takesList -> MonarchConditionValue.Values(emptyList())
                else -> when (field?.type) {
                    MonarchConditionFieldType.MultiSelect,
                    MonarchConditionFieldType.Tags,
                    -> MonarchConditionValue.Values(emptyList())
                    MonarchConditionFieldType.Number -> MonarchConditionValue.Number("")
                    MonarchConditionFieldType.Date -> MonarchConditionValue.Date(null)
                    else -> MonarchConditionValue.Text("")
                }
            }

        private fun MonarchConditionFieldType.choosesOptions(): Boolean =
            this == MonarchConditionFieldType.Select || this == MonarchConditionFieldType.MultiSelect
    }
}
