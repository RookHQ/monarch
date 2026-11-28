package dev.monarchui.components.workflow.backlogplanner

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchAutoScrollWhileHeld
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private typealias MainTokens = MonarchBacklogPlannerTokens
private typealias BacklogRules = MonarchBacklogPlannerMoves

/**
 * Backlog and iteration planning on one screen: buckets stacked top to bottom
 * (sprints, releases, the backlog), with items moved within and between them
 * by dragging, TalkBack actions or a Move to menu, and points against
 * capacity.
 *
 * Built by Monarch on a `LazyColumn`: Compose has no list that moves items
 * between sections. Each bucket is a header (a button that opens and closes
 * it, its summary and a `MonarchProgress` capacity meter) followed by its
 * rows. A long press on a row picks its item up
 * (`detectDragGesturesAfterLongPress`); the row follows the finger across
 * bucket headers, the other rows slide out of its way (`Modifier.animateItem`,
 * immediate when animations are off) and the list scrolls when the row is held
 * past its edge. Closed buckets and buckets [canMove] refuses are skipped.
 * Each row starts with a `MonarchDragHandle` whose Move up and Move down
 * actions (TalkBack, Switch Access, arrow keys) cross into the next open
 * bucket, and ends with a Move to button that opens a Material 3
 * `DropdownMenu` of the other buckets, the path that always works.
 *
 * The planner is controlled: each finished move calls [onMove] once with the
 * move and the buckets after it. The move stays on screen while [onMove]
 * runs and until new [buckets] arrive; if it throws, the item goes back and
 * the failure is announced. Every pick up, move, drop, cancel and failure is
 * announced through a polite live region. Give the planner a bounded height:
 * it is lazy and does not go inside a vertically scrolling parent.
 *
 * @param buckets the buckets, top to bottom: usually the active and planned iterations, then the backlog.
 * @param key a stable, unique key for an item, across every bucket.
 * @param itemLabel the short plain-text name of an item, used in the handle's name, the menu and announcements.
 * @param modifier the modifier of the planner.
 * @param itemPoints the points of an item, `null` when it has no estimate. Adds totals, points on rows and the
 * capacity meters.
 * @param onMove saves a finished move; receives the move and the buckets after it. Without it items cannot be
 * moved. Throwing puts the item back.
 * @param canMove whether an item may go from the bucket `fromBucketId` to `toBucketId`. Refused buckets are
 * skipped by the drag and the actions and disabled in the menu.
 * @param moveMenu adds the Move to button to each row.
 * @param collapsed the ids of closed buckets. `null` keeps them inside the planner, all open at first.
 * @param onCollapsedChange called with the new set when the person opens or closes a bucket.
 * @param state the scroll state of the list.
 * @param labels replacements for the built-in strings.
 * @param collapseIcon an icon at the start of each bucket title, such as a chevron that points to the end; it
 * turns a quarter turn while the bucket is open. Monarch bundles no icons, so there is none by default.
 * @param menuIcon an icon for the Move to button, such as three dots. `null` shows the word "Move".
 * @param itemContent the content of a row, after the handle: the item's key, title and pickers.
 */
@ExperimentalMonarchApi
@Composable
public fun <T> MonarchBacklogPlanner(
    buckets: List<MonarchBacklogBucket<T>>,
    key: (T) -> String,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    itemPoints: ((T) -> Int?)? = null,
    onMove: (suspend (move: MonarchBacklogMove<T>, buckets: List<MonarchBacklogBucket<T>>) -> Unit)? = null,
    canMove: ((item: T, toBucketId: String, fromBucketId: String) -> Boolean)? = null,
    moveMenu: Boolean = true,
    collapsed: Set<String>? = null,
    onCollapsedChange: ((Set<String>) -> Unit)? = null,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchBacklogPlannerLabels = MonarchBacklogPlannerLabels(),
    collapseIcon: (@Composable () -> Unit)? = null,
    menuIcon: (@Composable () -> Unit)? = null,
    itemContent: @Composable RowScope.(item: T, state: MonarchBacklogPlannerItemState) -> Unit,
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchBacklogPlannerText(labels, resources) }
    var ownCollapsed by rememberSaveable { mutableStateOf(listOf<String>()) }
    val closed = collapsed ?: ownCollapsed.toSet()
    val toggle: (String) -> Unit = { id ->
        val next = if (id in closed) closed - id else closed + id
        if (collapsed == null) ownCollapsed = next.toList()
        onCollapsedChange?.invoke(next)
    }
    val rules = MonarchBacklogPlannerRules(key, closed, canMove)
    val scope = rememberCoroutineScope()
    val moves = remember(scope) { BacklogMoves<T>(scope) }
    val base = moves.base(buckets)
    val drag = remember(state) { MonarchBacklogPlannerDragState<T>(state) }
    val shown = drag.preview ?: base
    val commit: (MonarchBacklogMove<T>, List<MonarchBacklogBucket<T>>) -> Unit = { move, next ->
        val save = onMove
        if (save != null) moves.save(buckets, move, next, save, text.announce.failed(itemLabel(move.item)))
    }
    val inputs by rememberUpdatedState(BacklogInputs(base, rules, itemLabel, onMove != null, text, commit))
    val maxStep = with(LocalDensity.current) { MainTokens.AutoScrollStep.toPx() }
    val gestures = remember(drag) {
        BacklogGestures(drag, { inputs }, moves::announce) {
            scope.launch { autoScroll(drag, state, maxStep) { inputs.rules } }
        }
    }
    val actionsFor: (T, String) -> MonarchBacklogPlannerRowActions? = { item, bucketId ->
        if (onMove == null) {
            null
        } else {
            MonarchBacklogPlannerRowActions(
                onStep = { direction -> gestures.step(item, direction) },
                onMoveTo = { to -> gestures.moveTo(item, to) },
                targets = if (moveMenu) {
                    base.filter { it.id != bucketId }.map {
                        MonarchBacklogPlannerTarget(it.id, it.title, rules.allowed(item, it.id, bucketId))
                    }
                } else {
                    emptyList()
                },
            )
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
            verticalArrangement = Arrangement.spacedBy(MainTokens.RowGap),
            // The held row moves with the finger; the list scrolls only by the automatic scrolling.
            userScrollEnabled = !drag.active,
        ) {
            shown.forEach { bucket ->
                bucketEntries(
                    bucket = bucket,
                    expanded = bucket.id !in closed,
                    model = BacklogBucketModel(text, itemPoints, collapseIcon, menuIcon, drag.key, moves.pendingKey),
                    onToggle = { toggle(bucket.id) },
                    keyOf = key,
                    itemLabel = itemLabel,
                    actionsFor = actionsFor,
                    placement = { dragged -> Modifier.rowPlacement(this, dragged, drag) },
                    itemContent = itemContent,
                )
            }
        }
        // Always present, so each result is announced once as a change of this live region.
        MonarchLiveStatus(text = moves.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** What every bucket's entries need from the planner. */
private class BacklogBucketModel<T>(
    val text: MonarchBacklogPlannerText,
    val itemPoints: ((T) -> Int?)?,
    val collapseIcon: (@Composable () -> Unit)?,
    val menuIcon: (@Composable () -> Unit)?,
    val draggedKey: String?,
    val pendingKey: String?,
)

/** The lazy entries of one bucket: its header, then, when open, its rows, empty text and footer. */
@Suppress("LongParameterList")
private fun <T> LazyListScope.bucketEntries(
    bucket: MonarchBacklogBucket<T>,
    expanded: Boolean,
    model: BacklogBucketModel<T>,
    onToggle: () -> Unit,
    keyOf: (T) -> String,
    itemLabel: (T) -> String,
    actionsFor: (T, String) -> MonarchBacklogPlannerRowActions?,
    placement: @Composable LazyItemScope.(Boolean) -> Modifier,
    itemContent: @Composable RowScope.(item: T, state: MonarchBacklogPlannerItemState) -> Unit,
) {
    item(key = MainTokens.headerKey(bucket.id)) {
        MonarchBacklogPlannerHeader(
            bucket = bucket,
            state = MonarchBacklogPlannerHeaderState(
                expanded = expanded,
                points = BacklogRules.points(bucket.items, model.itemPoints),
                unestimated = BacklogRules.unestimated(bucket.items, model.itemPoints),
            ),
            onToggle = onToggle,
            text = model.text,
            collapseIcon = model.collapseIcon,
        )
    }
    if (!expanded) return
    itemsIndexed(bucket.items, key = { _, item -> MainTokens.itemKey(keyOf(item)) }) { index, item ->
        val itemKey = keyOf(item)
        val dragged = model.draggedKey == itemKey
        MonarchBacklogPlannerRow(
            modifier = placement(dragged),
            label = itemLabel(item),
            dragged = dragged,
            points = model.itemPoints?.invoke(item),
            showPoints = model.itemPoints != null,
            actions = actionsFor(item, bucket.id),
            text = model.text,
            menuIcon = model.menuIcon,
        ) {
            itemContent(item, MonarchBacklogPlannerItemState(bucket.id, index, dragged, model.pendingKey == itemKey))
        }
    }
    if (bucket.items.isEmpty()) {
        item(key = MainTokens.emptyKey(bucket.id)) {
            MonarchText(
                text = bucket.emptyText ?: model.text.empty,
                modifier = Modifier.fillMaxWidth().padding(MainTokens.EmptyPadding),
                style = MonarchTextStyle.Caption,
                color = MainTokens.meta(MonarchTheme.colors),
            )
        }
    }
    bucket.footer?.let { footer -> item(key = MainTokens.footerKey(bucket.id)) { footer() } }
}

/** The held row follows the finger above the others; the other rows slide to their new places. */
@Composable
private fun Modifier.rowPlacement(
    itemScope: LazyItemScope,
    dragged: Boolean,
    drag: MonarchBacklogPlannerDragState<*>,
): Modifier = with(itemScope) {
    if (dragged) {
        this@rowPlacement
            .zIndex(MainTokens.DRAGGED_Z)
            .graphicsLayer { translationY = drag.translation() }
    } else {
        this@rowPlacement.animateItem(
            fadeInSpec = null,
            placementSpec = MonarchTheme.motion.spec<IntOffset>(),
            fadeOutSpec = null,
        )
    }
}

/** Scrolls one step a frame while the held row is past the edge and the list can still scroll. */
private suspend fun <T> autoScroll(
    drag: MonarchBacklogPlannerDragState<T>,
    state: LazyListState,
    maxStep: Float,
    rules: () -> MonarchBacklogPlannerRules<T>,
) {
    state.monarchAutoScrollWhileHeld(maxStep, { drag.active }, drag::overscroll) { drag.retarget(rules()) }
}

/** The values the gestures and actions read when an event arrives, kept current across recompositions. */
private class BacklogInputs<T>(
    val buckets: List<MonarchBacklogBucket<T>>,
    val rules: MonarchBacklogPlannerRules<T>,
    val itemLabel: (T) -> String,
    val editable: Boolean,
    val text: MonarchBacklogPlannerText,
    val commit: (MonarchBacklogMove<T>, List<MonarchBacklogBucket<T>>) -> Unit,
)

/** Pick up, move, drop and cancel by drag, and the one-step and Move to moves, each announced. */
private class BacklogGestures<T>(
    private val drag: MonarchBacklogPlannerDragState<T>,
    private val inputs: () -> BacklogInputs<T>,
    private val announce: (String) -> Unit,
    private val startAutoScroll: () -> Job,
) {
    private var autoScroll: Job? = null

    fun start(y: Float) {
        val now = inputs()
        val key = if (now.editable) drag.start(y, now.buckets, now.rules.keyOf) else null
        val at = key?.let { BacklogRules.locate(now.buckets, it, now.rules.keyOf) } ?: return
        val bucket = now.buckets[at.bucket]
        announce(now.text.announce.pickedUp(now.itemLabel(at.item), bucket.title, at.index + 1, bucket.items.size))
    }

    /** Moves the held row; returns whether the event was used. */
    fun move(dy: Float): Boolean {
        if (!drag.active) return false
        drag.drag(dy, inputs().rules)
        if (drag.overscroll() != 0f && autoScroll?.isActive != true) autoScroll = startAutoScroll()
        return true
    }

    fun end() {
        val key = drag.key
        val origin = drag.origin
        val next = drag.end()
        if (key != null && origin != null && next != null) finish(key, origin, next, dropped = true)
    }

    fun cancel() {
        val key = drag.key ?: return
        val now = inputs()
        drag.reset()
        val item = BacklogRules.locate(now.buckets, key, now.rules.keyOf)?.item ?: return
        announce(now.text.announce.cancelled(now.itemLabel(item)))
    }

    /** Moves [item] one place up (-1) or down (+1), into the next open bucket at an edge. */
    fun step(item: T, direction: Int) {
        val now = inputs()
        val key = now.rules.keyOf(item)
        val origin = BacklogRules.where(now.buckets, key, now.rules.keyOf)
        if (drag.active || origin == null) return
        val blocked = now.rules.blocked(item, origin.bucketId)
        val to = BacklogRules.step(now.buckets, key, direction, now.rules.keyOf, blocked) ?: return
        finish(key, origin, BacklogRules.move(now.buckets, key, to, now.rules.keyOf), dropped = false)
    }

    /** Moves [item] to the end of the bucket [bucketId]. */
    fun moveTo(item: T, bucketId: String) {
        val now = inputs()
        val key = now.rules.keyOf(item)
        val origin = BacklogRules.where(now.buckets, key, now.rules.keyOf)
        val target = now.buckets.firstOrNull { it.id == bucketId }
        if (drag.active || origin == null || target == null) return
        if (!now.rules.allowed(item, bucketId, origin.bucketId)) return
        val end = MonarchBacklogPlannerMoves.Location(bucketId, target.items.size)
        finish(key, origin, BacklogRules.move(now.buckets, key, end, now.rules.keyOf), dropped = true)
    }

    private fun finish(
        key: String,
        origin: MonarchBacklogPlannerMoves.Location,
        next: List<MonarchBacklogBucket<T>>,
        dropped: Boolean,
    ) {
        val now = inputs()
        val at = BacklogRules.locate(next, key, now.rules.keyOf) ?: return
        val bucket = next[at.bucket]
        val label = now.itemLabel(at.item)
        val position = at.index + 1
        val total = bucket.items.size
        announce(
            if (dropped) {
                now.text.announce.dropped(label, bucket.title, position, total)
            } else {
                now.text.announce.moved(label, bucket.title, position, total)
            },
        )
        if (bucket.id == origin.bucketId && at.index == origin.index) return
        now.commit(MonarchBacklogMove(at.item, key, origin.bucketId, origin.index, bucket.id, at.index), next)
    }
}

/** The move being saved, the buckets shown for it, and the last announcement. */
private class BacklogMoves<T>(private val scope: CoroutineScope) {
    private var optimistic: Pair<List<MonarchBacklogBucket<T>>, List<MonarchBacklogBucket<T>>>? by mutableStateOf(null)
    var pendingKey: String? by mutableStateOf(null)
        private set
    var announcement: String by mutableStateOf("")
        private set

    /** The buckets to show: a saved move's result until the app passes new buckets. */
    fun base(buckets: List<MonarchBacklogBucket<T>>): List<MonarchBacklogBucket<T>> =
        optimistic?.takeIf { it.first === buckets }?.second ?: buckets

    fun announce(message: String) {
        // A repeated message is changed slightly so the live region reads it again.
        announcement = if (message == announcement) "$message " else message
    }

    fun save(
        buckets: List<MonarchBacklogBucket<T>>,
        move: MonarchBacklogMove<T>,
        next: List<MonarchBacklogBucket<T>>,
        onMove: suspend (MonarchBacklogMove<T>, List<MonarchBacklogBucket<T>>) -> Unit,
        failure: String,
    ) {
        optimistic = buckets to next
        pendingKey = move.itemKey
        scope.launch {
            val error = runCatching { onMove(move, next) }.exceptionOrNull()
            if (pendingKey == move.itemKey) pendingKey = null
            if (error is CancellationException) throw error
            if (error != null) {
                optimistic = null
                announce(failure)
            }
        }
    }
}
