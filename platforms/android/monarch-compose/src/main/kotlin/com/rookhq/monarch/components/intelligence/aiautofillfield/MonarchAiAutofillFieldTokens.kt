package com.rookhq.monarch.components.intelligence.aiautofillfield

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeKind
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiAutofillField. */
internal object MonarchAiAutofillFieldTokens {
    /** The width of the marker bar beside a suggested control. */
    val MarkerWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between the marker and the control. */
    val MarkerGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the control, the status line and the actions. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the parts of the status line and between the actions. */
    val LineGap: Dp = MonarchTokens.Space.space2.dp

    /** Whether the status line is shown. */
    fun showsStatus(state: MonarchAiAutofillState): Boolean = state != MonarchAiAutofillState.Cleared

    /** Whether Accept and Clear and the marker are shown. */
    fun showsActions(state: MonarchAiAutofillState): Boolean = state == MonarchAiAutofillState.Suggested

    /** Whether the confidence badge is shown. */
    fun showsConfidence(state: MonarchAiAutofillState, confidence: Float?): Boolean =
        state == MonarchAiAutofillState.Suggested && confidence != null && !confidence.isNaN()

    /** Whether the source line is shown: until the person edits the value. */
    fun showsSource(state: MonarchAiAutofillState, source: String?): Boolean =
        !source.isNullOrBlank() &&
            (state == MonarchAiAutofillState.Suggested || state == MonarchAiAutofillState.Accepted)

    /** The kind of the AI badge: generated while suggested or accepted, edited after an edit. */
    fun badgeKind(state: MonarchAiAutofillState): MonarchAiGeneratedBadgeKind =
        if (state == MonarchAiAutofillState.Edited) {
            MonarchAiGeneratedBadgeKind.Edited
        } else {
            MonarchAiGeneratedBadgeKind.Generated
        }

    /** The colour of the marker bar. */
    fun markerColor(colors: MonarchColors): Color = colors.aiBorder

    /** The colour of the source and original lines. */
    fun lineColor(colors: MonarchColors): Color = colors.textMuted
}
