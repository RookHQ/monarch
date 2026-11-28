package dev.monarchui.components.search.searchresultitem

import androidx.compose.ui.text.TextRange

/**
 * Splits a title or snippet into matched and unmatched runs. Pure and
 * string-only: the text is never parsed as markup, as on the web.
 */
internal object MonarchSearchResultItemHighlight {
    /** One run of text and whether it matched. */
    data class Segment(val text: String, val match: Boolean)

    private val MarkTag = Regex("</?mark>", RegexOption.IGNORE_CASE)

    /**
     * The runs of [text]. [markTags] reads `<mark>` and `</mark>` markers and nothing else; otherwise
     * [ranges] (UTF-16 offsets, end exclusive) are used when not empty, and [terms] (case-insensitive)
     * otherwise.
     */
    fun segments(
        text: String,
        terms: List<String> = emptyList(),
        ranges: List<TextRange>? = null,
        markTags: Boolean = false,
    ): List<Segment> = when {
        text.isEmpty() -> emptyList()
        markTags -> fromMarkTags(text)
        !ranges.isNullOrEmpty() -> fromRanges(text, ranges.map { it.min to it.max })
        else -> fromTerms(text, terms)
    }

    private fun fromMarkTags(text: String): List<Segment> {
        val out = mutableListOf<Segment>()
        var match = false
        var last = 0
        for (tag in MarkTag.findAll(text)) {
            if (tag.range.first > last) out += Segment(text.substring(last, tag.range.first), match)
            match = !tag.value.startsWith("</")
            last = tag.range.last + 1
        }
        if (last < text.length) out += Segment(text.substring(last), match)
        return out
    }

    private fun fromTerms(text: String, terms: List<String>): List<Segment> {
        val words = terms.map { it.trim() }.filter { it.isNotEmpty() }
        if (words.isEmpty()) return listOf(Segment(text, false))
        val pattern = Regex(
            words.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) },
            RegexOption.IGNORE_CASE,
        )
        return fromRanges(text, pattern.findAll(text).map { it.range.first to it.range.last + 1 }.toList())
    }

    /** Merges overlapping ranges and clamps them to the text. */
    private fun merge(ranges: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        for ((start, end) in ranges.filter { it.second > it.first }.sortedBy { it.first }) {
            val previous = out.lastOrNull()
            if (previous != null && start <= previous.second) {
                out[out.lastIndex] = previous.first to maxOf(previous.second, end)
            } else {
                out += start to end
            }
        }
        return out
    }

    private fun fromRanges(text: String, ranges: List<Pair<Int, Int>>): List<Segment> {
        val out = mutableListOf<Segment>()
        var last = 0
        for ((rangeStart, rangeEnd) in merge(ranges)) {
            val start = maxOf(last, minOf(rangeStart, text.length))
            val end = minOf(rangeEnd, text.length)
            if (end <= start) continue
            if (start > last) out += Segment(text.substring(last, start), false)
            out += Segment(text.substring(start, end), true)
            last = end
        }
        if (last < text.length) out += Segment(text.substring(last), false)
        return out
    }
}
