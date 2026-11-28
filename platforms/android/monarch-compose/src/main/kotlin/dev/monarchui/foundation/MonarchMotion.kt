package dev.monarchui.foundation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import dev.monarchui.tokens.MonarchCubicBezier
import dev.monarchui.tokens.MonarchTokens

/**
 * Monarch durations and easing curves, and whether the user has turned
 * animations off.
 *
 * Compose already applies the system animator duration scale to every
 * animation. [reducedMotion] is for the cases that scaling does not cover: a
 * component can pick a non-animated alternative, and [spec] returns an
 * immediate change.
 *
 * Read it through `MonarchTheme.motion`.
 *
 * @property reducedMotion `true` when the system animator duration scale is 0 ("Remove animations").
 */
@Immutable
public class MonarchMotion(public val reducedMotion: Boolean) {
    /** 120 ms: hover, press and other small state changes. */
    public val fast: Int get() = MonarchTokens.Duration.fast

    /** 180 ms: the default. */
    public val base: Int get() = MonarchTokens.Duration.base

    /** 260 ms: elements that enter or leave. */
    public val slow: Int get() = MonarchTokens.Duration.slow

    /** 400 ms: large surfaces. */
    public val slower: Int get() = MonarchTokens.Duration.slower

    /** The default curve. */
    public val standard: Easing = MonarchTokens.Easing.standard.toEasing()

    /** For elements that enter. */
    public val enter: Easing = MonarchTokens.Easing.enter.toEasing()

    /** For elements that leave. */
    public val exit: Easing = MonarchTokens.Easing.exit.toEasing()

    /** A curve with a slight overshoot. */
    public val emphasized: Easing = MonarchTokens.Easing.emphasized.toEasing()

    /**
     * An animation spec with a Monarch duration and curve, or an immediate
     * change when [reducedMotion] is `true`.
     *
     * @param durationMillis one of [fast], [base], [slow] or [slower].
     * @param easing one of [standard], [enter], [exit] or [emphasized].
     */
    public fun <T> spec(durationMillis: Int = base, easing: Easing = standard): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis = durationMillis, easing = easing)

    override fun equals(other: Any?): Boolean = other is MonarchMotion && other.reducedMotion == reducedMotion

    override fun hashCode(): Int = reducedMotion.hashCode()
}

private fun MonarchCubicBezier.toEasing(): Easing = CubicBezierEasing(x1, y1, x2, y2)
