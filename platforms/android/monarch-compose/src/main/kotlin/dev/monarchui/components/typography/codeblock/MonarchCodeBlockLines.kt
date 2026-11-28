package dev.monarchui.components.typography.codeblock

/** The role of a line in a diff. Ported from the web `DiffKind`. */
internal enum class MonarchCodeBlockDiffKind { Add, Remove, Hunk, Meta, Context }

/**
 * One drawn line of a `MonarchCodeBlock`.
 *
 * @property kind the diff role, or null outside diff mode.
 * @property tokens the highlighted runs, with tabs drawn as spaces.
 * @property text the plain text of the line, as TalkBack reads it.
 */
internal data class MonarchCodeBlockLine(
    val kind: MonarchCodeBlockDiffKind?,
    val tokens: List<MonarchCodeBlockToken>,
) {
    val text: String get() = tokens.joinToString("") { it.text }
}

/** Splits code into drawn lines. Pure, so the rules are tested without a screen. */
internal object MonarchCodeBlockLines {
    private const val TAB = "    "
    private val diffLanguage = Regex("^(diff|patch)$", RegexOption.IGNORE_CASE)
    private val diffMeta = Regex("^(diff |index |\\+\\+\\+ |--- |new file|deleted file|similarity|rename )")

    /** Whether the block is a diff: [diff] when given, else true for the languages "diff" and "patch". */
    fun isDiff(diff: Boolean?, language: String?): Boolean =
        diff ?: diffLanguage.matches(language?.trim().orEmpty())

    /**
     * The drawn lines of [code]. Windows line breaks are line breaks, one
     * final line break adds no empty line, empty code gives one empty line and
     * tabs are drawn as four spaces.
     */
    fun lines(code: String, language: String?, diff: Boolean, highlighted: Boolean): List<MonarchCodeBlockLine> {
        val normalised = code.replace("\r\n", "\n").replace('\r', '\n')
        val source = normalised.removeSuffix("\n")
        if (diff) {
            val raw = source.split('\n')
            return raw.zip(diffKinds(raw)) { text, kind ->
                val dropsMarker = kind == MonarchCodeBlockDiffKind.Add ||
                    kind == MonarchCodeBlockDiffKind.Remove ||
                    (kind == MonarchCodeBlockDiffKind.Context && text.startsWith(' '))
                val body = (if (dropsMarker) text.drop(1) else text).replace("\t", TAB)
                val tokens = if (body.isEmpty()) emptyList() else listOf(plain(body))
                MonarchCodeBlockLine(kind, tokens)
            }
        }
        val tokens = if (highlighted && MonarchCodeBlockHighlighter.resolve(language) != null) {
            MonarchCodeBlockHighlighter.tokenize(source, language)
        } else {
            listOf(plain(source))
        }
        return MonarchCodeBlockHighlighter.toLines(tokens).map { line ->
            MonarchCodeBlockLine(null, line.map { it.copy(text = it.text.replace("\t", TAB)) })
        }
    }

    /** The diff role of each line: hunk headers, file headers before the first hunk, added and removed lines. */
    fun diffKinds(lines: List<String>): List<MonarchCodeBlockDiffKind> {
        var inHunk = false
        return lines.map { line ->
            when {
                line.startsWith("@@") -> {
                    inHunk = true
                    MonarchCodeBlockDiffKind.Hunk
                }
                !inHunk && diffMeta.containsMatchIn(line) -> MonarchCodeBlockDiffKind.Meta
                line.startsWith('+') -> MonarchCodeBlockDiffKind.Add
                line.startsWith('-') -> MonarchCodeBlockDiffKind.Remove
                else -> MonarchCodeBlockDiffKind.Context
            }
        }
    }

    /** The visible marker of a diff line; a change never depends on colour alone. */
    fun marker(kind: MonarchCodeBlockDiffKind?): String = when (kind) {
        MonarchCodeBlockDiffKind.Add -> "+"
        MonarchCodeBlockDiffKind.Remove -> "\u2212"
        else -> " "
    }

    private fun plain(text: String) = MonarchCodeBlockToken(MonarchCodeBlockTokenType.Plain, text)
}
