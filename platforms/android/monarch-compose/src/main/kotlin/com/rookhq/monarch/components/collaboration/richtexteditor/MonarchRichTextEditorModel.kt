package com.rookhq.monarch.components.collaboration.richtexteditor

import kotlin.math.max
import kotlin.math.min

/** One change of the text: [removed] characters at [start] replaced by [inserted] characters. */
internal data class RichTextEdit(val start: Int, val removed: Int, val inserted: Int) {
    /** The index after the replaced characters, in the old text. */
    val end: Int get() = start + removed

    /** Where an index of the old text is after the edit; removed characters go to the end of the insertion. */
    fun map(index: Int): Int = when {
        index < start -> index
        index >= end && index > start -> index + inserted - removed
        index == start && removed == 0 -> index + inserted
        else -> start + inserted
    }
}

/**
 * The editing model of MonarchRichTextEditor: plain text plus spans, kept in
 * step with every edit. Pure functions, so the rules are tested on their own.
 */
internal object MonarchRichTextEditorModel {
    /** The inline marks that typing at their end continues. */
    val Continuing: Set<MonarchRichTextMark> = setOf(
        MonarchRichTextMark.Bold,
        MonarchRichTextMark.Italic,
        MonarchRichTextMark.Strikethrough,
        MonarchRichTextMark.Code,
    )

    /** The marks pending formats can turn on and off at the caret. */
    val Inline: Set<MonarchRichTextMark> get() = Continuing

    /**
     * The edit that turns [old] into [new], placed so that it ends at or after
     * [caret] in [new], which puts a repeated letter where it was typed.
     */
    fun diff(old: String, new: String, caret: Int): RichTextEdit? {
        if (old == new) return null
        val common = min(old.length, new.length)
        val suffixLimit = min(common, max(0, new.length - caret))
        var suffix = 0
        while (suffix < suffixLimit && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++
        var prefix = 0
        while (prefix < common - suffix && old[prefix] == new[prefix]) prefix++
        return RichTextEdit(prefix, old.length - prefix - suffix, new.length - prefix - suffix)
    }

    /** The paragraph around [index]: from after the previous line break to the next one. */
    fun paragraph(text: String, index: Int): IntRange {
        val at = index.coerceIn(0, text.length)
        val start = if (at == 0) 0 else text.lastIndexOf('\n', at - 1) + 1
        val end = text.indexOf('\n', at).let { if (it < 0) text.length else it }
        return start until end
    }

    /** The paragraphs that a selection from [start] to [end] touches, first to last. */
    fun paragraphs(text: String, start: Int, end: Int): List<IntRange> {
        val out = mutableListOf(paragraph(text, start))
        while (out.last().last + 2 <= end && out.last().last + 1 < text.length) {
            out += paragraph(text, out.last().last + 2)
        }
        return out
    }

    /** [spans] with [mark] removed from [start] to [end]; spans that cross the range are split. */
    fun remove(
        spans: List<MonarchRichTextSpan>,
        mark: MonarchRichTextMark,
        start: Int,
        end: Int,
    ): List<MonarchRichTextSpan> = spans.flatMap { span ->
        if (span.mark != mark || span.end <= start || span.start >= end) {
            listOf(span)
        } else {
            listOfNotNull(
                span.copy(end = start).takeIf { span.start < start },
                span.copy(start = end).takeIf { span.end > end },
            )
        }
    }

    /**
     * [spans] made tidy for [text]: inline spans clamped, without empty ones,
     * and joined where they touch; headings cover whole paragraphs, one per
     * paragraph, and a heading that no longer starts its paragraph is dropped.
     */
    fun normalize(spans: List<MonarchRichTextSpan>, text: String): List<MonarchRichTextSpan> {
        val inline = spans.filter { !it.mark.isHeading }
            .map { it.copy(start = it.start.coerceIn(0, text.length), end = it.end.coerceIn(0, text.length)) }
            .filter { it.end > it.start }
            .groupBy { it.mark to it.url }
            .flatMap { (_, group) -> merge(group) }
        val headings = spans.filter { it.mark.isHeading }
            .flatMap { headingParagraphs(it, text) }
            .distinctBy { it.start }
        return (inline + headings).sortedWith(compareBy({ it.start }, { it.mark.ordinal }))
    }

    private fun merge(group: List<MonarchRichTextSpan>): List<MonarchRichTextSpan> {
        val out = mutableListOf<MonarchRichTextSpan>()
        group.sortedBy { it.start }.forEach { span ->
            val last = out.lastOrNull()
            if (last != null && span.start <= last.end) {
                out[out.lastIndex] = last.copy(end = max(last.end, span.end))
            } else {
                out += span
            }
        }
        return out
    }

    private fun headingParagraphs(span: MonarchRichTextSpan, text: String): List<MonarchRichTextSpan> {
        val start = span.start.coerceIn(0, text.length)
        val end = span.end.coerceIn(start, text.length)
        val first = paragraph(text, start)
        val out = mutableListOf<MonarchRichTextSpan>()
        if (start == first.first) out += span.copy(start = first.first, end = first.last + 1)
        var next = first.last + 2
        while (next < end) {
            val range = paragraph(text, next)
            out += span.copy(start = range.first, end = range.last + 1)
            next = range.last + 2
        }
        return out
    }

    /** Whether [mark] covers every character from [start] to [end] that is not a line break. */
    fun covers(
        spans: List<MonarchRichTextSpan>,
        text: String,
        mark: MonarchRichTextMark,
        start: Int,
        end: Int,
    ): Boolean {
        val indices = (start until min(end, text.length)).filter { text[it] != '\n' }
        return indices.isNotEmpty() && indices.all { i -> spans.any { it.mark == mark && i >= it.start && i < it.end } }
    }

    /** The marks that apply to the character before [caret], which typing there continues. */
    fun marksAt(spans: List<MonarchRichTextSpan>, caret: Int): Set<MonarchRichTextMark> =
        spans.filter { !it.mark.isHeading && it.start < caret && caret <= it.end }.mapTo(mutableSetOf()) { it.mark }

    /** The heading of the paragraph that starts at [paragraphStart], or `null` for body text. */
    fun headingAt(spans: List<MonarchRichTextSpan>, paragraphStart: Int): MonarchRichTextMark? =
        spans.firstOrNull { it.mark.isHeading && it.start == paragraphStart }?.mark
}
