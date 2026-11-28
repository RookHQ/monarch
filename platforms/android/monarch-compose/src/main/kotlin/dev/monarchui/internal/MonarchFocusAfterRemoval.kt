package dev.monarchui.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Where focus goes after the person removes an item from a list (a chip, a
 * row, an attachment): to the item that takes its place, else the one before,
 * else to the list's own fallback (an add button, the field). Android does not
 * move focus by itself when the focused node leaves, so TalkBack would start
 * again at the top of the screen.
 */
internal object MonarchFocusAfterRemoval {
    /**
     * The id to focus after [removed] leaves [ids] (the ids before the
     * removal, in order), or `null` for the fallback.
     */
    fun <T> target(ids: List<T>, removed: T): T? {
        val index = ids.indexOf(removed)
        if (index < 0) return null
        return ids.getOrNull(index + 1) ?: ids.getOrNull(index - 1)
    }
}

/**
 * The Compose side of [MonarchFocusAfterRemoval]: one [FocusRequester] per
 * item, and the move itself once the removed item has left the list.
 *
 * Put [requester] on each item's remove button (`Modifier.focusRequester`),
 * call [removing] when the person asks to remove one, and give the list's
 * fallback (an add button, a field, the heading) [fallback]. The removal may
 * take a while (a confirmation, a suspending call); focus moves only once the
 * app has taken the item out of the ids passed to
 * [rememberMonarchRemovalFocus]. A failed removal leaves the item in the list,
 * and focus where it is.
 */
@Stable
internal class MonarchRemovalFocus<K> internal constructor() {
    private val requesters = mutableMapOf<K, FocusRequester>()
    private var pending by mutableStateOf<Pending<K>?>(null)

    /** Where focus goes when there is no item left to take it. */
    val fallback: FocusRequester = FocusRequester()

    /** The app's own fallback (the web `fallbackFocusRef`), which wins over [fallback]. */
    internal var external: FocusRequester? = null

    /** The requester of the item [key]: attach it to the item's remove button. */
    fun requester(key: K): FocusRequester = requesters.getOrPut(key) { FocusRequester() }

    /** Call when the person removes [removed] from [ids] (the ids before the removal, in order). */
    fun removing(ids: List<K>, removed: K) {
        pending = Pending(removed, MonarchFocusAfterRemoval.target(ids, removed))
    }

    /**
     * Runs a suspending removal of [removed] from [ids]: focus moves once the item leaves the list, and stays
     * where it is when [block] throws (the error is passed on).
     */
    suspend fun <R> remove(ids: List<K>, removed: K, block: suspend () -> R): R {
        removing(ids, removed)
        return try {
            block()
        } catch (@Suppress("TooGenericExceptionCaught") failure: Throwable) { // Passed on unchanged.
            cancel()
            throw failure
        }
    }

    /** Forgets a pending move, when the removal failed and the item stays. */
    fun cancel() {
        pending = null
    }

    /** Moves focus once [removed][Pending.removed] is gone from [ids]. */
    internal fun settle(ids: List<K>) {
        val move = pending ?: return
        if (move.removed in ids) return
        pending = null
        requesters.keys.retainAll(ids.toSet())
        val target = move.target?.takeIf { it in ids }?.let { requesters[it] }
        (target ?: external ?: fallback).tryFocus()
    }

    private fun FocusRequester.tryFocus() {
        // A requester with no node (no fallback was given, or the node just left) throws; focus then stays.
        runCatching { requestFocus() }
    }

    private class Pending<K>(val removed: K, val target: K?)
}

/**
 * A [MonarchRemovalFocus] for a list whose items are [ids], in order. Focus
 * moves after a removal to the next item, else the previous, else the
 * [fallback] the app gave, else the list's own
 * [fallback][MonarchRemovalFocus.fallback] (see [monarchRemovalFallback]).
 */
@Composable
internal fun <K> rememberMonarchRemovalFocus(ids: List<K>, fallback: FocusRequester? = null): MonarchRemovalFocus<K> {
    val focus = remember { MonarchRemovalFocus<K>() }
    focus.external = fallback
    LaunchedEffect(ids) {
        // Waits one frame, so the next item's button is attached before it is focused.
        withFrameNanos { }
        focus.settle(ids)
    }
    return focus
}

/** Makes this node the fallback of [focus]: it takes focus when the last item is removed. */
internal fun Modifier.monarchRemovalFallback(focus: MonarchRemovalFocus<*>): Modifier = focusRequester(focus.fallback)
