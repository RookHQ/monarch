package com.rookhq.monarch.components.intelligence.aitonepicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchAiTonePicker, built from primitives and roles only. */
internal object MonarchAiTonePickerTokens {
    /** The gap between the legend, the options and the custom field, and between options. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The padding at the start and end of an option. */
    val HorizontalPadding: Dp = MonarchTokens.Space.space3.dp

    /** The border of an option that is not selected. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The border of the selected option: 1dp too; the subtle AI fill and the AI edge colour carry the choice. */
    val SelectedBorderWidth: Dp = BorderWidth

    /** The options to show: the tones, then Custom when [custom] is given. A tone with the Custom id is dropped. */
    fun options(tones: List<MonarchAiTone>, custom: MonarchAiTone?): List<MonarchAiTone> =
        if (custom == null) tones else tones.filter { it.id != MonarchAiTone.CUSTOM_ID } + custom

    /** The fill, text colour and border of an option. */
    fun optionColors(selected: Boolean, enabled: Boolean, colors: MonarchColors): MonarchSurfaceColors = when {
        selected && enabled -> MonarchSurfaceColors(colors.aiSubtle, colors.textDefault, colors.aiBorder)
        selected -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderStrong)
        enabled -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderDefault)
        else -> MonarchSurfaceColors(colors.bgSurface, colors.textDisabled, colors.borderSubtle)
    }

    /** The border width of an option. The selected one keeps 1dp; it has its own fill and edge colour. */
    fun borderWidth(selected: Boolean): Dp = if (selected) SelectedBorderWidth else BorderWidth

}
