package com.rookhq.monarch.components.interaction.reorderablelist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandle
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleState
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.internal.MonarchAnnouncerNode
import com.rookhq.monarch.internal.monarchAutoScrollWhileHeld
import com.rookhq.monarch.internal.rememberMonarchAnnouncer
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A list the user reorders by dragging rows, with TalkBack actions, arrow
 * keys and optional move buttons as alternatives to the drag.
 *
 * Built by Monarch on a `LazyColumn`: Compose has no reorderable list. A long
 * press anywhere on a row picks it up (`detectDragGesturesAfterLongPress`);
 * the row follows the finger, the other rows slide out of its way
 * (`Modifier.animateItem`, immediate when animations are off), and the list
 * scrolls when the row is held past its edge. The new order is reported once,
 * on drop, through [onReorder]; the app moves the item in its own list. Each
 * row starts with a `MonarchDragHandle` that offers Move up and Move down to
 * TalkBack, Switch Access and the arrow keys. Every pick up, move, drop and
 * cancel is announced through a polite live region.
 *
 * @param items the items, in order.
 * @param key a stable, unique key for an item.
 * @param itemLabel the plain-text name of an item, used in the handle's name and the announcements.
 * @param onReorder called once per finished move with the old and the new index. Move the item in [items].
 * @param modifier the modifier of the list.
 * @param enabled whether any row can be moved.
 * @param itemEnabled whether an item can be moved. Other rows can still move past it.
 * @param moveButtons also shows Up and Down buttons on every row, a one-tap alternative to the drag.
 * @param state the scroll state of the list.
 * @param labels replacements for the built-in strings.
 * @param itemContent the content of a row, after the handle. It knows its index and whether it is dragged.
 */
@Composable
public fun <T> MonarchReorderableList(
    items: List<T>,
    key: (T) -> Any,
    itemLabel: (T) -> String,
    onReorder: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemEnabled: (T) -> Boolean = { true },
    moveButtons: Boolean = false,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchReorderableListLabels = MonarchReorderableListLabels(),
    itemContent: @Composable RowScope.(item: T, state: MonarchReorderableListItemState) -> Unit,
) {
    val drag = remember(state) { MonarchReorderableListDragState(state) }
    val texts = reorderableListTexts(labels)
    val announcer = rememberMonarchAnnouncer()
    val announce: (String) -> Unit = announcer::announce
    val inputs by rememberUpdatedState(ReorderableListInputs(items, itemLabel, onReorder, enabled, itemEnabled, texts))
    val scope = rememberCoroutineScope()
    val maxStep = with(LocalDensity.current) { MonarchReorderableListTokens.AutoScrollStep.toPx() }
    val gestures = remember(drag) {
        ReorderableListGestures(drag, { inputs }, { announce(it) }) {
            scope.launch {
                state.monarchAutoScrollWhileHeld(maxStep, { drag.active }, drag::overscroll, drag::retarget)
            }
        }
    }
    val shown = shownOrder(items, drag.order)
    val moveBy: (Int, Int) -> Unit = { from, step ->
        val to = from + step
        if (!drag.active && to in items.indices) {
            val label = itemLabel(items[from])
            val total = items.size
            onReorder(from, to)
            announce(texts.moved(label, to + 1, total))
        }
    }
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(gestures) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { gestures.start(it.y) },
                        onDrag = { change, amount -> if (gestures.move(amount.y)) change.consume() },
                        onDragEnd = { gestures.end() },
                        onDragCancel = { gestures.cancel() },
                    )
                },
            state = state,
            verticalArrangement = Arrangement.spacedBy(MonarchReorderableListTokens.RowGap),
            // The held row moves with the finger; the list scrolls only by the automatic scrolling.
            userScrollEnabled = !drag.active,
        ) {
            itemsIndexed(items = shown, key = { _, item -> key(item) }) { index, item ->
                val itemKey = key(item)
                val dragged = drag.key == itemKey
                val movable = enabled && itemEnabled(item)
                val from = if (drag.active) items.indexOf(item) else index
                ReorderableRow(
                    modifier = Modifier.rowPlacement(this, dragged, drag),
                    label = itemLabel(item),
                    index = index,
                    count = shown.size,
                    dragged = dragged,
                    movable = movable,
                    busy = drag.active,
                    moveButtons = moveButtons,
                    texts = texts,
                    labels = labels,
                    onMove = { step -> moveBy(from, step) },
                ) { itemContent(item, MonarchReorderableListItemState(index, dragged, movable)) }
            }
        }
        MonarchAnnouncerNode(announcer)
    }
}

/** The items in the order shown: the drag's order while a row is held. */
private fun <T> shownOrder(items: List<T>, order: List<Int>?): List<T> =
    order?.mapNotNull { items.getOrNull(it) } ?: items

/** The values the drag gesture reads when an event arrives, kept current across recompositions. */
private class ReorderableListInputs<T>(
    val items: List<T>,
    val itemLabel: (T) -> String,
    val onReorder: (Int, Int) -> Unit,
    val enabled: Boolean,
    val itemEnabled: (T) -> Boolean,
    val texts: ReorderableListTexts,
)

/** What the long-press drag does: pick up, move, drop or cancel, each announced. */
private class ReorderableListGestures<T>(
    private val drag: MonarchReorderableListDragState,
    private val inputs: () -> ReorderableListInputs<T>,
    private val announce: (String) -> Unit,
    private val startAutoScroll: () -> Job,
) {
    private var autoScroll: Job? = null

    fun start(y: Float) {
        val now = inputs()
        val started = now.enabled && drag.start(y, now.items.size) { now.itemEnabled(now.items[it]) }
        if (started) announce(now.texts.pickedUp(now.itemLabel(now.items[drag.from]), drag.from + 1, now.items.size))
    }

    /** Moves the held row; returns whether the event was used. */
    fun move(dy: Float): Boolean {
        if (!drag.active) return false
        drag.drag(dy)
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

/** The held row follows the finger above the others; the other rows slide to their new places. */
@Composable
private fun Modifier.rowPlacement(
    itemScope: LazyItemScope,
    dragged: Boolean,
    drag: MonarchReorderableListDragState,
): Modifier = with(itemScope) {
    if (dragged) {
        this@rowPlacement
            .zIndex(MonarchReorderableListTokens.DRAGGED_Z)
            .graphicsLayer { translationY = drag.translation() }
    } else {
        this@rowPlacement.animateItem(
            fadeInSpec = null,
            placementSpec = MonarchTheme.motion.spec<IntOffset>(),
            fadeOutSpec = null,
        )
    }
}

@Composable
private fun ReorderableRow(
    modifier: Modifier,
    label: String,
    index: Int,
    count: Int,
    dragged: Boolean,
    movable: Boolean,
    busy: Boolean,
    moveButtons: Boolean,
    texts: ReorderableListTexts,
    labels: MonarchReorderableListLabels,
    onMove: (Int) -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = MonarchReorderableListTokens
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.md
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (dragged) Modifier.monarchShadow(MonarchTheme.elevation.presented, shape) else Modifier)
            .background(tokens.container(colors), shape)
            .border(tokens.BorderWidth, tokens.border(dragged, colors), shape)
            .padding(
                start = tokens.RowPaddingHorizontal,
                top = tokens.RowPaddingVertical,
                end = tokens.RowPaddingEnd,
                bottom = tokens.RowPaddingVertical,
            ),
        horizontalArrangement = Arrangement.spacedBy(tokens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchDragHandle(
            label = texts.handle(label),
            state = if (dragged) MonarchDragHandleState.Grabbed else MonarchDragHandleState.Idle,
            enabled = movable,
            onMove = { direction -> onMove(if (direction == MonarchDragHandleDirection.Up) -1 else 1) },
            actionLabels = labels.actions,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(tokens.PartGap),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        if (moveButtons) {
            MoveButton(MonarchDefaultIcons.ChevronUp, texts.moveUp(label), movable && !busy && index > 0) { onMove(-1) }
            MoveButton(
                MonarchDefaultIcons.ChevronDown,
                texts.moveDown(label),
                movable && !busy && index < count - 1,
            ) { onMove(1) }
        }
    }
}

@Composable
private fun MoveButton(icon: @Composable () -> Unit, name: String, enabled: Boolean, onClick: () -> Unit) {
    // Chevrons, as the web draws them; the spoken name carries the direction.
    MonarchIconButton(label = name, onClick = onClick, icon = icon, enabled = enabled)
}

/** The built-in strings, with the caller's replacements applied. */
private class ReorderableListTexts(
    val handle: (String) -> String,
    val moveUp: (String) -> String,
    val moveDown: (String) -> String,
    val up: String,
    val down: String,
    val pickedUp: (String, Int, Int) -> String,
    val moved: (String, Int, Int) -> String,
    val dropped: (String, Int, Int) -> String,
    val cancelled: (String, Int, Int) -> String,
)

@Composable
private fun reorderableListTexts(labels: MonarchReorderableListLabels): ReorderableListTexts {
    val handle = stringResource(R.string.monarch_reorderablelist_handle)
    val moveUp = stringResource(R.string.monarch_reorderablelist_move_up)
    val moveDown = stringResource(R.string.monarch_reorderablelist_move_down)
    val pickedUp = stringResource(R.string.monarch_reorderablelist_picked_up)
    val moved = stringResource(R.string.monarch_reorderablelist_moved)
    val dropped = stringResource(R.string.monarch_reorderablelist_dropped)
    val cancelled = stringResource(R.string.monarch_reorderablelist_cancelled)
    return ReorderableListTexts(
        handle = labels.handle ?: { handle.format(it) },
        moveUp = labels.moveUp ?: { moveUp.format(it) },
        moveDown = labels.moveDown ?: { moveDown.format(it) },
        up = labels.up ?: stringResource(R.string.monarch_reorderablelist_up),
        down = labels.down ?: stringResource(R.string.monarch_reorderablelist_down),
        pickedUp = labels.pickedUp ?: { label, position, total -> pickedUp.format(label, position, total) },
        moved = labels.moved ?: { label, position, total -> moved.format(label, position, total) },
        dropped = labels.dropped ?: { label, position, total -> dropped.format(label, position, total) },
        cancelled = labels.cancelled ?: { label, position, total -> cancelled.format(label, position, total) },
    )
}
