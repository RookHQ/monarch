package dev.monarchui.components.typography.codeblock

/** The kind of a highlighted run of code. Ported from the web `CodeTokenType`. */
internal enum class MonarchCodeBlockTokenType { Plain, Keyword, Str, Comment, Number, Tag, Attr, Property }

/** One highlighted run of code. */
internal data class MonarchCodeBlockToken(val type: MonarchCodeBlockTokenType, val text: String)

/**
 * A deliberately small syntax tokenizer: keywords, strings, comments, numbers
 * (plus tags, attributes and properties for markup, JSON and CSS). Ported
 * from the web `ai-code-block.tokenize.ts` so both platforms colour code the
 * same way, without the web's incremental mode. It is not a grammar; it only
 * makes common snippets easier to scan. Unknown languages give one plain token.
 */
internal object MonarchCodeBlockHighlighter {
    private class CLike(
        val keywords: Set<String>,
        val strings: List<String>,
        val caseInsensitive: Boolean = false,
        val line: List<String> = emptyList(),
        val block: List<Pair<String, String>> = emptyList(),
        val multiline: List<String> = emptyList(),
        val keys: Boolean = false,
        val cssProps: Boolean = false,
        val hashAfterSpace: Boolean = false,
        val shortSingle: Boolean = false,
    )

    private const val MARKUP = "html"
    private const val COMMENT_OPEN = "<!--"
    private const val COMMENT_CLOSE = "-->"
    private const val LOOK_IDENT = 128
    private const val LOOK_NUMBER = 64
    private const val LOOK_COLON = 8
    private const val LOOK_SHORT = 8
    private const val LOOK_HEX = 10
    private const val LOOK_AT = 32

    private fun words(s: String): Set<String> = s.split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()

    private val jsKeywords = words(
        """
        abstract any as async await boolean break case catch class const constructor continue debugger declare
        default delete do else enum export extends false finally for from function get if implements import in
        infer instanceof interface is keyof let namespace never new null number of private protected public
        readonly return satisfies set static string super switch symbol this throw true try type typeof
        undefined unknown var void while with yield
        """,
    )

    private val grammars: Map<String, CLike> = mapOf(
        "js" to CLike(
            keywords = jsKeywords,
            line = listOf("//"),
            block = listOf("/*" to "*/"),
            strings = listOf("'", "\""),
            multiline = listOf("`"),
        ),
        "json" to CLike(keywords = words("true false null"), strings = listOf("\""), keys = true),
        "python" to CLike(
            keywords = words(
                """
                False None True and as assert async await break class continue def del elif else except finally for
                from global if import in is lambda match case nonlocal not or pass raise return self try while
                with yield
                """,
            ),
            line = listOf("#"),
            strings = listOf("'", "\""),
            multiline = listOf("\"\"\"", "'''"),
        ),
        "bash" to CLike(
            keywords = words(
                """
                if then else elif fi for while until do done case esac function in return local export readonly
                declare unset shift source exit echo cd set true false
                """,
            ),
            line = listOf("#"),
            strings = listOf("'", "\""),
            hashAfterSpace = true,
        ),
        "css" to CLike(
            keywords = words("important inherit initial unset revert none auto"),
            block = listOf("/*" to "*/"),
            strings = listOf("'", "\""),
            cssProps = true,
        ),
        "sql" to CLike(
            keywords = words(
                """
                select from where and or not null is in as on join left right inner outer full cross group by order
                having limit offset insert into values update set delete create table alter drop index view primary
                key foreign references default unique distinct union all case when then else end exists like between
                returning with asc desc count sum avg min max begin commit rollback transaction true false
                """,
            ),
            caseInsensitive = true,
            line = listOf("--"),
            block = listOf("/*" to "*/"),
            strings = listOf("'"),
        ),
        "go" to CLike(
            keywords = words(
                """
                break case chan const continue default defer else fallthrough for func go goto if import interface
                map package range return select struct switch type var nil true false iota error string int bool
                byte rune float64 any
                """,
            ),
            line = listOf("//"),
            block = listOf("/*" to "*/"),
            strings = listOf("\"", "'"),
            multiline = listOf("`"),
        ),
        "rust" to CLike(
            keywords = words(
                """
                as async await break const continue crate dyn else enum extern false fn for if impl in let loop match
                mod move mut pub ref return self Self static struct super trait true type unsafe use where while
                Some None Ok Err String Vec Option Result bool u8 u16 u32 u64 usize i8 i16 i32 i64 isize f32 f64 str
                """,
            ),
            line = listOf("//"),
            block = listOf("/*" to "*/"),
            strings = listOf("\""),
            shortSingle = true,
        ),
    )

    private val aliases: Map<String, String> = mapOf(
        "javascript" to "js", "jsx" to "js", "mjs" to "js", "cjs" to "js", "ts" to "js", "typescript" to "js",
        "tsx" to "js", "jsonc" to "json", "py" to "python", "sh" to "bash", "shell" to "bash", "zsh" to "bash",
        "console" to "bash", "scss" to "css", "xml" to MARKUP, "svg" to MARKUP, "vue" to MARKUP,
        "postgres" to "sql", "postgresql" to "sql", "mysql" to "sql", "sqlite" to "sql", "golang" to "go",
        "rs" to "rust",
    )

    private val languageNames: Map<String, String> = mapOf(
        "js" to "JavaScript", "javascript" to "JavaScript", "mjs" to "JavaScript", "cjs" to "JavaScript",
        "jsx" to "JSX", "ts" to "TypeScript", "typescript" to "TypeScript", "tsx" to "TSX", "json" to "JSON",
        "jsonc" to "JSON", "py" to "Python", "python" to "Python", "sh" to "Shell", "bash" to "Bash",
        "zsh" to "Shell", "shell" to "Shell", "console" to "Shell", "css" to "CSS", "scss" to "SCSS",
        "html" to "HTML", "xml" to "XML", "svg" to "SVG", "sql" to "SQL", "go" to "Go", "golang" to "Go",
        "rust" to "Rust", "rs" to "Rust", "diff" to "Diff", "yaml" to "YAML", "yml" to "YAML",
        "md" to "Markdown", "markdown" to "Markdown",
    )

    private val number =
        Regex("^(?:0[xX][\\da-fA-F_]+|0[bB][01_]+|(?:\\d[\\d_]*\\.?[\\d_]*|\\.\\d[\\d_]*)(?:[eE][+-]?\\d+)?)[a-zA-Z%]*")
    private val ident = Regex("^[A-Za-z_$][\\w$]*")
    private val cssIdent = Regex("^-{0,2}[A-Za-z_][\\w-]*")
    private val colonAhead = Regex("^\\s*:")
    private val shortSingleQuote = Regex("^'(?:\\\\.|[^\\\\'\\n]){1,4}'")
    private val hexColour = Regex("^#[\\da-fA-F]{3,8}\\b")
    private val atRule = Regex("^@[\\w-]+")
    private val digit = Regex("\\d")
    private val tagOpen = Regex("^</?[A-Za-z][\\w:.-]*")
    private val attribute = Regex("^[^\\s=>\"'/]+")

    /** A display name for a language id ("ts" gives "TypeScript"); unknown ids come back as given, blank as null. */
    fun languageName(language: String?): String? {
        val id = language?.trim().orEmpty()
        if (id.isEmpty()) return null
        return languageNames[id.lowercase()] ?: id
    }

    /** The grammar id used for [language], or null when it is plain text. */
    fun resolve(language: String?): String? {
        val id = language?.trim()?.lowercase().orEmpty()
        if (id.isEmpty()) return null
        val resolved = aliases[id] ?: id
        return if (resolved == MARKUP || grammars.containsKey(resolved)) resolved else null
    }

    /** Splits [code] into coloured tokens for [language]; unknown languages are one plain token. */
    fun tokenize(code: String, language: String?): List<MonarchCodeBlockToken> {
        val id = resolve(language)
        val out = Tokens()
        when {
            id == null -> out.push(MonarchCodeBlockTokenType.Plain, code)
            id == MARKUP -> tokenizeMarkup(code, out)
            else -> tokenizeClike(code, grammars.getValue(id), out)
        }
        return out.result()
    }

    /** Splits tokens at line breaks: one token list per line. */
    fun toLines(tokens: List<MonarchCodeBlockToken>): List<List<MonarchCodeBlockToken>> {
        val lines = mutableListOf(mutableListOf<MonarchCodeBlockToken>())
        tokens.forEach { token ->
            token.text.split('\n').forEachIndexed { index, part ->
                if (index > 0) lines.add(mutableListOf())
                if (part.isNotEmpty()) lines.last().add(MonarchCodeBlockToken(token.type, part))
            }
        }
        return lines
    }

    private class Tokens {
        private val types = mutableListOf<MonarchCodeBlockTokenType>()
        private val texts = mutableListOf<StringBuilder>()

        fun push(type: MonarchCodeBlockTokenType, text: String) {
            if (text.isEmpty()) return
            if (types.isNotEmpty() && types.last() == type) {
                texts.last().append(text)
            } else {
                types.add(type)
                texts.add(StringBuilder(text))
            }
        }

        fun result(): List<MonarchCodeBlockToken> =
            types.indices.map { MonarchCodeBlockToken(types[it], texts[it].toString()) }
    }

    private fun String.window(start: Int, length: Int): String =
        if (start >= this.length) "" else substring(start, minOf(this.length, start + length))

    private fun Char.isWordChar(): Boolean =
        this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this == '_' || this == '$'

    private fun readString(code: String, start: Int, quote: String, multiline: Boolean): Int {
        var j = start + quote.length
        while (j < code.length) {
            when {
                code[j] == '\\' -> j += 2
                code.startsWith(quote, j) -> return j + quote.length
                !multiline && code[j] == '\n' -> return j
                else -> j += 1
            }
        }
        return code.length
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    private fun tokenizeClike(code: String, g: CLike, out: Tokens) {
        val delims = (g.multiline.map { it to true } + g.strings.map { it to false })
            .sortedByDescending { it.first.length }
        var depth = 0
        var i = 0
        while (i < code.length) {
            val prev = if (i == 0) '\n' else code[i - 1]
            val block = g.block.firstOrNull { code.startsWith(it.first, i) }
            if (block != null) {
                val end = code.indexOf(block.second, i + block.first.length)
                val stop = if (end < 0) code.length else end + block.second.length
                out.push(MonarchCodeBlockTokenType.Comment, code.substring(i, stop))
                i = stop
                continue
            }
            val line = g.line.firstOrNull { code.startsWith(it, i) }
            if (line != null && !(g.hashAfterSpace && !prev.isWhitespace())) {
                val end = code.indexOf('\n', i)
                val stop = if (end < 0) code.length else end
                out.push(MonarchCodeBlockTokenType.Comment, code.substring(i, stop))
                i = stop
                continue
            }
            if (g.shortSingle && code[i] == '\'') {
                val match = shortSingleQuote.find(code.window(i, LOOK_SHORT))
                if (match != null) {
                    out.push(MonarchCodeBlockTokenType.Str, match.value)
                    i += match.value.length
                    continue
                }
            }
            val delim = delims.firstOrNull { code.startsWith(it.first, i) }
            if (delim != null) {
                val stop = readString(code, i, delim.first, delim.second)
                val isKey = g.keys && colonAhead.containsMatchIn(code.window(stop, LOOK_COLON))
                val type = if (isKey) MonarchCodeBlockTokenType.Property else MonarchCodeBlockTokenType.Str
                out.push(type, code.substring(i, stop))
                i = stop
                continue
            }
            val c = code[i]
            if ((c in '0'..'9' || c == '.') && !prev.isWordChar()) {
                val match = number.find(code.window(i, LOOK_NUMBER))
                if (match != null && digit.containsMatchIn(match.value)) {
                    out.push(MonarchCodeBlockTokenType.Number, match.value)
                    i += match.value.length
                    continue
                }
            }
            if (g.cssProps && c == '#' && depth > 0) {
                val match = hexColour.find(code.window(i, LOOK_HEX))
                if (match != null) {
                    out.push(MonarchCodeBlockTokenType.Number, match.value)
                    i += match.value.length
                    continue
                }
            }
            if (g.cssProps && c == '@') {
                val match = atRule.find(code.window(i, LOOK_AT))
                if (match != null) {
                    out.push(MonarchCodeBlockTokenType.Keyword, match.value)
                    i += match.value.length
                    continue
                }
            }
            val word = (if (g.cssProps) cssIdent else ident).find(code.window(i, LOOK_IDENT))?.value
            if (word != null && !prev.isWordChar()) {
                val key = if (g.caseInsensitive) word.lowercase() else word
                val after = code.window(i + word.length, LOOK_COLON)
                val type = when {
                    g.cssProps && depth > 0 && colonAhead.containsMatchIn(after) -> MonarchCodeBlockTokenType.Property
                    key in g.keywords -> MonarchCodeBlockTokenType.Keyword
                    else -> MonarchCodeBlockTokenType.Plain
                }
                out.push(type, word)
                i += word.length
                continue
            }
            if (c == '{') depth += 1
            if (c == '}') depth = maxOf(0, depth - 1)
            out.push(MonarchCodeBlockTokenType.Plain, c.toString())
            i += 1
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    private fun tokenizeMarkup(code: String, out: Tokens) {
        var i = 0
        while (i < code.length) {
            if (code.startsWith(COMMENT_OPEN, i)) {
                val end = code.indexOf(COMMENT_CLOSE, i + COMMENT_OPEN.length)
                val stop = if (end < 0) code.length else end + COMMENT_CLOSE.length
                out.push(MonarchCodeBlockTokenType.Comment, code.substring(i, stop))
                i = stop
                continue
            }
            val open = tagOpen.find(code.window(i, LOOK_IDENT))
            if (open == null) {
                out.push(MonarchCodeBlockTokenType.Plain, code[i].toString())
                i += 1
                continue
            }
            out.push(MonarchCodeBlockTokenType.Tag, open.value)
            i += open.value.length
            // Attributes until the tag closes.
            while (i < code.length && code[i] != '>' && !code.startsWith("/>", i)) {
                val q = code[i]
                if (q == '"' || q == '\'') {
                    val stop = readString(code, i, q.toString(), true)
                    out.push(MonarchCodeBlockTokenType.Str, code.substring(i, stop))
                    i = stop
                    continue
                }
                val attr = attribute.find(code.window(i, LOOK_IDENT))
                if (attr != null) {
                    out.push(MonarchCodeBlockTokenType.Attr, attr.value)
                    i += attr.value.length
                    continue
                }
                out.push(MonarchCodeBlockTokenType.Plain, q.toString())
                i += 1
            }
            val close = when {
                code.startsWith("/>", i) -> "/>"
                i < code.length && code[i] == '>' -> ">"
                else -> ""
            }
            out.push(MonarchCodeBlockTokenType.Tag, close)
            i += close.length
        }
    }
}
