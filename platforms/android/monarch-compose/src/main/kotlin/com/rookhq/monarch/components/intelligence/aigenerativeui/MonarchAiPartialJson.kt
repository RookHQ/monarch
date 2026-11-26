package com.rookhq.monarch.components.intelligence.aigenerativeui

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON

/**
 * Best-effort reading of JSON that is still arriving, such as a component's
 * props while they stream: open strings, arrays and objects are closed, and
 * a dangling key, colon, comma or unfinished literal is dropped. Never
 * throws. The same rules as iOS.
 */
internal object MonarchAiPartialJson {
    /** Complete JSON, or the longest sensible reading of a prefix of it. `null` when nothing can be read. */
    fun parse(text: String): MonarchAiJSON? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        // The whole text, then shorter and shorter readings: up to each comma or just after each opening bracket.
        val candidates = listOf(trimmed.length) + cutPoints(trimmed).reversed()
        return decode(trimmed) ?: candidates.asSequence()
            .mapNotNull { end -> close(trimmed.substring(0, end))?.let(::decode) }
            .firstOrNull()
    }

    private fun decode(text: String): MonarchAiJSON? = try {
        MonarchAiJSON.parse(text)
    } catch (_: IllegalArgumentException) {
        null
    }

    /** Where the text can be cut: before each comma, and after each `{` or `[`, outside strings. */
    private fun cutPoints(text: String): List<Int> {
        val points = mutableListOf<Int>()
        val scanner = StringScanner()
        text.forEachIndexed { index, char ->
            if (scanner.inString(char)) return@forEachIndexed
            when (char) {
                ',' -> points += index
                '{', '[' -> points += index + 1
            }
        }
        return points
    }

    /** One open container while scanning. */
    private class Level(val opener: Char) {
        var hasColon: Boolean = false
    }

    /** Tracks whether the scan is inside a string. */
    private class StringScanner {
        var inside: Boolean = false
            private set
        var escaped: Boolean = false
            private set

        /** Feeds one character; `true` when it belongs to a string (including its quotes). */
        fun inString(char: Char): Boolean {
            if (inside) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inside = false
                }
                return true
            }
            if (char == '"') {
                inside = true
                return true
            }
            return false
        }
    }

    /**
     * Closes a prefix: ends an open string, completes a dangling key or colon
     * with `null`, drops a trailing comma and closes the open containers.
     */
    private fun close(prefix: String): String? {
        val stack = ArrayDeque<Level>()
        val scanner = StringScanner()
        prefix.forEach { char -> if (!scanner.inString(char)) track(char, stack) }
        val head = StringBuilder(prefix)
        if (scanner.inside) {
            if (scanner.escaped) head.setLength(head.length - 1)
            head.append('"')
        }
        while (head.isNotEmpty() && head.last().isWhitespace()) head.setLength(head.length - 1)
        if (head.lastOrNull() == ',') head.setLength(head.length - 1)
        completeMember(head, stack.lastOrNull())
        if (head.isEmpty()) return null
        stack.reversed().forEach { head.append(if (it.opener == '{') '}' else ']') }
        return head.toString()
    }

    /** Follows the containers and whether the current object member has its colon. */
    private fun track(char: Char, stack: ArrayDeque<Level>) {
        when (char) {
            '{', '[' -> stack.addLast(Level(char))
            '}', ']' -> stack.removeLastOrNull()
            ':' -> stack.lastOrNull()?.hasColon = true
            ',' -> stack.lastOrNull()?.hasColon = false
        }
    }

    /** Completes a dangling key or colon in the innermost open object with `null`. */
    private fun completeMember(head: StringBuilder, top: Level?) {
        if (top == null || top.opener != '{') return
        if (head.lastOrNull() == ':') {
            head.append("null")
        } else if (!top.hasColon && head.lastOrNull() == '"') {
            head.append(":null")
        }
    }
}
