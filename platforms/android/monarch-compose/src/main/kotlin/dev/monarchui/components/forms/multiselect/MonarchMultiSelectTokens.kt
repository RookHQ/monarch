package dev.monarchui.components.forms.multiselect

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The resolved strings of the multi-select sheet. */
internal class MultiSelectSheetTexts(val search: String, val done: String)

/** Component tokens and the pure rules of MonarchMultiSelect. */
internal object MonarchMultiSelectTokens {
    /** The least height of a row in the sheet: the touch target. */
    val RowMinHeight: Dp = MonarchTouchTarget.Minimum

    /** The padding of the sheet's content. */
    val SheetPadding: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the parts of the field and of the sheet. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between a row's mark and its texts. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The vertical padding of a row, and of a heading or a message. */
    val RowVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** Whether [selected] has reached [maxSelections]. */
    fun atLimit(selected: List<String>, maxSelections: Int?): Boolean =
        maxSelections != null && selected.size >= maxSelections

    /**
     * The selection after tapping [id]: removed when chosen, else appended in pick order unless the limit is
     * reached.
     */
    fun toggle(selected: List<String>, id: String, maxSelections: Int?): List<String> = when {
        id in selected -> selected - id
        atLimit(selected, maxSelections) -> selected
        else -> selected + id
    }

    /** Whether an option's row can be tapped: an enabled option that is chosen, or that fits under the limit. */
    fun rowEnabled(option: MonarchComboboxOption, chosen: Boolean, atLimit: Boolean, enabled: Boolean): Boolean =
        enabled && option.enabled && (chosen || !atLimit)

    /** The labels of the chosen ids, in pick order; an id without an option shows as itself. */
    fun chosenLabels(selected: List<String>, options: List<MonarchComboboxOption>): List<Pair<String, String>> =
        selected.map { id -> id to (options.firstOrNull { it.id == id }?.label ?: id) }

    /** The colour of a row's label. */
    fun labelColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of a description, a hint, a heading, a message and the count. */
    fun secondaryColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The background of the sheet. */
    fun sheetColor(colors: MonarchColors): Color = colors.bgSurface
}
