package com.rookhq.monarch.components.intelligence.aimarkdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdownBlock as Block
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The colours of the parts of MonarchAiMarkdown that are not running text. */
internal data class MonarchAiMarkdownColors(
    val text: Color,
    val quoteText: Color,
    val quoteBar: Color,
    val marker: Color,
    val codeBackground: Color,
    val linkHost: Color,
    val tableBorder: Color,
    val tableHeader: Color,
    val rule: Color,
)

/**
 * Component tokens and pure logic of MonarchAiMarkdown: the web's type scale
 * and rhythm for answers (small body text with relaxed leading, compact
 * headings), link safety and footnote numbering.
 */
internal object MonarchAiMarkdownTokens {
    /** Space between blocks. */
    val BlockGap: Dp = MonarchTokens.Space.space3.dp

    /** Space above a heading that follows another block. */
    val HeadingGap: Dp = MonarchTokens.Space.space4.dp

    /** Space under a heading. */
    val AfterHeadingGap: Dp = MonarchTokens.Space.space2.dp

    /** Space between the items and the blocks of a tight list. */
    val TightGap: Dp = MonarchTokens.Space.space1.dp

    /** The narrowest list marker column. */
    val MarkerWidth: Dp = MonarchTokens.Space.space6.dp

    /** Space between a list marker and the item. */
    val MarkerGap: Dp = MonarchTokens.Space.space1.dp

    /** The width of the quotation bar. */
    val QuoteBarWidth: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp

    /** Space between the quotation bar and the quoted text. */
    val QuoteIndent: Dp = MonarchTokens.Space.space3.dp

    /** The padding of a table cell. */
    val CellHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val CellVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** A table column never gets wider than this; its text wraps. */
    val MaxCellWidth: Dp = (MonarchTokens.Space.space24 * CELL_WIDTH_FACTOR).dp

    /** The width of table borders and rules. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The size of a footnote reference, relative to the text. */
    const val FOOTNOTE_EM: Float = 0.75f

    /** The size of inline code, relative to the text. */
    const val CODE_EM: Float = 0.9f

    /** The size of the host after a link, relative to the text. */
    const val LINK_HOST_EM: Float = 0.875f

    private const val CELL_WIDTH_FACTOR = 2.5f


    /** Running text: the body face at the small size, relaxed leading, in the default text colour. */
    fun body(typography: MonarchTypography, color: Color): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = MonarchTokens.LineHeight.relaxed.em,
        color = color,
    )

    /**
     * A heading by its Markdown level: large for `#`, the base size for `##`
     * and the small size from `###` on, semibold with snug leading, as the web
     * renders answers so a model's `#` never shouts.
     */
    fun heading(level: Int, typography: MonarchTypography, color: Color): TextStyle = typography.body.copy(
        fontSize = when (level) {
            1 -> MonarchTokens.FontSize.lg
            2 -> MonarchTokens.FontSize.base
            else -> MonarchTokens.FontSize.sm
        }.sp,
        fontWeight = FontWeight(MonarchTokens.FontWeight.semibold),
        lineHeight = MonarchTokens.LineHeight.snug.em,
        letterSpacing = MonarchTokens.LetterSpacing.snug.em,
        color = color,
    )

    /** The colours by role: muted text in quotations, for markers and struck text, subtle borders, muted code fill. */
    fun colors(colors: MonarchColors): MonarchAiMarkdownColors = MonarchAiMarkdownColors(
        text = colors.textDefault,
        quoteText = colors.textMuted,
        quoteBar = colors.borderDefault,
        marker = colors.textMuted,
        codeBackground = colors.bgMuted,
        linkHost = colors.textMuted,
        tableBorder = colors.borderSubtle,
        tableHeader = colors.bgSubtle,
        rule = colors.borderSubtle,
    )

    /**
     * The span styles of inline tokens: links always underlined (so they never
     * depend on colour), inline code in the code face on the muted fill, the
     * link host and struck text in the muted colour, footnote numbers raised.
     */
    fun spanStyles(
        colors: MonarchColors,
        typography: MonarchTypography,
        link: TextLinkStyles,
    ): MonarchAiMarkdownSpanStyles {
        val underline = SpanStyle(textDecoration = TextDecoration.Underline)
        fun SpanStyle?.underlined(): SpanStyle = this?.merge(underline) ?: underline
        return MonarchAiMarkdownSpanStyles(
            link = TextLinkStyles(
                style = link.style.underlined(),
                focusedStyle = link.focusedStyle.underlined(),
                hoveredStyle = link.hoveredStyle.underlined(),
                pressedStyle = link.pressedStyle.underlined(),
            ),
            code = SpanStyle(
                fontFamily = typography.code.fontFamily,
                fontSize = CODE_EM.em,
                background = colors.bgMuted,
            ),
            host = SpanStyle(color = colors.textMuted, fontSize = LINK_HOST_EM.em),
            footnote = SpanStyle(fontSize = FOOTNOTE_EM.em, baselineShift = BaselineShift.Superscript),
            deleted = SpanStyle(color = colors.textMuted, textDecoration = TextDecoration.LineThrough),
        )
    }

    /** The space above [block] after [previous], in a tight list or not. */
    fun gapBefore(block: Block, previous: Block?, tight: Boolean): Dp = when {
        previous == null -> MonarchTokens.Space.space0.dp
        block is Block.Heading -> HeadingGap
        previous is Block.Heading -> AfterHeadingGap
        tight -> TightGap
        else -> BlockGap
    }

    /** The bullet of an unnumbered item: a disc, and a ring for nested lists. */
    fun bullet(depth: Int): String = if (depth % 2 == 0) "•" else "◦"

    /** Whether a paragraph is a single image that may be handed to the app's image slot. */
    fun standaloneImage(block: Block.Paragraph): MonarchAiMarkdownInline.Image? {
        val content = block.content.filterNot { it is MonarchAiMarkdownInline.Text && it.value.isBlank() }
        val image = content.singleOrNull() as? MonarchAiMarkdownInline.Image ?: return null
        return image.takeIf { MonarchAiMarkdownLinks.safeImage(it.src) && it.alt.isNotBlank() }
    }
}
