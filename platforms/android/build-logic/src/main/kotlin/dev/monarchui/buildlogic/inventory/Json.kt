package dev.monarchui.buildlogic.inventory

/**
 * Writes JSON the same way on every machine: two-space indent, keys in the
 * order given, non-ASCII characters kept. Used for the generated components.json.
 */
object Json {
    fun write(value: Any?): String = StringBuilder().also { append(it, value, 0) }.append('\n').toString()

    private fun append(out: StringBuilder, value: Any?, depth: Int) {
        when (value) {
            null -> out.append("null")
            is String -> appendString(out, value)
            is Boolean, is Number -> out.append(value.toString())
            is Map<*, *> -> appendContainer(out, '{', '}', depth, value.entries.toList()) { entry ->
                appendString(out, entry.key.toString())
                out.append(": ")
                append(out, entry.value, depth + 1)
            }
            is Collection<*> -> appendContainer(out, '[', ']', depth, value.toList()) { append(out, it, depth + 1) }
            else -> error("Cannot write ${value::class} as JSON")
        }
    }

    private fun <T> appendContainer(
        out: StringBuilder,
        open: Char,
        close: Char,
        depth: Int,
        items: List<T>,
        appendItem: (T) -> Unit,
    ) {
        if (items.isEmpty()) {
            out.append(open).append(close)
            return
        }
        out.append(open).append('\n')
        items.forEachIndexed { index, item ->
            out.append("  ".repeat(depth + 1))
            appendItem(item)
            if (index < items.lastIndex) out.append(',')
            out.append('\n')
        }
        out.append("  ".repeat(depth)).append(close)
    }

    private fun appendString(out: StringBuilder, text: String) {
        out.append('"')
        for (char in text) {
            when {
                char == '"' -> out.append("\\\"")
                char == '\\' -> out.append("\\\\")
                char == '\n' -> out.append("\\n")
                char == '\r' -> out.append("\\r")
                char == '\t' -> out.append("\\t")
                char < ' ' -> out.append("\\u%04x".format(char.code))
                else -> out.append(char)
            }
        }
        out.append('"')
    }
}
