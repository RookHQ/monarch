package com.rookhq.monarch.components.intelligence.aitoolcall

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON

/** How MonarchAiToolCall writes a tool's input and output for display. */
internal object MonarchAiToolCallFormat {
    private const val NEAR_LINE_END = 0.8
    private const val INDENT = "  "

    /**
     * A value for display: objects and arrays as JSON indented by two spaces, a string that holds JSON
     * re-indented, any other string as it is.
     */
    fun format(value: MonarchAiJSON): MonarchAiToolCallValue {
        if (value is MonarchAiJSON.Text) {
            val trimmed = value.value.trim()
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                val parsed = runCatching { MonarchAiJSON.parse(trimmed) }.getOrNull()
                if (parsed != null) return MonarchAiToolCallValue(pretty(parsed), json = true)
            }
            return MonarchAiToolCallValue(value.value, json = false)
        }
        return MonarchAiToolCallValue(pretty(value), json = true)
    }

    /** JSON text indented by two spaces, with the members in their order. */
    fun pretty(value: MonarchAiJSON): String = StringBuilder().also { pretty(value, "", it) }.toString()

    private fun pretty(value: MonarchAiJSON, indent: String, out: StringBuilder) {
        val inner = indent + INDENT
        when (value) {
            is MonarchAiJSON.Array ->
                if (value.values.isEmpty()) {
                    out.append("[]")
                } else {
                    out.append("[\n")
                    value.values.forEachIndexed { index, item ->
                        if (index > 0) out.append(",\n")
                        out.append(inner)
                        pretty(item, inner, out)
                    }
                    out.append('\n').append(indent).append(']')
                }
            is MonarchAiJSON.Object ->
                if (value.members.isEmpty()) {
                    out.append("{}")
                } else {
                    out.append("{\n")
                    value.members.entries.forEachIndexed { index, (key, item) ->
                        if (index > 0) out.append(",\n")
                        out.append(inner).append(MonarchAiJSON.Text(key).toJsonString()).append(": ")
                        pretty(item, inner, out)
                    }
                    out.append('\n').append(indent).append('}')
                }
            else -> out.append(value.toJsonString())
        }
    }

    /** Cuts [text] to [max] characters, at a line end when one is near. `0` or less keeps everything. */
    fun truncate(text: String, max: Int): MonarchAiToolCallCut {
        if (max <= 0 || text.length <= max) return MonarchAiToolCallCut(text, truncated = false)
        val cut = text.lastIndexOf('\n', max)
        val end = if (cut > max * NEAR_LINE_END) cut else max
        return MonarchAiToolCallCut(text.substring(0, end), truncated = true)
    }
}
