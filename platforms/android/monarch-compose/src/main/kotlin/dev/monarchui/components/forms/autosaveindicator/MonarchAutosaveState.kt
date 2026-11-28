package dev.monarchui.components.forms.autosaveindicator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Saves a value automatically after the person stops changing it: debounced,
 * one save at a time, retried with a doubling wait after a failure, and
 * flushable on demand. Show it with `MonarchAutosaveIndicator`.
 *
 * Built by Monarch on coroutines; Compose has no equivalent. Pass every new
 * value to [update] (`rememberMonarchAutosaveState` does it for you). The
 * value given at creation counts as saved. A change starts a timer of
 * [delayMillis]; another change restarts it, and a change back to the saved
 * value cancels it. When the timer ends, [onSave] runs with the latest value.
 * Only one save runs at a time: a change made during a save is saved after
 * it, once the delay has passed again. A failed save is retried [retries]
 * times, after [retryDelayMillis] and then twice as long each time, before
 * [status] becomes `Error`.
 *
 * Create it in a `ViewModel` with `viewModelScope` to keep saving across
 * configuration changes, or with `rememberMonarchAutosaveState` in a screen.
 *
 * @param T the type of the value. Values are compared with [isEqual].
 * @param initialValue the value as it is stored now.
 * @param scope where the timers and the saves run. Cancelling it stops autosaving.
 * @param onSave stores a value. Suspend until it is stored; throw to report a failure.
 * @param delayMillis milliseconds to wait after the last change before saving.
 * @param retries automatic retries after a failed save before the status becomes an error.
 * @param retryDelayMillis milliseconds before the first automatic retry; doubled for each further one.
 * @param initialSavedAt when [initialValue] was last saved, in epoch milliseconds (UTC), if known.
 * @param isEqual decides whether two values are the same. By default `==`.
 * @param clock the current time in epoch milliseconds (UTC), for [savedAt]. Replace it in tests.
 */
@Stable
public class MonarchAutosaveState<T>(
    initialValue: T,
    private val scope: CoroutineScope,
    onSave: suspend (T) -> Unit,
    private val delayMillis: Long = MonarchAutosaveIndicatorTokens.DELAY_MILLIS,
    private val retries: Int = MonarchAutosaveIndicatorTokens.RETRIES,
    private val retryDelayMillis: Long = MonarchAutosaveIndicatorTokens.RETRY_DELAY_MILLIS,
    initialSavedAt: Long? = null,
    private val isEqual: (T, T) -> Boolean = { a, b -> a == b },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Stores a value. Replace it when the function changes, for example after a recomposition. */
    public var onSave: suspend (T) -> Unit = onSave

    /** Where saving stands. `Saved` at first when `initialSavedAt` is given, else `Idle`. */
    public var status: MonarchSaveStatus by mutableStateOf(MonarchAutosaveIndicatorTokens.settled(initialSavedAt))
        private set

    /** When the last save finished, in epoch milliseconds (UTC), or `initialSavedAt`, or `null`. */
    public var savedAt: Long? by mutableStateOf(initialSavedAt)
        private set

    private var lastError: Throwable? by mutableStateOf(null)

    /** The failure of the last save while [status] is `Error`, else `null`. */
    public val error: Throwable? get() = if (status == MonarchSaveStatus.Error) lastError else null

    /** Whether there are changes that are not saved yet: unsaved, saving or failed. Use it to guard leaving. */
    public val dirty: Boolean get() = MonarchAutosaveIndicatorTokens.dirty(status)

    /** The latest value passed to [update]. */
    public var value: T = initialValue
        private set

    /**
     * Whether saving runs on its own. While `false`, changes only mark the
     * value unsaved (for example while the form is invalid); [flush] and
     * [retry] still save. Turning it back on schedules a pending change.
     */
    public var enabled: Boolean = true
        set(on) {
            if (field == on) return
            field = on
            armed = null
            update(value)
        }

    private var lastSaved: T = initialValue
    private var armed: Pending<T>? = null
    private var timer: Job? = null
    private var inFlight = false
    private var attempt = 0
    private val mutex = Mutex()

    /**
     * Takes a new value: marks it unsaved and starts the timer, or cancels
     * the timer when the value is back to the saved one.
     */
    public fun update(value: T) {
        this.value = value
        if (isEqual(value, lastSaved)) {
            if (armed != null) {
                cancelTimer()
                armed = null
            }
            if (!inFlight && status == MonarchSaveStatus.Unsaved) {
                status = MonarchAutosaveIndicatorTokens.settled(savedAt)
            }
            return
        }
        if (!enabled) {
            cancelTimer()
            armed = null
            markUnsaved()
            return
        }
        val pending = armed
        if (pending != null && isEqual(value, pending.value)) return
        armed = Pending(value)
        attempt = 0
        markUnsaved()
        schedule(delayMillis)
    }

    /** Saves again now after a failure, with the automatic retries reset. */
    public fun retry() {
        attempt = 0
        lastError = null
        cancelTimer()
        scope.launch { save() }
    }

    /**
     * Saves a pending change now, without waiting for the delay: on leaving a
     * screen or a field. Returns `true` when everything is saved and `false`
     * when the save failed.
     */
    public suspend fun flush(): Boolean {
        attempt = 0
        cancelTimer()
        return save()
    }

    private fun markUnsaved() {
        if (!inFlight && status != MonarchSaveStatus.Error) status = MonarchSaveStatus.Unsaved
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    private fun schedule(wait: Long) {
        cancelTimer()
        timer = scope.launch {
            delay(wait)
            timer = null
            save()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun save(): Boolean = mutex.withLock {
        val snapshot = value
        armed = Pending(snapshot)
        if (isEqual(snapshot, lastSaved)) {
            if (status != MonarchSaveStatus.Saving) status = MonarchAutosaveIndicatorTokens.settled(savedAt)
            return@withLock true
        }
        status = MonarchSaveStatus.Saving
        inFlight = true
        // The app's save may fail with any exception; each is a failed save. Cancellation is passed on.
        val failure = try {
            onSave(snapshot)
            null
        } catch (cancelled: CancellationException) {
            inFlight = false
            throw cancelled
        } catch (failed: Exception) {
            failed
        }
        inFlight = false
        if (failure == null) saved(snapshot) else failed(failure)
    }

    private fun saved(snapshot: T): Boolean {
        lastSaved = snapshot
        attempt = 0
        savedAt = clock()
        lastError = null
        if (isEqual(value, snapshot)) {
            status = MonarchSaveStatus.Saved
        } else {
            // Edited during the save: save the newer value after the usual delay.
            status = MonarchSaveStatus.Unsaved
            armed = Pending(value)
            schedule(delayMillis)
        }
        return true
    }

    private fun failed(failure: Throwable): Boolean {
        attempt += 1
        if (attempt <= retries) {
            schedule(MonarchAutosaveIndicatorTokens.retryWait(retryDelayMillis, attempt))
        } else {
            lastError = failure
            status = MonarchSaveStatus.Error
        }
        return false
    }

    /** A value the timer was started for, so the same value does not restart it. Boxed, as `T` may be nullable. */
    private class Pending<T>(val value: T)
}

/**
 * Remembers a [MonarchAutosaveState] for [value] and passes it every new
 * value and [enabled] after each composition. The first [value] counts as
 * saved. [delayMillis], [retries], [retryDelayMillis] and [isEqual] are read
 * once; [onSave] and [enabled] are kept current. Saving stops when the
 * composable leaves the composition: call `flush` before leaving to keep
 * the last change.
 *
 * @param T the type of the value.
 * @param value the current value, for example the text of a draft.
 * @param onSave stores a value. Suspend until it is stored; throw to report a failure.
 * @param enabled whether saving runs on its own. While `false`, changes only mark the value unsaved.
 * @param delayMillis milliseconds to wait after the last change before saving.
 * @param retries automatic retries after a failed save before the status becomes an error.
 * @param retryDelayMillis milliseconds before the first automatic retry; doubled for each further one.
 * @param initialSavedAt when the first value was last saved, in epoch milliseconds (UTC), if known.
 * @param isEqual decides whether two values are the same. By default `==`.
 */
@Composable
public fun <T> rememberMonarchAutosaveState(
    value: T,
    onSave: suspend (T) -> Unit,
    enabled: Boolean = true,
    delayMillis: Long = MonarchAutosaveIndicatorTokens.DELAY_MILLIS,
    retries: Int = MonarchAutosaveIndicatorTokens.RETRIES,
    retryDelayMillis: Long = MonarchAutosaveIndicatorTokens.RETRY_DELAY_MILLIS,
    initialSavedAt: Long? = null,
    isEqual: (T, T) -> Boolean = { a, b -> a == b },
): MonarchAutosaveState<T> {
    val scope = rememberCoroutineScope()
    val state = remember(scope) {
        MonarchAutosaveState(
            initialValue = value,
            scope = scope,
            onSave = onSave,
            delayMillis = delayMillis,
            retries = retries,
            retryDelayMillis = retryDelayMillis,
            initialSavedAt = initialSavedAt,
            isEqual = isEqual,
        )
    }
    SideEffect {
        state.onSave = onSave
        state.enabled = enabled
        state.update(value)
    }
    return state
}
