package dev.monarchui.components.collaboration.linkcard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.net.URI
import java.net.URISyntaxException

/** Component tokens and the pure rules of MonarchLinkCard. */
internal object MonarchLinkCardTokens {
    /** The padding around the text. */
    val TextPadding: Dp = MonarchTokens.Space.space3.dp

    /** The space between the lines of text, and between the favicon and the site. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The side of the thumbnail of the small card. */
    val Thumbnail: Dp = MonarchTokens.Space.space16.dp

    /** The space kept at the end of the text for the remove button. */
    val DismissSpace: Dp = MonarchTokens.Space.space12.dp

    /** The inset of the remove button and the video badge from the corner. */
    val CornerInset: Dp = MonarchTokens.Space.space1.dp

    /** The most lines of the title and of the description. */
    const val MAX_LINES: Int = 2

    /** The narrowest picture ratio (width over height); taller pictures are cropped. */
    const val MIN_RATIO: Float = 1.2f

    /** The widest picture ratio, also used when the size of the picture is unknown. */
    const val MAX_RATIO: Float = 2.2f

    private val safeSchemes = setOf("http", "https", "mailto")

    /** The width-to-height ratio of the banner: the picture's own, kept between 1.2 and 2.2. */
    fun aspectRatio(imageAspectRatio: Float?): Float =
        imageAspectRatio?.takeIf { it > 0f && it.isFinite() }?.coerceIn(MIN_RATIO, MAX_RATIO) ?: MAX_RATIO

    /** Whether the link opens: only http, https and mailto links do; anything else is shown as plain text. */
    fun isSafe(url: String): Boolean = parse(url)?.scheme?.lowercase() in safeSchemes

    /** The site line: the given name, or the host of the link without "www.", or `null` (a mailto link). */
    fun siteName(siteName: String?, url: String): String? =
        siteName?.takeIf { it.isNotBlank() } ?: parse(url)?.host?.removePrefix("www.")?.takeIf { it.isNotEmpty() }

    /** The colour of the picture's placeholder tile. */
    fun placeholderColor(colors: MonarchColors): Color = colors.bgMuted

    private fun parse(url: String): URI? = try {
        URI(url.trim())
    } catch (_: URISyntaxException) {
        null
    }
}
