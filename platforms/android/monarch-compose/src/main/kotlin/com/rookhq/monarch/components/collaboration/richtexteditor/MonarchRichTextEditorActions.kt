package com.rookhq.monarch.components.collaboration.richtexteditor

import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorTool
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextEditorLists as Lists
import com.rookhq.monarch.components.collaboration.richtexteditor.MonarchRichTextEditorModel as Model

/** The heading marks in level order. */
private val RichTextHeadings = listOf(
    MonarchRichTextMark.Heading1,
    MonarchRichTextMark.Heading2,
    MonarchRichTextMark.Heading3,
)

/** The inline marks and the toolbar toggles that show them. */
private val RichTextInlineTools = mapOf(
    MonarchRichTextMark.Bold to MonarchEditorTool.Bold,
    MonarchRichTextMark.Italic to MonarchEditorTool.Italic,
    MonarchRichTextMark.Strikethrough to MonarchEditorTool.Strikethrough,
    MonarchRichTextMark.Code to MonarchEditorTool.Code,
    MonarchRichTextMark.Link to MonarchEditorTool.Link,
)

/** The heading mark of a heading tool of level 1 to 3, or `null`. */
internal fun richTextHeadingMark(tool: MonarchEditorTool): MonarchRichTextMark? =
    tool.level?.let { RichTextHeadings.getOrNull(it - 1) }

/** The inline mark of a format tool, or `null`. */
internal fun richTextInlineMark(tool: MonarchEditorTool): MonarchRichTextMark? =
    RichTextInlineTools.entries.firstOrNull { it.value == tool }?.key

/** Whether the editor applies [tool] itself; other built-in tools are left out of its toolbar. */
internal fun richTextHandles(tool: MonarchEditorTool): Boolean =
    tool.id.startsWith("custom:") || richTextInlineMark(tool) != null || richTextHeadingMark(tool) != null ||
        tool in setOf(
            MonarchEditorTool.Undo,
            MonarchEditorTool.Redo,
            MonarchEditorTool.Paragraph,
            MonarchEditorTool.BulletList,
            MonarchEditorTool.OrderedList,
            MonarchEditorTool.TaskList,
            MonarchEditorTool.Blockquote,
        )

/** The formats that apply at the caret or to the whole selection, and the paragraph's style and list. */
internal fun MonarchRichTextEditorState.activeTools(): Set<MonarchEditorTool> {
    val paragraph = Model.paragraph(text, selection.min)
    val style = Model.headingAt(spans, paragraph.first)?.let { mark ->
        MonarchEditorTool.heading(RichTextHeadings.indexOf(mark) + 1)
    }
    val prefix = Lists.listPrefix(text, paragraph)
    val list = when {
        prefix == Lists.BULLET -> MonarchEditorTool.BulletList
        Lists.isOrdered(prefix) -> MonarchEditorTool.OrderedList
        prefix == Lists.TASK || prefix == Lists.TASK_DONE -> MonarchEditorTool.TaskList
        prefix == Lists.QUOTE -> MonarchEditorTool.Blockquote
        else -> null
    }
    return activeMarks().mapNotNullTo(mutableSetOf()) { RichTextInlineTools[it] } +
        listOfNotNull(style ?: MonarchEditorTool.Paragraph, list)
}

/** The inline marks at the caret (pending ones first) or over the whole selection. */
private fun MonarchRichTextEditorState.activeMarks(): Set<MonarchRichTextMark> {
    val start = selection.min
    val end = selection.max
    if (start != end) {
        return RichTextInlineTools.keys.filterTo(mutableSetOf()) { Model.covers(spans, text, it, start, end) }
    }
    val around = Model.marksAt(spans, start)
    val link = if (MonarchRichTextMark.Link in around) setOf(MonarchRichTextMark.Link) else emptySet()
    return (pending ?: around) + link
}

/** Applies a built-in [tool]: undo and redo, an inline format, a text style or a list. */
internal fun MonarchRichTextEditorState.applyTool(tool: MonarchEditorTool) {
    val inline = richTextInlineMark(tool)
    val heading = richTextHeadingMark(tool)
    when {
        tool == MonarchEditorTool.Undo -> undo()
        tool == MonarchEditorTool.Redo -> redo()
        tool == MonarchEditorTool.Link -> Unit
        inline != null -> toggleInline(inline)
        tool == MonarchEditorTool.Paragraph || heading != null -> setHeading(heading)
        tool == MonarchEditorTool.BulletList -> toggleList(Lists.BULLET)
        tool == MonarchEditorTool.OrderedList -> toggleList(null)
        tool == MonarchEditorTool.TaskList -> toggleList(Lists.TASK)
        tool == MonarchEditorTool.Blockquote -> toggleList(Lists.QUOTE)
    }
}

private fun MonarchRichTextEditorState.toggleInline(mark: MonarchRichTextMark) {
    val start = selection.min
    val end = selection.max
    if (start == end) {
        val current = pending ?: Model.marksAt(spans, start).filter { it in Model.Inline }.toSet()
        updatePending(if (mark in current) current - mark else current + mark)
        return
    }
    val removing = Model.covers(spans, text, mark, start, end)
    val without = Model.remove(spans, mark, start, end)
    updateSpans(if (removing) without else without + MonarchRichTextSpan(mark, start, end))
}

private fun MonarchRichTextEditorState.setHeading(mark: MonarchRichTextMark?) {
    val paragraphs = Model.paragraphs(text, selection.min, selection.max)
    val starts = paragraphs.map { it.first }.toSet()
    val kept = spans.filter { !(it.mark.isHeading && it.start in starts) }
    val added = if (mark == null) emptyList() else paragraphs.map { MonarchRichTextSpan(mark, it.first, it.last + 1) }
    updateSpans(kept + added)
}

/** Toggles a prefix on the selected paragraphs: [fixed] for a bullet, task or quote, `null` for numbers. */
private fun MonarchRichTextEditorState.toggleList(fixed: String?) {
    val paragraphs = Model.paragraphs(text, selection.min, selection.max)
    val prefixes = paragraphs.map { Lists.listPrefix(text, it) }
    val all = prefixes.all {
        when (fixed) {
            null -> Lists.isOrdered(it)
            Lists.TASK -> it == Lists.TASK || it == Lists.TASK_DONE
            else -> it == fixed
        }
    }
    var first = true
    paragraphs.indices.reversed().forEach { index ->
        val start = paragraphs[index].first
        val existing = prefixes[index]?.length ?: 0
        val prefix = when {
            all -> ""
            fixed == null -> "${index + 1}. "
            else -> fixed
        }
        replace(start, start + existing, prefix, record = first)
        first = false
    }
}

/**
 * Applies the link form's address to the selection, or to the link at the
 * caret; with no selection and no link, writes the address as a link.
 * `null` removes the link.
 */
internal fun MonarchRichTextEditorState.applyLink(url: String?) {
    val start = selection.min
    val end = selection.max
    val around = spans.firstOrNull { it.mark == MonarchRichTextMark.Link && it.start < start && start <= it.end }
    val (from, to) = when {
        start != end -> start to end
        around != null -> around.start to around.end
        else -> start to start
    }
    val without = Model.remove(spans, MonarchRichTextMark.Link, from, to)
    when {
        url == null -> updateSpans(without)
        from == to -> {
            replace(from, to, url)
            val link = MonarchRichTextSpan(MonarchRichTextMark.Link, from, from + url.length, url)
            updateSpans(Model.remove(spans, MonarchRichTextMark.Link, link.start, link.end) + link, record = false)
        }
        else -> updateSpans(without + MonarchRichTextSpan(MonarchRichTextMark.Link, from, to, url))
    }
}
