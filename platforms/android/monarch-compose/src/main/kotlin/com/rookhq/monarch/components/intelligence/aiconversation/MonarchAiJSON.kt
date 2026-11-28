package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * A JSON value: the arguments and the output of a tool call, the props of
 * generative UI, a diff, or extra request fields. The web model types these
 * fields as `unknown`.
 *
 * ```kotlin
 * val input = MonarchAiJSON.Object(
 *     mapOf("query" to MonarchAiJSON.Text("invoices"), "limit" to MonarchAiJSON.Number(10.0)),
 * )
 * ```
 */
public sealed interface MonarchAiJSON {
    /** JSON `null`. */
    public data object Null : MonarchAiJSON

    /** A boolean. */
    public data class Bool(public val value: Boolean) : MonarchAiJSON

    /** A number. */
    public data class Number(public val value: Double) : MonarchAiJSON

    /** A string. */
    public data class Text(public val value: String) : MonarchAiJSON

    /** An array. */
    public data class Array(public val values: List<MonarchAiJSON>) : MonarchAiJSON

    /** An object. Its members keep their order. */
    public data class Object(public val members: Map<String, MonarchAiJSON>) : MonarchAiJSON

    /** The string, if the value is one. */
    public val stringValue: String?
        get() = (this as? Text)?.value

    /** The member of an object with this key, if the value is an object that has it. */
    public operator fun get(key: String): MonarchAiJSON? = (this as? Object)?.members?.get(key)

    /** The value as JSON text, compact, with whole numbers written without a fraction, as JavaScript does. */
    public fun toJsonString(): String = MonarchAiJsonText.write(this)

    /** Reads JSON text. */
    public companion object {
        /**
         * Parses JSON text.
         *
         * @throws IllegalArgumentException when the text is not valid JSON.
         */
        public fun parse(text: String): MonarchAiJSON = MonarchAiJsonText.parse(text)
    }
}
