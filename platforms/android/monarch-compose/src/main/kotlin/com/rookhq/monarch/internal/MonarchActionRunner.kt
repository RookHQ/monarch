package com.rookhq.monarch.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Where a suspending action started from a component is. */
internal enum class MonarchActionState {
    /** Not started, or finished. */
    Idle,

    /** Running. The control that started it shows a busy state and ignores further taps. */
    Busy,

    /** The last run threw. The component shows a message and lets the person try again. */
    Failed,
}

/**
 * Runs a suspending action (save, send, retry) once at a time and tracks
 * whether it is busy or failed, so components do not each keep their own
 * flags. Cancellation is not a failure.
 */
@Stable
internal class MonarchActionRunner(private val scope: CoroutineScope) {
    /** The state of the last run. */
    var state: MonarchActionState by mutableStateOf(MonarchActionState.Idle)
        private set

    /** Whether an action is running. */
    val busy: Boolean get() = state == MonarchActionState.Busy

    /** Starts [action] unless one is running. Returns whether it started. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun run(action: suspend () -> Unit): Boolean {
        if (busy) return false
        state = MonarchActionState.Busy
        scope.launch {
            try {
                action()
                state = MonarchActionState.Idle
            } catch (cancelled: CancellationException) {
                state = MonarchActionState.Idle
                throw cancelled
            } catch (failure: Exception) {
                state = MonarchActionState.Failed
            }
        }
        return true
    }

    /** Forgets a failure, for example when the person dismisses the message. */
    fun reset() {
        if (state == MonarchActionState.Failed) state = MonarchActionState.Idle
    }
}

/** A [MonarchActionRunner] bound to the composition: its runs stop when the component leaves. */
@Composable
internal fun rememberMonarchActionRunner(): MonarchActionRunner {
    val scope = rememberCoroutineScope()
    return remember(scope) { MonarchActionRunner(scope) }
}
