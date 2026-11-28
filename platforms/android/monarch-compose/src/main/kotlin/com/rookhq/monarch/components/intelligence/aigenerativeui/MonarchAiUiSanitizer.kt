package com.rookhq.monarch.components.intelligence.aigenerativeui

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON

/**
 * Turns model-supplied props into data a component can receive safely.
 *
 * Props are already plain JSON ([MonarchAiJSON]) and are decoded by each
 * component's own decoder, so nothing is ever evaluated. This pass adds the
 * limits the web applies: every string that is a URL by key (`href`, `src`,
 * `url`…) or by shape (`javascript:…`, `data:…`) must be http, https, mailto
 * or relative, or it is dropped; strings are cut at 20,000 characters,
 * arrays and objects at 500 members, nesting at 8 levels, and numbers that
 * are not finite are dropped. The same rules as iOS.
 */
internal object MonarchAiUiSanitizer {
    /** The deepest nesting that is kept. */
    const val MAX_DEPTH: Int = 8

    /** The most members of an array or object that are kept. */
    const val MAX_ITEMS: Int = 500

    /** The longest string that is kept, in UTF-16 units. */
    const val MAX_STRING: Int = 20_000

    private const val LAST_SPACE_OR_CONTROL = 0x20
    private const val FIRST_C1_CONTROL = 0x7F
    private const val LAST_C1_CONTROL = 0x9F

    /** Keys whose string values are URLs and must be safe. */
    val UrlKeys: Set<String> = setOf(
        "href", "src", "srcSet", "action", "formAction", "poster", "xlinkHref", "data", "url", "imageUrl",
        "avatar", "favicon", "cite", "background", "ping",
    )

    /** Schemes that are never data a component should receive, whatever the key. */
    val UnsafeSchemes: Set<String> = setOf(
        "javascript", "vbscript", "livescript", "data", "file", "blob", "filesystem", "about", "jar",
        "view-source", "intent", "chrome", "chrome-extension", "moz-extension", "ms-browser-extension",
    )

    /** The schemes a link may use. */
    val LinkSchemes: Set<String> = setOf("http", "https", "mailto")

    /** The props as plain, limited data, or `null` when they are not an object. `null` props are an empty object. */
    fun sanitize(props: MonarchAiJSON): MonarchAiJSON? = when (props) {
        MonarchAiJSON.Null -> MonarchAiJSON.Object(emptyMap())
        is MonarchAiJSON.Object -> clean(props, key = "", depth = 0)
        else -> null
    }

    /** One value, cleaned. `null` drops it. */
    fun clean(value: MonarchAiJSON, key: String, depth: Int): MonarchAiJSON? = when (value) {
        MonarchAiJSON.Null, is MonarchAiJSON.Bool -> value
        is MonarchAiJSON.Number -> value.takeIf { it.value.isFinite() }
        is MonarchAiJSON.Text -> cleanText(value, key)
        is MonarchAiJSON.Array ->
            if (depth >= MAX_DEPTH) null else MonarchAiJSON.Array(cleanItems(value.values, key, depth))
        is MonarchAiJSON.Object ->
            if (depth >= MAX_DEPTH) null else MonarchAiJSON.Object(cleanMembers(value.members, depth))
    }

    private fun cleanItems(values: List<MonarchAiJSON>, key: String, depth: Int): List<MonarchAiJSON> =
        values.take(MAX_ITEMS).mapNotNull { clean(it, key, depth + 1) }

    private fun cleanMembers(members: Map<String, MonarchAiJSON>, depth: Int): Map<String, MonarchAiJSON> {
        val result = LinkedHashMap<String, MonarchAiJSON>()
        members.entries.take(MAX_ITEMS).forEach { (name, member) ->
            clean(member, name, depth + 1)?.let { result[name] = it }
        }
        return result
    }

    private fun cleanText(value: MonarchAiJSON.Text, key: String): MonarchAiJSON? {
        val text = value.value
        if ((key in UrlKeys || urlShaped(text)) && !safeUrl(text)) return null
        return if (text.length > MAX_STRING) MonarchAiJSON.Text(text.take(MAX_STRING)) else value
    }

    /** The text without the whitespace and control characters browsers ignore inside a scheme. */
    fun compact(text: String): String = text.trim().filterNot { char ->
        char.code <= LAST_SPACE_OR_CONTROL || char.code in FIRST_C1_CONTROL..LAST_C1_CONTROL
    }

    /** The lower-case scheme at the start of the text, if it has one. */
    fun scheme(text: String): String? {
        val compacted = compact(text)
        val colon = compacted.indexOf(':')
        if (compacted.isEmpty() || !compacted[0].isAsciiLetter() || colon < 0) return null
        val candidate = compacted.substring(0, colon)
        return candidate.takeIf { it.all { char -> char.isAsciiLetter() || char in '0'..'9' || char in "+.-" } }
            ?.lowercase()
    }

    /**
     * Whether a string is shaped like a URL, whatever its key: it starts with
     * a scheme and either has no spaces or names a scheme that runs code.
     */
    fun urlShaped(text: String): Boolean {
        val scheme = scheme(text) ?: return false
        return scheme in UnsafeSchemes || text.trim().none { it.isWhitespace() }
    }

    /** Whether a URL may be passed on: http, https, mailto, or a relative path. */
    fun safeUrl(text: String): Boolean {
        if (compact(text).isEmpty()) return false
        val scheme = scheme(text) ?: return true
        return scheme in LinkSchemes
    }

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
}
