package com.rookhq.monarch.components.intelligence.aiinlineprompt

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Sizes, colours and rules of `MonarchAiInlinePrompt`. */
internal object MonarchAiInlinePromptTokens {
    /** The most characters of the selection shown before it is cut with an ellipsis, as on the web. */
    const val EXCERPT_MAX: Int = 140

    /** The gap between the title, the excerpt, the prompt and the suggestions. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between suggestion buttons, and inside the excerpt. */
    val SmallGap: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside the excerpt box. */
    val ExcerptPadding: Dp = MonarchTokens.Space.space3.dp

    /**
     * The excerpt of [context] shown in the sheet: `null` when there is no
     * non-blank context, otherwise the first [max] characters with trailing
     * spaces removed and an ellipsis when it was cut.
     */
    fun excerpt(context: String?, max: Int = EXCERPT_MAX): String? {
        if (context.isNullOrBlank()) return null
        return if (context.length > max) context.take(max).trimEnd() + "…" else context
    }

    /** The prompt that is sent for [text]: trimmed, or `null` when nothing but spaces was typed. */
    fun prompt(text: CharSequence): String? = text.toString().trim().ifEmpty { null }

    /** The excerpt box: the subtle fill with the AI border, as the web panel's AI edge. */
    fun excerptColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.bgSubtle, content = colors.textDefault, border = colors.aiBorder)

    /** The "Selection" label before the excerpt. */
    fun contextLabelColor(colors: MonarchColors): Color = colors.aiStrong

    /** The excerpt and the pending status. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
