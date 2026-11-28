package dev.monarchui.components.datagrid.editablecell

import androidx.compose.runtime.Immutable
import dev.monarchui.components.forms.select.MonarchSelectOption

/**
 * The editor a `MonarchEditableCell` opens, and the type of value it holds.
 *
 * @param T the type of the cell's value: `String` for [Text], `Double` for [Number], the option value for
 * [Select] and epoch milliseconds in UTC (`Long`) for [Date].
 */
@Immutable
public sealed class MonarchEditableCellEditor<T : Any> {
    /** A single-line text field. Text is trimmed and an empty field is `null`. */
    public data object Text : MonarchEditableCellEditor<String>()

    /**
     * A single-line field with the decimal keyboard. The value is aligned to the end with tabular figures.
     *
     * @property min the lowest value allowed, or `null` for none.
     * @property max the highest value allowed, or `null` for none.
     */
    public class Number(
        public val min: Double? = null,
        public val max: Double? = null,
    ) : MonarchEditableCellEditor<Double>()

    /**
     * A menu of choices that opens from the cell; choosing one saves it.
     *
     * @property options the choices in order. A disabled option is shown and cannot be chosen.
     */
    public class Select<T : Any>(
        public val options: List<MonarchSelectOption<T>>,
    ) : MonarchEditableCellEditor<T>()

    /**
     * The Material 3 date picker in a dialog. Dates are epoch milliseconds at midnight UTC, as in
     * `DatePickerState`.
     *
     * @property min the earliest date that can be chosen, or `null` for none.
     * @property max the latest date that can be chosen, or `null` for none.
     */
    public class Date(
        public val min: Long? = null,
        public val max: Long? = null,
    ) : MonarchEditableCellEditor<Long>()
}
