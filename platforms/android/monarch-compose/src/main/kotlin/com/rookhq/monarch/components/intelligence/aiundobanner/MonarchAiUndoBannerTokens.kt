package com.rookhq.monarch.components.intelligence.aiundobanner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Where a `MonarchAiUndoBanner` is in its life. */
internal enum class MonarchAiUndoBannerState {
    /** Undo is offered. */
    Active,

    /** Undo is running. */
    Undoing,

    /** Undo worked. */
    Undone,

    /** Undo threw; it is offered again. */
    Failed,

    /** The time to undo ran out. */
    Expired,
}

/** The colours of the banner in one state. */
internal data class MonarchAiUndoBannerColors(
    val container: Color,
    val border: Color,
    val icon: Color,
    val status: Color,
)

/** Component tokens and the pure logic of MonarchAiUndoBanner. */
internal object MonarchAiUndoBannerTokens {
    /** More changes than this are put in a disclosure. */
    const val VISIBLE_CHANGES: Int = 3

    /** The step of the countdown, so that a pause loses at most this much time. */
    const val TICK_MILLIS: Long = 250L

    /** The padding around the content. */
    val Padding: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the icon and the text, and between the blocks of the content. */
    val ContentGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between lines and between the changes. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the actions. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The colours, as the web: the subtle AI fill and AI border while Undo is offered, neutral once it is over. */
    fun colors(state: MonarchAiUndoBannerState, colors: MonarchColors): MonarchAiUndoBannerColors {
        val settled = settled(state)
        val ai = MonarchTonePalette.of(MonarchTone.Ai, colors)
        return MonarchAiUndoBannerColors(
            container = if (settled) colors.bgSubtle else ai.subtle,
            border = if (settled) colors.borderDefault else ai.border,
            icon = if (settled) colors.textMuted else ai.strong,
            status = if (state == MonarchAiUndoBannerState.Failed) colors.dangerStrong else colors.textMuted,
        )
    }

    /** Whether the banner is over: undone or expired. */
    fun settled(state: MonarchAiUndoBannerState): Boolean =
        state == MonarchAiUndoBannerState.Undone || state == MonarchAiUndoBannerState.Expired

    /** Whether Undo is shown: while it is offered, running or failed. */
    fun showsUndo(state: MonarchAiUndoBannerState): Boolean = !settled(state)

    /** Whether a tap on Undo starts it. */
    fun canUndo(state: MonarchAiUndoBannerState): Boolean =
        state == MonarchAiUndoBannerState.Active || state == MonarchAiUndoBannerState.Failed

    /**
     * Whether the time limit runs: only with a limit, while Undo is offered and not running, and not while
     * the person reads the changes.
     */
    fun counting(durationMillis: Long?, state: MonarchAiUndoBannerState, changesOpen: Boolean): Boolean =
        durationMillis != null && canUndo(state) && !changesOpen

    /** Whether the changes are put in a disclosure. */
    fun collapsesChanges(count: Int): Boolean = count > VISIBLE_CHANGES

    /** The decorative arrow between the old and the new value, pointing the reading direction. */
    fun arrow(layoutDirection: LayoutDirection): String =
        if (layoutDirection == LayoutDirection.Rtl) "←" else "→"
}
