package dev.monarchui.components.search.searchresultitem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchSearchResultItem, built from primitives and roles only. */
internal object MonarchSearchResultItemTokens {
    /** The side of the leading tile. */
    val TileSize: Dp = MonarchTokens.Control.lg.dp

    /** The space between the path and the snippet. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The most lines of a snippet. */
    const val SNIPPET_LINES: Int = 4

    /** The visible separator between path segments. A glyph, not text to translate. */
    const val PATH_SEPARATOR: String = " / "

    /** The weight of a matched run, so a match does not rest on the fill colour alone. */
    val MatchWeight: FontWeight = FontWeight.SemiBold

    /** The snippet: body text at the 14 of the title, as on the web, so it is never larger than the title. */
    fun snippetStyle(typography: MonarchTypography): TextStyle =
        typography.body.copy(fontSize = typography.label.fontSize)

    /** The colour of the title: the default text, whether or not the row does something (as the web's list items). */
    fun titleColor(interactive: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        // The web result rows read as list items, not links: the default text, whatever the row does.
        interactive -> colors.textDefault
        else -> colors.textDefault
    }

    /** The fill of a matched run. */
    fun matchFill(colors: MonarchColors): Color = colors.warningSubtle

    /** The text colour of a matched run, readable on [matchFill]. */
    fun matchText(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of the path, the snippet and the meta. */
    fun secondaryColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the path separators. */
    fun separatorColor(colors: MonarchColors): Color = colors.textSubtle

    /** The fill and the content colour of the leading tile. */
    fun tileFill(colors: MonarchColors): Color = colors.bgMuted

    /** The content colour of the leading tile. */
    fun tileContent(colors: MonarchColors): Color = colors.textMuted

    /** [segments] as styled text: matched runs on the match fill, semibold. */
    fun highlighted(
        segments: List<MonarchSearchResultItemHighlight.Segment>,
        enabled: Boolean,
        colors: MonarchColors,
    ): AnnotatedString = buildAnnotatedString {
        val match = SpanStyle(
            color = matchText(enabled, colors),
            background = matchFill(colors),
            fontWeight = MatchWeight,
        )
        segments.forEach { segment ->
            if (segment.match) withStyle(match) { append(segment.text) } else append(segment.text)
        }
    }

    /** The visible path, with the separators in their own colour. */
    fun path(segments: List<String>, colors: MonarchColors): AnnotatedString = buildAnnotatedString {
        segments.forEachIndexed { index, segment ->
            if (index > 0) withStyle(SpanStyle(color = separatorColor(colors))) { append(PATH_SEPARATOR) }
            append(segment)
        }
    }
}
