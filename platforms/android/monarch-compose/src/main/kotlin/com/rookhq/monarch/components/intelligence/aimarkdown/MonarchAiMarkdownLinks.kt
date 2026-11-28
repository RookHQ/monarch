package com.rookhq.monarch.components.intelligence.aimarkdown

/**
 * Link safety of MonarchAiMarkdown: which links may be followed, which
 * images may be handed to the app, and the host shown after a link so it
 * cannot pretend to go elsewhere.
 */
internal object MonarchAiMarkdownLinks {
    private const val PROTOCOL_RELATIVE_PREFIX = "//"
    private const val LAST_SPACE_OR_CONTROL = 0x20
    private const val FIRST_C1_CONTROL = 0x7F
    private const val LAST_C1_CONTROL = 0x9F

    private val SCHEME = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")
    private val ABSOLUTE = Regex("^(?:[a-zA-Z][a-zA-Z0-9+.-]*:)?//(?:[^@/?#]*@)?(\\[[^\\]]*\\]|[^:/?#]+)")
    private val LINK_SCHEMES = setOf("http", "https", "mailto")
    private val IMAGE_SCHEMES = setOf("http", "https")

    private fun compact(url: String): String = url.trim().filterNot { char ->
        char.code <= LAST_SPACE_OR_CONTROL || char.code in FIRST_C1_CONTROL..LAST_C1_CONTROL
    }

    private fun scheme(url: String): String? = SCHEME.find(compact(url))?.groupValues?.get(1)?.lowercase()

    /**
     * Whether a link may be followed: `http`, `https` and `mailto` always; a
     * relative link only when the app handles links ([relativeAllowed]);
     * never `javascript:`, `data:`, any other scheme or a protocol-relative
     * `//host` link.
     */
    fun safeLink(href: String, relativeAllowed: Boolean): Boolean {
        val compacted = compact(href)
        if (compacted.isEmpty()) return false
        val scheme = scheme(href)
        return when {
            scheme != null -> scheme in LINK_SCHEMES
            compacted.startsWith(PROTOCOL_RELATIVE_PREFIX) -> false
            else -> relativeAllowed
        }
    }

    /** Whether an image may be shown: an absolute `http` or `https` source. */
    fun safeImage(src: String): Boolean = scheme(src) in IMAGE_SCHEMES

    /** The host of an absolute URL, lower-cased, or `null` for a relative one or `mailto:`. */
    fun urlHost(url: String): String? =
        ABSOLUTE.find(url.trim())?.groupValues?.get(1)?.lowercase()?.takeIf { it.isNotEmpty() }

    /**
     * The host to show after a link, or `null` when the link text already
     * names it (ignoring `www.`), so a link cannot pretend to go elsewhere.
     */
    fun shownHost(href: String, linkText: String): String? {
        val host = urlHost(href) ?: return null
        return host.takeUnless { linkText.lowercase().contains(host.removePrefix("www.")) }
    }
}
