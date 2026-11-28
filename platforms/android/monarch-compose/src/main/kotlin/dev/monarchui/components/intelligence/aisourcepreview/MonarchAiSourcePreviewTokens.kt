package dev.monarchui.components.intelligence.aisourcepreview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiSourcePreview. */
internal object MonarchAiSourcePreviewTokens {
    val SectionGap: Dp = MonarchTokens.Space.space4.dp
    val ParagraphGap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space1.dp
    val HeaderGap: Dp = MonarchTokens.Space.space3.dp
    val Padding: Dp = MonarchTokens.Space.space4.dp
    val QuoteBarWidth: Dp = MonarchTokens.Space.space0_5.dp
    val QuoteBarGap: Dp = MonarchTokens.Space.space3.dp

    private val ParagraphBreak = Regex("\\n\\s*\\n")
    private val Space = Regex("\\s+")

    /** One paragraph of the content, split around the highlighted passage. */
    data class Paragraph(val before: String, val mark: String, val after: String) {
        /** Whether the paragraph holds the highlighted passage. */
        val marked: Boolean get() = mark.isNotEmpty()
    }

    /** Where [quote] first appears in [text], ignoring case and differences in white space, or `null`. */
    fun quoteRange(text: String, quote: String): IntRange? {
        val words = quote.trim().split(Space).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val pattern = words.joinToString(separator = "\\s+") { Regex.escape(it) }
        return Regex(pattern, RegexOption.IGNORE_CASE).find(text)?.range
    }

    /**
     * The content as paragraphs, split on blank lines with empty ones dropped,
     * and the first match of [quote] marked in each paragraph it covers.
     */
    fun paragraphs(text: String, quote: String?): List<Paragraph> {
        val range = quote?.let { quoteRange(text, it) }
        val bounds = mutableListOf<IntRange>()
        var start = 0
        ParagraphBreak.findAll(text).forEach { gap ->
            bounds += start until gap.range.first
            start = gap.range.last + 1
        }
        bounds += start until text.length
        return bounds.filter { text.substring(it.first, it.last + 1).isNotBlank() }.map { bound ->
            val end = bound.last + 1
            if (range == null || range.first >= end || range.last + 1 <= bound.first) {
                Paragraph(text.substring(bound.first, end), "", "")
            } else {
                val from = maxOf(range.first, bound.first)
                val to = minOf(range.last + 1, end)
                Paragraph(text.substring(bound.first, from), text.substring(from, to), text.substring(to, end))
            }
        }
    }

    /** Whether the quote is shown in its own block: there is one, and no content or content without it. */
    fun showsQuoteBlock(quote: String?, content: String?): Boolean {
        if (quote.isNullOrBlank()) return false
        return content == null || quoteRange(content, quote) == null
    }

    /** Whether there is nothing to preview: no content and no quote. */
    fun empty(quote: String?, content: String?): Boolean = content.isNullOrEmpty() && quote.isNullOrBlank()

    /** Where to scroll so the highlighted paragraph sits in the middle of the viewport. */
    fun centredScroll(top: Int, height: Int, viewport: Int, maxScroll: Int): Int =
        (top - (viewport - height) / 2).coerceIn(0, maxOf(0, maxScroll))

    /** The fill of the highlighted passage. */
    fun markColor(colors: MonarchColors): Color = colors.aiSubtle

    /** The bar before the quote block. */
    fun quoteBarColor(colors: MonarchColors): Color = colors.aiBorder
}
