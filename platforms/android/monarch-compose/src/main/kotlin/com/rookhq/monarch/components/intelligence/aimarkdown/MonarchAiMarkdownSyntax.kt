package com.rookhq.monarch.components.intelligence.aimarkdown

/** An inline token inside a paragraph, heading, table cell or link, as the web parser produces it. */
internal sealed interface MonarchAiMarkdownInline {
    /** Plain text. A soft line break inside a paragraph is kept as `\n`. */
    data class Text(val value: String) : MonarchAiMarkdownInline

    /** An inline code span. */
    data class Code(val value: String) : MonarchAiMarkdownInline

    /** Inline maths source written as `\(…\)` or `$$…$$` inside a line. */
    data class Math(val value: String) : MonarchAiMarkdownInline

    /** Emphasis, strong emphasis or strikethrough around [children]. */
    data class Styled(val kind: MonarchAiMarkdownEmphasis, val children: List<MonarchAiMarkdownInline>) :
        MonarchAiMarkdownInline

    /** A link. [autolink] is true for `<https://…>` and bare `https://…` or `www.…` links. */
    data class Link(
        val href: String,
        val title: String?,
        val autolink: Boolean,
        val children: List<MonarchAiMarkdownInline>,
    ) : MonarchAiMarkdownInline

    /** An image; shown as its alt text unless the app renders images. */
    data class Image(val src: String, val alt: String, val title: String?) : MonarchAiMarkdownInline

    /** A hard line break. */
    data object Break : MonarchAiMarkdownInline

    /** A footnote reference, `[^label]`, with the label lower-cased. */
    data class FootnoteRef(val label: String) : MonarchAiMarkdownInline
}

/** The three kinds of delimited inline style. */
internal enum class MonarchAiMarkdownEmphasis { Em, Strong, Del }

/** The alignment of a table column from its delimiter row. `null` in a list means none. */
internal enum class MonarchAiMarkdownAlign { Left, Center, Right }

/** One list item: `checked` is `true` or `false` for a task item, `null` otherwise. */
internal data class MonarchAiMarkdownListItem(val checked: Boolean?, val children: List<MonarchAiMarkdownBlock>)

/** A block token. Top-level blocks are independent of the ones after them. */
internal sealed interface MonarchAiMarkdownBlock {
    /** The source lines of the block, as written (container markers removed when nested). */
    val raw: String

    data class Heading(val level: Int, val content: List<MonarchAiMarkdownInline>, override val raw: String) :
        MonarchAiMarkdownBlock

    data class Paragraph(val content: List<MonarchAiMarkdownInline>, override val raw: String) :
        MonarchAiMarkdownBlock

    /**
     * Fenced, indented or display-maths code. [lang] is the first word of the
     * info string, lower-cased, or `math` for display maths; [closed] is false
     * while the closing fence has not arrived.
     */
    data class Code(
        val lang: String,
        val info: String,
        val filename: String?,
        val code: String,
        val closed: Boolean,
        override val raw: String,
    ) : MonarchAiMarkdownBlock

    data class Quote(val children: List<MonarchAiMarkdownBlock>, override val raw: String) : MonarchAiMarkdownBlock

    /** A list. A loose list separates its items' paragraphs; a tight one does not. */
    data class ListBlock(
        val ordered: Boolean,
        val start: Int,
        val loose: Boolean,
        val items: List<MonarchAiMarkdownListItem>,
        override val raw: String,
    ) : MonarchAiMarkdownBlock

    data class Table(
        val align: List<MonarchAiMarkdownAlign?>,
        val header: List<List<MonarchAiMarkdownInline>>,
        val rows: List<List<List<MonarchAiMarkdownInline>>>,
        override val raw: String,
    ) : MonarchAiMarkdownBlock

    data class Rule(override val raw: String) : MonarchAiMarkdownBlock

    data class FootnoteDefinition(
        val label: String,
        val children: List<MonarchAiMarkdownBlock>,
        override val raw: String,
    ) : MonarchAiMarkdownBlock
}

/** The result of an incremental parse; passed back in on the next update. */
internal class MonarchAiMarkdownParseState(
    val text: String,
    val blocks: List<MonarchAiMarkdownBlock>,
    /** Character offset of each block's first line. */
    val offsets: List<Int>,
)
