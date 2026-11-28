package dev.monarchui.components.intelligence.aimarkdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdownInline as Inline
import dev.monarchui.tokens.MonarchTokens

/** The span styles of inline tokens, resolved from the theme. */
internal class MonarchAiMarkdownSpanStyles(
    val link: TextLinkStyles,
    val code: SpanStyle,
    val host: SpanStyle,
    val footnote: SpanStyle,
    val deleted: SpanStyle,
)

/**
 * Turns inline tokens into an `AnnotatedString`, never into markup: links
 * become `LinkAnnotation.Url` only when [MonarchAiMarkdownLinks.safeLink]
 * allows them, and keep their text otherwise; links do not nest; images
 * become their alt text; footnote references become superscript numbers.
 */
internal class MonarchAiMarkdownAnnotator(
    private val numbers: Map<String, Int>,
    private val styles: MonarchAiMarkdownSpanStyles,
    private val showLinkHosts: Boolean,
    private val linkHost: (String) -> String,
    private val onOpenLink: ((String) -> Unit)?,
) {
    fun annotate(nodes: List<Inline>): AnnotatedString = buildAnnotatedString { appendNodes(nodes, inLink = false) }

    private fun AnnotatedString.Builder.appendNodes(nodes: List<Inline>, inLink: Boolean) {
        for (node in nodes) {
            when (node) {
                // A soft line break inside a paragraph is a space, as in a browser.
                is Inline.Text -> append(node.value.replace('\n', ' '))
                is Inline.Code -> withStyle(styles.code) { append(node.value) }
                is Inline.Math -> withStyle(styles.code) { append(node.value) }
                is Inline.Styled -> withStyle(spanStyle(node.kind)) { appendNodes(node.children, inLink) }
                Inline.Break -> append('\n')
                is Inline.Image -> append(node.alt)
                is Inline.Link -> appendLink(node, inLink)
                is Inline.FootnoteRef -> {
                    val number = numbers[node.label]
                    if (number == null) {
                        append("[^${node.label}]")
                    } else {
                        withStyle(styles.footnote) { append(number.toString()) }
                    }
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendLink(node: Inline.Link, inLink: Boolean) {
        if (inLink || !MonarchAiMarkdownLinks.safeLink(node.href, relativeAllowed = onOpenLink != null)) {
            appendNodes(node.children, inLink = true)
            return
        }
        val host = if (showLinkHosts) {
            MonarchAiMarkdownLinks.shownHost(node.href, MonarchAiMarkdownParser.inlineText(node.children))
        } else {
            null
        }
        val listener = onOpenLink?.let { open -> LinkInteractionListener { open(node.href) } }
        withLink(LinkAnnotation.Url(node.href, styles.link, listener)) { appendNodes(node.children, inLink = true) }
        if (host != null) withStyle(styles.host) { append(" ${linkHost(host)}") }
    }

    private fun spanStyle(kind: MonarchAiMarkdownEmphasis): SpanStyle = when (kind) {
        MonarchAiMarkdownEmphasis.Em -> SpanStyle(fontStyle = FontStyle.Italic)
        MonarchAiMarkdownEmphasis.Strong -> SpanStyle(fontWeight = FontWeight(MonarchTokens.FontWeight.semibold))
        MonarchAiMarkdownEmphasis.Del -> styles.deleted
    }
}
