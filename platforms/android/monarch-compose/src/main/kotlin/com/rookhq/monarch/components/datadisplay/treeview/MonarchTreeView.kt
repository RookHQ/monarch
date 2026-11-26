package com.rookhq.monarch.components.datadisplay.treeview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * A hierarchy people can browse and pick from: departments, goals and key
 * results, a document outline.
 *
 * Built by Monarch on a `LazyColumn`: Compose has no tree. The tree is
 * flattened into rows, each open parent followed by its children, indented by
 * level. Each row is one TalkBack element read with its state ("Expanded,
 * level 2") and its selection, with the TalkBack expand and collapse actions on
 * parents; the expand icon is a separate 48dp button named "Expand
 * Engineering". With a hardware keyboard, the arrow towards the end opens a
 * parent and the arrow towards the start closes it. Expansion and selection
 * are hoisted. Children that are not loaded yet are asked for with
 * [onLoadChildren]; a "Loading" row, a polite live region, shows while their id
 * is in [loading].
 *
 * Give the tree a bounded height (or let it fill a screen): a lazy list cannot
 * sit inside another vertical scroll.
 *
 * @param items the top-level items.
 * @param expanded the ids of the open parents.
 * @param onExpandedChange called with the new set when the user opens or closes a parent.
 * @param expandIcon a chevron that points to the end, for example a `MonarchIcon` of size `Sm` with a `null`
 * content description and `mirrorInRtl = true`. It turns a quarter turn while the parent is open. Required:
 * Monarch bundles no icons and a tree is not readable without it.
 * @param modifier the modifier of the list.
 * @param selectionMode no selection, one item, or several.
 * @param selected the ids of the selected items.
 * @param current the id of the item that matches the current screen or position, for example the heading in view.
 * Drawn in strong accent text.
 * @param loading the ids of the parents whose children are loading.
 * @param state the scroll state of the list.
 * @param onSelectedChange called with the new selection when the user taps an item.
 * @param onActivate called when the user taps an item: open the record, scroll to the heading.
 * @param onLoadChildren called when the user opens an item with `hasChildren` and no children yet. Add its id to
 * [loading], then give the children or close it again.
 * @param icon a decorative icon before the label of an item.
 * @param meta content at the end of a row that is not an action, for example a `MonarchBadge` or a count.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchTreeView(
    items: List<MonarchTreeViewItem>,
    expanded: Set<String>,
    onExpandedChange: (Set<String>) -> Unit,
    expandIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: MonarchTreeViewSelectionMode = MonarchTreeViewSelectionMode.Single,
    selected: Set<String> = emptySet(),
    current: String? = null,
    loading: Set<String> = emptySet(),
    state: LazyListState = rememberLazyListState(),
    onSelectedChange: ((Set<String>) -> Unit)? = null,
    onActivate: ((MonarchTreeViewItem) -> Unit)? = null,
    onLoadChildren: ((MonarchTreeViewItem) -> Unit)? = null,
    icon: (@Composable (MonarchTreeViewItem) -> Unit)? = null,
    meta: (@Composable (MonarchTreeViewItem) -> Unit)? = null,
    labels: MonarchTreeViewLabels = MonarchTreeViewLabels(),
) {
    val rows = MonarchTreeViewTokens.flatten(items, expanded, loading)
    val toggle: (MonarchTreeViewItem) -> Unit = { item ->
        if (MonarchTreeViewTokens.needsLoad(item, expanded)) onLoadChildren?.invoke(item)
        onExpandedChange(MonarchTreeViewTokens.toggle(expanded, item))
    }
    val activate: (MonarchTreeViewItem) -> Unit = { item ->
        if (selectionMode != MonarchTreeViewSelectionMode.None) {
            onSelectedChange?.invoke(MonarchTreeViewTokens.select(selectionMode, selected, item.id))
        }
        when {
            onActivate != null -> onActivate(item)
            selectionMode == MonarchTreeViewSelectionMode.None && item.isParent -> toggle(item)
        }
    }
    LazyColumn(modifier = modifier, state = state) {
        items(rows, key = { it.key }) { row ->
            if (row.loadingRow) {
                LoadingRow(row.level, monarchString(labels.loading, R.string.monarch_treeview_loading))
            } else {
                TreeRowView(
                    row = row,
                    open = row.item.id in expanded,
                    selected = selectionMode != MonarchTreeViewSelectionMode.None && row.item.id in selected,
                    showSelection = selectionMode != MonarchTreeViewSelectionMode.None,
                    current = row.item.id == current,
                    expandIcon = expandIcon,
                    icon = icon,
                    meta = meta,
                    labels = labels,
                    onToggle = toggle,
                    onActivate = activate,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun TreeRowView(
    row: TreeRow,
    open: Boolean,
    selected: Boolean,
    showSelection: Boolean,
    current: Boolean,
    expandIcon: @Composable () -> Unit,
    icon: (@Composable (MonarchTreeViewItem) -> Unit)?,
    meta: (@Composable (MonarchTreeViewItem) -> Unit)?,
    labels: MonarchTreeViewLabels,
    onToggle: (MonarchTreeViewItem) -> Unit,
    onActivate: (MonarchTreeViewItem) -> Unit,
) {
    val item = row.item
    val themeColors = MonarchTheme.colors
    val colors = MonarchTreeViewTokens.colors(selected, current, item.enabled, themeColors)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val state = rowState(item.isParent, open, row.level, labels)
    val canToggle = item.enabled && item.isParent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .rowBackdrop(colors, rtl, row.level, themeColors.borderSubtle)
            .padding(start = MonarchTreeViewTokens.Indent * (row.level - 1), end = MonarchTreeViewTokens.EndPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isParent) {
            ToggleButton(item, open, canToggle, rtl, expandIcon, labels, onToggle)
        } else {
            Spacer(Modifier.size(MonarchTouchTarget.Minimum))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MonarchTouchTarget.Minimum)
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .rowInteraction(row.item, open, rtl, showSelection, selected, state, onToggle, onActivate)
                .padding(vertical = MonarchTreeViewTokens.VerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(MonarchTreeViewTokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowContent(item, colors, emphasised = selected || current, icon = icon, meta = meta)
        }
    }
}

@Composable
private fun RowScope.RowContent(
    item: MonarchTreeViewItem,
    colors: TreeRowColors,
    emphasised: Boolean,
    icon: (@Composable (MonarchTreeViewItem) -> Unit)?,
    meta: (@Composable (MonarchTreeViewItem) -> Unit)?,
) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.text) {
                Box(Modifier.clearAndSetSemantics { }) { icon(item) }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MonarchTheme.typography.body,
                fontWeight = if (emphasised) FontWeight(MonarchTokens.FontWeight.semibold) else null,
                color = colors.text,
            )
            if (item.description != null) {
                Text(text = item.description, style = MonarchTheme.typography.caption, color = colors.description)
            }
        }
        if (meta != null) meta(item)
}

/** The arrow towards the end opens a parent, the arrow towards the start closes it. */
private fun Modifier.arrowKeys(
    item: MonarchTreeViewItem,
    open: Boolean,
    rtl: Boolean,
    canToggle: Boolean,
    onToggle: (MonarchTreeViewItem) -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    val opens = if (rtl) Key.DirectionLeft else Key.DirectionRight
    val closes = if (rtl) Key.DirectionRight else Key.DirectionLeft
    val handled = event.type == KeyEventType.KeyDown && canToggle &&
        ((event.key == opens && !open) || (event.key == closes && open))
    if (handled) onToggle(item)
    handled
}

/** The tap, the arrow keys and the TalkBack state and actions of the body of a row. */
@Suppress("LongParameterList")
private fun Modifier.rowInteraction(
    item: MonarchTreeViewItem,
    open: Boolean,
    rtl: Boolean,
    showSelection: Boolean,
    selected: Boolean,
    state: String,
    onToggle: (MonarchTreeViewItem) -> Unit,
    onActivate: (MonarchTreeViewItem) -> Unit,
): Modifier {
    val canToggle = item.enabled && item.isParent
    return arrowKeys(item, open, rtl, canToggle, onToggle)
        .clickable(
            enabled = item.enabled,
            role = if (showSelection) null else Role.Button,
            interactionSource = null,
            indication = ripple(),
        ) { onActivate(item) }
        .semantics(mergeDescendants = true) {
            stateDescription = state
            if (showSelection) this.selected = selected
            if (!item.enabled) disabled()
            if (canToggle) {
                if (open) {
                    collapse {
                        onToggle(item)
                        true
                    }
                } else {
                    expand {
                        onToggle(item)
                        true
                    }
                }
            }
        }
}

@Suppress("LongParameterList")
@Composable
private fun ToggleButton(
    item: MonarchTreeViewItem,
    open: Boolean,
    enabled: Boolean,
    rtl: Boolean,
    expandIcon: @Composable () -> Unit,
    labels: MonarchTreeViewLabels,
    onToggle: (MonarchTreeViewItem) -> Unit,
) {
    val name = if (open) {
        labels.collapse?.invoke(item.label) ?: stringResource(R.string.monarch_treeview_collapse, item.label)
    } else {
        labels.expand?.invoke(item.label) ?: stringResource(R.string.monarch_treeview_expand, item.label)
    }
    val turn = if (rtl) -MonarchTreeViewTokens.EXPANDED_ROTATION else MonarchTreeViewTokens.EXPANDED_ROTATION
    val rotation by animateFloatAsState(
        targetValue = if (open) turn else 0f,
        animationSpec = MonarchTheme.motion.spec(MonarchTheme.motion.fast),
        label = "tree expand icon",
    )
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = null, indication = ripple()) {
                onToggle(item)
            }
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        val color = MonarchTreeViewTokens.iconColor(enabled, MonarchTheme.colors)
        CompositionLocalProvider(LocalContentColor provides color) {
            Box(Modifier.graphicsLayer { rotationZ = rotation }.clearAndSetSemantics { }) { expandIcon() }
        }
    }
}

@Composable
private fun LoadingRow(level: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .padding(start = MonarchTreeViewTokens.Indent * (level - 1) + MonarchTouchTarget.Minimum),
        horizontalArrangement = Arrangement.spacedBy(MonarchTreeViewTokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!MonarchTheme.motion.reducedMotion) {
            CircularProgressIndicator(
                modifier = Modifier.size(MonarchTreeViewTokens.SpinnerSize).clearAndSetSemantics { },
                color = MonarchTheme.colors.textMuted,
                strokeWidth = MonarchTreeViewTokens.SpinnerStroke,
                trackColor = Color.Transparent,
            )
        }
        Text(
            text = text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.caption,
            color = MonarchTheme.colors.textMuted,
        )
    }
}

/** "Expanded, level 2" for an open parent, "Level 3" for a leaf. */
@Composable
private fun rowState(parent: Boolean, open: Boolean, level: Int, labels: MonarchTreeViewLabels): String {
    val levelText = labels.level?.invoke(level) ?: stringResource(R.string.monarch_treeview_level, level)
    if (!parent) return levelText
    val openText = if (open) {
        monarchString(labels.expanded, R.string.monarch_treeview_expanded)
    } else {
        monarchString(labels.collapsed, R.string.monarch_treeview_collapsed)
    }
    return stringResource(R.string.monarch_treeview_state, openText, levelText)
}

/**
 * What a row draws behind its content: the highlight, 40dp high inside the 48dp target (4dp short at the top and
 * bottom), the bar at the start edge of a selected row (on the right in right to left), and one subtle guide line
 * for each level above the row so the indent reads as a tree.
 */
private fun Modifier.rowBackdrop(
    colors: TreeRowColors,
    rtl: Boolean,
    level: Int,
    guide: Color,
): Modifier = drawBehind {
    val inset = MonarchTreeViewTokens.HighlightInset.toPx()
    val radius = MonarchTreeViewTokens.HighlightRadius.toPx()
    val width = MonarchTreeViewTokens.SelectedBar.toPx()
    val guideWidth = MonarchTreeViewTokens.GuideWidth.toPx()
    val half = MonarchTouchTarget.Minimum.toPx() / 2
    val step = MonarchTreeViewTokens.Indent.toPx()
    for (ancestor in 0 until (level - 1)) {
        val x = if (rtl) size.width - (step * ancestor + half) else step * ancestor + half
        drawRect(color = guide, topLeft = Offset(x - guideWidth / 2, 0f), size = Size(guideWidth, size.height))
    }
    if (colors.container != Color.Transparent) {
        drawRoundRect(
            color = colors.container,
            topLeft = Offset(0f, inset),
            size = Size(size.width, size.height - 2 * inset),
            cornerRadius = CornerRadius(radius, radius),
        )
    }
    if (colors.bar != null) {
        val x = if (rtl) size.width - width else 0f
        drawRect(color = colors.bar, topLeft = Offset(x, inset), size = Size(width, size.height - 2 * inset))
    }
}
