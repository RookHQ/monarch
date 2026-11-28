package dev.monarchui.components.workflow.kanbanboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.interaction.draghandle.MonarchDragHandle
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleAxis
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleDirection
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleState
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.theme.MonarchTheme

/** A column the Move to menu offers, and whether the card may go there. */
internal class MonarchKanbanBoardTarget(val id: String, val title: String, val enabled: Boolean)

/** What a card does when the person moves it without dragging. `null` parts are left out. */
internal class MonarchKanbanBoardCardActions(
    val onStep: ((MonarchDragHandleDirection) -> Unit)?,
    val onMoveTo: (String) -> Unit,
    val targets: List<MonarchKanbanBoardTarget>,
)

/**
 * The frame of one card: the drag handle with its four move actions, the
 * app's content, and the Move to menu. The [overlay] copy that follows the
 * finger has a grabbed handle and no menu.
 */
@Composable
internal fun MonarchKanbanBoardCard(
    label: String,
    enabled: Boolean,
    overlay: Boolean,
    actions: MonarchKanbanBoardCardActions?,
    text: MonarchKanbanBoardText,
    menuIcon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.md
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MonarchKanbanBoardTokens.card(colors), shape)
            .border(MonarchKanbanBoardTokens.BorderWidth, MonarchKanbanBoardTokens.cardBorder(overlay, colors), shape)
            .padding(MonarchKanbanBoardTokens.CardPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchKanbanBoardTokens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (actions != null || overlay) {
            MonarchDragHandle(
                label = text.handle(label),
                state = if (overlay) MonarchDragHandleState.Grabbed else MonarchDragHandleState.Idle,
                axis = MonarchDragHandleAxis.Both,
                enabled = enabled,
                onMove = actions?.onStep,
                actionLabels = text.actions,
            )
        }
        Column(modifier = Modifier.weight(1f), content = content)
        if (!overlay && actions != null && actions.targets.isNotEmpty()) {
            MonarchKanbanBoardMoveMenu(label, actions, text, menuIcon)
        }
    }
}

/** The Move to button and its `MonarchMenu` of the other columns. */
@Composable
private fun MonarchKanbanBoardMoveMenu(
    label: String,
    actions: MonarchKanbanBoardCardActions,
    text: MonarchKanbanBoardText,
    menuIcon: (@Composable () -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    val name = text.moveMenu(label)
    Box {
        if (menuIcon != null) {
            MonarchIconButton(label = name, onClick = { open = true }, icon = menuIcon)
        } else {
            MonarchButton(
                text = text.move,
                onClick = { open = true },
                modifier = Modifier.semantics { contentDescription = name },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.targets.forEach { target ->
                MonarchMenuItem(
                    text = text.moveTo(target.title),
                    onClick = {
                        open = false
                        actions.onMoveTo(target.id)
                    },
                    enabled = target.enabled,
                )
            }
        }
    }
}
