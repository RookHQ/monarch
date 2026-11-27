package com.rookhq.monarch.components.interaction.draghandle

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The grip an item is dragged by: a dotted glyph in a 48dp touch target, with
 * a spoken name, idle, grabbed and dropped states, and move actions for
 * TalkBack, Switch Access and hardware keyboards.
 *
 * Built by Monarch from a focusable `Box` with a drawn grip: Material 3 has no
 * drag handle. It is not a button and moves nothing itself. Attach the drag
 * gesture through [modifier] (`Modifier.draggable`, `pointerInput` or
 * `dragAndDropSource`) and keep [state] in the component that moves items.
 * With [onMove], TalkBack gets one custom action per direction of [axis], and
 * the arrow keys move the item while the handle has keyboard focus.
 *
 * @param label the TalkBack name, naming the item: "Reorder Design review".
 * @param modifier the modifier of the handle. Attach the drag gesture here.
 * @param state idle, grabbed or dropped. The component that moves items owns it.
 * @param axis which way the item moves. Selects the grip pattern and the move actions.
 * @param size the visible size. The touch target is at least 48dp at both sizes.
 * @param enabled whether the handle offers its actions. A disabled handle draws its dots in the disabled colour.
 * @param onMove moves the item one step in the direction given. Without it the handle offers no actions.
 * Announce the new position from here if it matters.
 * @param actionLabels replacements for the built-in action names "Move up", "Move down", "Move backward" and
 * "Move forward" (`monarch_draghandle_move_…`). An empty string keeps the built-in name.
 * @param stateLabels replacements for the built-in states "Picked up" and "Dropped"
 * (`monarch_draghandle_grabbed`, `monarch_draghandle_dropped`). An empty string keeps the built-in state.
 * @param interactionSource the source of the focus interactions of the handle.
 */
@Composable
public fun MonarchDragHandle(
    label: String,
    modifier: Modifier = Modifier,
    state: MonarchDragHandleState = MonarchDragHandleState.Idle,
    axis: MonarchDragHandleAxis = MonarchDragHandleAxis.Vertical,
    size: MonarchDragHandleSize = MonarchDragHandleSize.Md,
    enabled: Boolean = true,
    onMove: ((MonarchDragHandleDirection) -> Unit)? = null,
    actionLabels: Map<MonarchDragHandleDirection, String> = emptyMap(),
    stateLabels: Map<MonarchDragHandleState, String> = emptyMap(),
    interactionSource: MutableInteractionSource? = null,
) {
    val target = MonarchDragHandleTokens.colors(state, enabled, MonarchTheme.colors)
    val spec = MonarchTheme.motion.spec<Color>(MonarchTheme.motion.fast)
    val container by animateColorAsState(target.container, spec, label = "handle fill")
    val dots by animateColorAsState(target.content, spec, label = "handle dots")
    val shape = MonarchTheme.shapes.sm
    val spokenState = stateLabel(state, stateLabels)
    val directions = MonarchDragHandleTokens.directions(axis)
    val names = directions.associateWith { actionLabel(it, actionLabels) }
    val move = if (enabled) onMove else null
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val outline = target.border
    Box(
        modifier = modifier
            .clearAndSetSemantics {
                contentDescription = label
                spokenState?.let { stateDescription = it }
                if (!enabled) disabled()
                if (move != null) {
                    customActions = directions.map { direction ->
                        CustomAccessibilityAction(names.getValue(direction)) {
                            move(direction)
                            true
                        }
                    }
                }
            }
            .monarchMinimumTouchTarget()
            .onKeyEvent { event -> move != null && handleKey(event, directions, rtl, move) }
            .monarchFocusRing(shape)
            .focusable(enabled = enabled && move != null, interactionSource = interactionSource)
            .size(MonarchDragHandleTokens.visibleSize(size))
            .background(container, shape)
            .then(
                if (outline != null) {
                    Modifier.border(MonarchDragHandleTokens.GrabbedOutline, outline, shape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(MonarchDragHandleTokens.GlyphSize)) {
            val unit = this.size.width / MonarchDragHandleTokens.GRID
            MonarchDragHandleTokens.dots(axis).forEach { dot ->
                drawCircle(
                    color = dots,
                    radius = MonarchDragHandleTokens.DOT_RADIUS * unit,
                    center = Offset(dot.x, dot.y) * unit,
                )
            }
        }
    }
}

private fun handleKey(
    event: KeyEvent,
    directions: List<MonarchDragHandleDirection>,
    rtl: Boolean,
    move: (MonarchDragHandleDirection) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val direction = when (event.key) {
        Key.DirectionUp -> MonarchDragHandleDirection.Up
        Key.DirectionDown -> MonarchDragHandleDirection.Down
        Key.DirectionLeft -> if (rtl) MonarchDragHandleDirection.Forward else MonarchDragHandleDirection.Backward
        Key.DirectionRight -> if (rtl) MonarchDragHandleDirection.Backward else MonarchDragHandleDirection.Forward
        else -> null
    }
    if (direction == null || direction !in directions) return false
    move(direction)
    return true
}

@Composable
private fun actionLabel(direction: MonarchDragHandleDirection, overrides: Map<MonarchDragHandleDirection, String>) =
    overrides[direction]?.takeIf { it.isNotEmpty() } ?: stringResource(
        when (direction) {
            MonarchDragHandleDirection.Up -> R.string.monarch_draghandle_move_up
            MonarchDragHandleDirection.Down -> R.string.monarch_draghandle_move_down
            MonarchDragHandleDirection.Backward -> R.string.monarch_draghandle_move_backward
            MonarchDragHandleDirection.Forward -> R.string.monarch_draghandle_move_forward
        },
    )

@Composable
private fun stateLabel(state: MonarchDragHandleState, overrides: Map<MonarchDragHandleState, String>): String? {
    val resource = when (state) {
        MonarchDragHandleState.Idle -> return null
        MonarchDragHandleState.Grabbed -> R.string.monarch_draghandle_grabbed
        MonarchDragHandleState.Dropped -> R.string.monarch_draghandle_dropped
    }
    return overrides[state]?.takeIf { it.isNotEmpty() } ?: stringResource(resource)
}
