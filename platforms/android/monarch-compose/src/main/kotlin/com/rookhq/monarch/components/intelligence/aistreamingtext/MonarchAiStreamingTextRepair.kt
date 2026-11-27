package com.rookhq.monarch.components.intelligence.aistreamingtext

import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdownBlock
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdownParser

/**
 * Makes the unfinished end of a streaming answer displayable without flashes,
 * a port of the web's `completeStreamingMarkdown`: closes open emphasis and
 * code spans, shows only the text of a link whose target is still arriving,
 * hides a lone trailing `*`, `_`, `~` or backtick, and holds back table rows
 * until the delimiter row makes them a table. Pass only the last block's
 * source; an unclosed code fence already renders as code.
 */
internal object MonarchAiStreamingTextRepair {
    private val LITERAL_START = Regex("^ {0,3}(`{3,}|~{3,}|\\$\\$|\\\\\\[)")
    private val INDENTED = Regex("^( {4}|\\t)")
    private val TABLE_ROW = Regex("^\\s*\\|")
    private val OPEN_LINK_TARGET = Regex("!?\\[([^\\]\\n]*)\\]\\([^)\\s]*$")
    private val OPEN_LINK_AFTER_TEXT = Regex("!?\\[([^\\]\\n^][^\\]\\n]*)\\]$")
    private val OPEN_LINK_TEXT = Regex("!?\\[\\^?[^\\]\\n]*$")
    private val LINK_OPEN = Regex("^!?\\[")
    private val LONE_DELIMITER = Regex("(^|\\s)[*_~`]+$")
    private val ESCAPE_OR_CODE = Regex("\\\\.|`+")
    private val EMPHASIS = Regex("\\\\.|(`+)|([*_~]+)")
    private val SPACE = Regex("\\s")
    private val PUNCTUATION = Regex("[\\p{P}\\p{S}]")
    private val TRAILING_SPACE = Regex("\\s+$")
    private const val MAX_STRIKE = 2

    /** The repaired source of the last block. */
    fun complete(source: String): String {
        val firstLine = source.substringBefore('\n')
        // Code and display maths are not Markdown: leave their source alone.
        if (LITERAL_START.containsMatchIn(firstLine) || INDENTED.containsMatchIn(firstLine)) return source
        var lines = source.split('\n')
        if (MonarchAiMarkdownParser.parse(source).none { it is MonarchAiMarkdownBlock.Table }) {
            // A table header (and a partial delimiter row) that is not yet a table is held back.
            while (lines.isNotEmpty() && TABLE_ROW.containsMatchIn(lines.last())) lines = lines.dropLast(1)
        }
        var s = lines.joinToString("\n")
        // A link or image whose target is still arriving shows its text only.
        s = OPEN_LINK_TARGET.replace(s, "$1")
        s = OPEN_LINK_AFTER_TEXT.replace(s, "$1")
        s = OPEN_LINK_TEXT.replace(s) { m -> if (m.value.startsWith("[^")) "" else m.value.replace(LINK_OPEN, "") }
        // A delimiter run with nothing after it yet.
        s = LONE_DELIMITER.replace(s, "$1")
        val openCode = openCodeRun(s)
        if (openCode > 0) return TRAILING_SPACE.replace(s, "") + "`".repeat(openCode)
        val closers = openEmphasis(s)
        return if (closers.isEmpty()) s else TRAILING_SPACE.replace(s, "") + closers.joinToString("")
    }

    /** The length of the backtick run of a code span left open, or 0. */
    private fun openCodeRun(s: String): Int {
        var open = 0
        for (m in ESCAPE_OR_CODE.findAll(s)) {
            if (m.value[0] != '`') continue
            open = when (open) {
                0 -> m.value.length
                m.value.length -> 0
                else -> open
            }
        }
        return open
    }

    private class Run(val char: Char, val length: Int)

    /** The closing delimiters of emphasis left open, innermost first, with CommonMark flanking rules. */
    @Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
    fun openEmphasis(src: String): List<String> {
        val stack = ArrayList<Run>()
        var codeRun = 0
        var at = 0
        while (at <= src.length) {
            val m = EMPHASIS.find(src, at) ?: break
            val code = m.groups[1]
            val delimiters = m.groups[2]
            at = m.range.last + 1
            if (code != null) {
                codeRun = when (codeRun) {
                    0 -> code.value.length
                    code.value.length -> 0
                    else -> codeRun
                }
                continue
            }
            if (delimiters == null || codeRun != 0) continue
            // A run may mix characters ("*_"); handle the first character's run only.
            val char = delimiters.value[0]
            var length = 1
            while (length < delimiters.value.length && delimiters.value[length] == char) length++
            val start = m.range.first
            at = start + length
            val before = if (start == 0) " " else src[start - 1].toString()
            val after = src.getOrNull(start + length)?.toString() ?: " "
            val spaceBefore = SPACE.matches(before)
            val spaceAfter = SPACE.matches(after)
            val punctBefore = PUNCTUATION.matches(before)
            val punctAfter = PUNCTUATION.matches(after)
            val left = !spaceAfter && (!punctAfter || spaceBefore || punctBefore)
            val right = !spaceBefore && (!punctBefore || spaceAfter || punctAfter)
            val canOpen = if (char == '_') left && (!right || punctBefore) else left
            val canClose = if (char == '_') right && (!left || punctAfter) else right
            if (char == '~' && length > MAX_STRIKE) continue
            val index = if (canClose) stack.indexOfLast { it.char == char } else -1
            if (index >= 0) {
                while (stack.size > index) stack.removeAt(stack.lastIndex)
            } else if (canOpen) {
                stack += Run(char, length)
            }
        }
        return stack.map { it.char.toString().repeat(it.length) }.reversed()
    }
}
