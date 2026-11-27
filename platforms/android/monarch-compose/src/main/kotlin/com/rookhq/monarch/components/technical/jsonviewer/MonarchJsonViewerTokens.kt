package com.rookhq.monarch.components.technical.jsonviewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure colour rules of MonarchJsonViewer. */
internal object MonarchJsonViewerTokens {
    /** The indentation of each level. */
    val Indent: Dp = MonarchTokens.Space.space4.dp

    /** The width of the chevron column. */
    val Chevron: Dp = MonarchTokens.Space.space6.dp

    /** The padding at the start and end of a row. */
    val RowHorizontal: Dp = MonarchTokens.Space.space2.dp

    /** The padding above and below a row's text. */
    val RowVertical: Dp = MonarchTokens.Space.space1.dp

    /** The padding inside the toolbar and the footer, and the gap between their items. */
    val Bar: Dp = MonarchTokens.Space.space2.dp

    /** A quarter turn of the chevron while its container is open. */
    const val OpenRotation: Float = 90f

    /** The fill of the viewer. */
    fun surface(colors: MonarchColors): Color = colors.bgSurface

    /** The border of the viewer and the lines between its parts. */
    fun border(colors: MonarchColors): Color = colors.borderSubtle

    /** The fill of a row: the muted background when selected, the surface otherwise. */
    fun row(selected: Boolean, colors: MonarchColors): Color = if (selected) colors.bgMuted else colors.bgSurface

    /** The fill behind matched text, which is also bold. */
    fun match(colors: MonarchColors): Color = colors.warningSubtle

    /** Punctuation, the chevron, the path and the match count: muted text. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The "Show more" row: the link colour. */
    fun more(colors: MonarchColors): Color = colors.textLink

    /** The colour of a key: array indexes are muted, object keys the strong info colour. */
    fun key(segment: MonarchJsonViewerSegment, colors: MonarchColors): Color =
        if (segment is MonarchJsonViewerSegment.Index) colors.textMuted else colors.infoStrong

    /**
     * The colour of a value by its kind. The kind is also shown by its punctuation (quotes, braces, brackets), not by
     * colour alone.
     */
    fun value(value: MonarchJsonValue, colors: MonarchColors): Color = when (value) {
        is MonarchJsonValue.Object, is MonarchJsonValue.Array -> colors.textMuted
        is MonarchJsonValue.String -> colors.successStrong
        is MonarchJsonValue.Number -> colors.accentStrong
        is MonarchJsonValue.Bool, MonarchJsonValue.Null -> colors.warningStrong
    }
}
