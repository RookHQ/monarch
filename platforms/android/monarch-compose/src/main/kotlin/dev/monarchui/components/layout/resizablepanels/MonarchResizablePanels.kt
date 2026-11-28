package dev.monarchui.components.layout.resizablepanels

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.window.core.layout.WindowSizeClass
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat

/**
 * Two panels with a separator between them that people drag to share the
 * space, for an editor beside a preview or a list beside a detail on a tablet.
 *
 * Built by Monarch from a Compose `Layout`, `draggable` and key handling:
 * Material 3 has no general resizable split. For a list and its detail, use
 * the Material `ListDetailPaneScaffold`, whose `paneExpansionDragHandle` also
 * resizes (see the split-layout entry). The app owns [fraction], the share of
 * the first panel, and saves it from [onResizeFinished].
 *
 * The separator reserves a 48dp touch target between the panels and draws a
 * line in its middle, thicker and in the accent colour while dragged or
 * focused. It is one TalkBack element named "Resize panels" whose state is the
 * share of the first panel as a percentage, adjusted with the slider gestures
 * by [step]. With a hardware keyboard it takes focus; the arrow keys along the
 * axis move it by [step] in the direction they point, and Home and End move it
 * to the ends of [range]. In a right-to-left layout the first panel is on the
 * right and dragging follows the finger. On a compact window (narrower than
 * 600dp) side-by-side panels are stacked at their natural height with a
 * divider and no resizing, unless [stacksWhenCompact] is `false`.
 *
 * @param fraction the share of the first panel, from 0 to 1. Kept within [range].
 * @param onFractionChange called with the new share while the separator moves.
 * @param modifier the modifier of the panels.
 * @param axis side by side, or stacked with the first panel on top. A vertical layout needs a bounded
 * height; without one the panels take their natural height.
 * @param range the smallest and largest share of the first panel, within 0..1.
 * @param step the share one arrow key press or one TalkBack adjustment moves the separator.
 * @param enabled whether people can move the separator.
 * @param showGrip draws a grip in the middle of the separator.
 * @param stacksWhenCompact stacks side-by-side panels without resizing on a compact window.
 * @param handleLabel replaces the built-in name "Resize panels" read by TalkBack.
 * @param onResizeFinished called with the share when a drag ends and after each key press or TalkBack
 * adjustment. Save the layout here.
 * @param interactionSource the interaction source of the separator.
 * @param first the start or top panel.
 * @param second the end or bottom panel.
 */
@Composable
public fun MonarchResizablePanels(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    axis: MonarchResizablePanelsAxis = MonarchResizablePanelsAxis.Horizontal,
    range: ClosedFloatingPointRange<Float> = MonarchResizablePanelsDefaults.Range,
    step: Float = MonarchResizablePanelsDefaults.Step,
    enabled: Boolean = true,
    showGrip: Boolean = false,
    stacksWhenCompact: Boolean = true,
    handleLabel: String? = null,
    onResizeFinished: ((Float) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    val tokens = MonarchResizablePanelsTokens
    val compact = !currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    if (tokens.stacks(axis, stacksWhenCompact, compact)) {
        Column(modifier = modifier) {
            first()
            MonarchDivider()
            second()
        }
        return
    }
    val shown = tokens.clamp(fraction, range)
    var totalPx by remember { mutableIntStateOf(0) }
    val handlePx = with(LocalDensity.current) { tokens.HandleThickness.roundToPx() }
    val handle: @Composable () -> Unit = {
        ResizeHandle(
            fraction = shown,
            onFractionChange = onFractionChange,
            availablePx = totalPx - handlePx,
            axis = axis,
            range = range,
            step = step,
            enabled = enabled,
            showGrip = showGrip,
            label = monarchString(handleLabel, R.string.monarch_resizablepanels_handle),
            onResizeFinished = onResizeFinished,
            interactionSource = interactionSource,
        )
    }
    Layout(
        contents = listOf(first, handle, second),
        modifier = modifier.onSizeChanged {
            totalPx = if (axis == MonarchResizablePanelsAxis.Horizontal) it.width else it.height
        },
    ) { measurables, constraints ->
        measurePanels(
            measurables = measurables,
            constraints = constraints,
            horizontal = axis == MonarchResizablePanelsAxis.Horizontal,
            handlePx = handlePx,
            fraction = shown,
        )
    }
}

/** Measures the two panels by their share of the length left by the separator, and places them in order. */
private fun MeasureScope.measurePanels(
    measurables: List<List<Measurable>>,
    constraints: Constraints,
    horizontal: Boolean,
    handlePx: Int,
    fraction: Float,
): MeasureResult {
    val bounded = if (horizontal) constraints.hasBoundedWidth else constraints.hasBoundedHeight
    val total = if (horizontal) constraints.maxWidth else constraints.maxHeight
    val sizes = if (bounded) {
        MonarchResizablePanelsTokens.panelSizes(total, handlePx, fraction)
    } else {
        Constraints.Infinity to Constraints.Infinity
    }
    val (firstMeasurables, handleMeasurables, secondMeasurables) = measurables
    val first = firstMeasurables.map { it.measure(constraints.along(horizontal, sizes.first)) }
    val second = secondMeasurables.map { it.measure(constraints.along(horizontal, sizes.second)) }
    val cross = crossSize(constraints, horizontal, first + second)
    val handle = handleMeasurables.map {
        it.measure(if (horizontal) Constraints.fixed(handlePx, cross) else Constraints.fixed(cross, handlePx))
    }
    val firstExtent = first.extent(horizontal)
    val secondExtent = second.extent(horizontal)
    val length = firstExtent + handlePx + secondExtent
    return layout(if (horizontal) length else cross, if (horizontal) cross else length) {
        fun Placeable.at(offset: Int) = if (horizontal) placeRelative(offset, 0) else placeRelative(0, offset)
        first.forEach { it.at(0) }
        handle.forEach { it.at(firstExtent) }
        second.forEach { it.at(firstExtent + handlePx) }
    }
}

/** The largest size of these placeables along the axis. */
private fun List<Placeable>.extent(horizontal: Boolean): Int =
    maxOfOrNull { if (horizontal) it.width else it.height } ?: 0

/** The constraints of a panel of [size] pixels along the axis, or of its natural size when unbounded. */
private fun Constraints.along(horizontal: Boolean, size: Int): Constraints = when {
    horizontal && size == Constraints.Infinity -> copy(minWidth = 0)
    horizontal -> copy(minWidth = size, maxWidth = size)
    size == Constraints.Infinity -> copy(minHeight = 0)
    else -> copy(minHeight = size, maxHeight = size)
}

/** The size across the axis: the fixed size when there is one, otherwise the largest panel. */
private fun crossSize(
    constraints: Constraints,
    horizontal: Boolean,
    placeables: List<Placeable>,
): Int = if (horizontal) {
    if (constraints.hasBoundedHeight && constraints.minHeight == constraints.maxHeight) {
        constraints.maxHeight
    } else {
        (placeables.maxOfOrNull { it.height } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
    }
} else {
    if (constraints.hasBoundedWidth) constraints.maxWidth else placeables.maxOfOrNull { it.width } ?: 0
}

/** The separator: a 48dp target with a line in its middle, dragged, focused and adjusted. */
@Composable
private fun ResizeHandle(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    availablePx: Int,
    axis: MonarchResizablePanelsAxis,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    enabled: Boolean,
    showGrip: Boolean,
    label: String,
    onResizeFinished: ((Float) -> Unit)?,
    interactionSource: MutableInteractionSource?,
) {
    val tokens = MonarchResizablePanelsTokens
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val dragged by interactions.collectIsDraggedAsState()
    val focused by interactions.collectIsFocusedAsState()
    val active = enabled && (dragged || focused)
    val layoutDirection = LocalLayoutDirection.current
    val current by rememberUpdatedState(fraction)
    val change by rememberUpdatedState(onFractionChange)
    val finished by rememberUpdatedState(onResizeFinished)
    var dragFraction by remember { mutableFloatStateOf(fraction) }
    val dragState = rememberDraggableState { delta ->
        dragFraction = tokens.clamp(dragFraction + tokens.dragChange(delta, availablePx, axis, layoutDirection), range)
        change(dragFraction)
    }
    fun commit(value: Float) {
        change(value)
        finished?.invoke(value)
    }
    val colors = MonarchTheme.colors
    val handleColors = tokens.colors(active, enabled, colors)
    val lineColor = handleColors.line
    val gripColor = handleColors.grip
    val lineWidth = tokens.lineWidth(active)
    val horizontal = axis == MonarchResizablePanelsAxis.Horizontal
    val state = remember(fraction) { NumberFormat.getPercentInstance().format(fraction.toDouble()) }
    val bounds = tokens.sanitize(range)
    Box(
        modifier = Modifier
            .semantics {
                contentDescription = label
                stateDescription = state
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, bounds, tokens.accessibilitySteps(bounds, step))
                if (enabled) {
                    setProgress { target ->
                        commit(tokens.clamp(target, bounds))
                        true
                    }
                } else {
                    disabled()
                }
            }
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .onKeyEvent { event ->
                if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val direction = tokens.keyDirection(event.key, axis, layoutDirection)
                val target = tokens.keyTarget(event.key, current, step, bounds, direction) ?: return@onKeyEvent false
                commit(target)
                true
            }
            .focusable(enabled = enabled, interactionSource = interactions)
            .draggable(
                state = dragState,
                orientation = if (horizontal) Orientation.Horizontal else Orientation.Vertical,
                enabled = enabled,
                interactionSource = interactions,
                onDragStarted = { dragFraction = current },
                onDragStopped = { finished?.invoke(dragFraction) },
            )
            .drawBehind {
                val line = lineWidth.toPx()
                if (horizontal) {
                    drawRect(lineColor, Offset((size.width - line) / 2, 0f), Size(line, size.height))
                } else {
                    drawRect(lineColor, Offset(0f, (size.height - line) / 2), Size(size.width, line))
                }
                if (showGrip) drawGrip(gripColor, horizontal, tokens.GripThickness.toPx())
            },
    )
}


/** Two columns of three dots in the middle of the separator, as the web grip, across the line whatever the axis. */
private fun DrawScope.drawGrip(color: Color, horizontal: Boolean, dot: Float) {
    val pitch = dot * 2
    val across = pitch * (MonarchResizablePanelsTokens.GRIP_COLUMNS - 1)
    val along = pitch * (MonarchResizablePanelsTokens.GRIP_ROWS - 1)
    val origin = Offset(
        (size.width - (if (horizontal) across else along)) / 2,
        (size.height - (if (horizontal) along else across)) / 2,
    )
    repeat(MonarchResizablePanelsTokens.GRIP_COLUMNS) { column ->
        repeat(MonarchResizablePanelsTokens.GRIP_ROWS) { row ->
            val shift = if (horizontal) Offset(column * pitch, row * pitch) else Offset(row * pitch, column * pitch)
            drawCircle(color, radius = dot / 2, center = origin + shift)
        }
    }
}
