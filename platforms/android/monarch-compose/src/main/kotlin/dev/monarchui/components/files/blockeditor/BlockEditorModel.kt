package dev.monarchui.components.files.blockeditor

import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextSpan
import dev.monarchui.components.files.editortoolbar.MonarchEditorTool
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorBlock as Block
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorTextStyle as Style

/** One line of text split off a block, with its formats and where it started in the old text. */
internal class BlockEditorSegment(val text: String, val spans: List<MonarchRichTextSpan>, val start: Int)

/** The pure document rules of MonarchBlockEditor, tested without a view. */
@Suppress("TooManyFunctions")
internal object BlockEditorModel {
    /** The Markdown-style prefixes that turn a paragraph into another style when a space is typed after them. */
    private val Shortcuts: Map<String, Style> = mapOf(
        "-" to Style.BulletedList,
        "*" to Style.BulletedList,
        "•" to Style.BulletedList,
        "1." to Style.NumberedList,
        "[]" to Style.Checklist,
        "[ ]" to Style.Checklist,
        "#" to Style.Heading1,
        "##" to Style.Heading2,
        "###" to Style.Heading3,
        ">" to Style.Quote,
        "```" to Style.Code,
    )

    /** The style a typed prefix such as "##" stands for, or `null`. */
    fun shortcut(prefix: String): Style? = Shortcuts[prefix]

    /** The text of a block that has text: the summary of a toggle. `null` for a table or an embed. */
    fun textOf(block: Block): String? = when (block) {
        is Block.Text -> block.text
        is Block.Callout -> block.text
        is Block.Toggle -> block.text
        is Block.Table, is Block.Embed -> null
    }

    /** The inline formats of a block that has text. */
    fun spansOf(block: Block): List<MonarchRichTextSpan> = when (block) {
        is Block.Text -> block.spans
        is Block.Callout -> block.spans
        is Block.Toggle -> block.spans
        is Block.Table, is Block.Embed -> emptyList()
    }

    /** A copy of [block] with [text] and [spans]; a table or an embed is returned unchanged. */
    fun withText(block: Block, text: String, spans: List<MonarchRichTextSpan>): Block = when (block) {
        is Block.Text -> block.copy(text = text, spans = if (block.style == Style.Code) emptyList() else spans)
        is Block.Callout -> block.copy(text = text, spans = spans)
        is Block.Toggle -> block.copy(text = text, spans = spans)
        is Block.Table, is Block.Embed -> block
    }

    /** The spans of [spans] inside [start] to [end], moved to start at 0. */
    fun slice(spans: List<MonarchRichTextSpan>, start: Int, end: Int): List<MonarchRichTextSpan> =
        spans.mapNotNull { span ->
            val from = maxOf(span.start, start)
            val to = minOf(span.end, end)
            if (from < to) span.copy(start = from - start, end = to - start) else null
        }

    /** [text] cut at each line break into segments with their formats. */
    fun lines(text: String, spans: List<MonarchRichTextSpan>): List<BlockEditorSegment> {
        val out = mutableListOf<BlockEditorSegment>()
        var start = 0
        while (true) {
            val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
            out += BlockEditorSegment(text.substring(start, end), slice(spans, start, end), start)
            if (end == text.length) return out
            start = end + 1
        }
    }

    /** The index of the segment that holds [caret], and the caret inside it. */
    fun caretIn(segments: List<BlockEditorSegment>, caret: Int): Pair<Int, Int> {
        val index = segments.indexOfLast { it.start <= caret }.coerceAtLeast(0)
        val segment = segments[index]
        return index to (caret - segment.start).coerceIn(0, segment.text.length)
    }

    /** [first] followed by [second], with the formats of [second] moved after the text of [first]. */
    fun joined(
        first: String,
        firstSpans: List<MonarchRichTextSpan>,
        second: String,
        secondSpans: List<MonarchRichTextSpan>,
    ): Pair<String, List<MonarchRichTextSpan>> =
        (first + second) to
            (firstSpans + secondSpans.map { it.copy(start = it.start + first.length, end = it.end + first.length) })

    /** The style of the block that Return starts after a block of [style]. */
    fun nextStyle(style: Style): Style = if (style.isList) style else Style.Paragraph

    /** The number of a numbered list item at [index]: one more than the numbered items right before it. */
    fun number(blocks: List<Block>, index: Int): Int {
        var count = 1
        var at = index - 1
        while (at >= 0 && (blocks[at] as? Block.Text)?.style == Style.NumberedList) {
            count++
            at--
        }
        return count
    }

    /** [block] turned into [type], keeping its text where the new type has text. `null` when it cannot be. */
    fun convert(block: Block, type: MonarchBlockEditorBlockType): Block? {
        val text = textOf(block) ?: return null
        val spans = spansOf(block)
        val style = Style.of(type)
        return when {
            style != null -> Block.Text(
                id = block.id,
                style = style,
                text = text,
                spans = if (style == Style.Code) emptyList() else spans,
                checked = (block as? Block.Text)?.checked ?: false,
            )
            type == MonarchBlockEditorBlockType.Callout -> Block.Callout(block.id, text, spans)
            type == MonarchBlockEditorBlockType.Toggle -> Block.Toggle(block.id, text, spans)
            else -> null
        }
    }

    /** A copy of [list] with the item at [from] moved to [to], clamped to the list. */
    fun <T> moved(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices) return list
        val target = to.coerceIn(0, list.lastIndex)
        return list.toMutableList().apply { add(target, removeAt(from)) }
    }

    /** The toolbar tool that shows or sets a text style. */
    fun tool(style: Style): MonarchEditorTool = when (style) {
        Style.Paragraph -> MonarchEditorTool.Paragraph
        Style.Heading1 -> MonarchEditorTool.heading(HEADING_1)
        Style.Heading2 -> MonarchEditorTool.heading(HEADING_2)
        Style.Heading3 -> MonarchEditorTool.heading(HEADING_3)
        Style.BulletedList -> MonarchEditorTool.BulletList
        Style.NumberedList -> MonarchEditorTool.OrderedList
        Style.Checklist -> MonarchEditorTool.TaskList
        Style.Quote -> MonarchEditorTool.Blockquote
        Style.Code -> MonarchEditorTool.CodeBlock
    }

    /** The text style a toolbar tool sets, or `null` for a tool that is not a style. */
    fun style(tool: MonarchEditorTool): Style? = Style.entries.firstOrNull { tool(it) == tool }

    private const val HEADING_1 = 1
    private const val HEADING_2 = 2
    private const val HEADING_3 = 3

    /** The inline format tools the editor applies to the text of a block. */
    val InlineTools: List<MonarchEditorTool> = listOf(
        MonarchEditorTool.Bold,
        MonarchEditorTool.Italic,
        MonarchEditorTool.Strikethrough,
        MonarchEditorTool.Code,
        MonarchEditorTool.Link,
    )
}
