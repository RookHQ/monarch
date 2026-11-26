package com.rookhq.monarch.components.forms.taginput

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchTagInput, built from primitives and roles only. */
internal object MonarchTagInputTokens {
    /** The gap between the label, the box and the supporting text. */
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The gap between tags, and between the tags and the text field. */
    val TagGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The padding inside the box. */
    val BoxPadding: Dp = MonarchTokens.Space.space2.dp

    /** The text field is a touch target as a whole. */
    val FieldMinHeight: Dp = MonarchTouchTarget.Minimum

    /** The narrowest the text field gets before it moves to a line of its own. */
    val FieldMinWidth: Dp = MonarchTokens.Space.space24.dp

    /** The gap between the icon and the message. */
    val InlineGap: Dp = MonarchTokens.Space.space1.dp

    /**
     * The fill and the border of the box: the danger border while there is an
     * error, the focus colour while focused, the subtle fill when disabled or
     * read-only.
     */
    fun boxColors(
        enabled: Boolean,
        readOnly: Boolean,
        focused: Boolean,
        error: Boolean,
        colors: MonarchColors,
    ): MonarchSurfaceColors = when {
        !enabled -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderDefault)
        readOnly -> MonarchSurfaceColors(colors.bgSubtle, colors.textDefault, colors.borderDefault)
        error -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.dangerStrong)
        focused -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.focusRing)
        else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderInput)
    }

    /** The colour of the label. */
    fun labelColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of the help text and the placeholder. */
    fun helperColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the error and its icon. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of the caret. */
    fun cursorColor(colors: MonarchColors): Color = colors.accentStrong
}
