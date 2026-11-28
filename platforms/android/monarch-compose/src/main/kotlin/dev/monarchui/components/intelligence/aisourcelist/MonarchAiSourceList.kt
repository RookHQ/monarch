package dev.monarchui.components.intelligence.aisourcelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.favicon.MonarchFavicon
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The block of sources under an AI answer: each with its favicon, `[n]`
 * number, title, host, product, kind and snippet, grouped into Used and
 * Considered when some sources were not used, and collapsed to the first few.
 *
 * Built by Monarch as a `Column` of source rows with `MonarchFavicon` and a
 * `MonarchButton` that shows all or fewer. With [onOpen] every row is a
 * button that calls it, for example to show `MonarchAiSourcePreview`;
 * without it a row with an http, https or mailto address opens it through
 * `LocalUriHandler`, and any other row is plain text. Each row is one
 * TalkBack element: "Source 1: title", then the meta line and the snippet.
 *
 * Monarch loads no images: pass [favicon] to give each source the painter
 * the app loaded from its `favicon` address; otherwise a letter tile is shown.
 *
 * @param items the sources in citation order; the first is `[1]`. `MonarchAiSourceListItem.sources` builds
 * them from a message.
 * @param modifier the modifier of the list.
 * @param collapseAfter how many sources show while collapsed. `null` never collapses.
 * @param initiallyExpanded whether a collapsible list starts expanded.
 * @param numbered shows the `[n]` number that matches each source's citation marker.
 * @param favicon the loaded favicon of a source, or `null` for the letter tile.
 * @param plain lists each source as one line, "1. Title", with the title as a link, and no icon or details.
 * @param labels replacements for the built-in strings.
 * @param onOpen makes each source a button that calls this. Without it, safe addresses open as links.
 */
@Composable
public fun MonarchAiSourceList(
    items: List<MonarchAiSourceListItem>,
    modifier: Modifier = Modifier,
    collapseAfter: Int? = MonarchAiSourceListTokens.DEFAULT_COLLAPSE_AFTER,
    initiallyExpanded: Boolean = false,
    numbered: Boolean = true,
    favicon: (@Composable (MonarchAiSourcePart) -> Painter?)? = null,
    labels: MonarchAiSourceListLabels = MonarchAiSourceListLabels(),
    onOpen: ((MonarchAiSourceListItem) -> Unit)? = null,
    plain: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val shown = MonarchAiSourceListTokens.shown(items, collapseAfter, expanded)
    val grouped = MonarchAiSourceListTokens.grouped(items)
    val row: @Composable (MonarchAiSourceListTokens.Numbered) -> Unit = { entry ->
        SourceRow(entry, numbered, favicon, labels, onOpen, plain)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchAiSourceListTokens.SectionGap)) {
        Heading(monarchString(labels.heading, R.string.monarch_aisourcelist_heading), MonarchTheme.typography.label)
        if (grouped) {
            val usedCount = items.count { it.used }
            val consideredCount = items.size - usedCount
            val usedShown = shown.filter { it.item.used }
            val consideredShown = shown.filterNot { it.item.used }
            if (usedCount > 0) {
                Heading(
                    labels.used?.invoke(usedCount) ?: stringResource(R.string.monarch_aisourcelist_used, usedCount),
                    MonarchTheme.typography.caption,
                )
                Column { usedShown.forEach { row(it) } }
            }
            if (consideredCount > 0 && consideredShown.isNotEmpty()) {
                Heading(
                    labels.considered?.invoke(consideredCount)
                        ?: stringResource(R.string.monarch_aisourcelist_considered, consideredCount),
                    MonarchTheme.typography.caption,
                )
                Column { consideredShown.forEach { row(it) } }
            }
        } else {
            Column { shown.forEach { row(it) } }
        }
        if (MonarchAiSourceListTokens.collapsible(items.size, collapseAfter)) {
            MonarchButton(
                text = if (expanded) {
                    monarchString(labels.showFewer, R.string.monarch_aisourcelist_show_fewer)
                } else {
                    labels.showAll?.invoke(items.size)
                        ?: pluralStringResource(R.plurals.monarch_aisourcelist_show_all, items.size, items.size)
                },
                onClick = { expanded = !expanded },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                leadingIcon = if (expanded) MonarchDefaultIcons.ChevronUp else MonarchDefaultIcons.ChevronDown,
            )
        }
    }
}

@Composable
private fun Heading(text: String, style: TextStyle) {
    Text(
        text = text,
        modifier = Modifier.semantics { heading() },
        style = style,
        color = MonarchTheme.colors.textMuted,
    )
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun SourceRow(
    entry: MonarchAiSourceListTokens.Numbered,
    numbered: Boolean,
    favicon: (@Composable (MonarchAiSourcePart) -> Painter?)?,
    labels: MonarchAiSourceListLabels,
    onOpen: ((MonarchAiSourceListItem) -> Unit)?,
    plain: Boolean,
) {
    val source = entry.item.source
    val uriHandler = LocalUriHandler.current
    val url = MonarchAiSourceListTokens.safeUrl(source.url)
    val open: (() -> Unit)? = when {
        onOpen != null -> ({ onOpen(entry.item) })
        url != null -> ({ uriHandler.openUri(url) })
        else -> null
    }
    val host = MonarchAiSourceListTokens.host(source.url)
    val meta = MonarchAiSourceListTokens.meta(host, source.product, kindName(source.kind, labels))
    val name = if (numbered) {
        labels.source?.invoke(entry.number, source.title)
            ?: stringResource(R.string.monarch_aisourcelist_source, entry.number, source.title)
    } else {
        source.title
    }
    val spoken = listOfNotNull(name, meta, entry.item.snippet?.takeIf { it.isNotBlank() }).joinToString(", ")
    val shape = MonarchShapeDefaults.Row
    val interaction = if (open != null) {
        Modifier
            .monarchFocusRing(shape)
            .clickable(
                indication = ripple(),
                interactionSource = null,
                role = Role.Button,
                onClickLabel = monarchString(labels.openSource, R.string.monarch_aisourcelist_open_source),
                onClick = open,
            )
    } else {
        Modifier
    }
    if (plain) {
        PlainSourceRow(entry.number, source.title, open != null, interaction, spoken)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .then(interaction)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .padding(
                horizontal = MonarchAiSourceListTokens.RowHorizontalPadding,
                vertical = MonarchAiSourceListTokens.RowVerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiSourceListTokens.RowGap),
        verticalAlignment = Alignment.Top,
    ) {
        val picture = favicon?.invoke(source)
        if (picture != null) {
            MonarchFavicon(image = picture, name = host ?: source.title)
        } else {
            // As the web: the kind of source is a document, globe or table glyph, not a letter tile.
            CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
                Box(modifier = Modifier.clearAndSetSemantics { }) {
                    when (source.kind) {
                        MonarchAiSourceKind.Web -> MonarchDefaultIcons.Globe()
                        MonarchAiSourceKind.Record -> MonarchDefaultIcons.Table()
                        MonarchAiSourceKind.Doc, MonarchAiSourceKind.File -> MonarchDefaultIcons.Doc()
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(MonarchAiSourceListTokens.LineGap),
        ) {
            val colors = MonarchTheme.colors
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchAiSourceListTokens.LineGap)) {
                if (numbered) {
                    Text(text = "[${entry.number}]", style = MonarchTheme.typography.label, color = colors.aiCitation)
                }
                Text(
                    text = source.title,
                    style = MonarchTheme.typography.label,
                    color = MonarchAiSourceListTokens.titleColor(open != null, colors),
                )
            }
            Text(text = meta, style = MonarchTheme.typography.caption, color = colors.textMuted)
            val snippet = entry.item.snippet
            if (!snippet.isNullOrBlank()) {
                Text(
                    text = snippet,
                    style = MonarchTheme.typography.caption,
                    color = colors.textMuted,
                    maxLines = MonarchAiSourceListTokens.SNIPPET_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A source as one line, "1. Title", the title as a link. */
@Composable
private fun PlainSourceRow(number: Int, title: String, opens: Boolean, modifier: Modifier, spoken: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .then(modifier)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .padding(horizontal = MonarchAiSourceListTokens.RowHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiSourceListTokens.LineGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$number.", style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        Text(
            text = title,
            style = MonarchTheme.typography.caption.copy(textDecoration = TextDecoration.Underline),
            color = MonarchAiSourceListTokens.titleColor(opens, MonarchTheme.colors),
        )
    }
}

@Composable
private fun kindName(kind: MonarchAiSourceKind, labels: MonarchAiSourceListLabels): String =
    labels.kind?.invoke(kind) ?: stringResource(
        when (kind) {
            MonarchAiSourceKind.Web -> R.string.monarch_aisourcelist_kind_web
            MonarchAiSourceKind.Doc -> R.string.monarch_aisourcelist_kind_doc
            MonarchAiSourceKind.Record -> R.string.monarch_aisourcelist_kind_record
            MonarchAiSourceKind.File -> R.string.monarch_aisourcelist_kind_file
        },
    )
