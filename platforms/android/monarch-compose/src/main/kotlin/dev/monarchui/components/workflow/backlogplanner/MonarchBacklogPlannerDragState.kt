package dev.monarchui.components.workflow.backlogplanner

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.monarchui.internal.MonarchDragAutoScroll

private typealias DragMoves = MonarchBacklogPlannerMoves

/**
 * The drag in progress in a `MonarchBacklogPlanner`: which item is held, the
 * buckets shown while it moves, and how far its row is from where the layout
 * put it. The move is committed once, when the drag ends.
 */
@Stable
internal class MonarchBacklogPlannerDragState<T>(private val listState: LazyListState) {
    /** The key of the held item, or `null` when nothing is dragged. */
    var key: String? by mutableStateOf(null)
        private set

    /** The buckets shown while dragging, or `null`. */
    var preview: List<MonarchBacklogBucket<T>>? by mutableStateOf(null)
        private set

    /** Where the held item was before the drag. */
    var origin: MonarchBacklogPlannerMoves.Location? = null
        private set

    private var delta by mutableFloatStateOf(0f)
    private var startOffset = 0

    /** Whether an item is held. */
    val active: Boolean get() = key != null

    private fun heldInfo(): LazyListItemInfo? =
        key?.let { k ->
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == MonarchBacklogPlannerTokens.itemKey(k) }
        }

    /** How far the held row is drawn from its slot, so it follows the finger. */
    fun translation(): Float = heldInfo()?.let { startOffset + delta - it.offset } ?: 0f

    /**
     * Starts a drag on the row at [y] when its item is in [buckets]. Returns
     * the held item's key, or `null` when no drag started.
     */
    fun start(y: Float, buckets: List<MonarchBacklogBucket<T>>, keyOf: (T) -> String): String? {
        val row = listState.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y <= it.offset + it.size }
        val rowKey = (row?.key as? String)?.takeIf { it.startsWith("i:") }?.drop(2)
        val at = rowKey?.let { DragMoves.where(buckets, it, keyOf) }
        if (row == null || rowKey == null || at == null) return null
        key = rowKey
        preview = buckets
        origin = at
        startOffset = row.offset
        delta = 0f
        return rowKey
    }

    /** Moves the held row by [dy] and moves the item to the place under its middle. */
    fun drag(dy: Float, rules: MonarchBacklogPlannerRules<T>) {
        delta += dy
        retarget(rules)
    }

    /** Moves the item to the place under the held row's middle, once the layout shows the last move. */
    fun retarget(rules: MonarchBacklogPlannerRules<T>) {
        val held = heldInfo()
        val shown = preview
        val k = key
        if (held == null || shown == null || k == null) return
        // Waits for the layout to show the last move before the next one.
        if (held.index != rules.entries(shown).indexOf(MonarchBacklogPlannerTokens.itemKey(k))) return
        val middle = startOffset + delta + held.size / 2f
        val slot = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != held.key && middle >= it.offset && middle <= it.offset + it.size
        }
        val to = slot?.let { place(it, shown, k, rules) } ?: return
        val first = listState.firstVisibleItemIndex
        if (held.index == first || slot.index == first) {
            // Keeps the list where it is: by default it follows the key of its first visible row.
            listState.requestScrollToItem(first, listState.firstVisibleItemScrollOffset)
        }
        preview = DragMoves.move(shown, k, to, rules.keyOf)
    }

    /** Where the held item [k] goes over the lazy entry [slot], under [rules], or `null`. */
    private fun place(
        slot: LazyListItemInfo,
        shown: List<MonarchBacklogBucket<T>>,
        k: String,
        rules: MonarchBacklogPlannerRules<T>,
    ): MonarchBacklogPlannerMoves.Location? {
        val slotKey = slot.key as? String
        val item = DragMoves.locate(shown, k, rules.keyOf)?.item
        val from = origin
        if (slotKey == null || item == null || from == null) return null
        return DragMoves.target(slotKey, shown, k, rules.keyOf, rules.blocked(item, from.bucketId))
    }

    /** How far the held row is past the edge of the list in the direction it moves. */
    fun overscroll(): Float {
        val held = heldInfo() ?: return 0f
        val top = startOffset + delta
        val info = listState.layoutInfo
        return MonarchDragAutoScroll.overscroll(
            top,
            top + held.size,
            info.viewportStartOffset,
            info.viewportEndOffset,
            delta,
        )
    }

    /** Ends the drag and returns the buckets as they were shown. */
    fun end(): List<MonarchBacklogBucket<T>>? {
        val shown = preview
        reset()
        return shown
    }

    /** Ends the drag without a move. */
    fun reset() {
        key = null
        preview = null
        origin = null
        delta = 0f
    }
}
