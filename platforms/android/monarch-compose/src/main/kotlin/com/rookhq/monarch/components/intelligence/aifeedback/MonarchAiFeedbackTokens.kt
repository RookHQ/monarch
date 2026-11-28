package com.rookhq.monarch.components.intelligence.aifeedback

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchAiFeedback, from primitives and roles only. */
internal object MonarchAiFeedbackTokens {
    /** The space between the thumbs. */
    val ThumbGap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the parts of the sheet. */
    val SheetGap: Dp = MonarchTokens.Space.space4.dp

    /** The thank-you or the error under the thumbs. */
    fun messageColor(failed: Boolean, colors: MonarchColors): Color =
        if (failed) colors.dangerStrong else colors.textMuted
}
