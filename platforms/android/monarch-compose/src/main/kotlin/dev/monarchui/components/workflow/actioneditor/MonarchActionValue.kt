package dev.monarchui.components.workflow.actioneditor

import java.math.BigDecimal

/** The value of one parameter of a [MonarchRuleAction]. */
public sealed interface MonarchActionValue {
    /** Whether the value counts as not filled in: blank text or number, or an empty list. A flag never is. */
    public val isEmpty: Boolean

    /**
     * Text, as typed.
     *
     * @property text the text.
     */
    public data class Text(public val text: String) : MonarchActionValue {
        override val isEmpty: Boolean get() = text.isBlank()
    }

    /**
     * A number, kept as the typed text so a half-typed number stays as it is.
     *
     * @property text the typed text, with a dot as the decimal separator.
     */
    public data class Number(public val text: String) : MonarchActionValue {
        /** A number as text without trailing zeros: `Number(4.0)` reads "4". */
        public constructor(number: Double) : this(BigDecimal.valueOf(number).stripTrailingZeros().toPlainString())

        /** The number read from [text], or `null` when [text] is blank or not a finite number. */
        public val parsed: Double? get() = text.trim().toDoubleOrNull()?.takeIf { it.isFinite() }

        override val isEmpty: Boolean get() = text.isBlank()
    }

    /**
     * Yes or no.
     *
     * @property checked whether it is on.
     */
    public data class Flag(public val checked: Boolean) : MonarchActionValue {
        override val isEmpty: Boolean get() = false
    }

    /**
     * Several values: option values or free tags.
     *
     * @property values the values, in the order they were chosen.
     */
    public data class Values(public val values: List<String>) : MonarchActionValue {
        override val isEmpty: Boolean get() = values.isEmpty()
    }
}
