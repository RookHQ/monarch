package dev.monarchui.components.workflow.kanbanboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme

private typealias ColumnPageTokens = MonarchKanbanBoardTokens

/** What every column page needs from the board. */
internal class MonarchKanbanBoardPageModel<T>(
    val text: MonarchKanbanBoardText,
    val keyOf: (T) -> String,
    val itemLabel: (T) -> String,
    val drag: MonarchKanbanBoardDragState<T>,
    val geometry: MonarchKanbanBoardGeometry,
    val pendingKey: String?,
    val actionsFor: (T, String) -> MonarchKanbanBoardCardActions?,
    val menuIcon: (@Composable () -> Unit)?,
    val cardContent: @Composable ColumnScope.(item: T, state: MonarchKanbanCardState) -> Unit,
)

/** One entry of a column's list: a card, or the line where the dragged card will land. */
private sealed interface KanbanEntry<out T> {
    class Card<T>(val item: T, val index: Int) : KanbanEntry<T>
    data object Insertion : KanbanEntry<Nothing>
}

/**
 * One column: a header with the title as a heading, the count against the
 * WIP limit and the actions, a note while it refuses the dragged card, then
 * a `LazyColumn` of cards, the empty text and the footer.
 */
@Composable
internal fun <T> MonarchKanbanBoardColumnPage(
    column: MonarchKanbanColumn<T>,
    listState: LazyListState,
    model: MonarchKanbanBoardPageModel<T>,
) {
    val colors = MonarchTheme.colors
    val drag = model.drag
    val refusing = drag.refusing == column.id
    val wip = ColumnPageTokens.wip(column.items.size, column.wipLimit)
    val shape = MonarchTheme.shapes.lg
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColumnPageTokens.column(colors), shape)
            .border(ColumnPageTokens.BorderWidth, ColumnPageTokens.columnBorder(refusing, colors), shape)
            .padding(ColumnPageTokens.ColumnPadding),
        verticalArrangement = Arrangement.spacedBy(ColumnPageTokens.CardGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ColumnPageTokens.PartGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            column.icon?.let { icon -> Box(Modifier.clearAndSetSemantics { }) { icon() } }
            MonarchText(
                text = column.title,
                modifier = Modifier.weight(1f),
                style = MonarchTextStyle.Subheading,
                heading = true,
            )
            val spoken = model.text.count(column.items.size, column.wipLimit)
            if (wip == MonarchKanbanBoardTokens.Wip.Over || wip == MonarchKanbanBoardTokens.Wip.At) {
                // As the web: the count at or over its work-in-progress limit carries the warning glyph.
                CompositionLocalProvider(LocalContentColor provides ColumnPageTokens.countColor(wip, colors)) {
                    Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Warning() }
                }
            }
            MonarchText(
                text = model.text.countText(column.items.size, column.wipLimit),
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                style = MonarchTextStyle.Caption,
                color = ColumnPageTokens.countColor(wip, colors),
            )
            column.actions?.invoke(this)
        }
        if (refusing) {
            MonarchText(
                text = model.text.blocked,
                style = MonarchTextStyle.Caption,
                color = ColumnPageTokens.meta(colors),
            )
        }
        ColumnCards(column, listState, model)
    }
}

@Composable
private fun <T> ColumnScope.ColumnCards(
    column: MonarchKanbanColumn<T>,
    listState: LazyListState,
    model: MonarchKanbanBoardPageModel<T>,
) {
    val drag = model.drag
    DisposableEffect(column.id, model.geometry) {
        onDispose { model.geometry.lists.remove(column.id) }
    }
    val entries = kanbanEntries(column, model.keyOf, drag.key, drag.target?.takeIf { it.columnId == column.id })
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .onGloballyPositioned { model.geometry.lists[column.id] = it },
        state = listState,
        verticalArrangement = Arrangement.spacedBy(ColumnPageTokens.CardGap),
        userScrollEnabled = !drag.active,
    ) {
        entries.forEach { entry ->
            when (entry) {
                KanbanEntry.Insertion -> item(key = InsertionKey) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(ColumnPageTokens.InsertionHeight)
                            .background(ColumnPageTokens.insertion(MonarchTheme.colors), MonarchTheme.shapes.full),
                    )
                }
                is KanbanEntry.Card -> item(key = ColumnPageTokens.cardKey(model.keyOf(entry.item))) {
                    ColumnCard(column.id, entry.item, entry.index, model)
                }
            }
        }
        if (column.items.isEmpty()) {
            item(key = EmptyKey) {
                MonarchText(
                    text = column.emptyText ?: model.text.empty,
                    modifier = Modifier.fillMaxWidth().padding(ColumnPageTokens.EmptyPadding),
                    style = MonarchTextStyle.Caption,
                    color = ColumnPageTokens.meta(MonarchTheme.colors),
                )
            }
        }
        column.footer?.let { footer -> item(key = FooterKey) { footer() } }
    }
}

/** The cards in order, with the insertion line before the [target] index among the cards other than [dragged]. */
private fun <T> kanbanEntries(
    column: MonarchKanbanColumn<T>,
    keyOf: (T) -> String,
    dragged: String?,
    target: MonarchKanbanBoardMoves.Location?,
): List<KanbanEntry<T>> {
    val result = mutableListOf<KanbanEntry<T>>()
    var others = 0
    column.items.forEachIndexed { index, item ->
        val held = keyOf(item) == dragged
        if (!held && target != null && target.index == others) result += KanbanEntry.Insertion
        result += KanbanEntry.Card(item, index)
        if (!held) others++
    }
    if (target != null && target.index >= others) result += KanbanEntry.Insertion
    return result
}

/** A card in its column, or, while it is dragged, a placeholder of its size where it was. */
@Composable
private fun <T> ColumnCard(columnId: String, item: T, index: Int, model: MonarchKanbanBoardPageModel<T>) {
    val key = model.keyOf(item)
    val drag = model.drag
    DisposableEffect(key, model.geometry) {
        onDispose { model.geometry.cards.remove(key) }
    }
    val actions = model.actionsFor(item, columnId)
    val enabled = actions != null
    Box(Modifier.onGloballyPositioned { model.geometry.cards[key] = it }) {
        if (drag.key == key) {
            val height = with(LocalDensity.current) { drag.size.height.toDp() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(ColumnPageTokens.placeholder(MonarchTheme.colors), MonarchTheme.shapes.md),
            )
        } else {
            MonarchKanbanBoardCard(
                label = model.itemLabel(item),
                enabled = enabled,
                overlay = false,
                actions = actions,
                text = model.text,
                menuIcon = model.menuIcon,
            ) {
                model.cardContent(
                    this,
                    item,
                    MonarchKanbanCardState(columnId, index, false, false, model.pendingKey == key, enabled),
                )
            }
        }
    }
}

private const val InsertionKey = "\u0000insertion"
private const val EmptyKey = "\u0000empty"
private const val FooterKey = "\u0000footer"
