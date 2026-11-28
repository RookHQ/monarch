package dev.monarchui.components.intelligence.aicitation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** The sizes, colours and pure rules of MonarchAiCitation. */
internal object MonarchAiCitationTokens {
    /** The widest the card grows. */
    val CardMaxWidth = MonarchTokens.Space.space24.dp * 3 + MonarchTokens.Space.space8.dp

    /** The gap between the favicon and the title. */
    val SourceGap = MonarchTokens.Space.space2.dp

    /** The width of the bar at the start of the quote. */
    val QuoteBarWidth = MonarchTokens.Space.space0_5.dp

    /** The padding inside the quote. */
    val QuotePadding = MonarchTokens.Space.space2.dp

    /** The padding around the marker's text inside its touch target. */
    val MarkerPadding = MonarchTokens.Space.space0_5.dp

    /** The colour of the marker. */
    fun marker(colors: MonarchColors): Color = colors.aiCitation

    /** The fill of the quote. */
    fun quoteBackground(colors: MonarchColors): Color = colors.bgSubtle

    /** The bar at the start of the quote. */
    fun quoteBar(colors: MonarchColors): Color = colors.borderStrong

    /** The colour of the meta line. */
    fun meta(colors: MonarchColors): Color = colors.textMuted

    /** The marker's visible text: "[1]". */
    fun markerText(index: Int): String = "[$index]"

    /**
     * The address when it can be opened outside the app: an absolute http, https or mailto address, or `null`.
     * A javascript:, file: or relative address is never opened.
     */
    fun safeUrl(url: String?): String? {
        val uri = parse(url) ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        return when {
            scheme == "mailto" -> url
            scheme in WebSchemes && !uri.host.isNullOrEmpty() -> url
            else -> null
        }
    }

    /** The host of an http or https address without a leading `www.`, or `null`. */
    fun host(url: String?): String? {
        val uri = parse(url) ?: return null
        if (uri.scheme?.lowercase(Locale.ROOT) !in WebSchemes) return null
        return uri.host?.takeIf { it.isNotEmpty() }?.removePrefix("www.")
    }

    private fun parse(url: String?): URI? {
        val text = url?.trim().orEmpty()
        if (text.isEmpty()) return null
        return try {
            URI(text)
        } catch (_: URISyntaxException) {
            null
        }
    }

    private val WebSchemes = setOf("http", "https")
}
