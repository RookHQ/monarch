package com.rookhq.monarch.components.forms.formfield

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and logic of MonarchFormField, built from primitives and roles only. */
internal object MonarchFormFieldTokens {
    /** The gap between the label, the control and the supporting text. */
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The gap between the label column and the control in the row layout. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the label and the required mark or the optional text, and between icon and error. */
    val InlineGap: Dp = MonarchTokens.Space.space1.dp

    /** The colour of the label. */
    fun labelColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of the help text and the optional text. */
    fun helperColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the required mark. */
    fun requiredColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.dangerStrong else colors.textDisabled

    /** The colour of the error message and its icon. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** An error that is blank is no error. */
    fun error(errorText: String?): String? = errorText?.takeIf { it.isNotBlank() }

    /**
     * What TalkBack reads as the name of the control: the label, or, when the
     * field is required, the label with the spoken required word ([requiredName]).
     */
    fun controlName(label: String, required: Boolean, requiredName: String): String =
        if (required) requiredName else label
}
