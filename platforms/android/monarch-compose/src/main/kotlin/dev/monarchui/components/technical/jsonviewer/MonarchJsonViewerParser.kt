package dev.monarchui.components.technical.jsonviewer

/**
 * A JSON parser that keeps object keys in document order, which `org.json` does not promise. It works on the UTF-16
 * text and limits nesting, so a hostile document cannot exhaust the stack here or in the recursive search,
 * flattening and copying.
 */
internal class MonarchJsonViewerParser(private val text: String) {
    private var index = 0

    /** Parses the whole text as one value. */
    fun document(): MonarchJsonValue {
        if (text.startsWith(BYTE_ORDER_MARK)) index = 1
        skipSpace()
        val value = value(depth = 0)
        skipSpace()
        if (index != text.length) fail()
        return value
    }

    private val current: Char? get() = text.getOrNull(index)

    private fun fail(at: Int = index): Nothing = throw MonarchJsonParseException(at)

    private fun skipSpace() {
        while (current in Whitespace) index++
    }

    private fun expect(char: Char) {
        if (current != char) fail()
        index++
    }

    private fun value(depth: Int): MonarchJsonValue {
        if (depth >= MAX_DEPTH) fail()
        val char = current ?: fail()
        return when {
            char == '{' -> objectValue(depth)
            char == '[' -> arrayValue(depth)
            char == '"' -> MonarchJsonValue.String(string())
            char == 't' -> literal("true", MonarchJsonValue.Bool(true))
            char == 'f' -> literal("false", MonarchJsonValue.Bool(false))
            char == 'n' -> literal("null", MonarchJsonValue.Null)
            char == '-' || char in '0'..'9' -> MonarchJsonValue.Number(number())
            else -> fail()
        }
    }

    private fun literal(word: String, result: MonarchJsonValue): MonarchJsonValue {
        word.forEach { expect(it) }
        return result
    }

    private fun objectValue(depth: Int): MonarchJsonValue {
        expect('{')
        val members = LinkedHashMap<String, MonarchJsonValue>()
        skipSpace()
        if (current == '}') {
            index++
            return MonarchJsonValue.Object(emptyList())
        }
        do {
            skipSpace()
            val key = string()
            skipSpace()
            expect(':')
            skipSpace()
            // A repeated key keeps its first position and its last value, as JSON.parse does.
            members[key] = value(depth + 1)
            skipSpace()
        } while (consume(','))
        expect('}')
        return MonarchJsonValue.Object(members.map { it.key to it.value })
    }

    private fun arrayValue(depth: Int): MonarchJsonValue {
        expect('[')
        val items = mutableListOf<MonarchJsonValue>()
        skipSpace()
        if (current == ']') {
            index++
            return MonarchJsonValue.Array(items)
        }
        do {
            skipSpace()
            items += value(depth + 1)
            skipSpace()
        } while (consume(','))
        expect(']')
        return MonarchJsonValue.Array(items)
    }

    private fun consume(char: Char): Boolean {
        if (current != char) return false
        index++
        return true
    }

    private fun number(): String {
        val start = index
        consume('-')
        digits()
        if (consume('.')) digits()
        if (current == 'e' || current == 'E') {
            index++
            if (current == '+' || current == '-') index++
            digits()
        }
        return text.substring(start, index)
    }

    private fun digits() {
        val start = index
        while (current?.let { it in '0'..'9' } == true) index++
        if (index == start) fail()
    }

    private fun string(): String {
        expect('"')
        val out = StringBuilder()
        while (true) {
            val char = current ?: fail()
            index++
            when {
                char == '"' -> return out.toString()
                char == '\\' -> out.append(escape())
                char < ' ' -> fail(index - 1)
                else -> out.append(char)
            }
        }
    }

    private fun escape(): Char {
        val char = current ?: fail()
        index++
        return when (char) {
            '"', '\\', '/' -> char
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> hex4()
            else -> fail(index - 1)
        }
    }

    private fun hex4(): Char {
        if (index + HEX_DIGITS > text.length) fail()
        val digits = text.substring(index, index + HEX_DIGITS)
        if (!digits.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) fail()
        val code = digits.toInt(HEX_RADIX)
        index += HEX_DIGITS
        // Surrogate pairs written as two escapes become the two UTF-16 halves of one character.
        return code.toChar()
    }

    private companion object {
        /** The deepest nesting accepted. */
        const val MAX_DEPTH = 100
        const val BYTE_ORDER_MARK = '\uFEFF'
        const val HEX_DIGITS = 4
        const val HEX_RADIX = 16
        val Whitespace = setOf(' ', '\n', '\r', '\t')
    }
}
