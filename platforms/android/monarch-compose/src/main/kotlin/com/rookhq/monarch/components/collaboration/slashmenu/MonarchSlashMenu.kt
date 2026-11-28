package com.rookhq.monarch.components.collaboration.slashmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The / menu of a composer or document editor: a grouped list of blocks to
 * insert, filtered by what is typed after the slash, in a strip that the app
 * places directly above the text field, so it sits above the keyboard.
 *
 * Built by Monarch from `MonarchListRow` rows in a `LazyColumn`, the
 * same placement as `MonarchMentionPicker`: Compose gives no caret rectangle
 * to anchor a popup to. Find the command with [MonarchSlashMenuQuery.find]
 * as the text changes, and in [onSelect] replace it with
 * [MonarchSlashMenuQuery.replacing]. The menu filters [items] by the query
 * with [MonarchSlashMenuDefaults.filter]. Focus stays in the text field; the
 * number of entries, or that nothing matches, is announced through a polite
 * live region. Nothing is drawn while [query] is `null`.
 *
 * @param query the command being typed, or `null` to close the menu.
 * @param items every entry the menu offers.
 * @param onSelect called with the chosen entry.
 * @param modifier the modifier of the strip.
 * @param groupOrder group headings in display order. Groups not listed follow in the order they first appear.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchSlashMenu(
    query: MonarchSlashMenuQuery?,
    items: List<MonarchSlashMenuItem>,
    onSelect: (MonarchSlashMenuItem) -> Unit,
    modifier: Modifier = Modifier,
    groupOrder: List<String> = emptyList(),
    labels: MonarchSlashMenuLabels = MonarchSlashMenuLabels(),
) {
    MonarchSlashMenuStrip(query, items, onSelect, modifier, groupOrder, labels, assistant = false)
}

/**
 * The strip of [MonarchSlashMenu]. With [assistant] the first entry that can be chosen is drawn as the active one
 * in the assistant's tone, and the names are set in code type, as the web's AI slash commands are.
 */
@Composable
internal fun MonarchSlashMenuStrip(
    query: MonarchSlashMenuQuery?,
    items: List<MonarchSlashMenuItem>,
    onSelect: (MonarchSlashMenuItem) -> Unit,
    modifier: Modifier,
    groupOrder: List<String>,
    labels: MonarchSlashMenuLabels,
    assistant: Boolean,
) {
    if (query == null) return
    val tokens = MonarchSlashMenuTokens
    val colors = MonarchTheme.colors
    val matches = remember(items, query.text) { MonarchSlashMenuDefaults.filter(items, query.text) }
    val typed = "${query.trigger}${query.text}"
    val empty = labels.empty?.invoke(typed) ?: stringResource(R.string.monarch_slashmenu_empty, typed)
    val announcement = if (matches.isEmpty()) {
        empty
    } else {
        labels.count?.invoke(matches.size)
            ?: pluralStringResource(R.plurals.monarch_slashmenu_count, matches.size, matches.size)
    }
    val name = monarchString(labels.label, R.string.monarch_slashmenu_label)
    val border = tokens.border(colors)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.background(colors))
            .drawBehind {
                val width = tokens.BorderWidth.toPx()
                drawLine(border, Offset(0f, width / 2), Offset(size.width, width / 2), width)
            }
            .semantics {
                contentDescription = name
                stateDescription = announcement
                liveRegion = LiveRegionMode.Polite
                isTraversalGroup = true
            },
    ) {
        if (matches.isEmpty()) {
            Text(
                text = empty,
                modifier = Modifier.padding(tokens.Padding),
                style = MonarchTheme.typography.body,
                color = tokens.noteColor(colors),
            )
        } else {
            val groups = remember(matches, groupOrder) { MonarchSlashMenuDefaults.group(matches, groupOrder) }
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = tokens.MaxListHeight)) {
                val active = if (assistant) matches.firstOrNull { it.enabled }?.id else null
                groups.forEachIndexed { index, (heading, list) ->
                    slashMenuGroup(index, heading, list, onSelect, SlashMenuLook(active, assistant))
                }
            }
        }
    }
}

/** How the entries are drawn: which one is active, and whether the menu is the assistant's. */
private class SlashMenuLook(val activeId: String?, val assistant: Boolean)

private fun LazyListScope.slashMenuGroup(
    index: Int,
    heading: String?,
    items: List<MonarchSlashMenuItem>,
    onSelect: (MonarchSlashMenuItem) -> Unit,
    look: SlashMenuLook,
) {
    if (heading != null) {
        item(key = "heading-$index") {
            Text(
                text = heading,
                modifier = Modifier
                    .padding(horizontal = MonarchSlashMenuTokens.Padding, vertical = MonarchSlashMenuTokens.Gap)
                    .semantics { heading() },
                style = MonarchTheme.typography.caption,
                color = MonarchSlashMenuTokens.noteColor(MonarchTheme.colors),
            )
        }
    }
    items.forEach { item ->
        item(key = "item-${item.id}") { SlashMenuRow(item, onSelect, item.id == look.activeId, look.assistant) }
    }
}

@Composable
private fun SlashMenuRow(
    item: MonarchSlashMenuItem,
    onSelect: (MonarchSlashMenuItem) -> Unit,
    active: Boolean,
    assistant: Boolean,
) {
    val tokens = MonarchSlashMenuTokens
    val colors = tokens.rowColors(item.enabled, MonarchTheme.colors, active)
    val icon = item.icon
    val labelStyle = if (assistant) MonarchTheme.typography.code else MonarchTheme.typography.body
    MonarchListRow(
        headlineContent = { Text(text = item.label, style = labelStyle, color = colors.label) },
        modifier = if (active) Modifier.background(tokens.activeFill(MonarchTheme.colors)) else Modifier,
        onClick = { onSelect(item) },
        enabled = item.enabled,
        supportingContent = item.description?.let { description ->
            { Text(text = description, style = MonarchTheme.typography.caption, color = colors.description) }
        },
        leadingContent = icon?.let { painter ->
            { Icon(painter = painter, contentDescription = null, tint = colors.icon) }
        },
    )
}
