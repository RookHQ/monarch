package dev.monarchui.components.datagrid.editablecell

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchEditableCell`. Every
 * `null` uses the built-in string named after it in `strings_editablecell.xml`.
 *
 * @property edit writes what TalkBack says a double tap on the cell does, from the cell's label: "Edit Status
 * for Acme".
 * @property save the button that saves a text or number: "Save".
 * @property cancel the button that leaves editing without saving: "Cancel".
 * @property saving the spoken name of the progress shown while the value is saved: "Saving".
 * @property invalidNumber the error when a number cell holds something that is not a number: "Enter a number.".
 * @property required the error when a required cell is empty: "Enter a value.".
 * @property belowMin writes the error when a value is below the minimum, from the formatted minimum.
 * @property aboveMax writes the error when a value is above the maximum, from the formatted maximum.
 * @property noValue the menu item of a select cell that empties it, when the cell is not required: "None".
 * @property saveFailed the error when a save failed without a message of its own: "Couldn't save. Try again.".
 */
@Immutable
public class MonarchEditableCellLabels(
    public val edit: ((label: String) -> String)? = null,
    public val save: String? = null,
    public val cancel: String? = null,
    public val saving: String? = null,
    public val invalidNumber: String? = null,
    public val required: String? = null,
    public val belowMin: ((min: String) -> String)? = null,
    public val aboveMax: ((max: String) -> String)? = null,
    public val noValue: String? = null,
    public val saveFailed: String? = null,
)
