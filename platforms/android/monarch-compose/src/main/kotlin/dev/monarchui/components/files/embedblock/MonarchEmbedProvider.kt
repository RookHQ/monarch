package dev.monarchui.components.files.embedblock

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * A site whose pages a `MonarchEmbedBlock` may show in a web view. A pasted
 * URL is checked against [hosts] (exact host names, https only) and turned
 * into the provider's own embed address by [toEmbedUrl]; anything else is
 * never loaded.
 *
 * @property id a stable id, for example "youtube".
 * @property name the name of the site, for example "YouTube". Used in the TalkBack name of the frame.
 * @property hosts the exact host names of share URLs, for example "youtube.com", "www.youtube.com" and "youtu.be".
 * @property aspectRatio the width over the height of the frame.
 * @property toEmbedUrl the https embed address for a share URL, or `null` when the URL has no embeddable form.
 */
@Immutable
public class MonarchEmbedProvider(
    public val id: String,
    public val name: String,
    public val hosts: List<String>,
    public val aspectRatio: Float = WIDE,
    public val toEmbedUrl: (Uri) -> String?,
) {
    /** The providers framed out of the box. Pass your own list to `MonarchEmbedBlock` to add or remove sites. */
    public companion object {
        private const val WIDE = 16f / 9f
        private const val CLASSIC = 4f / 3f
        private val YouTubeId = Regex("^[\\w-]{11}$")
        private val LoomId = Regex("^[a-f0-9]{16,64}$", RegexOption.IGNORE_CASE)
        private val Digits = Regex("^\\d+$")
        private val Word = Regex("^\\w+$")
        private val Handle = Regex("^[\\w-]+$")
        private val FigmaKinds = setOf("file", "design", "proto", "board", "slides")

        /** YouTube videos, through the privacy-enhanced youtube-nocookie.com player. */
        public val YouTube: MonarchEmbedProvider = MonarchEmbedProvider(
            id = "youtube",
            name = "YouTube",
            hosts = listOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"),
        ) { url ->
            val path = url.pathSegments
            val id = when {
                url.host == "youtu.be" -> path.getOrNull(0)
                path.getOrNull(0) == "watch" -> url.getQueryParameter("v")
                path.getOrNull(0) in setOf("embed", "shorts", "live") -> path.getOrNull(1)
                else -> null
            }
            id?.takeIf { YouTubeId.matches(it) }?.let { "https://www.youtube-nocookie.com/embed/$it" }
        }

        /** Vimeo videos. */
        public val Vimeo: MonarchEmbedProvider = MonarchEmbedProvider(
            id = "vimeo",
            name = "Vimeo",
            hosts = listOf("vimeo.com", "www.vimeo.com", "player.vimeo.com"),
        ) { url -> url.pathSegments.firstOrNull { Digits.matches(it) }?.let { "https://player.vimeo.com/video/$it" } }

        /** Loom recordings. */
        public val Loom: MonarchEmbedProvider = MonarchEmbedProvider(
            id = "loom",
            name = "Loom",
            hosts = listOf("loom.com", "www.loom.com"),
        ) { url ->
            val kind = url.pathSegments.getOrNull(0)
            val id = url.pathSegments.getOrNull(1)?.takeIf { LoomId.matches(it) }
            id?.takeIf { kind == "share" || kind == "embed" }?.let { "https://www.loom.com/embed/$it" }
        }

        /** Figma files, designs, prototypes, boards and slides. */
        public val Figma: MonarchEmbedProvider = MonarchEmbedProvider(
            id = "figma",
            name = "Figma",
            hosts = listOf("figma.com", "www.figma.com"),
            aspectRatio = CLASSIC,
        ) { url ->
            if (url.pathSegments.getOrNull(0) in FigmaKinds) {
                "https://www.figma.com/embed?embed_host=monarch&url=${Uri.encode(url.toString())}"
            } else {
                null
            }
        }

        /** CodePen pens, showing the result. */
        public val CodePen: MonarchEmbedProvider = MonarchEmbedProvider(
            id = "codepen",
            name = "CodePen",
            hosts = listOf("codepen.io"),
            aspectRatio = CLASSIC,
        ) { url ->
            val user = url.pathSegments.getOrNull(0)?.takeIf { Handle.matches(it) }
            val pen = url.pathSegments.getOrNull(1) in setOf("pen", "embed")
            val id = url.pathSegments.getOrNull(2)?.takeIf { pen && Word.matches(it) }
            if (user != null && id != null) "https://codepen.io/$user/embed/$id?default-tab=result" else null
        }

        /** YouTube, Vimeo, Loom, Figma and CodePen. */
        public val Defaults: List<MonarchEmbedProvider> = listOf(YouTube, Vimeo, Loom, Figma, CodePen)
    }
}
