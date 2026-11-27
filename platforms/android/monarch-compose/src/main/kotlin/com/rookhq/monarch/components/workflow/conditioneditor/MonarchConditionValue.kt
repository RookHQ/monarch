package com.rookhq.monarch.components.workflow.conditioneditor

import java.math.BigDecimal

/** The value of a [MonarchCondition]: text, a number, a day or a list of values. */
public sealed interface MonarchConditionValue {
    /** Whether the value is missing: blank text or number, no day, or an empty list. */
    public val isEmpty: Boolean

    /** The value as a list of strings, as `In`, `Nin` and `Contains` read it. Empty when [isEmpty]. */
    public val asList: List<String>

    /**
     * Text, as typed.
     *
     * @property text the text.
     */
    public data class Text(public val text: String) : MonarchConditionValue {
        override val isEmpty: Boolean get() = text.isBlank()
        override val asList: List<String> get() = if (isEmpty) emptyList() else listOf(text)
    }

    /**
     * A number, kept as the typed text so a half-typed number such as "1." stays as it is.
     *
     * @property text the typed text, with a dot as the decimal separator.
     */
    public data class Number(public val text: String) : MonarchConditionValue {
        /** A number as text without trailing zeros: `Number(500.0)` reads "500". */
        public constructor(number: Double) : this(BigDecimal.valueOf(number).stripTrailingZeros().toPlainString())

        /** The number read from [text], or `null` when [text] is blank or not a finite number. */
        public val parsed: Double? get() = text.trim().toDoubleOrNull()?.takeIf { it.isFinite() }

        override val isEmpty: Boolean get() = text.isBlank()
        override val asList: List<String> get() = if (isEmpty) emptyList() else listOf(text.trim())
    }

    /**
     * A calendar day.
     *
     * @property millis midnight of the day in UTC, in epoch milliseconds, as the Material 3 date picker gives it;
     * `null` before a day is chosen.
     */
    public data class Date(public val millis: Long?) : MonarchConditionValue {
        override val isEmpty: Boolean get() = millis == null
        override val asList: List<String> get() = listOfNotNull(millis?.toString())
    }

    /**
     * Several values: option values or free tags.
     *
     * @property values the values, in the order they were chosen.
     */
    public data class Values(public val values: List<String>) : MonarchConditionValue {
        override val isEmpty: Boolean get() = values.isEmpty()
        override val asList: List<String> get() = values
    }
}
