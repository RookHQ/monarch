package dev.monarchui.components.workflow.conditioneditor

import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.After
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Before
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Contains
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Eq
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Gt
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Gte
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.In
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.IsEmpty
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.IsFalse
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.IsNotEmpty
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.IsTrue
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Lt
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Lte
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Neq
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.Nin
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator.NotContains
import java.util.Locale

/** How a condition is tested against a record's value, and how its value follows a change of operator. */
internal object MonarchConditionEvaluation {
    private const val DAY_MILLIS: Long = 86_400_000L

    @Suppress("CyclomaticComplexMethod") // One branch per operator, as the web's switch.
    fun matches(condition: MonarchCondition, actual: Any?): Boolean {
        val expected = condition.value
        val empty = isEmpty(actual)
        return when (condition.operator) {
            IsEmpty -> empty
            IsNotEmpty -> !empty
            IsTrue -> actual == true || lower(actual) == "true"
            IsFalse -> actual == false || lower(actual) == "false"
            Eq -> equalTo(expected, actual)
            Neq -> !equalTo(expected, actual)
            In -> lower(actual) in expected.list().map(::lower)
            Nin -> lower(actual) !in expected.list().map(::lower)
            Contains -> contains(expected, actual)
            NotContains -> !contains(expected, actual)
            Gt, Gte, Lt, Lte -> !empty && compareNumbers(condition.operator, number(actual), expected.expectedNumber())
            Before -> !empty && compareDays(actual, expected) { a, e -> a < e }
            After -> !empty && compareDays(actual, expected) { a, e -> a > e }
        }
    }

    /**
     * The condition after the operator changed to [operator]: the value is kept when the new operator takes the
     * same shape, and converted between one value and a list otherwise.
     */
    fun withOperator(
        condition: MonarchCondition,
        operator: MonarchConditionOperator,
        field: MonarchConditionField?,
    ): MonarchCondition {
        val previous = condition.value
        val value = when {
            !operator.takesValue -> null
            !condition.operator.takesValue || previous == null -> MonarchCondition.blankValue(field, operator)
            condition.operator.takesList == operator.takesList -> previous
            operator.takesList -> MonarchConditionValue.Values(previous.asList)
            previous is MonarchConditionValue.Values -> single(previous.values.firstOrNull().orEmpty(), field)
            else -> previous
        }
        return condition.copy(operator = operator, value = value)
    }

    private fun single(text: String, field: MonarchConditionField?): MonarchConditionValue = when (field?.type) {
        MonarchConditionFieldType.Number -> MonarchConditionValue.Number(text)
        MonarchConditionFieldType.Date -> MonarchConditionValue.Date(text.toLongOrNull())
        else -> MonarchConditionValue.Text(text)
    }

    private fun isEmpty(actual: Any?): Boolean = when (actual) {
        null -> true
        is String -> actual.isEmpty()
        is Collection<*> -> actual.isEmpty()
        else -> false
    }

    private fun lower(x: Any?): String = when (x) {
        null -> ""
        is Collection<*> -> x.joinToString(",") { lower(it) }
        else -> x.toString().lowercase(Locale.ROOT)
    }

    private fun MonarchConditionValue?.list(): List<String> = this?.asList.orEmpty()

    private fun MonarchConditionValue?.expectedNumber(): Double? = when (this) {
        is MonarchConditionValue.Number -> parsed
        is MonarchConditionValue.Text -> text.trim().toDoubleOrNull()
        else -> null
    }

    private fun number(actual: Any?): Double? = when (actual) {
        is kotlin.Number -> actual.toDouble()
        is String -> actual.trim().toDoubleOrNull()
        else -> null
    }

    private fun day(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS)

    private fun equalTo(expected: MonarchConditionValue?, actual: Any?): Boolean = when (expected) {
        is MonarchConditionValue.Number -> expected.parsed != null && number(actual) == expected.parsed
        is MonarchConditionValue.Date -> {
            val a = number(actual)?.toLong()
            expected.millis != null && a != null && day(a) == day(expected.millis)
        }
        else -> lower(actual) == lower(expected.list().firstOrNull())
    }

    private fun contains(expected: MonarchConditionValue?, actual: Any?): Boolean {
        val wanted = expected.list().map(::lower)
        return if (actual is Collection<*>) {
            val have = actual.map(::lower)
            wanted.any { it in have }
        } else {
            val text = lower(actual)
            wanted.any { text.contains(it) }
        }
    }

    private fun compareNumbers(operator: MonarchConditionOperator, actual: Double?, expected: Double?): Boolean {
        if (actual == null || expected == null) return false
        return when (operator) {
            Gt -> actual > expected
            Gte -> actual >= expected
            Lt -> actual < expected
            else -> actual <= expected
        }
    }

    private inline fun compareDays(
        actual: Any?,
        expected: MonarchConditionValue?,
        test: (Long, Long) -> Boolean,
    ): Boolean {
        val a = number(actual)?.toLong()
        val e = (expected as? MonarchConditionValue.Date)?.millis
        return a != null && e != null && test(day(a), day(e))
    }
}
