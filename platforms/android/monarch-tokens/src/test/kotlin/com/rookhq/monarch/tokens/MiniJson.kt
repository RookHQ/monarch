package com.rookhq.monarch.tokens

/**
 * A minimal JSON reader for the golden vectors, so the token module keeps no
 * test dependency beyond JUnit. Objects are maps, arrays lists, numbers
 * Double (parsed with correct rounding), and null is null.
 */
internal class MiniJson private constructor(private val text: String) {
    private var at = 0

    private fun fail(message: String): Nothing = throw IllegalArgumentException("$message at $at")

    private fun skipSpace() {
        while (at < text.length && text[at] in " \t\r\n") at++
    }

    private fun value(): Any? {
        skipSpace()
        return when (val c = text[at]) {
            '{' -> obj()
            '[' -> array()
            '"' -> string()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> if (c == '-' || c.isDigit()) number() else fail("Unexpected '$c'")
        }
    }

    private fun literal(word: String, result: Any?): Any? {
        if (!text.startsWith(word, at)) fail("Expected $word")
        at += word.length
        return result
    }

    private fun number(): Double {
        val start = at
        while (at < text.length && text[at] in "+-0123456789.eE") at++
        return text.substring(start, at).toDouble()
    }

    private fun string(): String {
        at++
        val out = StringBuilder()
        while (true) {
            val c = text[at++]
            when (c) {
                '"' -> return out.toString()
                '\\' -> {
                    when (val e = text[at++]) {
                        'n' -> out.append('\n')
                        't' -> out.append('\t')
                        'r' -> out.append('\r')
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'u' -> {
                            out.append(text.substring(at, at + 4).toInt(16).toChar())
                            at += 4
                        }
                        else -> out.append(e)
                    }
                }
                else -> out.append(c)
            }
        }
    }

    private fun array(): List<Any?> {
        at++
        val out = mutableListOf<Any?>()
        skipSpace()
        if (text[at] == ']') {
            at++
            return out
        }
        while (true) {
            out += value()
            skipSpace()
            when (text[at++]) {
                ',' -> continue
                ']' -> return out
                else -> fail("Expected , or ]")
            }
        }
    }

    private fun obj(): Map<String, Any?> {
        at++
        val out = LinkedHashMap<String, Any?>()
        skipSpace()
        if (text[at] == '}') {
            at++
            return out
        }
        while (true) {
            skipSpace()
            val key = string()
            skipSpace()
            if (text[at++] != ':') fail("Expected :")
            out[key] = value()
            skipSpace()
            when (text[at++]) {
                ',' -> continue
                '}' -> return out
                else -> fail("Expected , or }")
            }
        }
    }

    companion object {
        fun parse(text: String): Any? = MiniJson(text).value()
    }
}

@Suppress("UNCHECKED_CAST")
internal fun Any?.obj(): Map<String, Any?> = this as Map<String, Any?>

@Suppress("UNCHECKED_CAST")
internal fun Any?.list(): List<Any?> = this as List<Any?>
