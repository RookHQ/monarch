package com.rookhq.monarch.components.forms.textfield

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/** Component tokens of MonarchTextField, built from roles only. */
internal object MonarchTextFieldTokens {
    /** The lines a field may grow to: one when single-line, else at least [minLines]. */
    fun maxLines(singleLine: Boolean, minLines: Int, maxLines: Int): Int =
        if (singleLine) 1 else maxLines.coerceAtLeast(minLines)

    /** The colour of the required mark after the label. */
    fun requiredMarkColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of the outline and the message in the success state. */
    fun successColor(colors: MonarchColors): Color = colors.successStrong

    /** The colours of the field. In the [success] state the outline and the message are in the success colour. */
    @Composable
    fun colors(success: Boolean = false): TextFieldColors {
        val colors = MonarchTheme.colors
        val successRole = if (success) successColor(colors) else null
        return OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textDefault,
            unfocusedTextColor = colors.textDefault,
            disabledTextColor = colors.textDisabled,
            errorTextColor = colors.textDefault,
            focusedContainerColor = colors.bgSurface,
            unfocusedContainerColor = colors.bgSurface,
            disabledContainerColor = colors.bgSubtle,
            errorContainerColor = colors.bgSurface,
            cursorColor = colors.accentStrong,
            errorCursorColor = colors.dangerStrong,
            focusedBorderColor = successRole ?: colors.borderInput,
            unfocusedBorderColor = successRole ?: colors.borderInput,
            disabledBorderColor = colors.borderDefault,
            errorBorderColor = colors.dangerStrong,
            focusedLeadingIconColor = colors.textMuted,
            unfocusedLeadingIconColor = colors.textMuted,
            disabledLeadingIconColor = colors.textDisabled,
            errorLeadingIconColor = colors.textMuted,
            focusedTrailingIconColor = colors.textMuted,
            unfocusedTrailingIconColor = colors.textMuted,
            disabledTrailingIconColor = colors.textDisabled,
            errorTrailingIconColor = colors.dangerStrong,
            focusedLabelColor = colors.textDefault,
            unfocusedLabelColor = colors.textMuted,
            disabledLabelColor = colors.textDisabled,
            errorLabelColor = colors.dangerStrong,
            focusedPlaceholderColor = colors.textSubtle,
            unfocusedPlaceholderColor = colors.textSubtle,
            disabledPlaceholderColor = colors.textDisabled,
            errorPlaceholderColor = colors.textSubtle,
            focusedSupportingTextColor = successRole ?: colors.textMuted,
            unfocusedSupportingTextColor = successRole ?: colors.textMuted,
            disabledSupportingTextColor = colors.textDisabled,
            errorSupportingTextColor = colors.dangerStrong,
        )
    }
}
