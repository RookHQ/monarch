package dev.monarchui.components.intelligence.aiartifactcanvas

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchAiArtifactCanvas. */
internal object MonarchAiArtifactCanvasTokens {
    val Padding: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val ItemGap: Dp = MonarchTokens.Space.space2.dp
    val TitleGap: Dp = MonarchTokens.Space.space0_5.dp

    /** Narrower windows show the canvas as a sheet when the placement is Auto, as the web does under 640px. */
    val SheetBelow: Dp = MonarchTokens.Breakpoint.sm.dp

    /** Whether [placement] is a sheet in a window [windowWidth] wide. */
    fun asSheet(placement: MonarchAiArtifactCanvasPlacement, windowWidth: Dp): Boolean = when (placement) {
        MonarchAiArtifactCanvasPlacement.Sheet -> true
        MonarchAiArtifactCanvasPlacement.Inline -> false
        MonarchAiArtifactCanvasPlacement.Auto -> windowWidth < SheetBelow
    }

    /** Whether the kind has a built-in preview: documents and tables as Markdown, web pages and images. */
    fun hasBuiltInPreview(kind: MonarchAiArtifactKind): Boolean = when (kind) {
        MonarchAiArtifactKind.Document, MonarchAiArtifactKind.Table,
        MonarchAiArtifactKind.Html, MonarchAiArtifactKind.Image,
        -> true
        MonarchAiArtifactKind.Code, MonarchAiArtifactKind.Chart -> false
    }

    /** The tab shown: the requested one, or Code when there is no preview. */
    fun shownTab(requested: MonarchAiArtifactCanvasTab, previewAvailable: Boolean): MonarchAiArtifactCanvasTab =
        if (previewAvailable) requested else MonarchAiArtifactCanvasTab.Code

    /** The language of the Code tab by kind, as on the web. */
    fun defaultLanguage(kind: MonarchAiArtifactKind): String? = when (kind) {
        MonarchAiArtifactKind.Document, MonarchAiArtifactKind.Table -> "markdown"
        MonarchAiArtifactKind.Chart -> "json"
        MonarchAiArtifactKind.Html -> "html"
        MonarchAiArtifactKind.Code, MonarchAiArtifactKind.Image -> null
    }

    @StringRes
    fun kindName(kind: MonarchAiArtifactKind): Int = when (kind) {
        MonarchAiArtifactKind.Document -> R.string.monarch_aiartifactcanvas_kind_document
        MonarchAiArtifactKind.Code -> R.string.monarch_aiartifactcanvas_kind_code
        MonarchAiArtifactKind.Table -> R.string.monarch_aiartifactcanvas_kind_table
        MonarchAiArtifactKind.Chart -> R.string.monarch_aiartifactcanvas_kind_chart
        MonarchAiArtifactKind.Image -> R.string.monarch_aiartifactcanvas_kind_image
        MonarchAiArtifactKind.Html -> R.string.monarch_aiartifactcanvas_kind_html
    }

    /**
     * Wraps untrusted HTML for the sandboxed `WebView`: a Content Security
     * Policy that blocks every network request and script, and the HTML with
     * scripts, frames, embeds, `<base>` and `<meta http-equiv>` removed (see
     * [inertHtml]).
     */
    fun sandboxedDocument(html: String): String =
        "<!doctype html><html><head><meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; img-src data:; " +
            "style-src 'unsafe-inline'; font-src data:; form-action 'none'; base-uri 'none'; frame-src 'none'; " +
            "object-src 'none'; script-src 'none'\">" +
            "</head><body>" + inertHtml(html) + "</body></html>"

    /** [html] without the elements that run code, load other documents or redirect. */
    fun inertHtml(html: String): String =
        InertPatterns.fold(html) { text, pattern -> pattern.replace(text, "") }

    private val InertPatterns: List<Regex> = listOf(
        Regex("<(script|iframe|frame|object|embed|template)\\b[\\s\\S]*?</\\1\\s*>", RegexOption.IGNORE_CASE),
        Regex("<(script|iframe|frame|object|embed|base|link|frameset)\\b[^>]*>", RegexOption.IGNORE_CASE),
        Regex("<meta\\b[^>]*http-equiv[^>]*>", RegexOption.IGNORE_CASE),
        Regex("\\son[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", RegexOption.IGNORE_CASE),
    )

    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    fun warningColor(colors: MonarchColors): Color = colors.warningStrong
}
