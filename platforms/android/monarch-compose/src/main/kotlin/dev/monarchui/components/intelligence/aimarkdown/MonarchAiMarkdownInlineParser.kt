package dev.monarchui.components.intelligence.aimarkdown

import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownInline as Inline

/**
 * The inline parser of MonarchAiMarkdown, ported from the web: code spans,
 * emphasis with CommonMark's delimiter rules, strikethrough, links, images,
 * autolinks (`<…>` and bare `https://` or `www.`), footnote references,
 * entities, escapes, hard breaks and inline maths (`\(…\)` or `$$…$$`).
 * Raw HTML is never interpreted: it stays text.
 */
internal object MonarchAiMarkdownInlineParser {
    private const val FOOTNOTE_WINDOW = 200
    private const val AUTOLINK_WINDOW = 2048
    private const val ENTITY_WINDOW = 12
    private const val MAX_CODE_POINT = 0x10FFFF
    private const val SURROGATE_START = 0xD800
    private const val SURROGATE_END = 0xDFFF
    private const val HEX = 16
    private const val RULE_OF_THREE = 3
    private const val STRONG_RUN = 2
    private const val NAMED_GROUP = 3

    private val ENTITY = Regex("^&(?:#(\\d{1,7})|#[xX]([\\da-fA-F]{1,6})|([a-zA-Z]+));")
    private val FOOTNOTE_REF = Regex("^\\[\\^([^\\]\\s]+)\\]")
    private val AUTOLINK_URI = Regex("^<([a-zA-Z][a-zA-Z0-9+.-]{1,31}:[^<>\\s]*)>")
    private val AUTOLINK_EMAIL = Regex(
        "^<([a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
            "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)>",
    )
    private val BARE_URL = Regex("^(?:https?://[^\\s<]+|www\\.[a-zA-Z0-9-][^\\s<]*)", RegexOption.IGNORE_CASE)
    private val BARE_URL_VALID = Regex("^(?:https?://|www\\.)[^./\\s]+(?:\\.[^./\\s]+)*", RegexOption.IGNORE_CASE)
    private val BARE_URL_SCHEME_ONLY = Regex("^https?://$", RegexOption.IGNORE_CASE)
    private val WWW = Regex("^www\\.", RegexOption.IGNORE_CASE)
    private val ESCAPED = Regex("\\\\([!-/:-@\\[-`{-~])")

    private val ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00A0",
        "copy" to "©", "reg" to "®", "trade" to "™", "hellip" to "…", "mdash" to "—", "ndash" to "–",
        "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”",
    )

    private val PUNCTUATION_TYPES = setOf(
        Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
        Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
        Character.OTHER_PUNCTUATION, Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL,
        Character.OTHER_SYMBOL,
    ).map { it.toInt() }.toSet()

    /** A run of `*`, `_` or `~` waiting to be matched. */
    private class Delim(val char: Char, var count: Int, val orig: Int, val canOpen: Boolean, val canClose: Boolean)

    private fun isAsciiPunct(c: Char): Boolean = c in '!'..'/' || c in ':'..'@' || c in '['..'`' || c in '{'..'~'

    private fun isPunct(c: Char): Boolean = Character.getType(c) in PUNCTUATION_TYPES

    private fun isSpace(c: Char): Boolean = c.isWhitespace() || c == '\uFEFF'

    private fun codePoint(n: Int): String =
        if (n in 1..MAX_CODE_POINT && n !in SURROGATE_START..SURROGATE_END) {
            String(Character.toChars(n))
        } else {
            "\uFFFD"
        }

    private fun unescape(s: String): String = s.replace(ESCAPED, "$1")

    private fun findBacktickRun(src: String, from: Int, run: Int): Int {
        var i = from
        while (i < src.length) {
            if (src[i] == '`') {
                var r = 1
                while (src.getOrNull(i + r) == '`') r += 1
                if (r == run) return i
                i += r
            } else {
                i += 1
            }
        }
        return -1
    }

    private fun findClosingBracket(src: String, open: Int): Int {
        var depth = 0
        var i = open
        while (i < src.length) {
            when (src[i]) {
                '\\' -> i += 1
                '`' -> {
                    var run = 1
                    while (src.getOrNull(i + run) == '`') run += 1
                    val close = findBacktickRun(src, i + run, run)
                    i = if (close >= 0) close + run - 1 else i + run - 1
                }
                '[' -> depth += 1
                ']' -> {
                    depth -= 1
                    if (depth == 0) return i
                }
                else -> Unit
            }
            i += 1
        }
        return -1
    }

    private class ParsedLink(val text: String, val href: String, val title: String?, val end: Int)

    // A line-for-line port of the web parser: the structure is kept so the two can be compared.
    @Suppress("CyclomaticComplexMethod", "ReturnCount", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    private fun parseLinkAt(src: String, open: Int): ParsedLink? {
        val close = findClosingBracket(src, open)
        if (close < 0 || src.getOrNull(close + 1) != '(') return null
        var j = close + 2
        fun skip() {
            while (j < src.length && isSpace(src[j])) j += 1
        }
        skip()
        val href: String
        if (src.getOrNull(j) == '<') {
            val end = src.indexOf('>', j)
            if (end < 0 || src.substring(j, end).contains('\n')) return null
            href = src.substring(j + 1, end)
            j = end + 1
        } else {
            val begin = j
            var depth = 0
            while (j < src.length) {
                val ch = src[j]
                if (ch == '\\' && j + 1 < src.length) {
                    j += 2
                    continue
                }
                if (isSpace(ch)) break
                if (ch == '(') depth += 1
                if (ch == ')') {
                    if (depth == 0) break
                    depth -= 1
                }
                j += 1
            }
            href = src.substring(begin, j)
        }
        skip()
        var title: String? = null
        val q = src.getOrNull(j)
        if (q == '"' || q == '\'' || q == '(') {
            val endChar = if (q == '(') ')' else q
            val end = src.indexOf(endChar, j + 1)
            if (end < 0) return null
            title = unescape(src.substring(j + 1, end))
            j = end + 1
            skip()
        }
        if (src.getOrNull(j) != ')') return null
        return ParsedLink(src.substring(open + 1, close), unescape(href), title, j + 1)
    }

    private fun trimBareUrl(url: String): String {
        var u = url
        while (true) {
            val last = u.lastOrNull() ?: return u
            u = when {
                last in "?!.,:*_~'\";" -> u.dropLast(1)
                last == ')' && u.count { it == '(' } < u.count { it == ')' } -> u.dropLast(1)
                else -> return u
            }
        }
    }

    /** Parses inline Markdown into tokens. `links = false` inside link text: links do not nest. */
    @Suppress(
        "CyclomaticComplexMethod",
        "LongMethod",
        "NestedBlockDepth",
        "LoopWithTooManyJumpStatements",
        "ComplexCondition",
    )
    fun parse(src: String, links: Boolean = true): List<Inline> {
        val nodes = ArrayList<Any>()
        val text = StringBuilder()
        fun flush() {
            if (text.isNotEmpty()) nodes += Inline.Text(text.toString())
            text.clear()
        }
        var i = 0
        while (i < src.length) {
            val c = src[i]

            if (c == '\\') {
                val next = src.getOrNull(i + 1)
                // `\(…\)` is inline maths; without its closer the backslash is an ordinary escape.
                if (next == '(') {
                    val close = src.indexOf("\\)", i + 2)
                    val value = if (close < 0) "" else src.substring(i + 2, close).trim()
                    if (value.isNotEmpty() && !value.contains("\n\n")) {
                        flush()
                        nodes += Inline.Math(value)
                        i = close + 2
                        continue
                    }
                }
                if (next == '\n') {
                    flush()
                    nodes += Inline.Break
                    i += 2
                    continue
                }
                if (next != null && isAsciiPunct(next)) {
                    text.append(next)
                    i += 2
                    continue
                }
                text.append(c)
                i += 1
                continue
            }

            if (c == '$' && src.getOrNull(i + 1) == '$') {
                // `$$…$$` inside a line. A single `$` is never maths: it is usually money.
                val close = src.indexOf("$$", i + 2)
                val value = if (close < 0) "" else src.substring(i + 2, close).trim()
                if (value.isNotEmpty() && !value.contains("\n\n")) {
                    flush()
                    nodes += Inline.Math(value)
                    i = close + 2
                    continue
                }
            }

            if (c == '`') {
                var run = 1
                while (src.getOrNull(i + run) == '`') run += 1
                val close = findBacktickRun(src, i + run, run)
                if (close >= 0) {
                    flush()
                    var value = src.substring(i + run, close).replace('\n', ' ')
                    if (value.length > 1 && value.startsWith(' ') && value.endsWith(' ') && value.isNotBlank()) {
                        value = value.substring(1, value.length - 1)
                    }
                    nodes += Inline.Code(value)
                    i = close + run
                } else {
                    text.append("`".repeat(run))
                    i += run
                }
                continue
            }

            if (c == '\n') {
                val spaces = text.length - text.trimEnd(' ').length
                if (spaces >= 2) {
                    text.setLength(text.length - spaces)
                    flush()
                    nodes += Inline.Break
                } else {
                    text.setLength(text.length - spaces)
                    text.append('\n')
                }
                i += 1
                while (src.getOrNull(i) == ' ' || src.getOrNull(i) == '\t') i += 1
                continue
            }

            if (c == '!' && src.getOrNull(i + 1) == '[') {
                val link = parseLinkAt(src, i + 1)
                if (link != null) {
                    flush()
                    val alt = MonarchAiMarkdownParser.inlineText(parse(link.text, false))
                    nodes += Inline.Image(link.href, alt, link.title)
                    i = link.end
                    continue
                }
                text.append(c)
                i += 1
                continue
            }

            if (c == '[') {
                val ref = FOOTNOTE_REF.find(src.substring(i, minOf(src.length, i + FOOTNOTE_WINDOW)))
                if (ref != null) {
                    flush()
                    nodes += Inline.FootnoteRef(MonarchAiMarkdownParser.normalizeLabel(ref.groupValues[1]))
                    i += ref.value.length
                    continue
                }
                val link = if (links) parseLinkAt(src, i) else null
                if (link != null) {
                    flush()
                    nodes += Inline.Link(link.href, link.title, false, parse(link.text, false))
                    i = link.end
                    continue
                }
                text.append(c)
                i += 1
                continue
            }

            if (c == '<' && links) {
                val rest = src.substring(i, minOf(src.length, i + AUTOLINK_WINDOW))
                val uri = AUTOLINK_URI.find(rest)
                val email = if (uri == null) AUTOLINK_EMAIL.find(rest) else null
                val m = uri ?: email
                if (m != null) {
                    flush()
                    val value = m.groupValues[1]
                    nodes += Inline.Link(
                        href = if (email != null) "mailto:$value" else value,
                        title = null,
                        autolink = true,
                        children = listOf(Inline.Text(value)),
                    )
                    i += m.value.length
                    continue
                }
            }

            if (c == '*' || c == '_' || c == '~') {
                var run = 1
                while (src.getOrNull(i + run) == c) run += 1
                val before = if (i == 0) ' ' else src[i - 1]
                val after = src.getOrNull(i + run) ?: ' '
                val left = !isSpace(after) && (!isPunct(after) || isSpace(before) || isPunct(before))
                val right = !isSpace(before) && (!isPunct(before) || isSpace(after) || isPunct(after))
                var canOpen = left
                var canClose = right
                if (c == '_') {
                    canOpen = left && (!right || isPunct(before))
                    canClose = right && (!left || isPunct(after))
                }
                if (c == '~' && run > 2) {
                    text.append(c.toString().repeat(run))
                } else {
                    flush()
                    nodes += Delim(c, run, run, canOpen, canClose)
                }
                i += run
                continue
            }

            if (c == '&') {
                val m = ENTITY.find(src.substring(i, minOf(src.length, i + ENTITY_WINDOW)))
                val named = m?.groups?.get(NAMED_GROUP)?.value?.let { ENTITIES[it] }
                val decimal = m?.groups?.get(1)?.value
                val hex = m?.groups?.get(2)?.value
                if (m != null && (decimal != null || hex != null || named != null)) {
                    text.append(
                        when {
                            decimal != null -> codePoint(decimal.toInt())
                            hex != null -> codePoint(hex.toInt(HEX))
                            else -> named
                        },
                    )
                    i += m.value.length
                    continue
                }
            }

            if (links && (c == 'h' || c == 'H' || c == 'w' || c == 'W')) {
                val before = if (i == 0) ' ' else src[i - 1]
                if (isSpace(before) || before in "*_~(") {
                    val m = BARE_URL.find(src.substring(i, minOf(src.length, i + AUTOLINK_WINDOW)))
                    if (m != null) {
                        val url = trimBareUrl(m.value)
                        if (BARE_URL_VALID.containsMatchIn(url) && !BARE_URL_SCHEME_ONLY.containsMatchIn(url)) {
                            flush()
                            nodes += Inline.Link(
                                href = if (WWW.containsMatchIn(url)) "https://$url" else url,
                                title = null,
                                autolink = true,
                                children = listOf(Inline.Text(url)),
                            )
                            i += url.length
                            continue
                        }
                    }
                }
            }

            text.append(c)
            i += 1
        }
        flush()
        return finalize(processEmphasis(nodes))
    }

    /** CommonMark's "process emphasis" over the delimiter runs, closest opener first. */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition", "LoopWithTooManyJumpStatements")
    private fun processEmphasis(nodes: ArrayList<Any>): List<Any> {
        var i = 0
        while (i < nodes.size) {
            val closer = nodes[i]
            if (closer !is Delim || !closer.canClose || closer.count == 0) {
                i += 1
                continue
            }
            var matched = false
            for (j in i - 1 downTo 0) {
                val opener = nodes[j]
                if (opener !is Delim || opener.char != closer.char || !opener.canOpen || opener.count == 0) continue
                if (closer.char != '~' &&
                    (opener.canClose || closer.canOpen) &&
                    (opener.orig + closer.orig) % RULE_OF_THREE == 0 &&
                    !(opener.orig % RULE_OF_THREE == 0 && closer.orig % RULE_OF_THREE == 0)
                ) {
                    continue
                }
                if (closer.char == '~' && opener.count != closer.count) continue
                val use = when {
                    closer.char == '~' -> opener.count
                    opener.count >= STRONG_RUN && closer.count >= STRONG_RUN -> STRONG_RUN
                    else -> 1
                }
                val kind = when {
                    closer.char == '~' -> MonarchAiMarkdownEmphasis.Del
                    use == STRONG_RUN -> MonarchAiMarkdownEmphasis.Strong
                    else -> MonarchAiMarkdownEmphasis.Em
                }
                opener.count -= use
                closer.count -= use
                val inner = finalize(nodes.subList(j + 1, i).toList())
                nodes.subList(j + 1, i).clear()
                nodes.add(j + 1, Inline.Styled(kind, inner))
                i = j + 2
                matched = true
                break
            }
            if (!matched) i += 1
        }
        return nodes
    }

    private fun finalize(nodes: List<Any>): List<Inline> {
        val out = ArrayList<Inline>()
        for (node in nodes) {
            val next: Inline = when (node) {
                is Delim -> if (node.count > 0) Inline.Text(node.char.toString().repeat(node.count)) else continue
                else -> node as Inline
            }
            val last = out.lastOrNull()
            if (next is Inline.Text && last is Inline.Text) {
                out[out.lastIndex] = Inline.Text(last.value + next.value)
            } else {
                out += next
            }
        }
        return out
    }
}
