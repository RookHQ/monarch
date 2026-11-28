package com.rookhq.monarch.components.collaboration.richtexteditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import kotlin.math.max
import kotlin.math.min

/** Component tokens and logic of MonarchRichTextEditor, built from primitives and roles only. */
internal object MonarchRichTextEditorTokens {
    /** The padding of the text inside the box. */
    val TextPadding: Dp = MonarchTokens.Space.space3.dp

    /** The space between the box, the count and the suggestions. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The smallest height of the text area, about four lines, as the web's 6rem. */
    val MinTextHeight: Dp = MonarchTokens.Space.space24.dp

    /** The box: the surface fill, or the subtle fill when disabled; the danger border when invalid or too long. */
    fun box(colors: MonarchColors, enabled: Boolean, error: Boolean): MonarchSurfaceColors = MonarchSurfaceColors(
        container = if (enabled) colors.bgSurface else colors.bgSubtle,
        content = if (enabled) colors.textDefault else colors.textDisabled,
        border = when {
            !enabled -> colors.borderDefault
            error -> colors.dangerStrong
            else -> colors.borderInput
        },
    )

    /** The prompt in the subtle text colour, as a text field's placeholder. */
    fun prompt(colors: MonarchColors): Color = colors.textSubtle

    /** The count: muted, or strong danger past the limit. */
    fun count(colors: MonarchColors, over: Boolean): Color = if (over) colors.dangerStrong else colors.textMuted

    /** The cursor colour. */
    fun cursor(colors: MonarchColors, error: Boolean): Color = if (error) colors.dangerStrong else colors.accentStrong

    /** How [mark] looks in the text. */
    fun style(mark: MonarchRichTextMark, typography: MonarchTypography, colors: MonarchColors): SpanStyle =
        when (mark) {
            MonarchRichTextMark.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
            MonarchRichTextMark.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
            MonarchRichTextMark.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            MonarchRichTextMark.Code -> SpanStyle(fontFamily = typography.code.fontFamily, background = colors.bgMuted)
            MonarchRichTextMark.Link -> SpanStyle(color = colors.textLink, textDecoration = TextDecoration.Underline)
            MonarchRichTextMark.Heading1 -> typography.title.toSpanStyle()
            MonarchRichTextMark.Heading2 -> typography.heading.toSpanStyle()
            MonarchRichTextMark.Heading3 -> typography.subheading.toSpanStyle()
        }

    /**
     * [text] with [spans] drawn. Strikethrough inside a link keeps both lines.
     * With [links], links are `LinkAnnotation.Url`s that open on a tap, for
     * the read-only document.
     */
    fun annotated(
        text: String,
        spans: List<MonarchRichTextSpan>,
        typography: MonarchTypography,
        colors: MonarchColors,
        links: TextLinkStyles? = null,
    ): AnnotatedString = buildAnnotatedString {
        append(text)
        val visible = spans.filter { it.start < it.end && it.end <= text.length }
        visible.sortedBy { it.mark.isHeading.not() }.forEach { span ->
            val url = span.url
            if (span.mark == MonarchRichTextMark.Link && links != null && url != null) {
                addLink(LinkAnnotation.Url(url, links), span.start, span.end)
            } else {
                addStyle(style(span.mark, typography, colors), span.start, span.end)
            }
        }
        val struck = visible.filter { it.mark == MonarchRichTextMark.Strikethrough }
        visible.filter { it.mark == MonarchRichTextMark.Link }.forEach { link ->
            struck.forEach { strike ->
                val start = max(link.start, strike.start)
                val end = min(link.end, strike.end)
                if (start < end) addStyle(BothLines, start, end)
            }
        }
    }

    private val BothLines = SpanStyle(
        textDecoration = TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)),
    )
}
