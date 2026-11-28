package com.rookhq.monarch.components.workflow.backlogplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandle
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleState
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.theme.MonarchTheme

private typealias RowTokens = MonarchBacklogPlannerTokens

/** A bucket the Move to menu offers, and whether the item may go there. */
internal class MonarchBacklogPlannerTarget(val id: String, val title: String, val enabled: Boolean)

/** What a row does when the person moves its item without dragging. */
internal class MonarchBacklogPlannerRowActions(
    val onStep: (Int) -> Unit,
    val onMoveTo: (String) -> Unit,
    val targets: List<MonarchBacklogPlannerTarget>,
)

/**
 * One item: the drag handle with Move up and Move down actions, the app's
 * content, the points, and the Move to menu. Without [actions] the item
 * cannot be moved and the handle and menu are left out.
 */
@Composable
internal fun MonarchBacklogPlannerRow(
    modifier: Modifier,
    label: String,
    dragged: Boolean,
    points: Int?,
    showPoints: Boolean,
    actions: MonarchBacklogPlannerRowActions?,
    text: MonarchBacklogPlannerText,
    menuIcon: (@Composable () -> Unit)?,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.md
    val divider = RowTokens.border(false, colors)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (dragged) Modifier.monarchShadow(MonarchTheme.elevation.md, shape) else Modifier)
            .then(
                if (dragged) {
                    Modifier
                        .background(RowTokens.container(colors), shape)
                        .border(RowTokens.BorderWidth, RowTokens.border(true, colors), shape)
                } else {
                    // Flat rows with a hairline between them, as on the web.
                    Modifier.drawBehind {
                        val stroke = RowTokens.BorderWidth.toPx()
                        drawLine(
                            color = divider,
                            start = Offset(0f, size.height - stroke / 2),
                            end = Offset(size.width, size.height - stroke / 2),
                            strokeWidth = stroke,
                        )
                    }
                },
            )
            .padding(horizontal = RowTokens.RowPaddingHorizontal, vertical = RowTokens.RowPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(RowTokens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (actions != null) {
            MonarchDragHandle(
                label = text.handle(label),
                state = if (dragged) MonarchDragHandleState.Grabbed else MonarchDragHandleState.Idle,
                onMove = { direction -> actions.onStep(if (direction == MonarchDragHandleDirection.Up) -1 else 1) },
                actionLabels = text.actions,
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(RowTokens.PartGap),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        if (showPoints) {
            val spoken = points?.let(text::points) ?: text.notEstimated
            MonarchText(
                text = points?.let(text::points) ?: text.unestimated,
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                style = MonarchTextStyle.Caption,
                color = RowTokens.meta(colors),
            )
        }
        if (actions != null && actions.targets.isNotEmpty()) {
            MonarchBacklogPlannerMoveMenu(label, actions, text, menuIcon)
        }
    }
}

/** The Move to button and its `MonarchMenu` of the other buckets. */
@Composable
private fun MonarchBacklogPlannerMoveMenu(
    label: String,
    actions: MonarchBacklogPlannerRowActions,
    text: MonarchBacklogPlannerText,
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
