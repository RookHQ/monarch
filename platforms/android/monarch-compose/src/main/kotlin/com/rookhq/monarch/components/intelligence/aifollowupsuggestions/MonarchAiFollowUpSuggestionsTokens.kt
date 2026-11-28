package com.rookhq.monarch.components.intelligence.aifollowupsuggestions

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchControlState
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchAiFollowUpSuggestions, built from primitives and roles only. */
internal object MonarchAiFollowUpSuggestionsTokens {
    /** The gap between chips in a row. Rows are separated by the chips' own 48dp touch targets. */
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The widest a chip grows, so one long suggestion does not fill the row: 288dp, as on iOS. */
    val MaxChipWidth: Dp = (MonarchTokens.Space.space24 * 3).dp

    /** The most lines of one suggestion; a longer one is cut at the end. TalkBack reads all of it. */
    const val MAX_LINES: Int = 2

    /** The visible height of a one-line chip; the touch target is 48dp. */
    val ChipHeight: Dp = MonarchTokens.Control.sm.dp

    /** The space at the start and end of a chip's text. */
    val ChipPaddingHorizontal: Dp = MonarchTokens.Space.space3.dp

    /** The space above and below a chip's text, so two lines do not touch the border. */
    val ChipPaddingVertical: Dp = MonarchTokens.Space.space1_5.dp

    /** The suggestions that are shown: trimmed, without blank ones or repeats, in order. */
    fun visible(suggestions: List<String>): List<String> =
        suggestions.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /**
     * The fill, text and border of a chip: the surface with the default border and text (the assistant colour is
     * kept for small accents, PREMIUM rule 8), a subtle fill when pressed or hovered, and disabled text when
     * disabled.
     */
    fun colors(state: MonarchControlState, colors: MonarchColors): MonarchSurfaceColors = state.pick(
        resting = MonarchSurfaceColors(colors.bgSurface, colors.aiStrong, colors.aiBorder),
        disabled = MonarchSurfaceColors(colors.bgSurface, colors.textDisabled, colors.borderDefault),
        pressed = MonarchSurfaceColors(colors.aiSubtleHover, colors.aiStrong, colors.aiBorder),
        hovered = MonarchSurfaceColors(colors.aiSubtle, colors.aiStrong, colors.aiBorder),
    )
}
