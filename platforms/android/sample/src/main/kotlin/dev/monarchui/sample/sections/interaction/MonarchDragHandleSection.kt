package dev.monarchui.sample.sections.interaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import dev.monarchui.components.interaction.draghandle.MonarchDragHandle
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleAxis
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleDirection
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleSize
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleState
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of DragHandle: a short list reordered with the TalkBack actions or the arrow keys. */
object MonarchDragHandleSection : CataloguePage {
    override val title = "DragHandle"

    @Composable
    override fun Content() {
        val items = remember { mutableStateListOf("Design review", "Budget", "Hiring plan") }
        CatalogueCaption("Reorder with the TalkBack actions or the arrow keys")
        items.forEachIndexed { index, item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchDragHandle(
                    label = "Reorder $item",
                    onMove = { direction ->
                        val target = if (direction == MonarchDragHandleDirection.Up) index - 1 else index + 1
                        if (target in items.indices) items.add(target, items.removeAt(index))
                    },
                )
                MonarchText(text = item)
            }
        }
        CatalogueCaption("States, axes and sizes")
        Row {
            MonarchDragHandle(label = "Grabbed", state = MonarchDragHandleState.Grabbed)
            MonarchDragHandle(label = "Dropped", state = MonarchDragHandleState.Dropped)
            MonarchDragHandle(label = "Disabled", enabled = false)
            MonarchDragHandle(
                label = "Horizontal",
                axis = MonarchDragHandleAxis.Horizontal,
                size = MonarchDragHandleSize.Sm,
            )
            MonarchDragHandle(label = "Grid", axis = MonarchDragHandleAxis.Both)
        }
    }
}
