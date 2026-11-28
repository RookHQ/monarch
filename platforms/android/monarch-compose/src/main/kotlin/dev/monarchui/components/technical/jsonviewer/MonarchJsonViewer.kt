package dev.monarchui.components.technical.jsonviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * A collapsible, searchable tree for a JSON document, with copy path and copy value for the selected node, that stays
 * fast on large payloads.
 *
 * Built by Monarch on a Compose `LazyColumn` of a flattened model of the open part of the tree: children are walked
 * only when their container is open, and a large object or array shows its children in batches behind a "Show more"
 * row, so only the rows on screen are built. The viewer scrolls itself: give it a bounded height, or let it fill a
 * screen or a pane. Search (a `MonarchTextField`) looks through every key and primitive value, opens the containers
 * that hold a match, marks the matched text and counts the matches; it stops after [maxMatches]. Tapping a row selects
 * it and opens or closes it; the footer shows the selected path with `MonarchCopyButton`s for the path and the value.
 *
 * For TalkBack, the tree is named "JSON". Each row is a button read as "key: value" (`{ } 3 keys` for a closed object)
 * with Expanded or Collapsed and the selected state. Matches are bold as well as highlighted, and the match count is a
 * polite live region. Rows are at least 48dp tall.
 *
 * @param value the document. Parse text with [MonarchJsonValue.parse], which keeps key order and number text.
 * @param expandIcon a chevron that points to the end, such as a `MonarchIcon` of size `Sm` with a `null` content
 * description and `mirrorInRtl = true`. It turns a quarter turn while its container is open. Required, as in
 * `MonarchTreeView`: Monarch bundles no icons.
 * @param modifier the modifier of the viewer.
 * @param expandDepth the levels open at first: 1 shows the root's children, 0 only the root.
 * @param maxChildren the children shown per object or array before a "Show more" row; each Show more adds this many.
 * At least 1.
 * @param maxStringLength strings longer than this are cut in the tree with "… (n characters)". Copy value always
 * copies the whole string.
 * @param searchable shows the search field and the match count.
 * @param maxMatches the search stops after this many matches and the count shows "500+ matches".
 * @param copyable shows the footer with the selected path, Copy path and Copy value.
 * @param rootPath the first part of shown and copied paths: `$` gives `$.users[0].name`, an empty string gives
 * `users[0].name`.
 * @param searchIcon an icon at the start of the search field.
 * @param clearIcon the icon of a button that clears the search, shown while there is a query.
 * @param state the scroll state of the rows.
 * @param labels replacements for the built-in strings.
 * @param onSelect called with the formatted path and the value when the user selects another node.
 * @param onCopy called with the copied text after the path or the value is copied.
 */
@Composable
public fun MonarchJsonViewer(
    value: MonarchJsonValue,
    expandIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    expandDepth: Int = 1,
    maxChildren: Int = 100,
    maxStringLength: Int = 500,
    searchable: Boolean = true,
    maxMatches: Int = 500,
    copyable: Boolean = true,
    rootPath: String = "$",
    searchIcon: (@Composable () -> Unit)? = null,
    clearIcon: (@Composable () -> Unit)? = null,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchJsonViewerLabels = MonarchJsonViewerLabels(),
    onSelect: ((path: String, value: MonarchJsonValue) -> Unit)? = null,
    onCopy: ((String) -> Unit)? = null,
) {
    val strings = monarchJsonViewerStrings(labels)
    val colors = MonarchTheme.colors
    val depth = expandDepth.coerceAtLeast(0)
    val batch = maxChildren.coerceAtLeast(1)
    val longest = maxStringLength.coerceAtLeast(1)
    var query by rememberSaveable { mutableStateOf("") }
    var baseDepth by remember(value) { mutableStateOf<Int?>(null) }
    var overrides by remember(value) { mutableStateOf(emptyMap<String, Boolean>()) }
    var limits by remember(value) { mutableStateOf(emptyMap<String, Int>()) }
    var selectedId by remember(value) { mutableStateOf<String?>(null) }
    val needle = query.trim().lowercase()
    val search = remember(value, needle, maxMatches) {
        MonarchJsonViewerModel.search(value, needle, maxMatches.coerceAtLeast(1))
    }
    val open = MonarchJsonViewerOpenState(overrides, limits, search, baseDepth ?: depth, batch)
    val rows = remember(value, open) { MonarchJsonViewerModel.rows(value, open) }
    val selected = rows.firstOrNull { it.id == selectedId && !it.more } ?: rows.first()
    val select: (MonarchJsonViewerRow) -> Unit = { row ->
        selectedId = row.id
        onSelect?.invoke(MonarchJsonViewerPaths.path(row.segments, rootPath), row.value)
    }
    val shape = MonarchShapeDefaults.Container
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MonarchJsonViewerTokens.surface(colors))
            .border(MonarchShapeDefaults.BorderWidth, MonarchJsonViewerTokens.border(colors), shape),
    ) {
        MonarchJsonViewerToolbar(
            searchable = searchable,
            query = query,
            onQueryChange = { query = it },
            matches = strings.matches(search.count, search.capped).takeIf { searchable && needle.isNotEmpty() },
            searchIcon = searchIcon,
            clearIcon = clearIcon,
            strings = strings,
            onCollapseAll = {
                baseDepth = minOf(1, depth)
                overrides = emptyMap()
                limits = emptyMap()
                query = ""
                select(rows.first())
            },
        )
        Divider()
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false).semantics { contentDescription = strings.label },
            state = state,
        ) {
            items(rows, key = { it.id }, contentType = { if (it.more) "more" else "node" }) { row ->
                RowItem(row, open, needle, selected.id, longest, batch, expandIcon, strings) { reveal ->
                    if (row.more) {
                        val parent = row.parentId ?: return@RowItem
                        limits = limits + (parent to row.shown + batch)
                        val container = rows.firstOrNull { it.id == parent }?.value ?: return@RowItem
                        val (segment, child) = MonarchJsonViewerPaths.children(container).getOrNull(row.shown)
                            ?: return@RowItem
                        val segments = row.segments + segment
                        select(
                            MonarchJsonViewerRow(
                                more = false,
                                id = MonarchJsonViewerPaths.id(segments),
                                segments = segments,
                                parentId = parent,
                                name = segment,
                                value = child,
                                depth = row.depth,
                                count = MonarchJsonViewerPaths.childCount(child),
                            ),
                        )
                    } else {
                        select(row)
                        if (row.expandable) overrides = overrides + (row.id to !reveal)
                    }
                }
            }
        }
        if (copyable) {
            Divider()
            val path = MonarchJsonViewerPaths.path(selected.segments, rootPath)
                .ifEmpty { MonarchJsonViewerPaths.IdRoot }
            MonarchJsonViewerFooter(path, selected.value, strings, onCopy)
        }
    }
}

/** One row: a node, or the "Show more" row. [onClick] receives whether a node was open before the tap. */
@Composable
@Suppress("LongParameterList")
private fun RowItem(
    row: MonarchJsonViewerRow,
    open: MonarchJsonViewerOpenState,
    needle: String,
    selectedId: String,
    longest: Int,
    batch: Int,
    expandIcon: @Composable () -> Unit,
    strings: MonarchJsonViewerStrings,
    onClick: (wasOpen: Boolean) -> Unit,
) {
    if (row.more) {
        MonarchJsonViewerMoreRow(strings.showMore(minOf(batch, row.remaining), row.remaining), row.depth) {
            onClick(false)
        }
        return
    }
    val isOpen = row.expandable && MonarchJsonViewerModel.open(row.id, row.depth, open)
    val node = MonarchJsonViewerNode(
        row = row,
        summary = strings.summary(row.value, longest),
        needle = if (row.id in open.search.matched) needle else "",
        open = isOpen,
        selected = row.id == selectedId,
    )
    MonarchJsonViewerNodeRow(node, expandIcon, strings) { onClick(isOpen) }
}

/** A line between the parts of the viewer. Decoration. */
@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MonarchShapeDefaults.BorderWidth)
            .background(MonarchJsonViewerTokens.border(MonarchTheme.colors))
            .clearAndSetSemantics { },
    )
}
