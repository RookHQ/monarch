package dev.monarchui.components.workflow.kanbanboard

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.zIndex
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleDirection
import dev.monarchui.foundation.monarchShadow
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchDragAutoScroll
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private typealias MainTokens = MonarchKanbanBoardTokens
private typealias BoardMoves = MonarchKanbanBoardMoves

/**
 * A board of columns of cards, such as To do, Doing and Done, where people
 * move cards within and between columns by dragging, TalkBack actions or a
 * Move to menu. Columns show their count against a work-in-progress limit.
 *
 * Built by Monarch for phones on a Compose `HorizontalPager`, as the web board: the columns sit side by side
 * and the board scrolls sideways; a phone shows one column at [minColumnWidth] with the next peeking in, a wider
 * screen as many as fit. Each column is
 * a `LazyColumn` of cards. A long press on a card lifts a copy that follows
 * the finger (`detectDragGesturesAfterLongPress`); a line shows where it will
 * land, holding the finger at the start or end of the board turns to the
 * previous or next column, holding it near the top or bottom of a column
 * scrolls the column, and lifting the finger drops it. A column [canMove]
 * refuses, or a full one with [enforceWipLimits], is outlined and says so,
 * and a drop there is refused. Each card starts with a `MonarchDragHandle`
 * whose Move up, Move down, Move backward and Move forward actions (TalkBack,
 * Switch Access, arrow keys) move it one step, and ends with a Move to button
 * that opens a Material 3 `DropdownMenu` of the other columns, the path that
 * always works.
 *
 * The board is controlled: each finished move calls [onMove] once with the
 * move and the columns after it. The move stays on screen while [onMove]
 * runs and until new [columns] arrive; if it throws, the card goes back and
 * the failure is announced. Every pick up, drop, move, cancel and refusal is
 * announced through a polite live region. Give the board a bounded height,
 * such as a screen: its columns are lazy.
 *
 * @param columns the columns and their cards.
 * @param key a stable, unique key for a card, across every column.
 * @param itemLabel the plain-text name of a card, used in the handle's name, the menu and announcements.
 * @param modifier the modifier of the board.
 * @param onMove saves a finished move; receives the move and the columns after it. Without it cards cannot be
 * moved. Throwing puts the card back.
 * @param canMove whether a card may go from the column `fromColumnId` to `toColumnId`. Its own column always
 * takes it.
 * @param enforceWipLimits a column at or over its `wipLimit` refuses cards from other columns.
 * @param enabled whether any card can be moved.
 * @param itemEnabled whether a card can be moved, such as one the person may not edit.
 * @param moveMenu adds the Move to button to each card.
 * @param minColumnWidth the narrowest a column gets. A phone shows one column per page.
 * @param pagerState the state of the pager, for the column shown first or to follow the column in view.
 * @param labels replacements for the built-in strings.
 * @param menuIcon an icon for the Move to button, such as three dots. `null` shows the word "Move".
 * @param cardContent the content of a card, after the handle; it knows its column and index and whether it is
 * dragged or pending.
 */
@Composable
public fun <T> MonarchKanbanBoard(
    columns: List<MonarchKanbanColumn<T>>,
    key: (T) -> String,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    onMove: (suspend (move: MonarchKanbanMove<T>, columns: List<MonarchKanbanColumn<T>>) -> Unit)? = null,
    canMove: ((item: T, toColumnId: String, fromColumnId: String) -> Boolean)? = null,
    enforceWipLimits: Boolean = false,
    enabled: Boolean = true,
    itemEnabled: (T) -> Boolean = { true },
    moveMenu: Boolean = true,
    minColumnWidth: Dp = MainTokens.MinColumnWidth,
    pagerState: PagerState = rememberPagerState { columns.size },
    labels: MonarchKanbanBoardLabels = MonarchKanbanBoardLabels(),
    menuIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.More,
    cardContent: @Composable ColumnScope.(item: T, state: MonarchKanbanCardState) -> Unit,
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchKanbanBoardText(labels, resources) }
    val scope = rememberCoroutineScope()
    val moves = remember(scope) { KanbanMoves<T>(scope) }
    val base = moves.base(columns)
    val drag = remember { MonarchKanbanBoardDragState<T>() }
    val geometry = remember { MonarchKanbanBoardGeometry() }
    val listStates = remember { mutableMapOf<String, LazyListState>() }
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val movable: (T) -> Boolean = { onMove != null && enabled && itemEnabled(it) }
    val rules = KanbanRules(key, itemLabel, movable, canMove, enforceWipLimits)
    val showPage: (String) -> Unit = { id ->
        val page = base.indexOfFirst { it.id == id }
        if (page >= 0) scope.launch { pagerState.animateScrollToPage(page) }
    }
    val inputs by rememberUpdatedState(
        KanbanInputs(base, rules, text, rtl, showPage) { move, next ->
            val save = onMove
            if (save != null) {
                val failure = text.announce.failed(itemLabel(move.item), base.titleOf(move.fromColumnId))
                moves.save(columns, move, next, save, failure)
            }
        },
    )
    val gestures = remember(drag, geometry) {
        KanbanGestures(
            drag = drag,
            geometry = geometry,
            inputs = { inputs },
            announce = moves::announce,
            scroll = KanbanScrolling(scope, pagerState, listStates, density),
        )
    }
    val actionsFor = kanbanActions(base, rules, gestures, moveMenu)
    val model = MonarchKanbanBoardPageModel(
        text = text,
        keyOf = key,
        itemLabel = itemLabel,
        drag = drag,
        geometry = geometry,
        pendingKey = moves.pendingKey,
        actionsFor = actionsFor,
        menuIcon = menuIcon,
        cardContent = cardContent,
    )
    Column(modifier = modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { geometry.board = it }
                .pointerInput(gestures) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { gestures.start(it) },
                        onDrag = { change, amount -> if (gestures.move(amount)) change.consume() },
                        onDragEnd = { gestures.end() },
                        onDragCancel = { gestures.cancel() },
                    )
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSize = KanbanPageSize(minColumnWidth),
                pageSpacing = MainTokens.ColumnGap,
                // A small board keeps every column composed, so a screen reader finds them all; while a card is
                // held every column stays composed, so its place can be read and it can land there.
                beyondViewportPageCount = if (drag.active) {
                    base.size
                } else {
                    base.size.coerceAtMost(MainTokens.COMPOSED_COLUMNS)
                },
                userScrollEnabled = !drag.active,
                key = { page -> base.getOrNull(page)?.id ?: page },
            ) { page ->
                base.getOrNull(page)?.let { column ->
                    MonarchKanbanBoardColumnPage(
                        column = column,
                        listState = listStates.getOrPut(column.id) { LazyListState() },
                        model = model,
                    )
                }
            }
            KanbanOverlay(drag, model)
        }
        // Always present, so each result is announced once as a change of this live region.
        MonarchLiveStatus(text = moves.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** What each card offers for moving without a drag, or `null` for a card that cannot move. */
private fun <T> kanbanActions(
    columns: List<MonarchKanbanColumn<T>>,
    rules: KanbanRules<T>,
    gestures: KanbanGestures<T>,
    moveMenu: Boolean,
): (T, String) -> MonarchKanbanBoardCardActions? = { item, columnId ->
    if (!rules.movable(item)) {
        null
    } else {
        MonarchKanbanBoardCardActions(
            onStep = { direction -> gestures.step(item, direction) },
            onMoveTo = { to -> gestures.moveTo(item, to) },
            targets = if (moveMenu) {
                columns.filter { it.id != columnId }.map {
                    MonarchKanbanBoardTarget(it.id, it.title, !rules.refuses(it, item, columnId))
                }
            } else {
                emptyList()
            },
        )
    }
}

/** The copy of the held card that follows the finger. Hidden from TalkBack, which hears the announcements. */
@Composable
private fun <T> BoxScope.KanbanOverlay(drag: MonarchKanbanBoardDragState<T>, model: MonarchKanbanBoardPageModel<T>) {
    val item = drag.item
    val origin = drag.origin
    if (!drag.active || item == null || origin == null) return
    val shape = MonarchTheme.shapes.md
    val width = with(LocalDensity.current) { drag.size.width.toDp() }
    Box(
        Modifier
            .align(AbsoluteAlignment.TopLeft)
            .zIndex(MainTokens.OVERLAY_Z)
            .absoluteOffset {
                val at = drag.pointer - drag.grab
                IntOffset(at.x.toInt(), at.y.toInt())
            }
            .width(width)
            .monarchShadow(MonarchTheme.elevation.lg, shape)
            .clearAndSetSemantics { },
    ) {
        MonarchKanbanBoardCard(
            label = model.itemLabel(item),
            enabled = true,
            overlay = true,
            actions = null,
            text = model.text,
            menuIcon = null,
        ) {
            val state = MonarchKanbanCardState(origin.columnId, origin.index, true, true, false, true)
            model.cardContent(this, item, state)
        }
    }
}

/** As many pages side by side as fit at the minimum width, at least one. */
private class KanbanPageSize(private val minWidth: Dp) : PageSize {
    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
        val min = minWidth.roundToPx()
        val count = BoardMoves.columnsThatFit(availableSpace, min, pageSpacing)
        // As the web: the columns sit side by side and the board scrolls sideways. On a phone one column has its
        // own width and the next one peeks in, so there is no tab strip standing in for the columns.
        if (count == 1 && availableSpace > min) return min
        return (availableSpace - (count - 1) * pageSpacing) / count
    }
}

/** The title of the column [id], or the id. */
private fun <T> List<MonarchKanbanColumn<T>>.titleOf(id: String): String = firstOrNull { it.id == id }?.title ?: id

/** The app's rules a board moves cards by. */
private class KanbanRules<T>(
    val keyOf: (T) -> String,
    val itemLabel: (T) -> String,
    val movable: (T) -> Boolean,
    private val canMove: ((T, String, String) -> Boolean)?,
    private val enforceWipLimits: Boolean,
) {
    fun refuses(column: MonarchKanbanColumn<T>, item: T, from: String): Boolean =
        BoardMoves.refuses(column, item, from, canMove, enforceWipLimits)
}

/** The values the gestures and actions read when an event arrives, kept current across recompositions. */
private class KanbanInputs<T>(
    val columns: List<MonarchKanbanColumn<T>>,
    val rules: KanbanRules<T>,
    val text: MonarchKanbanBoardText,
    val rtl: Boolean,
    val showPage: (String) -> Unit,
    val commit: (MonarchKanbanMove<T>, List<MonarchKanbanColumn<T>>) -> Unit,
)

/** The pager and the column lists the drag scrolls, and the coroutine scope it scrolls them in. */
private class KanbanScrolling(
    val scope: CoroutineScope,
    val pager: PagerState,
    val lists: Map<String, LazyListState>,
    val density: Density,
)

/** Pick up, move, drop and cancel by drag, and the one-step and Move to moves, each announced. */
private class KanbanGestures<T>(
    private val drag: MonarchKanbanBoardDragState<T>,
    private val geometry: MonarchKanbanBoardGeometry,
    private val inputs: () -> KanbanInputs<T>,
    private val announce: (String) -> Unit,
    private val scroll: KanbanScrolling,
) {
    private var autoScroll: Job? = null
    private val zone: Float get() = with(scroll.density) { MainTokens.EdgeZone.toPx() }
    private val maxStep: Float get() = with(scroll.density) { MainTokens.AutoScrollStep.toPx() }

    fun start(point: Offset) {
        val now = inputs()
        val key = geometry.cardAt(point)
        val at = key?.let { BoardMoves.locate(now.columns, it, now.rules.keyOf) }
        val movable = at != null && now.rules.movable(at.item)
        val bounds = key?.takeIf { movable }?.let { geometry.bounds(geometry.cards[it]) }
        if (key == null || at == null || bounds == null) return
        val column = now.columns[at.column]
        drag.start(key, at.item, MonarchKanbanBoardMoves.Location(column.id, at.index), point, bounds)
        val label = now.rules.itemLabel(at.item)
        announce(now.text.announce.pickedUp(label, column.title, at.index + 1, column.items.size))
    }

    /** Moves the held card; returns whether the event was used. */
    fun move(amount: Offset): Boolean {
        if (!drag.active) return false
        drag.pointer += amount
        retarget()
        if (edgeStep() != 0 || verticalOverscroll() != 0f) {
            if (autoScroll?.isActive != true) autoScroll = scroll.scope.launch { autoScroll() }
        }
        return true
    }

    /** Reads the place under the finger. */
    fun retarget() {
        val now = inputs()
        val item = drag.item
        val origin = drag.origin
        val column = geometry.columnAt(drag.pointer)?.let { id -> now.columns.firstOrNull { it.id == id } }
        if (item == null || origin == null || column == null) return
        if (now.rules.refuses(column, item, origin.columnId)) {
            drag.refusing = column.id
            drag.target = null
        } else {
            drag.refusing = null
            dropIndex(column, now.rules.keyOf)?.let { drag.target = MonarchKanbanBoardMoves.Location(column.id, it) }
        }
    }

    /** The index the held card would get in [column] at the finger, or `null` when the column is not laid out. */
    private fun dropIndex(column: MonarchKanbanColumn<T>, keyOf: (T) -> String): Int? {
        val list = geometry.bounds(geometry.lists[column.id]) ?: return null
        val others = column.items.map(keyOf).filter { it != drag.key }
        val slots = scroll.lists[column.id]?.layoutInfo?.visibleItemsInfo.orEmpty().mapNotNull { info ->
            val cardKey = (info.key as? String)?.takeIf { it.startsWith("c:") }?.drop(2)
            val index = cardKey?.let(others::indexOf) ?: -1
            if (index < 0) null else MonarchKanbanBoardMoves.Slot(index, info.offset + info.size / 2f)
        }
        return BoardMoves.indexAt(drag.pointer.y - list.top, slots, others.size)
    }

    private fun edgeStep(): Int {
        val board = geometry.board ?: return 0
        return BoardMoves.edge(drag.pointer.x, board.size.width.toFloat(), zone, inputs().rtl)
    }

    private fun verticalOverscroll(): Float {
        val columnId = drag.target?.columnId ?: return 0f
        val list = geometry.bounds(geometry.lists[columnId])?.roundToIntRect() ?: return 0f
        val y = drag.pointer.y
        return when {
            y < list.top + zone -> y - (list.top + zone)
            y > list.bottom - zone -> y - (list.bottom - zone)
            else -> 0f
        }
    }

    /** Turns the page after the finger rests at an edge, and scrolls the column near its top or bottom. */
    private suspend fun autoScroll() {
        var edgeSince = -1L
        while (drag.active) {
            val frame = withFrameMillis { it }
            val step = edgeStep()
            val pager = scroll.pager
            val page = pager.currentPage + step
            if (step != 0 && page in 0 until pager.pageCount) {
                if (edgeSince < 0) edgeSince = frame
                if (frame - edgeSince >= MainTokens.EDGE_DWELL_MILLIS) {
                    pager.animateScrollToPage(page)
                    edgeSince = -1L
                }
            } else {
                edgeSince = -1L
            }
            val overscroll = verticalOverscroll()
            val list = drag.target?.columnId?.let(scroll.lists::get)
            if (overscroll != 0f && list != null) list.scrollBy(MonarchDragAutoScroll.step(overscroll, maxStep))
            retarget()
            if (step == 0 && overscroll == 0f) break
        }
    }

    fun end() {
        val now = inputs()
        val key = drag.key ?: return
        val item = drag.item ?: return
        val origin = drag.origin ?: return
        val refusing = drag.refusing
        val target = drag.target
        drag.reset()
        autoScroll?.cancel()
        val label = now.rules.itemLabel(item)
        when {
            refusing != null -> {
                announce(now.text.announce.notAllowed(label, now.columns.titleOf(refusing)))
            }
            target == null -> announce(now.text.announce.cancelled(label, now.columns.titleOf(origin.columnId)))
            else -> finish(key, origin, BoardMoves.move(now.columns, key, target, now.rules.keyOf), dropped = true)
        }
    }

    fun cancel() {
        val now = inputs()
        val item = drag.item ?: return
        val origin = drag.origin ?: return
        drag.reset()
        autoScroll?.cancel()
        announce(now.text.announce.cancelled(now.rules.itemLabel(item), now.columns.titleOf(origin.columnId)))
    }

    /** Moves [item] one step in [direction]: within its column, or to the same place in the next column. */
    fun step(item: T, direction: MonarchDragHandleDirection) {
        val now = inputs()
        if (drag.active) return
        val key = now.rules.keyOf(item)
        val at = BoardMoves.locate(now.columns, key, now.rules.keyOf) ?: return
        val origin = MonarchKanbanBoardMoves.Location(now.columns[at.column].id, at.index)
        val blocked: (String) -> Boolean = { id ->
            now.columns.firstOrNull { it.id == id }?.let { now.rules.refuses(it, item, origin.columnId) } ?: true
        }
        val to = BoardMoves.step(now.columns, key, direction, now.rules.keyOf, blocked) ?: return
        finish(key, origin, BoardMoves.move(now.columns, key, to, now.rules.keyOf), dropped = false)
        if (to.columnId != origin.columnId) now.showPage(to.columnId)
    }

    /** Moves [item] to the end of the column [columnId]. */
    fun moveTo(item: T, columnId: String) {
        val now = inputs()
        val key = now.rules.keyOf(item)
        val origin = BoardMoves.where(now.columns, key, now.rules.keyOf)
        val target = now.columns.firstOrNull { it.id == columnId }
        if (drag.active || origin == null || target == null) return
        if (now.rules.refuses(target, item, origin.columnId)) return
        val end = MonarchKanbanBoardMoves.Location(columnId, target.items.size)
        finish(key, origin, BoardMoves.move(now.columns, key, end, now.rules.keyOf), dropped = true)
    }

    private fun finish(
        key: String,
        origin: MonarchKanbanBoardMoves.Location,
        next: List<MonarchKanbanColumn<T>>,
        dropped: Boolean,
    ) {
        val now = inputs()
        val at = BoardMoves.locate(next, key, now.rules.keyOf) ?: return
        val column = next[at.column]
        val label = now.rules.itemLabel(at.item)
        val position = at.index + 1
        val total = column.items.size
        announce(
            if (dropped) {
                now.text.announce.dropped(label, column.title, position, total)
            } else {
                now.text.announce.moved(label, column.title, position, total)
            },
        )
        if (column.id == origin.columnId && at.index == origin.index) return
        now.commit(MonarchKanbanMove(at.item, key, origin.columnId, origin.index, column.id, at.index), next)
    }
}

/** The move being saved, the columns shown for it, and the last announcement. */
private class KanbanMoves<T>(private val scope: CoroutineScope) {
    private var optimistic: Pair<List<MonarchKanbanColumn<T>>, List<MonarchKanbanColumn<T>>>? by mutableStateOf(null)
    var pendingKey: String? by mutableStateOf(null)
        private set
    var announcement: String by mutableStateOf("")
        private set

    /** The columns to show: a saved move's result until the app passes new columns. */
    fun base(columns: List<MonarchKanbanColumn<T>>): List<MonarchKanbanColumn<T>> =
        optimistic?.takeIf { it.first === columns }?.second ?: columns

    fun announce(message: String) {
        // A repeated message is changed slightly so the live region reads it again.
        announcement = if (message == announcement) "$message " else message
    }

    fun save(
        columns: List<MonarchKanbanColumn<T>>,
        move: MonarchKanbanMove<T>,
        next: List<MonarchKanbanColumn<T>>,
        onMove: suspend (MonarchKanbanMove<T>, List<MonarchKanbanColumn<T>>) -> Unit,
        failure: String,
    ) {
        optimistic = columns to next
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
