package com.rookhq.monarch.internal

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlin.math.sign

/**
 * The automatic scrolling of a long-press drag that crosses sections or
 * columns (backlog planner, Kanban board, reorderable list): while the held
 * row is past the edge of the list, the list scrolls a step each frame, at
 * most a fixed number of pixels, and the drop target is worked out again.
 */
internal object MonarchDragAutoScroll {
    /**
     * How far the held row, spanning [top] to [bottom], is past the viewport
     * ([viewportStart] to [viewportEnd]) in the direction it moves; zero when
     * it is inside or does not move.
     */
    fun overscroll(top: Float, bottom: Float, viewportStart: Int, viewportEnd: Int, direction: Float): Float = when {
        direction > 0f -> (bottom - viewportEnd).coerceAtLeast(0f)
        direction < 0f -> (top - viewportStart).coerceAtMost(0f)
        else -> 0f
    }

    /** One frame of automatic scrolling for [overscroll] pixels past the edge, at most [maxStep] pixels. */
    fun step(overscroll: Float, maxStep: Float): Float = sign(overscroll) * minOf(abs(overscroll), maxStep)
}

/**
 * Scrolls this list one [MonarchDragAutoScroll.step] a frame while [active]
 * and the held row is past an edge ([overscroll] is not zero), until the list
 * cannot scroll further. [retarget] runs after each step, so the drop target
 * follows the content that moved under the finger.
 */
internal suspend fun LazyListState.monarchAutoScrollWhileHeld(
    maxStep: Float,
    active: () -> Boolean,
    overscroll: () -> Float,
    retarget: () -> Unit,
) {
    var scrolled = true
    while (active() && overscroll() != 0f && scrolled) {
        scrolled = scrollBy(MonarchDragAutoScroll.step(overscroll(), maxStep)) != 0f
        retarget()
        withFrameNanos { }
    }
}
