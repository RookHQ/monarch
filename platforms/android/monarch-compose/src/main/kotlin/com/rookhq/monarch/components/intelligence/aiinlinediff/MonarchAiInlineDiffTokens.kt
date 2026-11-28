package com.rookhq.monarch.components.intelligence.aiinlinediff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the colour decisions of MonarchAiInlineDiff. */
internal object MonarchAiInlineDiffTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val ButtonGap: Dp = MonarchTokens.Space.space2.dp
    val OutlineWidth: Dp = MonarchTokens.Space.px.dp
    val OutlinePadding: Dp = MonarchTokens.Space.px.dp

    /** The unchanged text. */
    fun text(colors: MonarchColors): Color = colors.textDefault

    /** The position line, "Change 2 of 5". */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The outline around the active change. */
    fun activeOutline(colors: MonarchColors): Color = colors.aiBorder

    /**
     * The style of removed or added text: the AI diff colours, struck through when removed and underlined when
     * added, so the kind never depends on colour.
     */
    fun span(type: MonarchAiDiffSegmentType, colors: MonarchColors): SpanStyle = when (type) {
        MonarchAiDiffSegmentType.Delete -> SpanStyle(
            color = colors.aiDiffRemoveStrong,
            background = colors.aiDiffRemove,
            textDecoration = TextDecoration.LineThrough,
        )
        MonarchAiDiffSegmentType.Insert -> SpanStyle(
            color = colors.aiDiffAddStrong,
            background = colors.aiDiffAdd,
            textDecoration = TextDecoration.Underline,
        )
        MonarchAiDiffSegmentType.Equal -> SpanStyle(color = colors.textDefault)
    }
}
