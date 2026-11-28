package dev.monarchui.components.workflow.fieldrenderer

import androidx.compose.runtime.Immutable

/**
 * An answer to a question of a `MonarchFieldRenderer`. No answer is `null`.
 */
@Immutable
public sealed interface MonarchFormFieldValue {
    /** Whether the answer counts as empty: blank text, not a number, no options and no files. */
    public val isEmpty: Boolean

    /** The answer as the plain value `MonarchCondition.evaluate` tests. */
    public val raw: Any

    /**
     * Text: short and long text, email, phone, web address, the value of a select or radio option, and a time
     * as `HH:MM` on a 24-hour clock.
     *
     * @property text the text.
     */
    public data class Text(public val text: String) : MonarchFormFieldValue {
        override val isEmpty: Boolean get() = text.isBlank()
        override val raw: Any get() = text
    }

    /**
     * A number, and the value of a rating.
     *
     * @property number the number.
     */
    public data class Number(public val number: Double) : MonarchFormFieldValue {
        override val isEmpty: Boolean get() = number.isNaN()
        override val raw: Any get() = number
    }

    /**
     * A day, as the epoch milliseconds of its UTC midnight, as the Material 3 `DatePickerState` gives it.
     *
     * @property millis the day.
     */
    public data class Date(public val millis: Long) : MonarchFormFieldValue {
        override val isEmpty: Boolean get() = false
        override val raw: Any get() = millis
    }

    /**
     * Yes or no, and whether a consent box is ticked.
     *
     * @property checked yes, or ticked.
     */
    public data class Flag(public val checked: Boolean) : MonarchFormFieldValue {
        override val isEmpty: Boolean get() = false
        override val raw: Any get() = checked
    }

    /**
     * The values of the chosen options of a multi-select or checkboxes question.
     *
     * @property values the option values, in the order of the options.
     */
    public data class Values(public val values: List<String>) : MonarchFormFieldValue {
        override val isEmpty: Boolean get() = values.isEmpty()
        override val raw: Any get() = values
    }

    /**
     * Files.
     *
     * @property files the files, in the order they were added.
     */
    public data class Files(public val files: List<MonarchFormFieldFile>) : MonarchFormFieldValue {
        override val isEmpty: Boolean get() = files.isEmpty()
        override val raw: Any get() = files.map { it.name }
    }
}
