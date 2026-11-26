package com.rookhq.monarch.components.forms.inlineedittext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchInlineEditText, built from primitives and roles only. */
internal object MonarchInlineEditTextTokens {
    val MinSize: Dp = MonarchTouchTarget.Minimum
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val ViewPadding: Dp = MonarchTokens.Space.space1.dp

    /** The colour of the text in view mode: the subtle colour for the placeholder, disabled when disabled. */
    fun viewText(empty: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        empty -> colors.textSubtle
        else -> colors.textDefault
    }

    /** The colour of the edit icon. */
    fun icon(enabled: Boolean, colors: MonarchColors): Color = if (enabled) colors.textMuted else colors.textDisabled

    /** The text that is compared and saved: trimmed unless [trim] is off. */
    fun normalise(text: String, trim: Boolean): String = if (trim) text.trim() else text
}
