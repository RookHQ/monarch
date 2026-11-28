package dev.monarchui.components.intelligence.aimarkdown

import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownBlock as Block

/**
 * The block parser of MonarchAiMarkdown: a line-for-line port of the web
 * `ai-markdown.parser.ts`, so both platforms read model output the same way.
 *
 * Supported: ATX and setext headings, paragraphs, ordered, unordered, nested
 * and task lists, blockquotes, fenced and indented code, tables with
 * alignment, thematic breaks, footnotes and display maths (`$$` or `\[` on
 * their own line, kept as a `math` code block). Not supported, as on the web:
 * link reference definitions and HTML blocks, which stay text.
 */
internal object MonarchAiMarkdownParser {
    private const val TAB_STOP = 4
    private const val CODE_INDENT = 4
    private const val THIRD_GROUP = 3

    private val FENCE_OPEN = Regex("^( {0,3})(`{3,}|~{3,})(.*)$")
    private val ATX = Regex("^ {0,3}(#{1,6})(?:[ \\t]+(.*?))?[ \\t]*$")
    private val ATX_CLOSING = Regex("(?:^|[ \\t]+)#+$")
    private val HR = Regex("^ {0,3}(?:(?:\\*[ \\t]*){3,}|(?:-[ \\t]*){3,}|(?:_[ \\t]*){3,})$")
    private val QUOTE = Regex("^ {0,3}> ?")
    private val FOOTNOTE_DEF = Regex("^ {0,3}\\[\\^([^\\]\\s]+)\\]:[ \\t]*(.*)$")
    private val SETEXT_1 = Regex("^ {0,3}=+[ \\t]*$")
    private val SETEXT_2 = Regex("^ {0,3}-+[ \\t]*$")
    private val TABLE_DELIM = Regex("^ {0,3}\\|?[ \\t]*:?-+:?[ \\t]*(?:\\|[ \\t]*:?-+:?[ \\t]*)*\\|?[ \\t]*$")
    private val MARKER = Regex("^( {0,3})([-+*]|\\d{1,9}[.)])(?=[ \\t]|$)")
    private val MATH_OPEN = Regex("^ {0,3}(\\$\\$|\\\\\\[)(.*)$")
    private val UNESCAPED_PIPE = Regex("(^|[^\\\\])\\|")
    private val TASK = Regex("^\\[([ xX])\\](?:[ \\t]+|$)")
    private val WHITESPACE = Regex("\\s+")
    private val CODE_FILENAME = Regex("(?:title|filename|file)=(?:\"([^\"]*)\"|'([^']*)'|(\\S+))")
    private val CODE_COLON = Regex("^([\\w+#-]+):(\\S+)$")

    /** Parses Markdown into top-level block tokens. */
    fun parse(text: String): List<Block> = parseLines(splitLines(text).first).blocks

    /**
     * Re-parses only what can have changed. When [text] extends the previous
     * text, every block before the last is kept as is (the same object) and
     * parsing restarts at the last block's first line. Anything else is a
     * full parse.
     */
    @Suppress("ComplexCondition")
    fun parseIncremental(text: String, previous: MonarchAiMarkdownParseState?): MonarchAiMarkdownParseState {
        if (previous != null && previous.text == text) return previous
        var keep = 0
        var from = 0
        if (previous != null && previous.blocks.size > 1 && text.startsWith(previous.text) &&
            !previous.text.contains('\r')
        ) {
            keep = previous.blocks.size - 1
            from = previous.offsets[keep]
        }
        val (lines, offsets) = splitLines(text.substring(from))
        val parsed = parseLines(lines)
        return MonarchAiMarkdownParseState(
            text = text,
            blocks = previous?.blocks.orEmpty().take(keep) + parsed.blocks,
            offsets = previous?.offsets.orEmpty().take(keep) + parsed.starts.map { from + offsets[it] },
        )
    }

    /* Lines and indentation */

    private fun splitLines(text: String): Pair<List<String>, List<Int>> {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val offsets = ArrayList<Int>(lines.size)
        var at = 0
        for (line in lines) {
            offsets += at
            at += line.length + 1
        }
        return lines to offsets
    }

    fun isBlank(line: String): Boolean = line.all { it == ' ' || it == '\t' }

    /** Indentation in columns; a tab advances to the next multiple of 4. */
    fun indentOf(line: String): Int {
        var col = 0
        for (ch in line) {
            when (ch) {
                ' ' -> col += 1
                '\t' -> col += TAB_STOP - col % TAB_STOP
                else -> return col
            }
        }
        return col
    }

    /** Removes up to [cols] columns of leading white space, splitting a tab if needed. */
    fun stripCols(line: String, cols: Int): String {
        var col = 0
        var i = 0
        while (i < line.length && col < cols) {
            val ch = line[i]
            if (ch == ' ') {
                col += 1
            } else if (ch == '\t') {
                val width = TAB_STOP - col % TAB_STOP
                if (col + width > cols) return " ".repeat(col + width - cols) + line.substring(i + 1)
                col += width
            } else {
                break
            }
            i += 1
        }
        return line.substring(i)
    }

    /* Block patterns */

    private class MathOpen(val close: String, val single: String?)

    private fun mathOpen(line: String): MathOpen? {
        val m = MATH_OPEN.find(line) ?: return null
        val close = if (m.groupValues[1] == "$$") "$$" else "\\]"
        val rest = m.groupValues[2].trim()
        return when {
            rest.isEmpty() -> MathOpen(close, null)
            rest.length > close.length && rest.endsWith(close) -> MathOpen(close, rest.dropLast(close.length).trim())
            else -> null
        }
    }

    private class Fence(val indent: Int, val fence: String, val info: String)

    private fun fenceOpen(line: String): Fence? {
        val m = FENCE_OPEN.find(line) ?: return null
        val fence = m.groupValues[2]
        val info = m.groupValues[THIRD_GROUP]
        // A backtick fence's info string may not contain a backtick (that is inline code).
        if (fence[0] == '`' && info.contains('`')) return null
        return Fence(m.groupValues[1].length, fence, info.trim())
    }

    private class Marker(
        val ordered: Boolean,
        val key: String,
        val start: Int,
        val contentCol: Int,
        val first: String,
        val empty: Boolean,
    )

    private fun listMarker(line: String): Marker? {
        val m = MARKER.find(line) ?: return null
        val lead = m.groupValues[1].length
        val marker = m.groupValues[2]
        val markerEnd = lead + marker.length
        val after = line.substring(m.value.length)
        val padded = " ".repeat(markerEnd) + after
        val spaces = indentOf(padded) - markerEnd
        val empty = isBlank(after)
        val contentCol = if (empty || spaces > CODE_INDENT) markerEnd + 1 else markerEnd + spaces
        val ordered = marker[0] in '0'..'9'
        return Marker(
            ordered = ordered,
            key = if (ordered) marker.takeLast(1) else marker,
            start = if (ordered) marker.dropLast(1).toInt() else 1,
            contentCol = contentCol,
            first = if (empty) "" else stripCols(padded, contentCol),
            empty = empty,
        )
    }

    /** True when [line] ends a paragraph by starting another block. */
    @Suppress("ComplexCondition", "ReturnCount")
    private fun interruptsParagraph(line: String): Boolean {
        if (fenceOpen(line) != null || mathOpen(line) != null || ATX.containsMatchIn(line) ||
            HR.containsMatchIn(line) || QUOTE.containsMatchIn(line)
        ) {
            return true
        }
        if (FOOTNOTE_DEF.containsMatchIn(line)) return true
        val mk = listMarker(line) ?: return false
        // Only a non-empty item interrupts a paragraph; an ordered one must start at 1.
        return !mk.empty && (!mk.ordered || mk.start == 1)
    }

    /** Splits a table row into raw cell strings, honouring `\|` and code spans. */
    fun splitRow(line: String): List<String> {
        var s = line.trim()
        if (s.startsWith('|')) s = s.substring(1)
        if (s.endsWith('|') && !s.endsWith("\\|")) s = s.dropLast(1)
        val cells = ArrayList<String>()
        val cell = StringBuilder()
        var tick = 0
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == '\\' && s.getOrNull(i + 1) == '|') {
                cell.append('|')
                i += 1
            } else if (ch == '`') {
                var run = 1
                while (s.getOrNull(i + run) == '`') run += 1
                if (tick == 0) tick = run else if (tick == run) tick = 0
                cell.append("`".repeat(run))
                i += run - 1
            } else if (ch == '|' && tick == 0) {
                cells += cell.toString().trim()
                cell.clear()
            } else {
                cell.append(ch)
            }
            i += 1
        }
        cells += cell.toString().trim()
        return cells
    }

    @Suppress("ReturnCount")
    private fun tableStart(lines: List<String>, i: Int): List<MonarchAiMarkdownAlign?>? {
        val head = lines.getOrNull(i) ?: return null
        val delim = lines.getOrNull(i + 1) ?: return null
        if (!UNESCAPED_PIPE.containsMatchIn(head) || !TABLE_DELIM.containsMatchIn(delim)) return null
        val cells = splitRow(delim)
        if (cells.size != splitRow(head).size) return null
        return cells.map { c ->
            val left = c.startsWith(':')
            val right = c.endsWith(':')
            when {
                left && right -> MonarchAiMarkdownAlign.Center
                left -> MonarchAiMarkdownAlign.Left
                right -> MonarchAiMarkdownAlign.Right
                else -> null
            }
        }
    }

    /** The language and file name of a fence's info string ("ts title=app.ts", "ts:app.ts"). */
    fun codeInfo(info: String): Pair<String, String?> {
        val first = info.split(WHITESPACE)[0]
        val lang = first.lowercase()
        val m = CODE_FILENAME.find(info)
        val filename = m?.let { it.groups[1]?.value ?: it.groups[2]?.value ?: it.groups[THIRD_GROUP]?.value }
        val colon = CODE_COLON.find(first)
        if (colon != null && filename == null) return colon.groupValues[1].lowercase() to colon.groupValues[2]
        return lang to filename
    }

    /* Block parser */

    private class Parsed {
        val blocks = ArrayList<Block>()
        val starts = ArrayList<Int>()
        val ends = ArrayList<Int>()

        fun push(block: Block, start: Int, end: Int) {
            blocks += block
            starts += start
            ends += end
        }
    }

    private fun raw(lines: List<String>, start: Int, end: Int): String = lines.subList(start, end).joinToString("\n")

    // A line-for-line port of the web parser: the structure is kept so the two can be compared.
    @Suppress(
        "CyclomaticComplexMethod",
        "LongMethod",
        "NestedBlockDepth",
        "ComplexCondition",
        "LoopWithTooManyJumpStatements",
    )
    private fun parseLines(lines: List<String>): Parsed {
        val out = Parsed()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (isBlank(line)) {
                i += 1
                continue
            }
            val start = i

            val math = mathOpen(line)
            if (math != null) {
                val body = ArrayList<String>()
                var closed = math.single != null
                i += 1
                if (math.single != null) {
                    body += math.single
                } else {
                    while (i < lines.size) {
                        val l = lines[i]
                        i += 1
                        if (l.trim() == math.close) {
                            closed = true
                            break
                        }
                        body += l
                    }
                }
                val source = body.joinToString("\n")
                out.push(Block.Code("math", "math", null, source, closed, raw(lines, start, i)), start, i)
                continue
            }

            // Fenced code. An unclosed fence runs to the end, so a half-streamed block already renders as code.
            val fence = fenceOpen(line)
            if (fence != null) {
                val char = if (fence.fence[0] == '`') "`" else "~"
                val close = Regex("^ {0,3}$char{${fence.fence.length},}[ \\t]*$")
                val body = ArrayList<String>()
                var closed = false
                i += 1
                while (i < lines.size) {
                    val l = lines[i]
                    i += 1
                    if (close.containsMatchIn(l)) {
                        closed = true
                        break
                    }
                    body += stripCols(l, fence.indent)
                }
                val (lang, filename) = codeInfo(fence.info)
                out.push(
                    Block.Code(lang, fence.info, filename, body.joinToString("\n"), closed, raw(lines, start, i)),
                    start,
                    i,
                )
                continue
            }

            // Indented code.
            if (indentOf(line) >= CODE_INDENT) {
                var end = i
                while (end < lines.size && (isBlank(lines[end]) || indentOf(lines[end]) >= CODE_INDENT)) end += 1
                while (end > i && isBlank(lines[end - 1])) end -= 1
                val code = lines.subList(i, end).joinToString("\n") { stripCols(it, CODE_INDENT) }
                out.push(Block.Code("", "", null, code, true, raw(lines, i, end)), i, end)
                i = end
                continue
            }

            val atx = ATX.find(line)
            if (atx != null) {
                val text = atx.groupValues[2].replace(ATX_CLOSING, "").trim()
                val level = atx.groupValues[1].length
                i += 1
                val content = MonarchAiMarkdownInlineParser.parse(text)
                out.push(Block.Heading(level, content, raw(lines, start, i)), start, i)
                continue
            }

            if (HR.containsMatchIn(line)) {
                i += 1
                out.push(Block.Rule(line), start, i)
                continue
            }

            if (QUOTE.containsMatchIn(line)) {
                val inner = ArrayList<String>()
                while (i < lines.size) {
                    val l = lines[i]
                    if (QUOTE.containsMatchIn(l)) {
                        inner += l.replaceFirst(QUOTE, "")
                    } else if (!isBlank(l) && inner.isNotEmpty() && !isBlank(inner.last()) && !interruptsParagraph(l)) {
                        inner += l // lazy continuation
                    } else {
                        break
                    }
                    i += 1
                }
                out.push(Block.Quote(parseLines(inner).blocks, raw(lines, start, i)), start, i)
                continue
            }

            val fn = FOOTNOTE_DEF.find(line)
            if (fn != null) {
                val inner = arrayListOf(fn.groupValues[2])
                i += 1
                while (i < lines.size) {
                    val l = lines[i]
                    if (isBlank(l)) {
                        val next = lines.subList(i, lines.size).firstOrNull { !isBlank(it) }
                        if (next == null || indentOf(next) < CODE_INDENT) break
                        inner += ""
                    } else if (indentOf(l) >= CODE_INDENT) {
                        inner += stripCols(l, CODE_INDENT)
                    } else if (!isBlank(inner.last()) && !interruptsParagraph(l)) {
                        inner += l
                    } else {
                        break
                    }
                    i += 1
                }
                val label = normalizeLabel(fn.groupValues[1])
                out.push(Block.FootnoteDefinition(label, parseLines(inner).blocks, raw(lines, start, i)), start, i)
                continue
            }

            val marker = listMarker(line)
            if (marker != null) {
                val (end, list) = parseList(lines, i, marker)
                i = end
                out.push(list.copy(raw = raw(lines, start, i)), start, i)
                continue
            }

            val align = tableStart(lines, i)
            if (align != null) {
                val header = splitRow(line).map { MonarchAiMarkdownInlineParser.parse(it) }
                val rows = ArrayList<List<List<MonarchAiMarkdownInline>>>()
                i += 2
                while (i < lines.size) {
                    val l = lines[i]
                    if (isBlank(l) || interruptsParagraph(l)) break
                    val cells = splitRow(l)
                    rows += align.indices.map { c -> MonarchAiMarkdownInlineParser.parse(cells.getOrElse(c) { "" }) }
                    i += 1
                }
                out.push(Block.Table(align, header, rows, raw(lines, start, i)), start, i)
                continue
            }

            // Paragraph, possibly turned into a setext heading.
            val para = arrayListOf(line)
            var level: Int? = null
            i += 1
            while (i < lines.size) {
                val l = lines[i]
                if (isBlank(l)) break
                if (SETEXT_1.containsMatchIn(l) || SETEXT_2.containsMatchIn(l)) {
                    level = if (SETEXT_1.containsMatchIn(l)) 1 else 2
                    i += 1
                    break
                }
                if (interruptsParagraph(l) || tableStart(lines, i) != null) break
                para += l
                i += 1
            }
            val text = para.joinToString("\n") { it.trimStart(' ', '\t') }.trimEnd(' ', '\t')
            val content = MonarchAiMarkdownInlineParser.parse(text)
            out.push(
                if (level != null) {
                    Block.Heading(level, content, raw(lines, start, i))
                } else {
                    Block.Paragraph(content, raw(lines, start, i))
                },
                start,
                i,
            )
        }
        return out
    }

    @Suppress(
        "CyclomaticComplexMethod",
        "LongMethod",
        "NestedBlockDepth",
        "ComplexCondition",
        "LoopWithTooManyJumpStatements",
    )
    private fun parseList(lines: List<String>, from: Int, first: Marker): Pair<Int, Block.ListBlock> {
        val items = ArrayList<MonarchAiMarkdownListItem>()
        var loose = false
        var i = from
        while (i < lines.size) {
            val line = lines[i]
            if (HR.containsMatchIn(line)) break
            val mk = listMarker(line)
            if (mk == null || mk.key != first.key || mk.ordered != first.ordered) break
            val body = arrayListOf(mk.first)
            i += 1
            while (i < lines.size) {
                val l = lines[i]
                if (isBlank(l)) {
                    body += ""
                } else if (indentOf(l) >= mk.contentCol) {
                    body += stripCols(l, mk.contentCol)
                } else {
                    val prev = body.last()
                    val openFences = body.count { fenceOpen(it) != null } % 2 == 1
                    // Lazy continuation of the item's paragraph.
                    if (!isBlank(prev) && !openFences && !interruptsParagraph(l) && listMarker(l) == null) {
                        body += l.trim()
                    } else {
                        break
                    }
                }
                i += 1
            }
            // Trailing blank lines belong between items, not to this item.
            var trailing = 0
            while (body.size > 1 && isBlank(body.last())) {
                body.removeAt(body.lastIndex)
                trailing += 1
            }
            i -= trailing
            var checked: Boolean? = null
            val task = TASK.find(body[0])
            if (task != null) {
                checked = task.groupValues[1] != " "
                body[0] = body[0].substring(task.value.length)
            }
            val parsed = parseLines(body)
            for (c in 1 until parsed.blocks.size) {
                for (l in parsed.ends[c - 1] until parsed.starts[c]) {
                    if (isBlank(body[l])) loose = true
                }
            }
            items += MonarchAiMarkdownListItem(checked, parsed.blocks.toList())
            if (trailing > 0) {
                val next = lines.getOrNull(i + trailing)
                val nm = next?.let { listMarker(it) }
                if (nm != null && nm.key == first.key && !HR.containsMatchIn(next)) {
                    loose = true
                    i += trailing
                } else {
                    break
                }
            }
        }
        return i to Block.ListBlock(first.ordered, first.start, loose, items, raw = "")
    }

    /** Footnote labels match case-insensitively. */
    fun normalizeLabel(label: String): String = label.trim().lowercase()

    /** Plain text of inline tokens; images give their alt text. */
    fun inlineText(nodes: List<MonarchAiMarkdownInline>): String = buildString {
        for (n in nodes) {
            when (n) {
                is MonarchAiMarkdownInline.Text -> append(n.value)
                is MonarchAiMarkdownInline.Code -> append(n.value)
                is MonarchAiMarkdownInline.Math -> append(n.value)
                MonarchAiMarkdownInline.Break -> append('\n')
                is MonarchAiMarkdownInline.Image -> append(n.alt)
                is MonarchAiMarkdownInline.FootnoteRef -> Unit
                is MonarchAiMarkdownInline.Styled -> append(inlineText(n.children))
                is MonarchAiMarkdownInline.Link -> append(inlineText(n.children))
            }
        }
    }

    /** Plain text of a block, paragraphs separated by blank lines. */
    fun blockText(block: Block): String = when (block) {
        is Block.Heading -> inlineText(block.content)
        is Block.Paragraph -> inlineText(block.content)
        is Block.Code -> block.code
        is Block.Table -> (listOf(block.header) + block.rows).joinToString("\n") { row ->
            row.joinToString(" ") { inlineText(it) }
        }
        is Block.Quote -> block.children.joinToString("\n\n") { blockText(it) }
        is Block.FootnoteDefinition -> block.children.joinToString("\n\n") { blockText(it) }
        is Block.ListBlock -> block.items.joinToString("\n\n") { item ->
            item.children.joinToString("\n\n") { blockText(it) }
        }
        is Block.Rule -> ""
    }
}
