package dev.monarchui.components.forms.passwordinput

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import dev.monarchui.theme.MonarchColors

/** Component tokens of MonarchPasswordInput, built from primitives and roles only. */
internal object MonarchPasswordInputTokens {
    /** The autofill content type for [purpose]. */
    fun contentType(purpose: MonarchPasswordInputPurpose): ContentType = when (purpose) {
        MonarchPasswordInputPurpose.Current -> ContentType.Password
        MonarchPasswordInputPurpose.New -> ContentType.NewPassword
    }

    /** The colour of the eye of the reveal toggle. */
    fun toggleColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled
}
