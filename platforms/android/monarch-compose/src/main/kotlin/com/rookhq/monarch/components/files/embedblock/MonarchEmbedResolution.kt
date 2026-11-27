package com.rookhq.monarch.components.files.embedblock

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** How a `MonarchEmbedBlock` shows a URL, as decided by [MonarchEmbedResolution.resolve]. */
@Immutable
public sealed interface MonarchEmbedResolution {
    /** The URL as given, trimmed. */
    public val url: String

    /**
     * An https URL on an allow-listed host with an embeddable form: shown in a web view.
     *
     * @property url the share URL.
     * @property src the provider's embed address, the only address loaded.
     * @property provider the provider that matched.
     */
    public class Frame(override val url: String, public val src: String, public val provider: MonarchEmbedProvider) :
        MonarchEmbedResolution

    /**
     * Any other http or https URL: shown as a link card.
     *
     * @property url the link.
     * @property host the host without "www.".
     */
    public class Link(override val url: String, public val host: String) : MonarchEmbedResolution

    /**
     * A URL that cannot be shown: another scheme, a relative or malformed URL, or one with a user name.
     *
     * @property url the text as given, trimmed.
     */
    public class Invalid(override val url: String) : MonarchEmbedResolution

    /** Decides how a URL is shown. */
    public companion object {
        /**
         * A [Frame] when [raw] is an https URL without a port on a host of one
         * of [providers] whose embed address is https, a [Link] for any other
         * http or https URL, and [Invalid] for anything else (`javascript:`,
         * `data:`, relative or malformed URLs, or a user name in the URL).
         *
         * @param raw the URL someone pasted.
         * @param providers the allow-listed providers.
         */
        public fun resolve(
            raw: String,
            providers: List<MonarchEmbedProvider> = MonarchEmbedProvider.Defaults,
        ): MonarchEmbedResolution {
            val value = raw.trim()
            val parsed = try {
                URI(value)
            } catch (_: URISyntaxException) {
                null
            }
            val scheme = parsed?.scheme?.lowercase(Locale.ROOT)
            val host = parsed?.host?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
            val web = (scheme == "https" || scheme == "http") && parsed.rawUserInfo == null
            if (!web || host == null) return Invalid(value)
            val frame = if (scheme == "https" && parsed.port == -1) frame(value, host, providers) else null
            return frame ?: Link(value, host.removePrefix("www."))
        }

        /** The first provider of [host] with an https embed address for [value], as a [Frame]. */
        private fun frame(value: String, host: String, providers: List<MonarchEmbedProvider>): Frame? {
            val uri = value.toUri().buildUpon().authority(host).build()
            return providers.asSequence()
                .filter { host in it.hosts }
                .mapNotNull { provider ->
                    provider.toEmbedUrl(uri)?.takeIf { it.startsWith("https://") }?.let { Frame(value, it, provider) }
                }
                .firstOrNull()
        }
    }
}
