package dev.monarchui.components.intelligence.aisourcepreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.favicon.MonarchFavicon
import dev.monarchui.components.datadisplay.favicon.MonarchFaviconSize
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aisourcelist.MonarchAiSourceListTokens
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * The content of a sheet that shows the document, record or page an AI
 * answer cited, with the quoted passage highlighted and scrolled into view,
 * and a button that opens the original.
 *
 * Present it in a `MonarchBottomSheet`, which owns the drag handle,
 * the scrim, the back gesture and returning TalkBack focus; [onClose] adds a
 * Close button for people who do not drag. The header is the favicon, the
 * title as a heading and the host and kind. [content] is plain text split
 * into paragraphs at blank lines; the first match of [quote], ignoring case
 * and spacing, is highlighted with the AI subtle fill and an underline, and
 * scrolled to the middle when the preview appears (without animation when
 * animations are off). A quote that is not in the content is shown on its
 * own under "Quoted passage". The text can be selected.
 *
 * Open original opens [originalUrl], or the source's address, through
 * `LocalUriHandler` or [onOpenOriginal]; it is shown only for http, https and
 * mailto addresses.
 *
 * @param source the cited source from the conversation model: title, address, kind and product.
 * @param modifier the modifier of the preview.
 * @param quote the cited passage, for example the quote of the `MonarchAiCitationPart`.
 * @param content the text of the source. Paragraphs are separated by blank lines.
 * @param originalUrl where Open original goes. Defaults to the source's address.
 * @param favicon the favicon the app loaded for the source; without it a letter tile is shown.
 * @param labels replacements for the quote caption, Open original, the empty text, Close and the kind names.
 * @param onClose adds a Close button that calls this, for example to hide the sheet.
 * @param onOpenOriginal called with the address instead of opening it with `LocalUriHandler`.
 */
@Composable
public fun MonarchAiSourcePreview(
    source: MonarchAiSourcePart,
    modifier: Modifier = Modifier,
    quote: String? = null,
    content: String? = null,
    originalUrl: String? = null,
    favicon: Painter? = null,
    labels: MonarchAiSourcePreviewLabels = MonarchAiSourcePreviewLabels(),
    onClose: (() -> Unit)? = null,
    onOpenOriginal: ((String) -> Unit)? = null,
) {
    val url = MonarchAiSourceListTokens.safeUrl(originalUrl ?: source.url)
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier.padding(MonarchAiSourcePreviewTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(MonarchAiSourcePreviewTokens.SectionGap),
    ) {
        Header(source, favicon, labels, onClose)
        Body(quote, content, labels, Modifier.weight(1f, fill = false))
        if (url != null) {
            MonarchButton(
                text = monarchString(labels.openOriginal, R.string.monarch_aisourcepreview_open_original),
                onClick = { if (onOpenOriginal != null) onOpenOriginal(url) else uriHandler.openUri(url) },
                modifier = Modifier.fillMaxWidth(),
                variant = MonarchButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun Header(
    source: MonarchAiSourcePart,
    favicon: Painter?,
    labels: MonarchAiSourcePreviewLabels,
    onClose: (() -> Unit)?,
) {
    val host = MonarchAiSourceListTokens.host(source.url)
    val meta = MonarchAiSourceListTokens.meta(host, source.product, kindName(source.kind, labels))
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiSourcePreviewTokens.HeaderGap),
        verticalAlignment = Alignment.Top,
    ) {
        MonarchFavicon(image = favicon, name = host ?: source.title, size = MonarchFaviconSize.Md)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchAiSourcePreviewTokens.TextGap),
        ) {
            Text(
                text = source.title,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.subheading,
                color = MonarchTheme.colors.textDefault,
            )
            Text(text = meta, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        }
        if (onClose != null) {
            MonarchButton(
                text = monarchString(labels.close, R.string.monarch_aisourcepreview_close),
                onClick = onClose,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun Body(quote: String?, content: String?, labels: MonarchAiSourcePreviewLabels, modifier: Modifier) {
    val scroll = rememberScrollState()
    val motion = MonarchTheme.motion
    var viewport by remember { mutableIntStateOf(0) }
    var mark by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var centred by remember(quote, content) { mutableStateOf(false) }
    val quotedLabel = monarchString(labels.quoted, R.string.monarch_aisourcepreview_quoted)
    // Keyed on readiness only: a new key would cancel the scroll while it runs.
    LaunchedEffect(mark != null && viewport > 0) {
        val (top, height) = mark ?: return@LaunchedEffect
        if (centred || viewport == 0) return@LaunchedEffect
        scroll.animateScrollTo(
            MonarchAiSourcePreviewTokens.centredScroll(top, height, viewport, scroll.maxValue),
            motion.spec(),
        )
        centred = true
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewport = it.height }
            .verticalScroll(scroll),
    ) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(MonarchAiSourcePreviewTokens.ParagraphGap)) {
                if (MonarchAiSourcePreviewTokens.empty(quote, content)) {
                    Text(
                        text = monarchString(labels.noPreview, R.string.monarch_aisourcepreview_no_preview),
                        style = MonarchTheme.typography.body,
                        color = MonarchTheme.colors.textMuted,
                    )
                }
                if (quote != null && MonarchAiSourcePreviewTokens.showsQuoteBlock(quote, content)) {
                    QuoteBlock(quote.trim(), quotedLabel)
                }
                val paragraphs = content?.let { MonarchAiSourcePreviewTokens.paragraphs(it, quote) }.orEmpty()
                var first = true
                paragraphs.forEach { paragraph ->
                    val tracked = paragraph.marked && first
                    if (paragraph.marked) first = false
                    ParagraphText(
                        paragraph = paragraph,
                        quotedLabel = quotedLabel,
                        modifier = if (tracked) {
                            Modifier.onGloballyPositioned {
                                mark = it.positionInParent().y.roundToInt() to it.size.height
                            }
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ParagraphText(
    paragraph: MonarchAiSourcePreviewTokens.Paragraph,
    quotedLabel: String,
    modifier: Modifier,
) {
    val colors = MonarchTheme.colors
    Text(
        text = buildAnnotatedString {
            append(paragraph.before)
            withStyle(
                SpanStyle(
                    background = MonarchAiSourcePreviewTokens.markColor(colors),
                    textDecoration = TextDecoration.Underline,
                ),
            ) { append(paragraph.mark) }
            append(paragraph.after)
        },
        modifier = modifier.semantics { if (paragraph.marked) stateDescription = quotedLabel },
        style = MonarchTheme.typography.body,
        color = colors.textDefault,
    )
}

@Composable
private fun QuoteBlock(quote: String, quotedLabel: String) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiSourcePreviewTokens.QuoteBarGap),
    ) {
        Box(
            modifier = Modifier
                .width(MonarchAiSourcePreviewTokens.QuoteBarWidth)
                .fillMaxHeight()
                .background(MonarchAiSourcePreviewTokens.quoteBarColor(colors)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MonarchAiSourcePreviewTokens.TextGap)) {
            Text(text = quotedLabel, style = MonarchTheme.typography.caption, color = colors.textMuted)
            Text(text = quote, style = MonarchTheme.typography.body, color = colors.textDefault)
        }
    }
}

@Composable
private fun kindName(kind: MonarchAiSourceKind, labels: MonarchAiSourcePreviewLabels): String =
    labels.kind?.invoke(kind) ?: stringResource(
        when (kind) {
            MonarchAiSourceKind.Web -> R.string.monarch_aisourcelist_kind_web
            MonarchAiSourceKind.Doc -> R.string.monarch_aisourcelist_kind_doc
            MonarchAiSourceKind.Record -> R.string.monarch_aisourcelist_kind_record
            MonarchAiSourceKind.File -> R.string.monarch_aisourcelist_kind_file
        },
    )
