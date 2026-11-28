package dev.monarchui.components.files.filepreview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchFilePreview, built from primitives and roles only. */
internal object MonarchFilePreviewTokens {
    /** The share of the screen height a preview may take, as the web's 70vh. */
    const val MAX_HEIGHT_RATIO: Float = 0.7f

    /** The height of a PDF page while it renders, as a share of its width (A4). */
    const val PAGE_PLACEHOLDER_RATIO: Float = 1.414f

    val TextPadding: Dp = MonarchTokens.Space.space4.dp
    val PageGap: Dp = MonarchTokens.Space.space2.dp
    val FallbackHorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val FallbackVerticalPadding: Dp = MonarchTokens.Space.space8.dp
    val FallbackGap: Dp = MonarchTokens.Space.space2.dp
    val FooterHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val FooterVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /**
     * The colours of a preview.
     *
     * @property container the fill around the content and behind the fallback.
     * @property border the 1dp outline.
     * @property surface the fill behind text, PDF pages and the footer.
     * @property divider the line above the footer.
     * @property name the file name in the fallback.
     * @property message the message in the fallback.
     * @property media the fill behind a video player.
     */
    data class Colors(
        val container: Color,
        val border: Color,
        val surface: Color,
        val divider: Color,
        val name: Color,
        val message: Color,
        val media: Color,
    )

    /** The colours of a preview, from the surface, border, text and inverse roles. */
    fun colors(colors: MonarchColors): Colors = Colors(
        container = colors.bgSubtle,
        border = colors.borderDefault,
        surface = colors.bgSurface,
        divider = colors.borderSubtle,
        name = colors.textDefault,
        message = colors.textMuted,
        media = colors.bgInverse,
    )

    /**
     * What a preview shows.
     *
     * @property kind the kind drawn: the detected one when its content is there and nothing failed, else
     * [MonarchFilePreviewKind.None].
     * @property message which fallback message to show, or `null` for none.
     */
    data class Resolution(val kind: MonarchFilePreviewKind, val message: Message?)

    /** The message of the fallback. */
    enum class Message { Unavailable, Failed }

    /** The most height a preview takes in a window [windowHeight] high; no limit while the window is unknown. */
    fun maxHeight(windowHeight: Dp): Dp =
        if (windowHeight.value > 0f) windowHeight * MAX_HEIGHT_RATIO else Dp.Infinity

    /** Whether the download button is shown under a preview: on request, and never under the fallback. */
    fun showsFooter(alwaysShowDownload: Boolean, kind: MonarchFilePreviewKind): Boolean =
        alwaysShowDownload && kind != MonarchFilePreviewKind.None

    /** Whether the content that [kind] needs was given. */
    fun hasContent(
        kind: MonarchFilePreviewKind,
        image: Boolean,
        pdf: Boolean,
        text: Boolean,
        player: Boolean,
    ): Boolean = when (kind) {
        MonarchFilePreviewKind.Image -> image
        MonarchFilePreviewKind.Pdf -> pdf
        MonarchFilePreviewKind.Text -> text
        MonarchFilePreviewKind.Video, MonarchFilePreviewKind.Audio -> player
        MonarchFilePreviewKind.None -> false
    }

    /**
     * Decides what to show. A detected kind is shown when its content was
     * given and it has not failed. Otherwise the fallback says that the
     * preview failed, that the type has no preview, or nothing while the
     * content of a type that has a preview is still to come.
     */
    fun resolve(detected: MonarchFilePreviewKind, hasContent: Boolean, failed: Boolean): Resolution = when {
        failed -> Resolution(MonarchFilePreviewKind.None, Message.Failed)
        detected == MonarchFilePreviewKind.None -> Resolution(MonarchFilePreviewKind.None, Message.Unavailable)
        hasContent -> Resolution(detected, null)
        else -> Resolution(MonarchFilePreviewKind.None, null)
    }
}
