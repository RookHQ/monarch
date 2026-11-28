package dev.monarchui.components.forms.select

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** One heading of the menu and the options under it. */
internal class SelectSection<T>(val group: String?, val options: List<MonarchSelectOption<T>>)

/** Component tokens and the pure rules of MonarchSelect. */
internal object MonarchSelectTokens {
    /** The gap between an option's label and its description. */
    val ItemTextGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The padding of a group heading in the menu. */
    val HeaderHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val HeaderVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The chosen option, or `null` when nothing (or a value that is not an option) is chosen. */
    fun <T> chosen(options: List<MonarchSelectOption<T>>, selected: T?): MonarchSelectOption<T>? =
        if (selected == null) null else options.firstOrNull { it.value == selected }

    /** The options split into runs of the same group, in order. */
    fun <T> sections(options: List<MonarchSelectOption<T>>): List<SelectSection<T>> {
        val sections = mutableListOf<SelectSection<T>>()
        options.forEach { option ->
            val last = sections.lastOrNull()
            if (last != null && last.group == option.group) {
                sections[sections.lastIndex] = SelectSection(last.group, last.options + option)
            } else {
                sections += SelectSection(option.group, listOf(option))
            }
        }
        return sections
    }

    /** The colour of the text in the field: the chosen label, or the muted placeholder. */
    fun valueColor(chosen: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        chosen -> colors.textDefault
        else -> colors.textMuted
    }

    /** The colour of a group heading and of an option's description. */
    fun headerColor(colors: MonarchColors): Color = colors.textMuted
    fun descriptionColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The radio mark of an option: the accent when chosen, the input border otherwise, disabled text when off. */
    fun markColor(selected: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        selected -> colors.accentStrong
        else -> colors.borderInput
    }
}
