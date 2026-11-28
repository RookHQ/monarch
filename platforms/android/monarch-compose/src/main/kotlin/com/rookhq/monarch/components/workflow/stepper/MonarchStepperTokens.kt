package com.rookhq.monarch.components.workflow.stepper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The state a step is drawn in, derived from its position and its own flags. */
internal enum class MonarchStepperStatus { Complete, Current, Upcoming, Error }

/** The fill, content, border and outer ring colours of a step marker. */
internal data class MonarchStepperMarkerColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val ring: Color,
)

/** Component tokens and pure logic of MonarchStepper, built from primitives and roles only. */
internal object MonarchStepperTokens {
    /** The side of a step marker at the default text size; it grows with the font scale. */
    val Marker: Dp = MonarchTokens.Control.xs.dp

    /** The width of the marker's border. */
    val MarkerBorder: Dp = MonarchTokens.Space.space0_5.dp

    /** The width of the ring around the current step's marker. */
    val Ring: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp

    /** The padding inside a step item. */
    val ItemPadding: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the marker and the text, between a step and its line, and between steps. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the label, the description and the optional note. */
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The thickness of a line between steps. */
    val Line: Dp = MonarchTokens.Space.space0_5.dp

    /** The shortest line between two steps. */
    val LineMinimum: Dp = MonarchTokens.Space.space4.dp

    /** The space above and below a vertical line. */
    val LineMargin: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between the summary and the bar of the compact form. */
    val CompactGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the count and the name in the compact summary when they wrap. */
    val SummaryRowGap: Dp = MonarchTokens.Space.space1.dp

    /** The glyph of a finished or failed step fills half of the marker, as the web's 14px in 28px. */
    const val GLYPH_RATIO: Float = 0.5f

    /** The glyphs are drawn on a 16-unit grid, as the web's SVG. */
    const val GRID: Float = 16f

    /** The stroke of a glyph on that grid. */
    const val GLYPH_STROKE: Float = 2f

    /** The check mark of a finished step on the 16-unit grid: the web's `M3.5 8.5l3 3 6-7`. */
    val CheckPoints: List<Offset> = listOf(Offset(3.5f, 8.5f), Offset(6.5f, 11.5f), Offset(12.5f, 4.5f))

    /** The bar of the exclamation mark of a failed step on the 16-unit grid. */
    val ErrorBar: Pair<Offset, Offset> = Offset(8f, 4f) to Offset(8f, 9f)

    /** The dot of the exclamation mark of a failed step on the 16-unit grid. */
    val ErrorDot: Offset = Offset(8f, 11.75f)

    /** The marker side for a font scale: the base size, larger when the user enlarges text, never smaller. */
    fun markerSize(fontScale: Float): Dp = Marker * fontScale.coerceAtLeast(1f)

    /** A position moved inside the steps. */
    fun clamp(index: Int, count: Int): Int = index.coerceIn(0, (count - 1).coerceAtLeast(0))

    /** The position of the step with [id], or [fallback] when there is none. */
    fun indexOf(steps: List<MonarchStepperStep>, id: String?, fallback: Int): Int {
        if (id == null) return fallback
        val index = steps.indexOfFirst { it.id == id }
        return if (index == -1) fallback else index
    }

    /** The state of the step at [index]: an error wins, then the current step, then finished steps. */
    fun status(index: Int, current: Int, step: MonarchStepperStep): MonarchStepperStatus = when {
        step.error -> MonarchStepperStatus.Error
        index == current -> MonarchStepperStatus.Current
        step.complete || index < current -> MonarchStepperStatus.Complete
        else -> MonarchStepperStatus.Upcoming
    }

    /**
     * Whether the step at [index] is a button: only with a selection callback, never the current or a disabled
     * step, and only a step that was reached (up to [furthest]) or is finished.
     */
    fun selectable(index: Int, current: Int, furthest: Int, step: MonarchStepperStep, hasOnSelect: Boolean): Boolean {
        val reached = index <= furthest || status(index, current, step) == MonarchStepperStatus.Complete
        return hasOnSelect && step.enabled && index != current && reached
    }

    /** Whether the compact summary is shown. [fits] says whether the steps fit the width on one line each. */
    fun showsCompact(mode: MonarchStepperCompactMode, orientation: MonarchStepperOrientation, fits: Boolean): Boolean =
        when (mode) {
            MonarchStepperCompactMode.Always -> true
            MonarchStepperCompactMode.Never -> false
            MonarchStepperCompactMode.Automatic -> orientation == MonarchStepperOrientation.Horizontal && !fits
        }

    /** The colours of a marker in each state. */
    fun markerColors(status: MonarchStepperStatus, colors: MonarchColors): MonarchStepperMarkerColors = when (status) {
        MonarchStepperStatus.Complete ->
            MonarchStepperMarkerColors(colors.accentSolid, colors.accentOnSolid, colors.accentSolid, Color.Transparent)
        MonarchStepperStatus.Current ->
            MonarchStepperMarkerColors(colors.bgSurface, colors.accentStrong, colors.accentSolid, colors.accentSubtle)
        MonarchStepperStatus.Error ->
            MonarchStepperMarkerColors(colors.dangerSubtle, colors.dangerStrong, colors.dangerBorder, Color.Transparent)
        MonarchStepperStatus.Upcoming ->
            MonarchStepperMarkerColors(colors.bgSurface, colors.textMuted, colors.borderStrong, Color.Transparent)
    }

    /** The colour of a step's name. A disabled step in a selectable stepper uses the disabled text colour. */
    fun labelColor(status: MonarchStepperStatus, disabled: Boolean, colors: MonarchColors): Color = when {
        disabled -> colors.textDisabled
        status == MonarchStepperStatus.Error -> colors.dangerStrong
        status == MonarchStepperStatus.Upcoming -> colors.textMuted
        else -> colors.textDefault
    }

    /** The colour of the line after a step: the accent after a finished step, the default border otherwise. */
    fun lineColor(status: MonarchStepperStatus, colors: MonarchColors): Color =
        if (status == MonarchStepperStatus.Complete) colors.accentSolid else colors.borderDefault

    /** The colour of the description, the optional note and the compact count. */
    fun secondaryColor(colors: MonarchColors): Color = colors.textMuted
}
