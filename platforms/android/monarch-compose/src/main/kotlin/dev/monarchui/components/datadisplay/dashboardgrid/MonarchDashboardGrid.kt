package dev.monarchui.components.datadisplay.dashboardgrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.interaction.sortablegrid.MonarchSortableGrid
import dev.monarchui.components.interaction.sortablegrid.MonarchSortableGridLabels
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The grid a dashboard's widgets sit in: one column on phones and with large
 * text, up to [columns] on wide screens, widgets spanning columns, and an
 * edit mode in which people rearrange the widgets.
 *
 * Viewing, it is a `LazyVerticalGrid` with fixed columns chosen from its own
 * width: one below 640dp, at most two below 1024dp, and [columns] from there;
 * one at font scale 1.5 and above. Each widget spans the columns [span] gives
 * it, clamped to the row. In [editing] mode the widgets are a
 * `MonarchSortableGrid`: a long press drags a widget, its handle offers the
 * move actions to TalkBack, Switch Access and the arrow keys, and an arrange
 * bar on each widget adds Move earlier and Move later buttons, a one-tap
 * alternative. Every move is announced. Spans do not apply while editing.
 *
 * The order is hoisted: [layout] is the order of keys saved for this person
 * and [onLayoutChange] receives the new order after each move. Unknown keys in
 * [layout] are skipped and new widgets follow in their default order, so a
 * saved layout survives widgets being added or removed.
 *
 * The grid is lazy and scrolls itself: do not put it in a vertically
 * scrolling parent without a bounded height.
 *
 * @param items the widgets in their default order.
 * @param key a widget's stable, unique key, stored in [layout].
 * @param itemLabel a widget's plain-text name, for the arrange bar, the button names and the announcements.
 * @param modifier the modifier of the grid.
 * @param layout the order of keys saved for this person; empty for the default order.
 * @param onLayoutChange called with the whole new order after each finished move, to save it. Without it the
 * grid cannot be rearranged.
 * @param span the columns a widget spans on wide screens. One by default.
 * @param columns the columns from 1024dp of grid width, 1 to 4.
 * @param editing shows the arrange bars and makes the widgets draggable. Needs [onLayoutChange].
 * @param state the scroll state of the grid.
 * @param labels replacements for the built-in strings.
 * @param itemContent draws a widget, usually a `MonarchWidget`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun <T> MonarchDashboardGrid(
    items: List<T>,
    key: (T) -> String,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    layout: List<String> = emptyList(),
    onLayoutChange: ((layout: List<String>) -> Unit)? = null,
    span: (T) -> MonarchDashboardGridSpan = { MonarchDashboardGridSpan.One },
    columns: Int = DefaultColumns,
    editing: Boolean = false,
    state: LazyGridState = rememberLazyGridState(),
    labels: MonarchDashboardGridLabels = MonarchDashboardGridLabels(),
    itemContent: @Composable (item: T) -> Unit,
) {
    val tokens = MonarchDashboardGridTokens
    val byKey = items.associateBy(key)
    val order = tokens.resolve(items.map(key), layout)
    val ordered = order.mapNotNull { byKey[it] }
    val texts = dashboardGridTexts(labels)
    var announcement by remember { mutableStateOf("") }
    val announce: (String) -> Unit = { text -> announcement = if (text == announcement) "$text " else text }
    val gridLabel = monarchString(labels.label, R.string.monarch_dashboardgrid_label)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = gridLabel
        },
    ) {
        val across = tokens.columnsAt(maxWidth, fontScale, columns)
        if (editing && onLayoutChange != null) {
            val move: (Int, Int) -> Unit = { from, to ->
                val item = ordered[from]
                onLayoutChange(tokens.moved(order, from, to))
                announce(texts.moved(itemLabel(item), to + 1, ordered.size))
            }
            MonarchSortableGrid(
                items = ordered,
                key = key,
                itemLabel = itemLabel,
                onReorder = { from, to -> onLayoutChange(tokens.moved(order, from, to)) },
                columns = across,
                state = state,
                labels = MonarchSortableGridLabels(
                    handle = texts.handle,
                    pickedUp = texts.pickedUp,
                    moved = texts.moved,
                    dropped = texts.dropped,
                    cancelled = texts.cancelled,
                ),
            ) { item, itemState ->
                ArrangeBar(
                    label = itemLabel(item),
                    index = itemState.index,
                    count = ordered.size,
                    texts = texts,
                    onMove = { step -> move(itemState.index, itemState.index + step) },
                )
                itemContent(item)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(across),
                modifier = Modifier.fillMaxWidth(),
                state = state,
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            ) {
                items(
                    items = ordered,
                    key = key,
                    span = { item -> GridItemSpan(tokens.spanOf(span(item), maxLineSpan)) },
                ) { item -> Box { itemContent(item) } }
            }
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The columns from 1024dp of grid width unless the app asks for others. */
private const val DefaultColumns = 3

/** The widget's name and its Move earlier and Move later buttons on one quiet line, shown in edit mode. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArrangeBar(label: String, index: Int, count: Int, texts: DashboardGridTexts, onMove: (Int) -> Unit) {
    val tokens = MonarchDashboardGridTokens
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.BarPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.BarGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(
            text = label,
            modifier = Modifier.padding(end = tokens.BarPadding),
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        MoveButton(texts.earlier, texts.moveEarlier(label), enabled = index > 0) { onMove(-1) }
        MoveButton(texts.later, texts.moveLater(label), enabled = index < count - 1) { onMove(1) }
    }
}

@Composable
private fun MoveButton(text: String, name: String, enabled: Boolean, onClick: () -> Unit) {
    MonarchButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = name },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
    )
}

/** The built-in strings, with the caller's replacements applied. */
private class DashboardGridTexts(
    val handle: (String) -> String,
    val earlier: String,
    val later: String,
    val moveEarlier: (String) -> String,
    val moveLater: (String) -> String,
    val pickedUp: (String, Int, Int) -> String,
    val moved: (String, Int, Int) -> String,
    val dropped: (String, Int, Int) -> String,
    val cancelled: (String, Int, Int) -> String,
)

@Composable
private fun dashboardGridTexts(labels: MonarchDashboardGridLabels): DashboardGridTexts {
    val handle = stringResource(R.string.monarch_dashboardgrid_handle)
    val moveEarlier = stringResource(R.string.monarch_dashboardgrid_move_earlier)
    val moveLater = stringResource(R.string.monarch_dashboardgrid_move_later)
    val pickedUp = stringResource(R.string.monarch_dashboardgrid_picked_up)
    val moved = stringResource(R.string.monarch_dashboardgrid_moved)
    val dropped = stringResource(R.string.monarch_dashboardgrid_dropped)
    val cancelled = stringResource(R.string.monarch_dashboardgrid_cancelled)
    return DashboardGridTexts(
        handle = labels.handle ?: { handle.format(it) },
        earlier = monarchString(labels.earlier, R.string.monarch_dashboardgrid_earlier),
        later = monarchString(labels.later, R.string.monarch_dashboardgrid_later),
        moveEarlier = labels.moveEarlier ?: { moveEarlier.format(it) },
        moveLater = labels.moveLater ?: { moveLater.format(it) },
        pickedUp = labels.pickedUp ?: { label, position, total -> pickedUp.format(label, position, total) },
        moved = labels.moved ?: { label, position, total -> moved.format(label, position, total) },
        dropped = labels.dropped ?: { label, position, total -> dropped.format(label, position, total) },
        cancelled = labels.cancelled ?: { label, position, total -> cancelled.format(label, position, total) },
    )
}
