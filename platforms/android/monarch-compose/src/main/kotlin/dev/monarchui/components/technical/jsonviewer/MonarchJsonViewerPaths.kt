package dev.monarchui.components.technical.jsonviewer

/** One step in a JSON path: an object key or an array index. */
internal sealed interface MonarchJsonViewerSegment {
    /** An object key. */
    data class Key(val key: String) : MonarchJsonViewerSegment

    /** An array index. */
    data class Index(val index: Int) : MonarchJsonViewerSegment
}

/** The pure path and text rules of MonarchJsonViewer: paths, children, quoting and the JSON text of values. */
internal object MonarchJsonViewerPaths {
    /** The internal root of every row id. Shown paths use the viewer's root path. */
    const val IdRoot: String = "$"

    private const val Indent = "  "
    private const val ControlLimit = 0x20
    private const val HexWidth = 4
    private const val HexRadix = 16

    private fun isIdentifier(key: String): Boolean {
        fun letter(char: Char) = char in 'a'..'z' || char in 'A'..'Z' || char == '_' || char == '$'
        val first = key.firstOrNull() ?: return false
        return letter(first) && key.drop(1).all { letter(it) || it in '0'..'9' }
    }

    /**
     * A path as JSONPath-style text: `$.users[0].name`, with keys that are not identifiers quoted
     * (`$["first name"]`). With an empty [root] the first key has no dot (`users[0].name`).
     */
    fun path(segments: List<MonarchJsonViewerSegment>, root: String): String = buildString {
        append(root)
        segments.forEach { segment ->
            when {
                segment is MonarchJsonViewerSegment.Index -> append("[${segment.index}]")
                segment is MonarchJsonViewerSegment.Key && isIdentifier(segment.key) ->
                    append(if (isEmpty()) segment.key else ".${segment.key}")
                segment is MonarchJsonViewerSegment.Key -> append("[${quoted(segment.key)}]")
            }
        }
    }

    /** The internal id of a path. */
    fun id(segments: List<MonarchJsonViewerSegment>): String = path(segments, IdRoot)

    /** The children of a container with their path segments; none for anything else. */
    fun children(value: MonarchJsonValue): List<Pair<MonarchJsonViewerSegment, MonarchJsonValue>> = when (value) {
        is MonarchJsonValue.Object -> value.members.map { MonarchJsonViewerSegment.Key(it.first) to it.second }
        is MonarchJsonValue.Array -> value.items.mapIndexed { index, item ->
            MonarchJsonViewerSegment.Index(index) to item
        }
        else -> emptyList()
    }

    /** The number of children of a container, 0 for anything else. */
    fun childCount(value: MonarchJsonValue): Int = when (value) {
        is MonarchJsonValue.Object -> value.members.size
        is MonarchJsonValue.Array -> value.items.size
        else -> 0
    }

    /**
     * The text searched for a primitive: the string without quotes, the number as written, `true`, `false` or `null`.
     * `null` for containers.
     */
    fun searchableText(value: MonarchJsonValue): String? = when (value) {
        is MonarchJsonValue.String -> value.value
        is MonarchJsonValue.Number -> value.text
        is MonarchJsonValue.Bool -> value.value.toString()
        MonarchJsonValue.Null -> "null"
        is MonarchJsonValue.Object, is MonarchJsonValue.Array -> null
    }

    /** The text of a segment as shown before the colon. */
    fun name(segment: MonarchJsonViewerSegment): String = when (segment) {
        is MonarchJsonViewerSegment.Key -> segment.key
        is MonarchJsonViewerSegment.Index -> segment.index.toString()
    }

    /** [text] as a JSON string literal. */
    fun quoted(text: String): String = buildString {
        append('"')
        text.forEach { char ->
            when {
                char == '"' -> append("\\\"")
                char == '\\' -> append("\\\\")
                char == '\n' -> append("\\n")
                char == '\r' -> append("\\r")
                char == '\t' -> append("\\t")
                char == '\b' -> append("\\b")
                char == '\u000C' -> append("\\f")
                char.code < ControlLimit -> append("\\u" + char.code.toString(HexRadix).padStart(HexWidth, '0'))
                else -> append(char)
            }
        }
        append('"')
    }

    /** The JSON text of [value], indented by two spaces, keys in order. */
    fun stringify(value: MonarchJsonValue, indent: String = ""): String {
        val inner = indent + Indent
        return when (value) {
            is MonarchJsonValue.Object -> if (value.members.isEmpty()) {
                "{}"
            } else {
                value.members.joinToString(",\n", "{\n", "\n$indent}") { (key, item) ->
                    inner + quoted(key) + ": " + stringify(item, inner)
                }
            }
            is MonarchJsonValue.Array -> if (value.items.isEmpty()) {
                "[]"
            } else {
                value.items.joinToString(",\n", "[\n", "\n$indent]") { inner + stringify(it, inner) }
            }
            is MonarchJsonValue.String -> quoted(value.value)
            is MonarchJsonValue.Number -> value.text
            is MonarchJsonValue.Bool -> value.value.toString()
            MonarchJsonValue.Null -> "null"
        }
    }

    /** What Copy value copies: a string's own text, the JSON text of anything else. */
    fun copyText(value: MonarchJsonValue): String =
        if (value is MonarchJsonValue.String) value.value else stringify(value)
}
