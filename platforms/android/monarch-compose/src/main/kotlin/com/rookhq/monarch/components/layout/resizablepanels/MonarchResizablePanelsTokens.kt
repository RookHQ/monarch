package com.rookhq.monarch.components.layout.resizablepanels

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import kotlin.math.roundToInt

/** The colours of the separator of a MonarchResizablePanels. */
internal data class ResizeHandleColors(val line: Color, val grip: Color)

/** Component tokens and the pure logic of MonarchResizablePanels. */
internal object MonarchResizablePanelsTokens {
    /** The space the separator reserves between the panels: the 48dp touch target. */
    val HandleThickness: Dp = MonarchTouchTarget.Minimum

    /** The line at rest and while dragged or focused, where it is thicker as well as in the accent colour. */
    val LineWidth: Dp = MonarchShapeDefaults.BorderWidth
    val ActiveLineWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The optional grip in the middle of the line. */
    val GripThickness: Dp = MonarchTokens.Space.space1.dp

    /** The grip is a grid of dots: this many across the line and this many along it. */
    const val GRIP_COLUMNS: Int = 2
    const val GRIP_ROWS: Int = 3

    /**
     * The colours of the separator: accent while dragged or focused, the
     * default border (line) and strong border (grip) at rest, the subtle
     * border when disabled.
     */
    fun colors(active: Boolean, enabled: Boolean, colors: MonarchColors): ResizeHandleColors = when {
        !enabled -> ResizeHandleColors(line = colors.borderSubtle, grip = colors.borderSubtle)
        active -> ResizeHandleColors(line = colors.accentSolid, grip = colors.accentSolid)
        else -> ResizeHandleColors(line = colors.borderDefault, grip = colors.borderStrong)
    }

    /** The width of the line. */
    fun lineWidth(active: Boolean): Dp = if (active) ActiveLineWidth else LineWidth

    /** The range limited to 0..1, with its ends in order. */
    fun sanitize(range: ClosedFloatingPointRange<Float>): ClosedFloatingPointRange<Float> {
        val start = range.start.coerceIn(0f, 1f)
        val end = range.endInclusive.coerceIn(0f, 1f)
        return minOf(start, end)..maxOf(start, end)
    }

    /** [value] limited to [range]. */
    fun clamp(value: Float, range: ClosedFloatingPointRange<Float>): Float = value.coerceIn(sanitize(range))

    /**
     * The change of the share for a drag of [deltaPx] along the axis over
     * [availablePx]. Dragging towards the end grows the first panel; in a
     * right-to-left layout the first panel is on the right, so the sign flips.
     */
    fun dragChange(
        deltaPx: Float,
        availablePx: Int,
        axis: MonarchResizablePanelsAxis,
        layoutDirection: LayoutDirection,
    ): Float {
        if (availablePx <= 0) return 0f
        val mirrored = axis == MonarchResizablePanelsAxis.Horizontal && layoutDirection == LayoutDirection.Rtl
        val change = deltaPx / availablePx
        return if (mirrored) -change else change
    }

    /**
     * The direction a key moves the separator along the axis: -1 towards the
     * start or top, 1 towards the end or bottom, `null` for another key. The
     * arrow keys move it in the direction they point, so left and right swap
     * meaning in a right-to-left layout, where the first panel is on the right.
     */
    fun keyDirection(key: Key, axis: MonarchResizablePanelsAxis, layoutDirection: LayoutDirection): Int? {
        val rtl = layoutDirection == LayoutDirection.Rtl
        return when (axis) {
            MonarchResizablePanelsAxis.Horizontal -> when (key) {
                Key.DirectionLeft -> if (rtl) 1 else -1
                Key.DirectionRight -> if (rtl) -1 else 1
                else -> null
            }
            MonarchResizablePanelsAxis.Vertical -> when (key) {
                Key.DirectionUp -> -1
                Key.DirectionDown -> 1
                else -> null
            }
        }
    }

    /**
     * The share after a key press, or `null` when the key does nothing: the
     * arrow keys along the axis move it by [step] in [direction] (from
     * [keyDirection]), Home and End move it to the ends of [range].
     */
    fun keyTarget(
        key: Key,
        fraction: Float,
        step: Float,
        range: ClosedFloatingPointRange<Float>,
        direction: Int?,
    ): Float? {
        val bounds = sanitize(range)
        return when {
            direction != null -> clamp(fraction + direction * step, bounds)
            key == Key.MoveHome -> bounds.start
            key == Key.MoveEnd -> bounds.endInclusive
            else -> null
        }
    }

    /** The number of steps TalkBack offers between the ends of [range], as the slider semantics count them. */
    fun accessibilitySteps(range: ClosedFloatingPointRange<Float>, step: Float): Int {
        val bounds = sanitize(range)
        if (step <= 0f) return 0
        return ((bounds.endInclusive - bounds.start) / step).roundToInt().minus(1).coerceAtLeast(0)
    }

    /** The sizes of the two panels along the axis, in pixels, for [total] pixels and a [handle] between them. */
    fun panelSizes(total: Int, handle: Int, fraction: Float): Pair<Int, Int> {
        val available = (total - handle).coerceAtLeast(0)
        val first = (available * fraction.coerceIn(0f, 1f)).roundToInt()
        return first to available - first
    }

    /** Whether side-by-side panels are stacked: when asked to and the window is compact. */
    fun stacks(axis: MonarchResizablePanelsAxis, stacksWhenCompact: Boolean, compactWidth: Boolean): Boolean =
        axis == MonarchResizablePanelsAxis.Horizontal && stacksWhenCompact && compactWidth
}
