package com.rookhq.monarch.components.interaction.sortablegrid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.rookhq.monarch.R
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandle
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleSize
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleState
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.internal.MonarchAnnouncerNode
import com.rookhq.monarch.internal.MonarchDragAutoScroll
import com.rookhq.monarch.internal.rememberMonarchAnnouncer
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A grid of tiles the user reorders in two dimensions by dragging, with
 * TalkBack actions and arrow keys as alternatives to the drag.
 *
 * Built by Monarch on a `LazyVerticalGrid`: Compose has no sortable grid. A
 * long press anywhere on a tile picks it up; the tile follows the finger, the
 * other tiles slide out of its way (`Modifier.animateItem`, immediate when
 * animations are off), and the grid scrolls when the tile is held past its
 * top or bottom edge. The new order is reported once, on drop, through
 * [onReorder]; the app moves the item in its own list. Each tile has a
 * `MonarchDragHandle` that offers Move up, Move down, Move backward and Move
 * forward (up and down only in one column) to TalkBack, Switch Access and the
 * arrow keys. Every pick up, move, drop and cancel is announced through a
 * polite live region.
 *
 * @param items the items, in order.
 * @param key a stable, unique key for an item.
 * @param itemLabel the plain-text name of an item, used in the handle's name and the announcements.
 * @param onReorder called once per finished move with the old and the new index. Move the item in [items].
 * @param modifier the modifier of the grid.
 * @param columns a fixed number of columns. `null` fits as many columns of [minItemWidth] as the width allows.
 * @param minItemWidth the smallest tile width for adaptive columns. It grows with the font scale, so large text
 * gives fewer, wider tiles.
 * @param enabled whether any tile can be moved.
 * @param itemEnabled whether an item can be moved. Other tiles can still move past it.
 * @param state the scroll state of the grid.
 * @param labels replacements for the built-in strings.
 * @param itemContent the content of a tile, beside the handle. It knows its index and whether it is dragged.
 */
@Composable
public fun <T> MonarchSortableGrid(
    items: List<T>,
    key: (T) -> Any,
    itemLabel: (T) -> String,
    onReorder: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int? = null,
    minItemWidth: Dp = MonarchSortableGridTokens.MinItemWidth,
    enabled: Boolean = true,
    itemEnabled: (T) -> Boolean = { true },
    state: LazyGridState = rememberLazyGridState(),
    labels: MonarchSortableGridLabels = MonarchSortableGridLabels(),
    itemContent: @Composable ColumnScope.(item: T, state: MonarchSortableGridItemState) -> Unit,
) {
    val drag = remember(state) { MonarchSortableGridDragState(state) }
    val texts = sortableGridTexts(labels)
    val announcer = rememberMonarchAnnouncer()
    val announce: (String) -> Unit = announcer::announce
    val inputs by rememberUpdatedState(SortableGridInputs(items, itemLabel, onReorder, enabled, itemEnabled, texts))
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxStep = with(density) { MonarchSortableGridTokens.AutoScrollStep.toPx() }
    val gestures = remember(drag) {
        SortableGridGestures(drag, { inputs }, { announce(it) }) {
            scope.launch { autoScroll(drag, state, maxStep) }
        }
    }
    val shown = shownOrder(items, drag.order)
    val cells = MonarchSortableGridTokens.cells(columns, minItemWidth, density.fontScale)
    // Read through derivedStateOf: the layout info changes on every layout pass, the column count rarely.
    val across by remember(state) { derivedStateOf { state.layoutInfo.maxSpan.coerceAtLeast(1) } }
    val move: (Int, MonarchDragHandleDirection) -> Unit = { from, direction ->
        val to = MonarchSortableGridTokens.step(from, direction, across, items.size)
        if (!drag.active && to != null) {
            val label = itemLabel(items[from])
            val total = items.size
            onReorder(from, to)
            announce(texts.moved(label, to + 1, total))
        }
    }
    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = cells,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(gestures) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { gestures.start(it) },
                        onDrag = { change, amount -> if (gestures.move(amount)) change.consume() },
                        onDragEnd = { gestures.end() },
                        onDragCancel = { gestures.cancel() },
                    )
                },
            state = state,
            verticalArrangement = Arrangement.spacedBy(MonarchSortableGridTokens.Gap),
            horizontalArrangement = Arrangement.spacedBy(MonarchSortableGridTokens.Gap),
            // The held tile moves with the finger; the grid scrolls only by the automatic scrolling.
            userScrollEnabled = !drag.active,
        ) {
            itemsIndexed(items = shown, key = { _, item -> key(item) }) { index, item ->
                val dragged = drag.key == key(item)
                val movable = enabled && itemEnabled(item)
                val from = if (drag.active) items.indexOf(item) else index
                SortableTile(
                    modifier = Modifier.tilePlacement(this, dragged, drag),
                    label = texts.handle(itemLabel(item)),
                    dragged = dragged,
                    movable = movable,
                    columns = across,
                    labels = labels,
                    onMove = { direction -> move(from, direction) },
                ) { itemContent(item, MonarchSortableGridItemState(index, dragged, movable)) }
            }
        }
        MonarchAnnouncerNode(announcer)
    }
}

/** The items in the order shown: the drag's order while a tile is held. */
private fun <T> shownOrder(items: List<T>, order: List<Int>?): List<T> =
    order?.mapNotNull { items.getOrNull(it) } ?: items

/** Scrolls one step a frame while the held tile is past the edge and the grid can still scroll. */
private suspend fun autoScroll(drag: MonarchSortableGridDragState, state: LazyGridState, maxStep: Float) {
    var scrolled = true
    while (drag.active && drag.overscroll() != 0f && scrolled) {
        scrolled = state.scrollBy(MonarchDragAutoScroll.step(drag.overscroll(), maxStep)) != 0f
        drag.retarget()
        withFrameNanos { }
    }
}

/** The values the drag gesture reads when an event arrives, kept current across recompositions. */
private class SortableGridInputs<T>(
    val items: List<T>,
    val itemLabel: (T) -> String,
    val onReorder: (Int, Int) -> Unit,
    val enabled: Boolean,
    val itemEnabled: (T) -> Boolean,
    val texts: SortableGridTexts,
)

/** What the long-press drag does: pick up, move, drop or cancel, each announced. */
private class SortableGridGestures<T>(
    private val drag: MonarchSortableGridDragState,
    private val inputs: () -> SortableGridInputs<T>,
    private val announce: (String) -> Unit,
    private val startAutoScroll: () -> Job,
) {
    private var autoScroll: Job? = null

    fun start(point: Offset) {
        val now = inputs()
        val started = now.enabled && drag.start(point, now.items.size) { now.itemEnabled(now.items[it]) }
        if (started) announce(now.texts.pickedUp(now.itemLabel(now.items[drag.from]), drag.from + 1, now.items.size))
    }

    /** Moves the held tile; returns whether the event was used. */
    fun move(amount: Offset): Boolean {
        if (!drag.active) return false
        drag.drag(amount)
        if (drag.overscroll() != 0f && autoScroll?.isActive != true) autoScroll = startAutoScroll()
        return true
    }

    fun end() {
        if (!drag.active) return
        val now = inputs()
        val from = drag.from
        val to = drag.end()
        val item = now.items.getOrNull(from) ?: return
        if (to != from) now.onReorder(from, to)
        announce(now.texts.dropped(now.itemLabel(item), to + 1, now.items.size))
    }

    fun cancel() {
        if (!drag.active) return
        val now = inputs()
        val from = drag.from
        drag.reset()
        val item = now.items.getOrNull(from) ?: return
        announce(now.texts.cancelled(now.itemLabel(item), from + 1, now.items.size))
    }
}

/** The held tile follows the finger above the others; the other tiles slide to their new places. */
@Composable
private fun Modifier.tilePlacement(
    itemScope: LazyGridItemScope,
    dragged: Boolean,
    drag: MonarchSortableGridDragState,
): Modifier = with(itemScope) {
    if (dragged) {
        this@tilePlacement
            .zIndex(MonarchSortableGridTokens.DRAGGED_Z)
            .graphicsLayer {
                val offset = drag.translation()
                translationX = offset.x
                translationY = offset.y
            }
    } else {
        this@tilePlacement.animateItem(
            fadeInSpec = null,
            placementSpec = MonarchTheme.motion.spec<IntOffset>(),
            fadeOutSpec = null,
        )
    }
}

@Composable
private fun SortableTile(
    modifier: Modifier,
    label: String,
    dragged: Boolean,
    movable: Boolean,
    columns: Int,
    labels: MonarchSortableGridLabels,
    onMove: (MonarchDragHandleDirection) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MonarchSortableGridTokens
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.md
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (dragged) Modifier.monarchShadow(MonarchTheme.elevation.md, shape) else Modifier)
            .background(tokens.container(colors), shape)
            .border(tokens.BorderWidth, tokens.border(dragged, colors), shape)
            .padding(tokens.TilePadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.PartGap),
        verticalAlignment = Alignment.Top,
    ) {
        MonarchDragHandle(
            label = label,
            state = if (dragged) MonarchDragHandleState.Grabbed else MonarchDragHandleState.Idle,
            axis = tokens.axis(columns),
            size = MonarchDragHandleSize.Sm,
            enabled = movable,
            onMove = onMove,
            actionLabels = labels.actions,
        )
        Column(modifier = Modifier.weight(1f), content = content)
    }
}

/** The built-in strings, with the caller's replacements applied. */
private class SortableGridTexts(
    val handle: (String) -> String,
    val pickedUp: (String, Int, Int) -> String,
    val moved: (String, Int, Int) -> String,
    val dropped: (String, Int, Int) -> String,
    val cancelled: (String, Int, Int) -> String,
)

@Composable
private fun sortableGridTexts(labels: MonarchSortableGridLabels): SortableGridTexts {
    val handle = stringResource(R.string.monarch_sortablegrid_handle)
    val pickedUp = stringResource(R.string.monarch_sortablegrid_picked_up)
    val moved = stringResource(R.string.monarch_sortablegrid_moved)
    val dropped = stringResource(R.string.monarch_sortablegrid_dropped)
    val cancelled = stringResource(R.string.monarch_sortablegrid_cancelled)
    return SortableGridTexts(
        handle = labels.handle ?: { handle.format(it) },
        pickedUp = labels.pickedUp ?: { label, position, total -> pickedUp.format(label, position, total) },
        moved = labels.moved ?: { label, position, total -> moved.format(label, position, total) },
        dropped = labels.dropped ?: { label, position, total -> dropped.format(label, position, total) },
        cancelled = labels.cancelled ?: { label, position, total -> cancelled.format(label, position, total) },
    )
}
