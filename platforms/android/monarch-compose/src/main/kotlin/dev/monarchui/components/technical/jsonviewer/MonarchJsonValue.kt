package dev.monarchui.components.technical.jsonviewer

import androidx.compose.runtime.Immutable

/**
 * A JSON value for `MonarchJsonViewer`, with object keys kept in the order of the document and numbers kept as
 * written, so `1.50` and large integers are shown and copied exactly.
 *
 * Build one with [parse], with [from] for Kotlin maps and lists, or with the cases directly.
 */
@Immutable
public sealed interface MonarchJsonValue {
    /**
     * An object.
     *
     * @property members the members in document order. Keys should be unique: rows are identified by their path.
     */
    public data class Object(public val members: List<Pair<kotlin.String, MonarchJsonValue>>) : MonarchJsonValue

    /**
     * An array.
     *
     * @property items the items in order.
     */
    public data class Array(public val items: List<MonarchJsonValue>) : MonarchJsonValue

    /**
     * A string.
     *
     * @property value the text, without quotes or escapes.
     */
    public data class String(public val value: kotlin.String) : MonarchJsonValue

    /**
     * A number.
     *
     * @property text the number as written in the JSON text, such as `4200` or `1.50`.
     */
    public data class Number(public val text: kotlin.String) : MonarchJsonValue

    /**
     * `true` or `false`.
     *
     * @property value the value.
     */
    public data class Bool(public val value: Boolean) : MonarchJsonValue

    /** `null`. */
    public data object Null : MonarchJsonValue

    /** Builds values from text and from Kotlin values. */
    public companion object {
        /**
         * Parses JSON text. Object keys keep their order; a repeated key keeps its first position and its last value,
         * as `JSON.parse` does. Nesting deeper than 100 levels is rejected.
         *
         * @param text the JSON text.
         * @throws MonarchJsonParseException with the position of the problem.
         */
        public fun parse(text: kotlin.String): MonarchJsonValue = MonarchJsonViewerParser(text).document()

        /**
         * A value from Kotlin: a `Map` with string keys is an object (in the map's order), a `List` or an `Array` an
         * array, a `String` a string, a `Number` a number, a `Boolean` a boolean, `null` null, and a
         * `MonarchJsonValue` itself. Anything else is its `toString()` as a string.
         *
         * @param value the Kotlin value.
         */
        public fun from(value: Any?): MonarchJsonValue = when (value) {
            null -> Null
            is MonarchJsonValue -> value
            is Map<*, *> -> Object(value.entries.map { (key, item) -> key.toString() to from(item) })
            is List<*> -> Array(value.map(::from))
            is kotlin.Array<*> -> Array(value.map(::from))
            is kotlin.String -> String(value)
            is kotlin.Number -> Number(value.toString())
            is Boolean -> Bool(value)
            else -> String(value.toString())
        }
    }
}
