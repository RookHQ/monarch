package com.rookhq.monarch.components.technical.domainprogress

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** One step with its resolved state and text. */
internal data class MonarchDomainProgressItem(
    val step: MonarchDomainStep,
    val state: MonarchDomainStepState,
    val title: String,
    val detail: String,
)

/** The colours of a step marker: the fill, the border and the glyph or spinner. */
internal data class MonarchDomainProgressMarkerColors(val fill: Color, val border: Color, val content: Color)

/** Component tokens and the pure rules of MonarchDomainProgress. */
internal object MonarchDomainProgressTokens {
    /** The gap between the steps, the problems and the status lines. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The space after each step. */
    val StepGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the marker and the text of a step. */
    val MarkerGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the title and the detail, and between problems. */
    val TextGap: Dp = MonarchTokens.Space.space1.dp

    /** The side of the marker at font scale 1. */
    val Marker: Dp = MonarchTokens.Space.space6.dp

    /** The width of the marker border and of the line that joins the markers. */
    val Line: Dp = MonarchTokens.Space.space0_5.dp

    /** The share of the ring drawn when animations are off: the open ring the web spinner shows. */
    const val StillArc: Float = 0.75f

    /** The glyph fills half of the marker. */
    const val GlyphRatio: Float = 0.5f

    /** The spinner fills two thirds of the marker. */
    const val SpinnerRatio: Float = 0.66f

    /** The space under a step: [StepGap], none under the last. */
    fun stepGap(last: Boolean): Dp = if (last) MonarchTokens.Space.space0.dp else StepGap

    /** The marker side for a font scale: the base size, larger when the user enlarges text, never smaller. */
    fun markerSize(fontScale: Float): Dp = Marker * fontScale.coerceAtLeast(1f)

    /**
     * The colours of the marker of [state]: success fill when done, accent fill while in progress, warning fill when it
     * needs attention, a surface circle with the default border before it starts. Each state also has its own glyph,
     * so colour is not the only signal.
     */
    fun marker(state: MonarchDomainStepState, colors: MonarchColors): MonarchDomainProgressMarkerColors =
        when (state) {
            MonarchDomainStepState.Done ->
                MonarchDomainProgressMarkerColors(colors.successSolid, colors.successSolid, colors.successOnSolid)
            MonarchDomainStepState.Current ->
                MonarchDomainProgressMarkerColors(colors.accentSolid, colors.accentSolid, colors.accentOnSolid)
            MonarchDomainStepState.Error ->
                MonarchDomainProgressMarkerColors(colors.warningSolid, colors.warningSolid, colors.warningOnSolid)
            MonarchDomainStepState.Todo ->
                MonarchDomainProgressMarkerColors(colors.bgSurface, colors.borderDefault, colors.textMuted)
        }

    /** The colour of a step title: muted before it starts. */
    fun title(state: MonarchDomainStepState, colors: MonarchColors): Color =
        if (state == MonarchDomainStepState.Todo) colors.textMuted else colors.textDefault

    /** The colour of the line after a step: success after a finished step, the default border otherwise. */
    fun connector(after: MonarchDomainStepState, colors: MonarchColors): Color =
        if (after == MonarchDomainStepState.Done) colors.successSolid else colors.borderDefault

    /** The details and the status lines: muted text. */
    fun muted(colors: MonarchColors): Color = colors.textMuted
}
