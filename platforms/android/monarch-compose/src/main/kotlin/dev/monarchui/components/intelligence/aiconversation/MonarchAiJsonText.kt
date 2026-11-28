package dev.monarchui.components.intelligence.aiconversation

/** A small JSON reader and writer for [MonarchAiJSON], so the model needs no serialisation library. */
internal object MonarchAiJsonText {
    private const val HEX_RADIX = 16
    private const val UNICODE_ESCAPE_LENGTH = 4
    private const val FIRST_PRINTABLE = 0x20

    fun write(value: MonarchAiJSON): String = StringBuilder().also { write(value, it) }.toString()

    private fun write(value: MonarchAiJSON, out: StringBuilder) {
        when (value) {
            MonarchAiJSON.Null -> out.append("null")
            is MonarchAiJSON.Bool -> out.append(value.value)
            is MonarchAiJSON.Number -> out.append(number(value.value))
            is MonarchAiJSON.Text -> quote(value.value, out)
            is MonarchAiJSON.Array -> {
                out.append('[')
                value.values.forEachIndexed { index, item ->
                    if (index > 0) out.append(',')
                    write(item, out)
                }
                out.append(']')
            }
            is MonarchAiJSON.Object -> {
                out.append('{')
                value.members.entries.forEachIndexed { index, (key, item) ->
                    if (index > 0) out.append(',')
                    quote(key, out)
                    out.append(':')
                    write(item, out)
                }
                out.append('}')
            }
        }
    }

    /** Whole numbers without a fraction, as `JSON.stringify` writes them; not-finite numbers as `null`. */
    private fun number(value: Double): String = when {
        !value.isFinite() -> "null"
        value == Math.rint(value) && kotlin.math.abs(value) < Long.MAX_VALUE.toDouble() -> value.toLong().toString()
        else -> value.toString()
    }

    private fun quote(text: String, out: StringBuilder) {
        out.append('"')
        text.forEach { char ->
            when {
                char == '"' -> out.append("\\\"")
                char == '\\' -> out.append("\\\\")
                char == '\n' -> out.append("\\n")
                char == '\r' -> out.append("\\r")
                char == '\t' -> out.append("\\t")
                char.code < FIRST_PRINTABLE ->
                    out.append("\\u").append(char.code.toString(HEX_RADIX).padStart(UNICODE_ESCAPE_LENGTH, '0'))
                else -> out.append(char)
            }
        }
        out.append('"')
    }

    fun parse(text: String): MonarchAiJSON {
        val reader = Reader(text)
        val value = reader.value()
        reader.skipSpace()
        require(reader.atEnd()) { "Unexpected text after the JSON value at ${reader.position}" }
        return value
    }

    private class Reader(private val text: String) {
        var position = 0
            private set

        fun atEnd(): Boolean = position >= text.length

        fun skipSpace() {
            while (!atEnd() && text[position].isWhitespace()) position++
        }

        private fun peek(): Char {
            require(!atEnd()) { "Unexpected end of the JSON text" }
            return text[position]
        }

        private fun expect(word: String) {
            require(text.startsWith(word, position)) { "Expected $word at $position" }
            position += word.length
        }

        fun value(): MonarchAiJSON {
            skipSpace()
            return when (peek()) {
                '{' -> objectValue()
                '[' -> arrayValue()
                '"' -> MonarchAiJSON.Text(string())
                't' -> MonarchAiJSON.Bool(true).also { expect("true") }
                'f' -> MonarchAiJSON.Bool(false).also { expect("false") }
                'n' -> MonarchAiJSON.Null.also { expect("null") }
                else -> number()
            }
        }

        private fun objectValue(): MonarchAiJSON {
            expect("{")
            val members = LinkedHashMap<String, MonarchAiJSON>()
            skipSpace()
            if (peek() == '}') {
                position++
                return MonarchAiJSON.Object(members)
            }
            while (true) {
                skipSpace()
                val key = string()
                skipSpace()
                expect(":")
                members[key] = value()
                skipSpace()
                if (peek() == ',') position++ else break
            }
            expect("}")
            return MonarchAiJSON.Object(members)
        }

        private fun arrayValue(): MonarchAiJSON {
            expect("[")
            val values = mutableListOf<MonarchAiJSON>()
            skipSpace()
            if (peek() == ']') {
                position++
                return MonarchAiJSON.Array(values)
            }
            while (true) {
                values += value()
                skipSpace()
                if (peek() == ',') position++ else break
            }
            expect("]")
            return MonarchAiJSON.Array(values)
        }

        private fun string(): String {
            expect("\"")
            val out = StringBuilder()
            while (true) {
                val char = peek()
                position++
                when (char) {
                    '"' -> return out.toString()
                    '\\' -> escape(out)
                    else -> out.append(char)
                }
            }
        }

        private fun escape(out: StringBuilder) {
            val char = peek()
            position++
            when (char) {
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'b' -> out.append('\b')
                'f' -> out.append('\u000C')
                'u' -> {
                    require(position + UNICODE_ESCAPE_LENGTH <= text.length) { "Short unicode escape at $position" }
                    out.append(text.substring(position, position + UNICODE_ESCAPE_LENGTH).toInt(HEX_RADIX).toChar())
                    position += UNICODE_ESCAPE_LENGTH
                }
                else -> out.append(char)
            }
        }

        private fun number(): MonarchAiJSON {
            val start = position
            while (!atEnd() && (text[position].isDigit() || text[position] in "+-.eE")) position++
            val number = text.substring(start, position).toDoubleOrNull()
            requireNotNull(number) { "Expected a JSON value at $start" }
            return MonarchAiJSON.Number(number)
        }
    }
}
