package dev.monarchui.components.search.searchresultitem

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIcon
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * One search result: the title, the breadcrumb path, a snippet with the
 * matched words highlighted, a leading file icon or tile and trailing meta.
 *
 * Built on `MonarchListRow`. Matched words are drawn on the subtle
 * warning fill and in semibold, so a match does not rest on colour alone.
 * Every string is shown as plain text: the snippet's `<mark>` markers are
 * the only markup ever read. With [onClick] the whole row is one button and
 * the title takes the link colour; without it the row is static text. To
 * navigate, open the destination from [onClick].
 *
 * @param title the result's title, as plain text.
 * @param modifier the modifier of the row.
 * @param path breadcrumb segments, such as `listOf("Handbook", "Travel")`, shown with a slash between them.
 * @param snippet a short excerpt of the matching content, as plain text. At most three lines.
 * @param terms words to highlight in the title and snippet, matched without regard to case. Use
 * [monarchQueryTerms].
 * @param titleRanges exact matches in the title, as UTF-16 offsets with an exclusive end, as search services
 * and the web count them. Replaces [terms] for the title.
 * @param snippetRanges exact matches in the snippet. Replaces [terms] for the snippet.
 * @param snippetMarkTags the snippet carries `<mark>` and `</mark>` markers, for example from PostgreSQL
 * `ts_headline`. Only those two markers are read; anything else stays text.
 * @param fileName a file name; its `MonarchFileTypeIcon` fills the leading slot. Takes precedence over [icon].
 * @param enabled whether the row responds to taps. A disabled row draws its text in the disabled colour.
 * @param onClick called when the row is activated. Without it the row is not interactive.
 * @param icon a leading icon or avatar, drawn on a muted tile. Decorative.
 * @param meta trailing details such as a badge, a date or an owner, in the caption type and muted colour.
 * @param pathLabel replaces the built-in "Location" spoken before the path.
 * @param interactionSource the source of the press, focus and hover interactions of the row.
 * @param destructive draws the title in the danger colour, for a result that removes or archives something.
 */
@Composable
public fun MonarchSearchResultItem(
    title: String,
    modifier: Modifier = Modifier,
    path: List<String> = emptyList(),
    snippet: String? = null,
    terms: List<String> = emptyList(),
    titleRanges: List<TextRange>? = null,
    snippetRanges: List<TextRange>? = null,
    snippetMarkTags: Boolean = false,
    fileName: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    meta: (@Composable () -> Unit)? = null,
    pathLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    destructive: Boolean = false,
) {
    val tokens = MonarchSearchResultItemTokens
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val highlight = MonarchSearchResultItemHighlight
    val titleText = remember(title, terms, titleRanges, enabled, colors) {
        tokens.highlighted(highlight.segments(title, terms, titleRanges), enabled, colors)
    }
    val snippetText = remember(snippet, terms, snippetRanges, snippetMarkTags, enabled, colors) {
        snippet?.let {
            tokens.highlighted(highlight.segments(it, terms, snippetRanges, snippetMarkTags), enabled, colors)
        }
    }
    val spokenPath = if (path.isEmpty()) {
        null
    } else {
        stringResource(
            R.string.monarch_searchresultitem_path,
            monarchString(pathLabel, R.string.monarch_searchresultitem_location),
            path.joinToString(stringResource(R.string.monarch_searchresultitem_path_separator)),
        )
    }
    val secondary = tokens.secondaryColor(enabled, colors)
    MonarchListRow(
        headlineContent = {
            Text(
                text = titleText,
                style = typography.label,
                color = if (destructive && enabled) {
                    colors.dangerStrong
                } else {
                    tokens.titleColor(onClick != null, enabled, colors)
                },
            )
        },
        modifier = if (onClick == null) modifier.semantics(mergeDescendants = true) { } else modifier,
        supportingContent = if (spokenPath != null || snippetText != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
                    if (spokenPath != null) {
                        Text(
                            text = tokens.path(path, colors),
                            modifier = Modifier.clearAndSetSemantics { contentDescription = spokenPath },
                            style = typography.caption,
                            color = secondary,
                        )
                    }
                    if (snippetText != null) {
                        Text(
                            text = snippetText,
                            style = tokens.snippetStyle(typography),
                            color = secondary,
                            maxLines = tokens.SNIPPET_LINES,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            null
        },
        leadingContent = leading(fileName, icon),
        trailingContent = meta?.let { slot ->
            {
                CompositionLocalProvider(
                    LocalContentColor provides secondary,
                    LocalTextStyle provides typography.caption,
                    content = slot,
                )
            }
        },
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    )
}

/** The leading slot: the file type icon, the caller's icon on a muted tile, or nothing. */
private fun leading(fileName: String?, icon: (@Composable () -> Unit)?): (@Composable () -> Unit)? = when {
    fileName != null -> {
        { MonarchFileTypeIcon(fileName = fileName, decorative = true) }
    }
    icon != null -> {
        {
            val colors = MonarchTheme.colors
            Box(
                modifier = Modifier
                    .size(MonarchSearchResultItemTokens.TileSize)
                    .background(MonarchSearchResultItemTokens.tileFill(colors), MonarchTheme.shapes.md)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides MonarchSearchResultItemTokens.tileContent(colors)) {
                    icon()
                }
            }
        }
    }
    else -> null
}
