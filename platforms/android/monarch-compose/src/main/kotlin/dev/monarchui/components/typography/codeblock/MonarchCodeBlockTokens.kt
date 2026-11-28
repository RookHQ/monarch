package dev.monarchui.components.typography.codeblock

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchCodeBlock, built from primitives and roles only. */
internal object MonarchCodeBlockTokens {
    val HeaderPaddingStart: Dp = MonarchTokens.Space.space3.dp
    val HeaderPaddingEnd: Dp = MonarchTokens.Space.space1.dp
    val HeaderPaddingVertical: Dp = MonarchTokens.Space.space1.dp
    val HeaderGap: Dp = MonarchTokens.Space.space1.dp
    val CodePaddingVertical: Dp = MonarchTokens.Space.space2.dp
    val LinePaddingHorizontal: Dp = MonarchTokens.Space.space3.dp
    val NumberGap: Dp = MonarchTokens.Space.space3.dp

    /** Line numbers are padded to at least this many digits, so short samples keep a steady gutter. */
    const val MIN_NUMBER_DIGITS: Int = 3

    /** The fill of the block. */
    fun container(colors: MonarchColors): Color = colors.bgSubtle

    /** The border of the block and the line under the header. */
    fun border(colors: MonarchColors): Color = colors.borderSubtle

    /** The fill of the header. */
    fun header(colors: MonarchColors): Color = colors.bgMuted

    /** The colour of the header text and of line numbers. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The colour of a highlighted run: status "strong" roles keep text contrast on the subtle fill. */
    fun tokenColor(type: MonarchCodeBlockTokenType, colors: MonarchColors): Color = when (type) {
        MonarchCodeBlockTokenType.Plain -> colors.textDefault
        MonarchCodeBlockTokenType.Keyword -> colors.infoStrong
        MonarchCodeBlockTokenType.Str -> colors.successStrong
        MonarchCodeBlockTokenType.Number -> colors.warningStrong
        MonarchCodeBlockTokenType.Comment -> colors.textMuted
        MonarchCodeBlockTokenType.Tag -> colors.dangerStrong
        MonarchCodeBlockTokenType.Attr, MonarchCodeBlockTokenType.Property -> colors.aiStrong
    }

    /** The span of a highlighted run, or null for plain text. Comments are also italic. */
    fun tokenStyle(type: MonarchCodeBlockTokenType, colors: MonarchColors): SpanStyle? = when (type) {
        MonarchCodeBlockTokenType.Plain -> null
        MonarchCodeBlockTokenType.Comment -> SpanStyle(color = tokenColor(type, colors), fontStyle = FontStyle.Italic)
        else -> SpanStyle(color = tokenColor(type, colors))
    }

    /** The fill of a line: the diff fills for added and removed lines, none otherwise. */
    fun lineBackground(kind: MonarchCodeBlockDiffKind?, colors: MonarchColors): Color = when (kind) {
        MonarchCodeBlockDiffKind.Add -> colors.aiDiffAdd
        MonarchCodeBlockDiffKind.Remove -> colors.aiDiffRemove
        else -> Color.Transparent
    }

    /** The text colour of a line: hunk headers use the info role, file headers the muted role. */
    fun lineColor(kind: MonarchCodeBlockDiffKind?, colors: MonarchColors): Color = when (kind) {
        MonarchCodeBlockDiffKind.Hunk -> colors.infoStrong
        MonarchCodeBlockDiffKind.Meta -> colors.textMuted
        else -> colors.textDefault
    }

    /** The weight of a line: file headers are semibold. */
    fun lineWeight(kind: MonarchCodeBlockDiffKind?): FontWeight? =
        if (kind == MonarchCodeBlockDiffKind.Meta) FontWeight.SemiBold else null

    /** The colour of the + or − marker of a diff line. */
    fun markerColor(kind: MonarchCodeBlockDiffKind?, colors: MonarchColors): Color = when (kind) {
        MonarchCodeBlockDiffKind.Add -> colors.aiDiffAddStrong
        MonarchCodeBlockDiffKind.Remove -> colors.aiDiffRemoveStrong
        else -> colors.textMuted
    }
}
