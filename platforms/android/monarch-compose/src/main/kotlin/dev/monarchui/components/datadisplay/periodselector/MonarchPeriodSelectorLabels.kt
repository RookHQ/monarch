package dev.monarchui.components.datadisplay.periodselector

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchPeriodSelector` supplies. Each `null`
 * keeps the built-in English string `monarch_periodselector_<key>`.
 *
 * @property label the name of the control, read by TalkBack and shown on the select field. Default "Period".
 * @property preset the name of each preset, such as "Last 7 days".
 * @property customRange the name of the custom dates button, read before its dates. Default "Custom period".
 * @property chooseDates the custom dates button before any dates are picked. Default "Choose dates".
 * @property confirm the button that keeps the picked dates. Default "Save".
 * @property cancel the button that closes the picker without a change. Default "Cancel".
 * @property tooLong shown in the picker when the picked range is longer than `maxDays`. Default "Choose 30 days or
 * fewer".
 */
@Immutable
public class MonarchPeriodSelectorLabels(
    public val label: String? = null,
    public val preset: ((MonarchPeriodPreset) -> String)? = null,
    public val customRange: String? = null,
    public val chooseDates: String? = null,
    public val confirm: String? = null,
    public val cancel: String? = null,
    public val tooLong: ((maxDays: Int) -> String)? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchPeriodSelectorLabels && other.label == label && other.preset == preset &&
            other.customRange == customRange && other.chooseDates == chooseDates && other.confirm == confirm &&
            other.cancel == cancel && other.tooLong == tooLong

    override fun hashCode(): Int = listOf(label, preset, customRange, chooseDates, confirm, cancel, tooLong).hashCode()
}
